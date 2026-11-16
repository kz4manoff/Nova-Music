package com.novamusic.player.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOff
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SortByAlpha
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novamusic.player.data.model.RescanInterval
import com.novamusic.player.data.model.SortDirection
import com.novamusic.player.data.model.SortOrder
import com.novamusic.player.ui.components.rememberNovaHaptic
import com.novamusic.player.update.UpdateUiState
import com.novamusic.player.utils.FormatUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LibraryManagementSettingsPage(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    bottomContentPadding: Dp
) {
    val settings by viewModel.appSettings.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val availableFolders by viewModel.availableFolders.collectAsStateWithLifecycle()
    val totalSongsCount by viewModel.totalSongsCount.collectAsStateWithLifecycle()
    val accent = MaterialTheme.colorScheme.primary
    val performHaptic = rememberNovaHaptic()

    var showMinDurationDialog by remember { mutableStateOf(false) }
    var showExcludedFoldersDialog by remember { mutableStateOf(false) }

    val lastScanLabel = remember(settings.lastScanTimestampMs) {
        if (settings.lastScanTimestampMs <= 0L) "Never"
        else SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(settings.lastScanTimestampMs))
    }

    NovaSettingsSubpageScaffold(
        title = "Library & Music Management",
        subtitle = "$totalSongsCount tracks • ${availableFolders.size} folders",
        onBack = onBack,
        bottomContentPadding = bottomContentPadding
    ) {
        item(key = "library_scanning") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NovaSectionHeader(
                    title = "Scanning & Discovery",
                    icon = Icons.Rounded.LibraryMusic,
                    trailingBadge = "$totalSongsCount tracks"
                )
                NovaSettingsCard {
                    NovaActionSettingRow(
                        icon = Icons.Rounded.Refresh,
                        title = "Scan Music Library",
                        subtitle = if (isScanning) "Scanning audio storage..." else "Last scan: $lastScanLabel • MP3, FLAC, WAV, AAC, M4A, OGG, OPUS",
                        trailingContent = if (isScanning) {
                            { CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = accent) }
                        } else null,
                        onClick = viewModel::rescanLibrary
                    )
                    NovaSettingsDivider()
                    NovaDropdownSettingRow(
                        icon = Icons.Rounded.Schedule,
                        title = "Automatic Rescan",
                        subtitle = "Schedule background library discovery",
                        selectedLabel = settings.rescanInterval.label,
                        options = RescanInterval.entries.map { it to it.label },
                        selectedValue = settings.rescanInterval,
                        onSelect = viewModel::setRescanInterval
                    )
                }
            }
        }

        item(key = "library_filters_sorting") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NovaSectionHeader(title = "Filters & Sorting", icon = Icons.Rounded.SortByAlpha)
                NovaSettingsCard {
                    NovaActionSettingRow(
                        icon = Icons.Rounded.FolderOff,
                        title = "Excluded Folders",
                        subtitle = if (settings.excludedFolders.isEmpty()) "All music folders included" else "${settings.excludedFolders.size} folder(s) hidden from library",
                        trailingText = if (settings.excludedFolders.isEmpty()) "None" else "${settings.excludedFolders.size}",
                        onClick = { showExcludedFoldersDialog = true }
                    )
                    NovaSettingsDivider()
                    NovaActionSettingRow(
                        icon = Icons.Rounded.Schedule,
                        title = "Minimum Track Duration",
                        subtitle = "Ignore short notification tones or voice clips",
                        trailingText = if (settings.minTrackDurationSec == 0) "All" else "${settings.minTrackDurationSec}s",
                        onClick = { showMinDurationDialog = true }
                    )
                    NovaSettingsDivider()
                    NovaDropdownSettingRow(
                        icon = Icons.Rounded.SortByAlpha,
                        title = "Default Track Sort",
                        subtitle = "Default ordering across library song lists",
                        selectedLabel = settings.sortOrder.label,
                        options = SortOrder.entries.map { it to it.label },
                        selectedValue = settings.sortOrder,
                        onSelect = { viewModel.setSort(it, settings.sortDirection) }
                    )
                    NovaSettingsDivider()
                    Column(modifier = Modifier.padding(vertical = 10.dp)) {
                        Text(
                            text = "Sort Direction",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        NovaSegmentedGlassBar(
                            options = listOf(
                                SortDirection.ASCENDING to "Ascending",
                                SortDirection.DESCENDING to "Descending"
                            ),
                            selected = settings.sortDirection,
                            onSelect = { viewModel.setSort(settings.sortOrder, it) }
                        )
                    }
                    NovaSettingsDivider()
                    NovaSwitchSettingRow(
                        icon = Icons.Rounded.Image,
                        title = "High-Resolution Artwork",
                        subtitle = "Load and cache full-resolution album cover art",
                        checked = settings.highResArtwork,
                        onCheckedChange = viewModel::setHighResArtwork
                    )
                }
            }
        }
    }

    if (showMinDurationDialog) {
        val options = listOf(0, 5, 10, 30, 60)
        NovaGlassSelectionDialog(
            title = "Minimum Track Duration",
            subtitle = "Exclude short sound clips when scanning",
            onDismiss = { showMinDurationDialog = false }
        ) {
            Column {
                options.forEach { sec ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                performHaptic()
                                viewModel.setMinTrackDurationSec(sec)
                                showMinDurationDialog = false
                            }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = settings.minTrackDurationSec == sec,
                            onClick = {
                                performHaptic()
                                viewModel.setMinTrackDurationSec(sec)
                                showMinDurationDialog = false
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (sec == 0) "Show all audio files" else "At least $sec seconds",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }

    if (showExcludedFoldersDialog) {
        NovaGlassSelectionDialog(
            title = "Excluded Folders",
            subtitle = "Checked folders will be hidden from your music library",
            onDismiss = { showExcludedFoldersDialog = false },
            dismissText = if (settings.excludedFolders.isNotEmpty()) "Include All" else null,
            onDismissAction = if (settings.excludedFolders.isNotEmpty()) viewModel::clearExcludedFolders else null,
            confirmText = "Done"
        ) {
            if (availableFolders.isEmpty()) {
                Text(
                    text = "No folders discovered yet. Scan your music library first.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    items(availableFolders, key = { it.path }) { folder ->
                        val isExcluded = folder.path in settings.excludedFolders
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    performHaptic()
                                    viewModel.toggleExcludedFolder(folder.path)
                                }
                                .padding(vertical = 6.dp, horizontal = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isExcluded,
                                onCheckedChange = {
                                    performHaptic()
                                    viewModel.toggleExcludedFolder(folder.path)
                                },
                                colors = CheckboxDefaults.colors(checkedColor = accent)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Rounded.Folder,
                                contentDescription = null,
                                tint = if (isExcluded) MaterialTheme.colorScheme.error else accent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = folder.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${folder.songCount} tracks • ${folder.path}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadsSettingsPage(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    bottomContentPadding: Dp
) {
    val settings by viewModel.appSettings.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val downloadedFiles by viewModel.downloadedUpdateFiles.collectAsStateWithLifecycle()
    val downloadsSizeBytes by viewModel.updateDownloadsSizeBytes.collectAsStateWithLifecycle()
    var downloadsClearedBanner by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshStorageStats()
    }

    NovaSettingsSubpageScaffold(
        title = "Downloads",
        subtitle = "Release APK downloads & transfer rules",
        onBack = onBack,
        bottomContentPadding = bottomContentPadding
    ) {
        item(key = "downloads_rules") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NovaSectionHeader(title = "Transfer Preferences", icon = Icons.Rounded.Download)
                NovaSettingsCard {
                    NovaSwitchSettingRow(
                        icon = Icons.Rounded.Wifi,
                        title = "Download Updates on Wi-Fi Only",
                        subtitle = "Require an unmetered Wi-Fi connection before downloading APK releases",
                        checked = settings.wifiOnlyUpdateDownloads,
                        onCheckedChange = viewModel::setWifiOnlyUpdateDownloads
                    )
                    NovaSettingsDivider()
                    NovaInfoRow("Download Location", viewModel.updatesFolderLabel)
                    NovaSettingsDivider()
                    NovaInfoRow("Verified Source", "GitHub Releases • HTTPS + SHA-256")
                }
            }
        }

        item(key = "downloads_active_status") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NovaSectionHeader(title = "Release Packages", icon = Icons.Rounded.SystemUpdate)
                NovaSettingsCard {
                    when (val state = updateState) {
                        is UpdateUiState.Downloading -> {
                            val pct = state.progressFraction?.let { "${(it * 100f).toInt()}%" } ?: "Downloading"
                            NovaActionSettingRow(
                                icon = Icons.Rounded.Download,
                                title = "Downloading v${state.config.latestVersion}",
                                subtitle = "${FormatUtils.formatFileSize(state.bytesDownloaded)} • Tap to view or cancel",
                                trailingText = pct,
                                onClick = { viewModel.reopenUpdateDialog() }
                            )
                            NovaSettingsDivider()
                        }
                        is UpdateUiState.ReadyToInstall -> {
                            NovaActionSettingRow(
                                icon = Icons.Rounded.CheckCircle,
                                title = "Verified APK Ready (v${state.config.latestVersion})",
                                subtitle = "SHA-256 and signing key verified • Tap to install",
                                trailingText = "Install",
                                onClick = { viewModel.startUpdateDownload() }
                            )
                            NovaSettingsDivider()
                        }
                        is UpdateUiState.Available -> {
                            NovaActionSettingRow(
                                icon = Icons.Rounded.SystemUpdate,
                                title = "Update v${state.config.latestVersion} Available",
                                subtitle = "Tap to review release notes and download APK",
                                trailingText = "Download",
                                onClick = { viewModel.reopenUpdateDialog() }
                            )
                            NovaSettingsDivider()
                        }
                        else -> Unit
                    }

                    if (downloadedFiles.isEmpty()) {
                        NovaInfoRow("Downloaded APK Packages", "No local update files")
                    } else {
                        downloadedFiles.forEachIndexed { idx, file ->
                            val dateLabel = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
                                .format(Date(file.lastModifiedMs))
                            NovaInfoRow(file.name, "${FormatUtils.formatFileSize(file.sizeBytes)} • $dateLabel")
                            if (idx < downloadedFiles.lastIndex) {
                                NovaSettingsDivider()
                            }
                        }
                        NovaSettingsDivider()
                    }

                    NovaActionSettingRow(
                        icon = Icons.Rounded.DeleteSweep,
                        title = "Clear Downloaded Update APKs",
                        subtitle = if (downloadsClearedBanner) {
                            "Downloaded release packages removed"
                        } else if (downloadedFiles.isEmpty()) {
                            "No downloaded update files stored"
                        } else {
                            "Remove cached release installers to free space"
                        },
                        trailingText = FormatUtils.formatFileSize(downloadsSizeBytes),
                        onClick = {
                            if (downloadedFiles.isNotEmpty() || downloadsSizeBytes > 0L) {
                                viewModel.clearUpdateDownloads()
                                downloadsClearedBanner = true
                            }
                        }
                    )
                }
            }
        }

        item(key = "downloads_offline_note") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                NovaSettingsCard {
                    NovaInfoRow("Music Playback", "100% Local Offline Files")
                    NovaSettingsDivider()
                    NovaInfoRow("Integrity Check", "SHA-256 + Package Cert Verified")
                }
            }
        }
    }
}

