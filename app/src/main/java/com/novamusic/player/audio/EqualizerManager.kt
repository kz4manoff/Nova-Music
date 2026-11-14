package com.novamusic.player.audio

import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.os.Build
import androidx.annotation.RequiresApi
import com.novamusic.player.data.model.EqualizerBandInfo
import com.novamusic.player.data.model.EqualizerState
import com.novamusic.player.data.preferences.EqualizerPersistenceState
import com.novamusic.player.data.preferences.PreferencesRepository
import com.novamusic.player.utils.FormatUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EqualizerManager(
    private val preferences: PreferencesRepository
) {
    companion object {
        val TEN_BAND_CENTER_FREQS = intArrayOf(31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)
        private val TEN_BAND_CUTOFF_FREQS = floatArrayOf(
            45f, 90f, 180f, 360f, 720f, 1450f, 2900f, 5800f, 11500f, 20000f
        )

        val BUILT_IN_PRESETS: Map<String, List<Int>> = linkedMapOf(
            "Normal" to listOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
            "Bass Boost" to listOf(650, 550, 420, 220, 50, 0, 0, 100, 150, 150),
            "Rock" to listOf(450, 350, 200, -100, -200, 50, 250, 400, 480, 500),
            "Pop" to listOf(-150, 150, 350, 420, 250, -50, -150, 100, 250, 300),
            "Classical" to listOf(350, 250, 150, 50, -100, -100, 50, 200, 350, 450),
            "Jazz" to listOf(300, 200, 100, 200, -150, -150, 50, 200, 300, 350),
            "Electronic" to listOf(550, 450, 150, -100, -200, 100, 250, 400, 500, 550),
            "Hip-Hop" to listOf(600, 520, 300, 100, -100, -100, 150, 100, 200, 300),
            "Vocal" to listOf(-250, -200, -50, 200, 450, 500, 400, 200, 0, -100)
        )
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(EqualizerState())
    val state: StateFlow<EqualizerState> = _state.asStateFlow()

    private var currentSessionId: Int = 0
    private var attachJob: Job? = null
    private var dynamicsProcessing: DynamicsProcessing? = null
    private var systemEqualizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    init {
        scope.launch {
            val saved = preferences.getEqualizerPersistenceStateOnce()
            val defaultBands = TEN_BAND_CENTER_FREQS.mapIndexed { idx, freq ->
                val savedLevel = saved.bandLevelsMillibels.getOrNull(idx) ?: 0
                EqualizerBandInfo(
                    index = idx,
                    centerFreqHz = freq,
                    label = FormatUtils.formatFrequency(freq),
                    levelMillibels = savedLevel
                )
            }
            _state.value = _state.value.copy(
                isEnabled = saved.isEnabled,
                bands = defaultBands,
                activePresetName = saved.presetName,
                bassBoostStrength = saved.bassBoostStrength,
                virtualizerStrength = saved.virtualizerStrength,
                loudnessGainMillibels = saved.loudnessGainMillibels,
                preampGainDb = saved.preampGainDb
            )
        }
    }

    fun attachToSession(sessionId: Int) {
        if (sessionId <= 0) return
        if (currentSessionId == sessionId && (dynamicsProcessing != null || systemEqualizer != null)) {
            return
        }

        attachJob?.cancel()
        releaseEffects()
        currentSessionId = sessionId

        attachJob = scope.launch {
            val saved = preferences.getEqualizerPersistenceStateOnce()
            if (currentSessionId != sessionId) return@launch
            initializeEffectsForSession(sessionId, saved)
        }
    }

    private fun initializeEffectsForSession(
        sessionId: Int,
        saved: EqualizerPersistenceState
    ) {
        var useTenBand = false
        var eqSupported = false
        var preampSupported = false
        var minMb = -1500
        var maxMb = 1500
        var engineDesc = "Equalizer unavailable on this device"
        val bandInfos = mutableListOf<EqualizerBandInfo>()

        // 1. Try true 10-band DynamicsProcessing + Preamp on Android 9+ (API 28+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val dpResult = tryCreateTenBandDynamicsProcessing(sessionId, saved)
            if (dpResult != null) {
                dynamicsProcessing = dpResult
                useTenBand = true
                eqSupported = true
                preampSupported = true
                minMb = -1500
                maxMb = 1500
                engineDesc = "10-Band Precision DSP (DynamicsProcessing)"
                TEN_BAND_CENTER_FREQS.forEachIndexed { idx, freqHz ->
                    val level = (saved.bandLevelsMillibels.getOrNull(idx) ?: 0).coerceIn(minMb, maxMb)
                    bandInfos.add(
                        EqualizerBandInfo(
                            index = idx,
                            centerFreqHz = freqHz,
                            label = FormatUtils.formatFrequency(freqHz),
                            levelMillibels = level
                        )
                    )
                }
            }
        }

        // 2. Fallback to standard Android Equalizer API (API 26+)
        if (!eqSupported) {
            runCatching {
                val eq = Equalizer(0, sessionId)
                val status = eq.setEnabled(saved.isEnabled)
                if (status == AudioEffect.SUCCESS || !saved.isEnabled) {
                    systemEqualizer = eq
                    eqSupported = true
                    val range = eq.bandLevelRange
                    minMb = range.getOrNull(0)?.toInt() ?: -1500
                    maxMb = range.getOrNull(1)?.toInt() ?: 1500
                    val count = eq.numberOfBands.toInt()
                    useTenBand = count >= 10
                    engineDesc = "${count}-Band Hardware Equalizer"

                    for (i in 0 until count) {
                        val freqHz = eq.getCenterFreq(i.toShort()) / 1000
                        val level = resolveSavedLevelForBand(i, count, saved.bandLevelsMillibels)
                            .coerceIn(minMb, maxMb)
                        eq.setBandLevel(i.toShort(), level.toShort())
                        bandInfos.add(
                            EqualizerBandInfo(
                                index = i,
                                centerFreqHz = freqHz,
                                label = FormatUtils.formatFrequency(freqHz),
                                levelMillibels = level
                            )
                        )
                    }
                } else {
                    eq.release()
                }
            }
        }

        // 3. Bass Boost
        var bassSupported = false
        runCatching {
            val bb = BassBoost(0, sessionId)
            if (bb.strengthSupported) {
                bassBoost = bb
                bassSupported = true
                bb.setStrength(saved.bassBoostStrength.coerceIn(0, 1000).toShort())
                bb.setEnabled(saved.isEnabled && saved.bassBoostStrength > 0)
            } else {
                bb.release()
            }
        }

        // 4. Virtualizer
        var virtSupported = false
        runCatching {
            val virt = Virtualizer(0, sessionId)
            if (virt.strengthSupported) {
                virtualizer = virt
                virtSupported = true
                virt.setStrength(saved.virtualizerStrength.coerceIn(0, 1000).toShort())
                virt.setEnabled(saved.isEnabled && saved.virtualizerStrength > 0)
            } else {
                virt.release()
            }
        }

        // 5. Loudness Enhancer
        var loudSupported = false
        runCatching {
            val le = LoudnessEnhancer(sessionId)
            loudnessEnhancer = le
            loudSupported = true
            le.setTargetGain(saved.loudnessGainMillibels.coerceIn(0, 1500))
            le.setEnabled(saved.isEnabled && saved.loudnessGainMillibels > 0)
        }

        _state.value = EqualizerState(
            isEnabled = saved.isEnabled,
            isSupported = eqSupported,
            isTenBandEngine = useTenBand,
            engineDescription = engineDesc,
            minLevelMillibels = minMb,
            maxLevelMillibels = maxMb,
            bands = if (bandInfos.isNotEmpty()) bandInfos else _state.value.bands,
            activePresetName = saved.presetName,
            bassBoostSupported = bassSupported,
            bassBoostStrength = saved.bassBoostStrength,
            virtualizerSupported = virtSupported,
            virtualizerStrength = saved.virtualizerStrength,
            loudnessSupported = loudSupported,
            loudnessGainMillibels = saved.loudnessGainMillibels,
            preampSupported = preampSupported,
            preampGainDb = saved.preampGainDb,
            audioSessionId = sessionId
        )
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun tryCreateTenBandDynamicsProcessing(
        sessionId: Int,
        saved: EqualizerPersistenceState
    ): DynamicsProcessing? {
        return runCatching {
            val eqStage = DynamicsProcessing.Eq(true, true, 10)
            for (i in 0 until 10) {
                val gainDb = ((saved.bandLevelsMillibels.getOrNull(i) ?: 0) / 100f).coerceIn(-15f, 15f)
                eqStage.setBand(i, DynamicsProcessing.EqBand(true, TEN_BAND_CUTOFF_FREQS[i], gainDb))
            }

            val config = DynamicsProcessing.Config.Builder(
                DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                2,
                true,
                10,
                false,
                0,
                false,
                0,
                true
            )
                .setPreEqAllChannelsTo(eqStage)
                .setInputGainAllChannelsTo(saved.preampGainDb.coerceIn(-12f, 12f))
                .build()

            val dp = DynamicsProcessing(0, sessionId, config)
            val status = dp.setEnabled(saved.isEnabled)
            if (status != AudioEffect.SUCCESS && saved.isEnabled) {
                dp.release()
                null
            } else {
                dp
            }
        }.getOrNull()
    }

    private fun resolveSavedLevelForBand(index: Int, bandCount: Int, savedTenOrNBands: List<Int>): Int {
        if (savedTenOrNBands.isEmpty()) return 0
        if (savedTenOrNBands.size == bandCount) return savedTenOrNBands[index]
        val mappedIdx = ((index.toFloat() / (bandCount - 1).coerceAtLeast(1)) * (savedTenOrNBands.size - 1))
            .toInt()
            .coerceIn(0, savedTenOrNBands.lastIndex)
        return savedTenOrNBands[mappedIdx]
    }

    fun setEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(isEnabled = enabled)
        runCatching { dynamicsProcessing?.setEnabled(enabled) }
        runCatching { systemEqualizer?.setEnabled(enabled) }
        runCatching { bassBoost?.setEnabled(enabled && _state.value.bassBoostStrength > 0) }
        runCatching { virtualizer?.setEnabled(enabled && _state.value.virtualizerStrength > 0) }
        runCatching { loudnessEnhancer?.setEnabled(enabled && _state.value.loudnessGainMillibels > 0) }
        persistCurrentState()
    }

    fun setBandLevel(bandIndex: Int, levelMillibels: Int) {
        val current = _state.value
        val clamped = levelMillibels.coerceIn(current.minLevelMillibels, current.maxLevelMillibels)
        val updatedBands = current.bands.map {
            if (it.index == bandIndex) it.copy(levelMillibels = clamped) else it
        }
        _state.value = current.copy(bands = updatedBands, activePresetName = "Custom")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && dynamicsProcessing != null && bandIndex in 0..9) {
            runCatching {
                dynamicsProcessing?.setPreEqBandAllChannelsTo(
                    bandIndex,
                    DynamicsProcessing.EqBand(true, TEN_BAND_CUTOFF_FREQS[bandIndex], clamped / 100f)
                )
            }
        } else {
            runCatching {
                systemEqualizer?.setBandLevel(bandIndex.toShort(), clamped.toShort())
            }
        }
        persistCurrentState()
    }

    fun applyBuiltInPreset(presetName: String) {
        val tenBandValues = BUILT_IN_PRESETS[presetName] ?: return
        val current = _state.value
        val updatedBands = current.bands.mapIndexed { idx, band ->
            val targetLevel = resolveSavedLevelForBand(idx, current.bands.size, tenBandValues)
                .coerceIn(current.minLevelMillibels, current.maxLevelMillibels)
            applySingleBandToAudioFx(idx, targetLevel)
            band.copy(levelMillibels = targetLevel)
        }
        _state.value = current.copy(bands = updatedBands, activePresetName = presetName)
        persistCurrentState()
    }

    fun applyCustomPreset(
        name: String,
        bandLevelsMillibels: List<Int>,
        bass: Int,
        virt: Int,
        loudness: Int,
        preampDb: Float
    ) {
        val current = _state.value
        val updatedBands = current.bands.mapIndexed { idx, band ->
            val targetLevel = resolveSavedLevelForBand(idx, current.bands.size, bandLevelsMillibels)
                .coerceIn(current.minLevelMillibels, current.maxLevelMillibels)
            applySingleBandToAudioFx(idx, targetLevel)
            band.copy(levelMillibels = targetLevel)
        }
        _state.value = current.copy(
            bands = updatedBands,
            activePresetName = name
        )
        setBassBoost(bass)
        setVirtualizer(virt)
        setLoudnessGain(loudness)
        setPreampGain(preampDb)
        persistCurrentState()
    }

    private fun applySingleBandToAudioFx(index: Int, levelMillibels: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && dynamicsProcessing != null && index in 0..9) {
            runCatching {
                dynamicsProcessing?.setPreEqBandAllChannelsTo(
                    index,
                    DynamicsProcessing.EqBand(true, TEN_BAND_CUTOFF_FREQS[index], levelMillibels / 100f)
                )
            }
        } else {
            runCatching {
                systemEqualizer?.setBandLevel(index.toShort(), levelMillibels.toShort())
            }
        }
    }

    fun setBassBoost(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        _state.value = _state.value.copy(bassBoostStrength = clamped)
        runCatching {
            bassBoost?.setStrength(clamped.toShort())
            bassBoost?.setEnabled(_state.value.isEnabled && clamped > 0)
        }
        persistCurrentState()
    }

    fun setVirtualizer(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        _state.value = _state.value.copy(virtualizerStrength = clamped)
        runCatching {
            virtualizer?.setStrength(clamped.toShort())
            virtualizer?.setEnabled(_state.value.isEnabled && clamped > 0)
        }
        persistCurrentState()
    }

    fun setLoudnessGain(gainMillibels: Int) {
        val clamped = gainMillibels.coerceIn(0, 1500)
        _state.value = _state.value.copy(loudnessGainMillibels = clamped)
        runCatching {
            loudnessEnhancer?.setTargetGain(clamped)
            loudnessEnhancer?.setEnabled(_state.value.isEnabled && clamped > 0)
        }
        persistCurrentState()
    }

    fun setPreampGain(gainDb: Float) {
        val clamped = gainDb.coerceIn(-12f, 12f)
        _state.value = _state.value.copy(preampGainDb = clamped)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching {
                dynamicsProcessing?.setInputGainAllChannelsTo(clamped)
            }
        }
        persistCurrentState()
    }

    fun resetEqualizer() {
        val current = _state.value
        val resetBands = current.bands.mapIndexed { idx, band ->
            applySingleBandToAudioFx(idx, 0)
            band.copy(levelMillibels = 0)
        }
        _state.value = current.copy(
            bands = resetBands,
            activePresetName = "Normal",
            bassBoostStrength = 0,
            virtualizerStrength = 0,
            loudnessGainMillibels = 0,
            preampGainDb = 0f
        )
        runCatching {
            bassBoost?.setStrength(0)
            bassBoost?.setEnabled(false)
            virtualizer?.setStrength(0)
            virtualizer?.setEnabled(false)
            loudnessEnhancer?.setTargetGain(0)
            loudnessEnhancer?.setEnabled(false)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                dynamicsProcessing?.setInputGainAllChannelsTo(0f)
            }
        }
        persistCurrentState()
    }

    private fun persistCurrentState() {
        val snapshot = _state.value
        scope.launch {
            preferences.saveEqualizerState(
                EqualizerPersistenceState(
                    isEnabled = snapshot.isEnabled,
                    presetName = snapshot.activePresetName,
                    bandLevelsMillibels = snapshot.bands.map { it.levelMillibels },
                    bassBoostStrength = snapshot.bassBoostStrength,
                    virtualizerStrength = snapshot.virtualizerStrength,
                    loudnessGainMillibels = snapshot.loudnessGainMillibels,
                    preampGainDb = snapshot.preampGainDb
                )
            )
        }
    }

    fun releaseEffects() {
        attachJob?.cancel()
        attachJob = null
        runCatching { dynamicsProcessing?.release() }
        runCatching { systemEqualizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { virtualizer?.release() }
        runCatching { loudnessEnhancer?.release() }
        dynamicsProcessing = null
        systemEqualizer = null
        bassBoost = null
        virtualizer = null
        loudnessEnhancer = null
        currentSessionId = 0
    }
}
