package com.novamusic.player.ui.player

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novamusic.player.data.model.LyricsSource
import com.novamusic.player.data.model.Song
import com.novamusic.player.ui.components.LiquidGlassBackdrop
import com.novamusic.player.ui.components.LiquidGlassButton
import com.novamusic.player.ui.components.LiquidGlassCard
import com.novamusic.player.ui.components.LiquidGlassIconButton
import com.novamusic.player.ui.components.NovaAlbumArtwork
import com.novamusic.player.ui.theme.LocalNovaColors
import com.novamusic.player.ui.theme.NovaMonoTelemetry
import com.novamusic.player.utils.FormatUtils

@Composable
fun LyricsScreen(
    song: Song,
    viewModel: PlayerViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lyricsResult by viewModel.lyricsResult.collectAsStateWithLifecycle()
    val activeLineIndex by viewModel.activeLyricIndex.collectAsStateWithLifecycle()
    val rawLyricsForEdit by viewModel.rawLyricsForEdit.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    val accent = MaterialTheme.colorScheme.primary
    val novaColors = LocalNovaColors.current

    var showEditorDialog by remember { mutableStateOf(false) }

    val lrcPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importLrcFile(context, song, uri)
        }
    }

    LaunchedEffect(song.id) {
        viewModel.loadLyricsForSong(context, song)
    }

    LaunchedEffect(activeLineIndex) {
        if (activeLineIndex >= 0 && activeLineIndex < lyricsResult.lines.size) {
            listState.animateScrollToItem(index = (activeLineIndex - 2).coerceAtLeast(0))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LiquidGlassBackdrop(
            artworkUri = song.artworkUri,
            isPlaying = playerState.isPlaying,
            intensity = 1.2f
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // Floating Glass Header
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = 12.dp,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LiquidGlassIconButton(
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        size = 38.dp,
                        iconSize = 19.dp,
                        onClick = onBack
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    NovaAlbumArtwork(
                        artworkUri = song.artworkUri,
                        contentDescription = song.album,
                        modifier = Modifier.size(44.dp),
                        cornerRadius = 11.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        val sourceLabel = when (lyricsResult.source) {
                            LyricsSource.LRC_FILE -> "SYNCED LRC FILE"
                            LyricsSource.EMBEDDED -> "EMBEDDED TAG"
                            LyricsSource.MANUAL -> if (lyricsResult.isSynchronized) "IMPORTED LRC" else "MANUAL"
                            LyricsSource.NONE -> "NO LYRICS"
                        }
                        Text(
                            text = "${song.artist} • $sourceLabel",
                            style = NovaMonoTelemetry.copy(fontSize = 10.sp),
                            color = accent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    LiquidGlassIconButton(
                        icon = Icons.Rounded.FileOpen,
                        contentDescription = "Import .lrc",
                        size = 38.dp,
                        iconSize = 18.dp,
                        onClick = { lrcPickerLauncher.launch(arrayOf("*/*")) }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    LiquidGlassIconButton(
                        icon = Icons.Rounded.Edit,
                        contentDescription = "Edit Lyrics",
                        size = 38.dp,
                        iconSize = 18.dp,
                        onClick = { showEditorDialog = true }
                    )
                }
            }

            if (lyricsResult.lines.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LiquidGlassCard(
                        shape = RoundedCornerShape(28.dp),
                        contentPadding = PaddingValues(28.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(accent.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Lyrics,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No Offline Lyrics Available",
                                style = MaterialTheme.typography.headlineSmall,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Place a matching .lrc file in the song's folder, embed lyrics in the audio tag, or import/paste lyrics manually.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(22.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                LiquidGlassButton(
                                    onClick = { lrcPickerLauncher.launch(arrayOf("*/*")) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Import .lrc", style = MaterialTheme.typography.labelLarge)
                                }
                                LiquidGlassButton(
                                    onClick = { showEditorDialog = true },
                                    highlighted = true,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        "Add Lyrics",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(lyricsResult.lines) { index, line ->
                        val isActive = index == activeLineIndex
                        val textColor by animateColorAsState(
                            targetValue = when {
                                isActive -> MaterialTheme.colorScheme.onBackground
                                lyricsResult.isSynchronized && activeLineIndex > index ->
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.44f)
                                else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f)
                            },
                            animationSpec = tween(220),
                            label = "lyric_color"
                        )
                        val lineScale by animateFloatAsState(
                            targetValue = if (isActive) 1.02f else 1f,
                            animationSpec = tween(220),
                            label = "lyric_scale"
                        )

                        LiquidGlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .scale(lineScale),
                            shape = RoundedCornerShape(18.dp),
                            highlighted = isActive,
                            elevation = if (isActive) 12.dp else 0.dp,
                            onClick = if (line.timestampMs != null) {
                                { line.timestampMs?.let { viewModel.seekTo(it) } }
                            } else null,
                            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isActive) {
                                    Box(
                                        modifier = Modifier
                                            .width(3.5.dp)
                                            .height(26.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(accent)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                }
                                if (line.timestampMs != null) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isActive) accent.copy(alpha = 0.22f) else novaColors.recessedSurface
                                            )
                                            .padding(horizontal = 7.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = FormatUtils.formatDuration(line.timestampMs),
                                            style = NovaMonoTelemetry.copy(fontSize = 10.sp),
                                            color = if (isActive) accent else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                }
                                Text(
                                    text = line.text,
                                    style = if (isActive) {
                                        MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold)
                                    } else {
                                        MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium)
                                    },
                                    color = textColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditorDialog) {
        var draft by remember(rawLyricsForEdit) { mutableStateOf(rawLyricsForEdit) }
        AlertDialog(
            onDismissRequest = { showEditorDialog = false },
            containerColor = novaColors.elevatedSurface,
            title = { Text("Edit / Paste Lyrics", style = MaterialTheme.typography.titleLarge) },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = { Text("Paste plain lyrics or [mm:ss.xx] LRC timestamps...") },
                    minLines = 8,
                    maxLines = 14,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveManualLyrics(context, song, draft)
                        showEditorDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditorDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
