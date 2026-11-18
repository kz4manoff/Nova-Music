package com.novamusic.player.ui.wrapped

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novamusic.player.audio.AudioPlayerManager
import com.novamusic.player.data.repository.MusicRepository
import com.novamusic.player.wrapped.WrappedPeriod
import com.novamusic.player.wrapped.WrappedRepository
import com.novamusic.player.wrapped.WrappedShareImageGenerator
import com.novamusic.player.wrapped.WrappedSummaryStats
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WrappedScreenUiState(
    val selectedPeriod: WrappedPeriod = WrappedPeriod.THIS_YEAR,
    val stats: WrappedSummaryStats? = null,
    val isLoading: Boolean = true,
    val isGeneratingImage: Boolean = false
)

sealed interface WrappedFeedbackEvent {
    data class Message(val text: String) : WrappedFeedbackEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class WrappedViewModel(
    private val wrappedRepository: WrappedRepository,
    private val musicRepository: MusicRepository,
    private val playerManager: AudioPlayerManager
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(WrappedPeriod.THIS_YEAR)
    val selectedPeriod: StateFlow<WrappedPeriod> = _selectedPeriod.asStateFlow()

    private val _isGeneratingImage = MutableStateFlow(false)

    private val _feedbackEvents = MutableSharedFlow<WrappedFeedbackEvent>()
    val feedbackEvents: SharedFlow<WrappedFeedbackEvent> = _feedbackEvents.asSharedFlow()

    val uiState: StateFlow<WrappedScreenUiState> = combine(
        _selectedPeriod.flatMapLatest { period ->
            wrappedRepository.observeWrappedSummary(period)
        },
        _selectedPeriod,
        _isGeneratingImage
    ) { summary, period, generating ->
        WrappedScreenUiState(
            selectedPeriod = period,
            stats = summary,
            isLoading = false,
            isGeneratingImage = generating
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WrappedScreenUiState()
    )

    fun selectPeriod(period: WrappedPeriod) {
        _selectedPeriod.value = period
    }

    fun playSongFromWrapped(songId: Long) {
        if (songId <= 0L) return
        val library = musicRepository.allSongs.value
        val target = library.firstOrNull { it.id == songId } ?: return
        playerManager.playSong(target, library)
    }

    fun shareWrappedImage(context: Context) {
        val currentStats = uiState.value.stats ?: return
        if (!currentStats.hasData || _isGeneratingImage.value) return
        viewModelScope.launch {
            _isGeneratingImage.value = true
            try {
                val result = WrappedShareImageGenerator.createShareUri(context, currentStats)
                result.onSuccess { uri ->
                    WrappedShareImageGenerator.launchShareSheet(
                        context = context,
                        imageUri = uri,
                        periodTitle = currentStats.periodRange.displayTitle
                    )
                }.onFailure { err ->
                    _feedbackEvents.emit(
                        WrappedFeedbackEvent.Message(
                            err.localizedMessage ?: "Could not prepare share image"
                        )
                    )
                }
            } finally {
                _isGeneratingImage.value = false
            }
        }
    }

    fun saveWrappedImage(context: Context) {
        val currentStats = uiState.value.stats ?: return
        if (!currentStats.hasData || _isGeneratingImage.value) return
        viewModelScope.launch {
            _isGeneratingImage.value = true
            try {
                val result = WrappedShareImageGenerator.savePosterToPictures(context, currentStats)
                result.onSuccess { msg ->
                    _feedbackEvents.emit(WrappedFeedbackEvent.Message(msg))
                }.onFailure { err ->
                    _feedbackEvents.emit(
                        WrappedFeedbackEvent.Message(
                            err.localizedMessage ?: "Failed to save image"
                        )
                    )
                }
            } finally {
                _isGeneratingImage.value = false
            }
        }
    }
}
