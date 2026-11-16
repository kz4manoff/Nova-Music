package com.novamusic.player.audio

import android.annotation.SuppressLint
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaRoute2Info
import android.media.MediaRouter2
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.novamusic.player.data.model.AudioDeviceEventBanner
import com.novamusic.player.data.model.AudioOutputDevice
import com.novamusic.player.data.model.AudioOutputType
import com.novamusic.player.data.model.AudioRoutingUiState
import com.novamusic.player.data.model.OutputSwitchOutcome
import com.novamusic.player.permissions.PermissionHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Pure classification and state helpers shared between runtime detection and unit tests.
 */
internal fun classifyBluetoothDeviceType(
    deviceClassInt: Int?,
    deviceName: String?
): AudioOutputType {
    if (deviceClassInt != null) {
        when (deviceClassInt) {
            BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER,
            BluetoothClass.Device.AUDIO_VIDEO_PORTABLE_AUDIO,
            BluetoothClass.Device.AUDIO_VIDEO_HIFI_AUDIO,
            BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO,
            BluetoothClass.Device.AUDIO_VIDEO_VIDEO_DISPLAY_AND_LOUDSPEAKER -> {
                return AudioOutputType.BLUETOOTH_SPEAKER
            }
            BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET,
            BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES,
            BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE -> {
                return AudioOutputType.BLUETOOTH_HEADPHONES
            }
        }
    }
    val lower = deviceName.orEmpty().lowercase()
    val speakerHints = listOf(
        "speaker", "soundbar", "boombox", "jbl flip", "jbl charge", "jbl pulse",
        "jbl go", "jbl xtreme", "marshall", "bose soundlink", "homepod", "echo",
        "nest audio", "sonos", "car", "uconnect", "sync", "b&o beosound"
    )
    if (speakerHints.any { lower.contains(it) }) {
        return AudioOutputType.BLUETOOTH_SPEAKER
    }
    return AudioOutputType.BLUETOOTH_HEADPHONES
}

internal fun mapAndroidDeviceType(
    audioDeviceInfoType: Int,
    productName: String?,
    btClassInt: Int? = null
): AudioOutputType? {
    return when (audioDeviceInfoType) {
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> AudioOutputType.PHONE_SPEAKER
        AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> AudioOutputType.EARPIECE
        AudioDeviceInfo.TYPE_WIRED_HEADSET,
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> AudioOutputType.WIRED_HEADPHONES
        AudioDeviceInfo.TYPE_USB_HEADSET,
        AudioDeviceInfo.TYPE_USB_DEVICE,
        AudioDeviceInfo.TYPE_USB_ACCESSORY -> AudioOutputType.USB_AUDIO
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
        AudioDeviceInfo.TYPE_HEARING_AID -> classifyBluetoothDeviceType(btClassInt, productName)
        else -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                when (audioDeviceInfoType) {
                    AudioDeviceInfo.TYPE_BLE_HEADSET -> AudioOutputType.BLUETOOTH_HEADPHONES
                    AudioDeviceInfo.TYPE_BLE_SPEAKER -> AudioOutputType.BLUETOOTH_SPEAKER
                    else -> null
                }
            } else {
                null
            }
        }
    }
}

internal fun resolvePreferredSystemRouteActiveId(
    devices: List<AudioOutputDevice>,
    userPreferredDeviceId: String?,
    systemRouteTypeHint: AudioOutputType?
): String? {
    if (devices.isEmpty()) return null
    if (userPreferredDeviceId != null) {
        val exact = devices.firstOrNull { it.id == userPreferredDeviceId && it.isDirectSwitchSupported }
        if (exact != null) return exact.id
    }
    if (systemRouteTypeHint != null) {
        val byHint = devices.firstOrNull { it.type == systemRouteTypeHint && it.isDirectSwitchSupported }
            ?: devices.firstOrNull { it.type == systemRouteTypeHint }
        if (byHint != null) return byHint.id
    }
    // Standard Android system media route priority when auto-routing is active
    val priorityOrder = listOf(
        AudioOutputType.USB_AUDIO,
        AudioOutputType.WIRED_HEADPHONES,
        AudioOutputType.BLUETOOTH_HEADPHONES,
        AudioOutputType.BLUETOOTH_SPEAKER,
        AudioOutputType.PHONE_SPEAKER,
        AudioOutputType.EARPIECE
    )
    for (type in priorityOrder) {
        val match = devices.firstOrNull { it.type == type && it.isConnected }
        if (match != null) return match.id
    }
    return devices.firstOrNull()?.id
}