@Composable
fun StorageSettingsPage(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    bottomContentPadding: Dp
) {
    val totalSongsCount by viewModel.totalSongsCount.collectAsStateWithLifecycle()
    val totalLibraryBytes by viewModel.totalLibraryBytes.collectAsStateWithLifecycle()
    val cacheSizeBytes by viewModel.cacheSizeBytes.collectAsStateWithLifecycle()
    val updateDownloadsSizeBytes by viewModel.updateDownloadsSizeBytes.collectAsStateWithLifecycle()

    var cacheClearedBanner by remember { mutableStateOf(false) }
    var historyClearedBanner by remember { mutableStateOf(false) }
    var updatesClearedBanner by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshStorageStats()
    }

    NovaSettingsSubpageScaffold(
        title = "Storage",
        subtitle = "Usage breakdown & cache management",
        onBack = onBack,
        bottomContentPadding = bottomContentPadding
    ) {
        item(key = "storage_breakdown") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NovaSectionHeader(title = "Storage Overview", icon = Icons.Rounded.Storage)
                NovaSettingsCard {
                    NovaInfoRow("Indexed Tracks", "$totalSongsCount songs")
                    NovaSettingsDivider()
                    NovaInfoRow("Audio Library Size", FormatUtils.formatFileSize(totalLibraryBytes))
                    NovaSettingsDivider()
                    NovaInfoRow("Artwork & App Cache", FormatUtils.formatFileSize(cacheSizeBytes))
                    NovaSettingsDivider()
                    NovaInfoRow("Downloaded Update APKs", FormatUtils.formatFileSize(updateDownloadsSizeBytes))
                    NovaSettingsDivider()
                    NovaInfoRow("Database Engine", "100% Offline • Room + DataStore")
                }
            }
        }

        item(key = "storage_cleanup") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NovaSectionHeader(title = "Cleanup Actions", icon = Icons.Rounded.DeleteSweep)
                NovaSettingsCard {
                    NovaActionSettingRow(
                        icon = Icons.Rounded.DeleteSweep,
                        title = "Clear Artwork & App Cache",
                        subtitle = if (cacheClearedBanner) "Artwork and temporary cache cleared" else "Free cached album covers & temporary files",
                        trailingText = FormatUtils.formatFileSize(cacheSizeBytes),
                        onClick = {
                            viewModel.clearCache()
                            cacheClearedBanner = true
                        }
                    )
                    NovaSettingsDivider()
                    NovaActionSettingRow(
                        icon = Icons.Rounded.Download,
                        title = "Clear Downloaded Update APKs",
                        subtitle = if (updatesClearedBanner) "Update installer cache cleared" else "Remove downloaded release APK packages",
                        trailingText = FormatUtils.formatFileSize(updateDownloadsSizeBytes),
                        onClick = {
                            viewModel.clearUpdateDownloads()
                            updatesClearedBanner = true
                        }
                    )
                    NovaSettingsDivider()
                    NovaActionSettingRow(
                        icon = Icons.Rounded.History,
                        title = "Clear Playback History",
                        subtitle = if (historyClearedBanner) "Recently played & play counts reset" else "Reset recently played list and track play counts",
                        onClick = {
                            viewModel.clearHistory()
                            historyClearedBanner = true
                        }
                    )
                    NovaSettingsDivider()
                    NovaActionSettingRow(
                        icon = Icons.Rounded.Refresh,
                        title = "Recalculate Storage Usage",
                        subtitle = "Refresh current cache & update folder footprint",
                        onClick = { viewModel.refreshStorageStats() }
                    )
                }
            }
        }
    }
}
