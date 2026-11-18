package com.novamusic.player.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novamusic.player.data.model.ArtworkStyle
import com.novamusic.player.data.model.NavBarAppearance
import com.novamusic.player.data.model.PlayerAnimationStyle
import com.novamusic.player.data.model.ThemeMode
import com.novamusic.player.ui.components.rememberNovaHaptic
import kotlin.math.roundToInt

@Composable
fun AppearanceSettingsPage(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    bottomContentPadding: Dp
) {
    val settings by viewModel.appSettings.collectAsStateWithLifecycle()
    val accent = MaterialTheme.colorScheme.primary
    val performHaptic = rememberNovaHaptic()
    var showLanguageDialog by remember { mutableStateOf(false) }

    val languages = remember {
        listOf(
            "" to "System Default",
            "en" to "English",
            "fr" to "Français",
            "es" to "Español",
            "de" to "Deutsch",
            "ar" to "العربية"
        )
    }
    val currentLanguageLabel = languages.firstOrNull { it.first == settings.languageTag }?.second ?: "System Default"

    NovaSettingsSubpageScaffold(
        title = "Appearance",
        subtitle = "Theme, colors, motion & player styling",
        onBack = onBack,
        bottomContentPadding = bottomContentPadding
    ) {
        item(key = "theme_and_colors") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NovaSectionHeader(title = "Theme & Colors", icon = Icons.Rounded.Palette)
                NovaSettingsCard {
                    Column(modifier = Modifier.padding(vertical = 10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.DarkMode,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Theme",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        NovaSegmentedGlassBar(
                            options = listOf(
                                ThemeMode.SYSTEM to "Adaptive",
                                ThemeMode.DARK to "Charcoal",
                                ThemeMode.AMOLED to "AMOLED"
                            ),
                            selected = if (settings.themeMode == ThemeMode.LIGHT) ThemeMode.DARK else settings.themeMode,
                            onSelect = viewModel::setThemeMode
                        )
                    }
                    NovaSettingsDivider()
                    NovaAccentColorPicker(
                        selected = settings.accentColor,
                        onSelect = viewModel::setAccentColor
                    )
                    NovaSettingsDivider()
                    NovaSwitchSettingRow(
                        icon = Icons.Rounded.AutoAwesome,
                        title = "Dynamic Colors",
                        subtitle = "Adapt controls and ambient glow to active album artwork",
                        checked = settings.dynamicColors,
                        onCheckedChange = viewModel::setDynamicColors
                    )
                }
            }
        }

        item(key = "animations") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NovaSectionHeader(title = "App Animations", icon = Icons.Rounded.Animation)
                NovaSettingsCard {
                    NovaSwitchSettingRow(
                        icon = Icons.Rounded.Animation,
                        title = "App Animations",
                        subtitle = "Fluid transitions, glass highlights & breathing motion",
                        checked = settings.interfaceAnimations,
                        onCheckedChange = viewModel::setInterfaceAnimations
                    )
                    NovaSettingsDivider()
                    NovaDropdownSettingRow(
                        icon = Icons.Rounded.Tune,
                        title = "Player Transition Style",
                        subtitle = settings.playerAnimationStyle.description,
                        selectedLabel = settings.playerAnimationStyle.label,
                        options = PlayerAnimationStyle.entries.map { it to it.label },
                        selectedValue = settings.playerAnimationStyle,
                        onSelect = viewModel::setPlayerAnimationStyle
                    )
                }
            }
        }

        item(key = "player_customization") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NovaSectionHeader(title = "Player Customization", icon = Icons.Rounded.Album)
                NovaSettingsCard {
                    NovaDropdownSettingRow(
                        icon = Icons.Rounded.Album,
                        title = "Album Artwork Style",
                        subtitle = "Now Playing artwork frame presentation",
                        selectedLabel = settings.artworkStyle.label,
                        options = ArtworkStyle.entries.map { it to it.label },
                        selectedValue = settings.artworkStyle,
                        onSelect = viewModel::setArtworkStyle
                    )
                    NovaSettingsDivider()
                    NovaGlassSliderRow(
                        icon = Icons.Rounded.BlurOn,
                        title = "Background Blur",
                        subtitle = "Live artwork backdrop diffusion intensity",
                        valueLabel = "${(settings.blurIntensity * 100).roundToInt()}%",
                        value = settings.blurIntensity,
                        valueRange = 0.25f..1.50f,
                        onValueChange = viewModel::setBlurIntensity
                    )
                    NovaSettingsDivider()
                    NovaDropdownSettingRow(
                        icon = Icons.Rounded.Tune,
                        title = "Navigation Bar Style",
                        subtitle = settings.navBarAppearance.description,
                        selectedLabel = settings.navBarAppearance.label,
                        options = NavBarAppearance.entries.map { it to it.label },
                        selectedValue = settings.navBarAppearance,
                        onSelect = viewModel::setNavBarAppearance
                    )
                }
            }
        }

        item(key = "interface_behavior") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NovaSectionHeader(title = "Interface & Display", icon = Icons.Rounded.Language)
                NovaSettingsCard {
                    NovaActionSettingRow(
                        icon = Icons.Rounded.Language,
                        title = "Language",
                        subtitle = "App display language",
                        trailingText = currentLanguageLabel,
                        onClick = { showLanguageDialog = true }
                    )
                    NovaSettingsDivider()
                    NovaSwitchSettingRow(
                        icon = Icons.Rounded.Vibration,
                        title = "Haptic Feedback",
                        subtitle = "Subtle tactile pulse on controls and navigation",
                        checked = settings.hapticFeedback,
                        onCheckedChange = viewModel::setHapticFeedback
                    )
                    NovaSettingsDivider()
                    NovaSwitchSettingRow(
                        icon = Icons.Rounded.Visibility,
                        title = "Keep Screen On",
                        subtitle = "Prevent display sleep while NOVA MUSIC is open",
                        checked = settings.keepScreenOn,
                        onCheckedChange = viewModel::setKeepScreenOn
                    )
                }
            }
        }
    }

    if (showLanguageDialog) {
        NovaGlassSelectionDialog(
            title = "Language",
            subtitle = "Choose display language for NOVA MUSIC",
            onDismiss = { showLanguageDialog = false }
        ) {
            Column {
                languages.forEach { (tag, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                performHaptic()
                                viewModel.setLanguage(tag)
                                showLanguageDialog = false
                            }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = settings.languageTag == tag,
                            onClick = {
                                performHaptic()
                                viewModel.setLanguage(tag)
                                showLanguageDialog = false
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}