internal fun shouldEmitDeviceBanner(
    eventKey: String,
    isConnected: Boolean,
    lastKey: String?,
    lastIsConnected: Boolean?,
    lastTimestampMs: Long,
    nowMs: Long,
    cooldownMs: Long = 1_800L
): Boolean {
    if (eventKey == lastKey && isConnected == lastIsConnected && (nowMs - lastTimestampMs) < cooldownMs) {
        return false
    }
    return true
}

/**
 * Manages real-time audio device detection, system output routing via Media3 ExoPlayer.setPreferredAudioDevice,
 * Bluetooth metadata/battery observation (only when officially accessible), and connection banners.
 */
class AudioOutputManager(context: Context) {

    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val bluetoothManager = appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _routingState = MutableStateFlow(AudioRoutingUiState())
    val routingState: StateFlow<AudioRoutingUiState> = _routingState.asStateFlow()

    // Observed directly by MusicService to call exoPlayer.setPreferredAudioDevice(deviceInfo)
    private val _preferredAudioDeviceInfo = MutableStateFlow<AudioDeviceInfo?>(null)
    val preferredAudioDeviceInfo: StateFlow<AudioDeviceInfo?> = _preferredAudioDeviceInfo.asStateFlow()

    // Debounced external audio sink connection events for optional playback auto-resume
    private val _externalDeviceConnectedEvents = MutableSharedFlow<AudioOutputDevice>(extraBufferCapacity = 4)
    val externalDeviceConnectedEvents: SharedFlow<AudioOutputDevice> = _externalDeviceConnectedEvents.asSharedFlow()

    private var userPreferredDeviceId: String? = null
    private var a2dpProxy: BluetoothA2dp? = null
    private val batteryByAddress = mutableMapOf<String, Int>()
    private var knownExternalDevices = mapOf<String, AudioOutputDevice>()
    private var hasCompletedInitialSnapshot = false

    private var bannerDismissJob: Job? = null
    private var bannerSequence = 0L
    private var lastBannerKey: String? = null
    private var lastBannerConnected: Boolean? = null
    private var lastBannerTimeMs = 0L

