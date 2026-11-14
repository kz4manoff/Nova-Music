package com.novamusic.player.ui.equalizer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novamusic.player.audio.EqualizerManager
import com.novamusic.player.data.model.EqualizerState
import com.novamusic.player.data.repository.MusicRepository
import com.novamusic.player.database.entities.CustomPresetEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EqualizerViewModel(
    private val equalizerManager: EqualizerManager,
    private val repository: MusicRepository
) : ViewModel() {

    val eqState: StateFlow<EqualizerState> = equalizerManager.state

    val customPresets: StateFlow<List<CustomPresetEntity>> = repository.customEqualizerPresets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val builtInPresetNames: List<String> = EqualizerManager.BUILT_IN_PRESETS.keys.toList()

    fun setEnabled(enabled: Boolean) = equalizerManager.setEnabled(enabled)
    fun setBandLevel(index: Int, millibels: Int) = equalizerManager.setBandLevel(index, millibels)
    fun applyBuiltInPreset(name: String) = equalizerManager.applyBuiltInPreset(name)
    fun setBassBoost(strength: Int) = equalizerManager.setBassBoost(strength)
    fun setVirtualizer(strength: Int) = equalizerManager.setVirtualizer(strength)
    fun setLoudnessGain(gainMillibels: Int) = equalizerManager.setLoudnessGain(gainMillibels)
    fun setPreampGain(gainDb: Float) = equalizerManager.setPreampGain(gainDb)
    fun resetEqualizer() = equalizerManager.resetEqualizer()

    fun applyCustomPreset(preset: CustomPresetEntity) {
        val levels = preset.bandLevelsCsv.split(",").mapNotNull { it.trim().toIntOrNull() }
        equalizerManager.applyCustomPreset(
            name = preset.name,
            bandLevelsMillibels = levels,
            bass = preset.bassBoost,
            virt = preset.virtualizer,
            loudness = preset.loudness,
            preampDb = preset.preampDb
        )
    }

    fun saveCustomPreset(name: String) {
        val current = eqState.value
        viewModelScope.launch {
            repository.saveCustomPreset(
                name = name,
                bandLevelsMillibels = current.bands.map { it.levelMillibels },
                bassBoost = current.bassBoostStrength,
                virtualizer = current.virtualizerStrength,
                loudness = current.loudnessGainMillibels,
                preampDb = current.preampGainDb
            )
        }
    }

    fun deleteCustomPreset(presetId: Long) {
        viewModelScope.launch {
            repository.deleteCustomPreset(presetId)
        }
    }
}
