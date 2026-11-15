package com.novamusic.player.data.model

import androidx.compose.runtime.Immutable

enum class AudioOutputType(
    val label: String,
    val shortLabel: String,
    val isExternal: Boolean
) {
    PHONE_SPEAKER("Phone Speaker", "Speaker", false),
    EARPIECE("Built-in Earpiece", "Earpiece", false),
    WIRED_HEADPHONES("Wired Headphones", "Wired", true),
    USB_AUDIO("USB Audio", "USB Audio", true),
    BLUETOOTH_HEADPHONES("Bluetooth Headphones", "Bluetooth", true),
    BLUETOOTH_SPEAKER("Bluetooth Speaker", "BT Speaker", true)
}

enum class OutputSwitchOutcome {
    DIRECT_SWITCHED,
    ALREADY_ACTIVE,
    SYSTEM_SELECTOR_RECOMMENDED,
    DEVICE_UNAVAILABLE
}

@Immutable
data class AudioOutputDevice(
    val id: String,
    val audioDeviceId: Int?,
    val name: String,
    val type: AudioOutputType,
    val address: String? = null,
    val batteryPercent: Int? = null,
    val isActive: Boolean = false,
    val isConnected: Boolean = true,
    val isDirectSwitchSupported: Boolean = true
)

@Immutable
data class AudioDeviceEventBanner(
    val eventId: Long,
    val deviceName: String,
    val deviceType: AudioOutputType,
    val isConnectedEvent: Boolean,
    val batteryPercent: Int? = null
)

@Immutable
data class AudioRoutingUiState(
    val devices: List<AudioOutputDevice> = emptyList(),
    val activeDevice: AudioOutputDevice? = null,
    val isAutoRouteSelected: Boolean = true,
    val hasBluetoothConnectPermission: Boolean = true,
    val activeBanner: AudioDeviceEventBanner? = null,
    val statusNotice: String? = null
)