    private var mediaRouter2ControllerCallback: Any? = null
    private var isStarted = false

    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
            refreshDevices(emitEvents = hasCompletedInitialSnapshot)
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
            refreshDevices(emitEvents = hasCompletedInitialSnapshot)
        }
    }

    private val systemReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            when (action) {
                ACTION_BATTERY_LEVEL_CHANGED -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    val level = intent.getIntExtra(EXTRA_BATTERY_LEVEL, -1)
                    if (device != null && level in 0..100) {
                        batteryByAddress[device.address] = level
                        refreshDevices(emitEvents = false)
                    }
                }
                BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED,
                AudioManager.ACTION_HEADSET_PLUG,
                AudioManager.ACTION_AUDIO_BECOMING_NOISY -> {
                    refreshDevices(emitEvents = hasCompletedInitialSnapshot)
                }
            }
        }
    }

    private val a2dpServiceListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
            if (profile == BluetoothProfile.A2DP && proxy is BluetoothA2dp) {
                a2dpProxy = proxy
                refreshDevices(emitEvents = false)
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.A2DP) {
                a2dpProxy = null
                refreshDevices(emitEvents = false)
            }
        }
    }

    fun start() {
        if (isStarted) return
        isStarted = true

        audioManager.registerAudioDeviceCallback(audioDeviceCallback, mainHandler)

        val filter = IntentFilter().apply {
            addAction(ACTION_BATTERY_LEVEL_CHANGED)
            addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
            addAction(AudioManager.ACTION_HEADSET_PLUG)
            addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        }
        ContextCompat.registerReceiver(
            appContext,
            systemReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )

        attachBluetoothProfileProxyIfPermitted()
        registerMediaRouter2CallbackIfSupported()

        refreshDevices(emitEvents = false)
        hasCompletedInitialSnapshot = true
    }

    fun stop() {
        if (!isStarted) return
        isStarted = false
        runCatching { audioManager.unregisterAudioDeviceCallback(audioDeviceCallback) }
        runCatching { appContext.unregisterReceiver(systemReceiver) }
        unregisterMediaRouter2CallbackIfSupported()
        val adapter = bluetoothManager?.adapter
        val proxy = a2dpProxy
        if (adapter != null && proxy != null) {
            runCatching { adapter.closeProfileProxy(BluetoothProfile.A2DP, proxy) }
        }
        a2dpProxy = null
    }

    fun refreshPermissionsAndDevices() {
        attachBluetoothProfileProxyIfPermitted()
        refreshDevices(emitEvents = false)
    }

    fun dismissBanner() {
        bannerDismissJob?.cancel()
        _routingState.value = _routingState.value.copy(activeBanner = null)
    }

    fun clearStatusNotice() {
        _routingState.value = _routingState.value.copy(statusNotice = null)
    }

    /**
     * Routes audio output using legitimate Android AudioDeviceInfo / ExoPlayer.setPreferredAudioDevice.
     * Pass null to return to automatic system audio routing.
     */
    fun selectOutputDevice(deviceId: String?): OutputSwitchOutcome {
        if (deviceId == null) {
            userPreferredDeviceId = null
            _preferredAudioDeviceInfo.value = null
            refreshDevices(emitEvents = false, customNotice = "Automatic system audio routing active")
            return OutputSwitchOutcome.DIRECT_SWITCHED
        }

        val currentDevices = _routingState.value.devices
        val target = currentDevices.firstOrNull { it.id == deviceId }
            ?: return OutputSwitchOutcome.DEVICE_UNAVAILABLE

        if (target.isActive && (userPreferredDeviceId == target.id || !target.isDirectSwitchSupported)) {
            return OutputSwitchOutcome.ALREADY_ACTIVE
        }

        if (!target.isDirectSwitchSupported || target.audioDeviceId == null) {
            refreshDevices(
                emitEvents = false,
                customNotice = "Android requires changing secondary Bluetooth devices via System Audio Output"
            )
            return OutputSwitchOutcome.SYSTEM_SELECTOR_RECOMMENDED
        }

        val sink = audioManager
            .getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .firstOrNull { it.id == target.audioDeviceId }

        if (sink == null) {
            refreshDevices(
                emitEvents = false,
                customNotice = "${target.name} is no longer connected"
            )
            return OutputSwitchOutcome.DEVICE_UNAVAILABLE
        }

        if (target.type == AudioOutputType.EARPIECE) {
            userPreferredDeviceId = target.id
            _preferredAudioDeviceInfo.value = sink
            refreshDevices(
                emitEvents = false,
                customNotice = "Earpiece preference applied; some devices restrict media playback to speaker"
            )
            return OutputSwitchOutcome.DIRECT_SWITCHED
        }

        userPreferredDeviceId = target.id
        _preferredAudioDeviceInfo.value = sink
        refreshDevices(
            emitEvents = false,
            customNotice = "Switched output to ${target.name}"
        )
        return OutputSwitchOutcome.DIRECT_SWITCHED
    }

    private fun attachBluetoothProfileProxyIfPermitted() {
        if (a2dpProxy != null) return
        if (!PermissionHelper.hasBluetoothConnectPermission(appContext)) return
        val adapter: BluetoothAdapter = bluetoothManager?.adapter ?: return
        if (!adapter.isEnabled) return
        runCatching {
            adapter.getProfileProxy(appContext, a2dpServiceListener, BluetoothProfile.A2DP)
        }
    }

    private fun registerMediaRouter2CallbackIfSupported() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        if (mediaRouter2ControllerCallback != null) return
        registerMediaRouter2Api30()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun registerMediaRouter2Api30() {
        val router = MediaRouter2.getInstance(appContext)
        val callback = object : MediaRouter2.ControllerCallback() {
            override fun onControllerUpdated(controller: MediaRouter2.RoutingController) {
                refreshDevices(emitEvents = false)
            }
        }
        mediaRouter2ControllerCallback = callback
        router.registerControllerCallback(ContextCompat.getMainExecutor(appContext), callback)
    }

    private fun unregisterMediaRouter2CallbackIfSupported() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        unregisterMediaRouter2Api30()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun unregisterMediaRouter2Api30() {
        val cb = mediaRouter2ControllerCallback as? MediaRouter2.ControllerCallback ?: return
        runCatching {
            MediaRouter2.getInstance(appContext).unregisterControllerCallback(cb)
        }
        mediaRouter2ControllerCallback = null
    }

    @SuppressLint("MissingPermission")
    private fun readConnectedBluetoothDevices(): List<BluetoothDevice> {
        if (!PermissionHelper.hasBluetoothConnectPermission(appContext)) return emptyList()
        val proxy = a2dpProxy ?: return emptyList()
        return runCatching { proxy.connectedDevices }.getOrDefault(emptyList())
    }

    @SuppressLint("MissingPermission")
    private fun safeBluetoothDeviceName(device: BluetoothDevice): String {
        if (!PermissionHelper.hasBluetoothConnectPermission(appContext)) return "Bluetooth Audio"
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                device.alias?.takeIf { it.isNotBlank() } ?: device.name?.takeIf { it.isNotBlank() }
            } else {
                device.name?.takeIf { it.isNotBlank() }
            }
        }.getOrNull() ?: "Bluetooth Audio"
    }

    @SuppressLint("MissingPermission")
    private fun safeBluetoothClassInt(device: BluetoothDevice): Int? {
        if (!PermissionHelper.hasBluetoothConnectPermission(appContext)) return null
        return runCatching { device.bluetoothClass?.deviceClass }.getOrNull()
    }

    private fun resolveSystemRouteTypeHint(): AudioOutputType? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return null
        return resolveSystemRouteTypeHintApi34()
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun resolveSystemRouteTypeHintApi34(): AudioOutputType? {
        val routeType = runCatching {
            MediaRouter2.getInstance(appContext).systemController.selectedRoutes.firstOrNull()?.type
        }.getOrNull() ?: return null
        return when (routeType) {
            MediaRoute2Info.TYPE_BUILTIN_SPEAKER -> AudioOutputType.PHONE_SPEAKER
            MediaRoute2Info.TYPE_WIRED_HEADSET,
            MediaRoute2Info.TYPE_WIRED_HEADPHONES -> AudioOutputType.WIRED_HEADPHONES
            MediaRoute2Info.TYPE_USB_DEVICE,
            MediaRoute2Info.TYPE_USB_HEADSET,
            MediaRoute2Info.TYPE_USB_ACCESSORY -> AudioOutputType.USB_AUDIO
            MediaRoute2Info.TYPE_BLUETOOTH_A2DP,
            MediaRoute2Info.TYPE_HEARING_AID,
            MediaRoute2Info.TYPE_BLE_HEADSET -> AudioOutputType.BLUETOOTH_HEADPHONES
            else -> null
        }
    }

    private fun refreshDevices(emitEvents: Boolean, customNotice: String? = null) {
        val hasBtPerm = PermissionHelper.hasBluetoothConnectPermission(appContext)
        val rawSinks = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val btConnectedDevices = readConnectedBluetoothDevices()
        val btByAddress = btConnectedDevices.associateBy { it.address }

        val mappedDevices = mutableListOf<AudioOutputDevice>()
        val seenKeys = mutableSetOf<String>()
        val matchedBtAddresses = mutableSetOf<String>()

        for (sink in rawSinks) {
            val sinkAddress = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                sink.address.takeIf { it.isNotBlank() }
            } else {
                null
            }
            val matchedBtDevice = sinkAddress?.let { btByAddress[it] }
                ?: if ((sink.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                            (sink.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                                sink.type == AudioDeviceInfo.TYPE_BLE_SPEAKER))) &&
                    btConnectedDevices.size == 1
                ) {
                    btConnectedDevices.firstOrNull()
                } else {
                    null
                }

            val btClassInt = matchedBtDevice?.let { safeBluetoothClassInt(it) }
            val rawProductName = sink.productName?.toString()?.trim()?.takeIf { it.isNotBlank() }
            val btFriendlyName = matchedBtDevice?.let { safeBluetoothDeviceName(it) }
            val resolvedName = btFriendlyName ?: rawProductName

            val outputType = mapAndroidDeviceType(
                audioDeviceInfoType = sink.type,
                productName = resolvedName,
                btClassInt = btClassInt
            ) ?: continue

            if (matchedBtDevice != null) {
                matchedBtAddresses.add(matchedBtDevice.address)
            }

            val displayName = when (outputType) {
                AudioOutputType.PHONE_SPEAKER -> "Phone Speaker"
                AudioOutputType.EARPIECE -> "Built-in Earpiece"
                AudioOutputType.WIRED_HEADPHONES -> {
                    if (!resolvedName.isNullOrBlank() && !resolvedName.equals(Build.MODEL, ignoreCase = true)) {
                        resolvedName
                    } else {
                        "Wired Headphones"
                    }
                }
                AudioOutputType.USB_AUDIO -> {
                    if (!resolvedName.isNullOrBlank() && !resolvedName.equals(Build.MODEL, ignoreCase = true)) {
                        resolvedName
                    } else {
                        "USB Audio Device"
                    }
                }
                AudioOutputType.BLUETOOTH_HEADPHONES,
                AudioOutputType.BLUETOOTH_SPEAKER -> resolvedName ?: outputType.label
            }

            // Avoid duplicate rows when Android exposes both headset + headphone or speaker variants
            val dedupeKey = when (outputType) {
                AudioOutputType.PHONE_SPEAKER -> "builtin_speaker"
                AudioOutputType.EARPIECE -> "builtin_earpiece"
                AudioOutputType.WIRED_HEADPHONES -> "wired_${sinkAddress ?: displayName}"
                AudioOutputType.USB_AUDIO -> "usb_${sinkAddress ?: displayName}"
                AudioOutputType.BLUETOOTH_HEADPHONES,
                AudioOutputType.BLUETOOTH_SPEAKER -> "bt_${matchedBtDevice?.address ?: sinkAddress ?: displayName}"
            }
            if (!seenKeys.add(dedupeKey)) continue

            val battery = (matchedBtDevice?.address ?: sinkAddress)?.let { batteryByAddress[it] }
            val stableId = "sink_${sink.id}"

            mappedDevices.add(
                AudioOutputDevice(
                    id = stableId,
                    audioDeviceId = sink.id,
                    name = displayName,
                    type = outputType,
                    address = matchedBtDevice?.address ?: sinkAddress,
                    batteryPercent = battery,
                    isActive = false,
                    isConnected = true,
                    isDirectSwitchSupported = true
                )
            )
        }

        // Include any additional connected A2DP Bluetooth devices not separately addressable by AudioTrack.setPreferredDevice
        for (btDevice in btConnectedDevices) {
            if (btDevice.address in matchedBtAddresses) continue
            val btName = safeBluetoothDeviceName(btDevice)
            val btType = classifyBluetoothDeviceType(safeBluetoothClassInt(btDevice), btName)
            val dedupeKey = "bt_${btDevice.address}"
            if (!seenKeys.add(dedupeKey)) continue
            mappedDevices.add(
                AudioOutputDevice(
                    id = "bt_${btDevice.address}",
                    audioDeviceId = null,
                    name = btName,
                    type = btType,
                    address = btDevice.address,
                    batteryPercent = batteryByAddress[btDevice.address],
                    isActive = false,
                    isConnected = true,
                    isDirectSwitchSupported = false
                )
            )
        }

        // If the user explicitly chose a device that just disconnected, clear preference smoothly back to System Auto.
        val preferredStillConnected = userPreferredDeviceId?.let { prefId ->
            mappedDevices.any { it.id == prefId }
        } ?: false
        var fallbackNotice: String? = null
        if (userPreferredDeviceId != null && !preferredStillConnected) {
            userPreferredDeviceId = null
            _preferredAudioDeviceInfo.value = null
            fallbackNotice = "Selected output disconnected — restored automatic audio routing"
        }

        val activeId = resolvePreferredSystemRouteActiveId(
            devices = mappedDevices,
            userPreferredDeviceId = userPreferredDeviceId,
            systemRouteTypeHint = resolveSystemRouteTypeHint()
        )

        val sortedDevices = mappedDevices
            .map { dev -> dev.copy(isActive = dev.id == activeId) }
            .sortedWith(
                compareByDescending<AudioOutputDevice> { it.isActive }
                    .thenByDescending { it.type.isExternal }
                    .thenBy { it.type.ordinal }
                    .thenBy { it.name.lowercase() }
            )

        val currentExternalMap = sortedDevices
            .filter { it.type.isExternal }
            .associateBy { externalEventKey(it) }

        if (emitEvents) {
            val newlyConnected = currentExternalMap.entries
                .firstOrNull { (k, _) -> k !in knownExternalDevices }
                ?.value
            val newlyDisconnected = knownExternalDevices.entries
                .firstOrNull { (k, _) -> k !in currentExternalMap }
                ?.value

            when {
                newlyConnected != null -> {
                    emitBannerIfNeeded(
                        key = externalEventKey(newlyConnected),
                        device = newlyConnected,
                        connected = true
                    )
                }
                newlyDisconnected != null -> {
                    emitBannerIfNeeded(
                        key = externalEventKey(newlyDisconnected),
                        device = newlyDisconnected,
                        connected = false
                    )
                }
            }
        }
        knownExternalDevices = currentExternalMap

        _routingState.value = _routingState.value.copy(
            devices = sortedDevices,
            activeDevice = sortedDevices.firstOrNull { it.isActive },
            isAutoRouteSelected = userPreferredDeviceId == null,
            hasBluetoothConnectPermission = hasBtPerm,
            statusNotice = customNotice ?: fallbackNotice ?: _routingState.value.statusNotice
        )
    }

    private fun externalEventKey(device: AudioOutputDevice): String {
        return "${device.type.name}_${device.address ?: device.name.lowercase()}"
    }

    private fun emitBannerIfNeeded(
        key: String,
        device: AudioOutputDevice,
        connected: Boolean
    ) {
        val now = System.currentTimeMillis()
        if (!shouldEmitDeviceBanner(
                eventKey = key,
                isConnected = connected,
                lastKey = lastBannerKey,
                lastIsConnected = lastBannerConnected,
                lastTimestampMs = lastBannerTimeMs,
                nowMs = now
            )
        ) {
            return
        }
        lastBannerKey = key
        lastBannerConnected = connected
        lastBannerTimeMs = now
        bannerSequence++

        val banner = AudioDeviceEventBanner(
            eventId = bannerSequence,
            deviceName = device.name,
            deviceType = device.type,
            isConnectedEvent = connected,
            batteryPercent = if (connected) device.batteryPercent else null
        )
        _routingState.value = _routingState.value.copy(activeBanner = banner)
        if (connected) {
            _externalDeviceConnectedEvents.tryEmit(device)
        }

        bannerDismissJob?.cancel()
        bannerDismissJob = scope.launch {
            delay(3_400L)
            if (_routingState.value.activeBanner?.eventId == banner.eventId) {
                _routingState.value = _routingState.value.copy(activeBanner = null)
            }
        }
    }

    companion object {
        private const val ACTION_BATTERY_LEVEL_CHANGED = "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED"
        private const val EXTRA_BATTERY_LEVEL = "android.bluetooth.device.extra.BATTERY_LEVEL"
    }
}
