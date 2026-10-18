package org.parkjw.apps.pdfbook.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.parkjw.apps.pdfbook.R
import org.parkjw.apps.pdfbook.ReadingDirection
import org.parkjw.apps.pdfbook.ui.SafeTopBar
import org.parkjw.apps.pdfbook.ui.theme.CardCorner
import org.parkjw.apps.pdfbook.ui.theme.PdfBookNumeric
import org.parkjw.apps.pdfbook.ui.theme.PdfBookSpacing
import org.parkjw.apps.pdfbook.ui.theme.extendedColors
import org.parkjw.apps.pdfbook.ui.theme.pdfBookRadioButtonColors
import org.parkjw.apps.pdfbook.ui.theme.withNumericSpans

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onSettingsChange: ((AppSettings) -> AppSettings) -> Unit,
    versionName: String,
    modifier: Modifier = Modifier,
    hiddenCount: Int = 0,
    onOpenHiddenItems: () -> Unit = {},
    scrollToEnd: Boolean = false,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            SafeTopBar {
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(
                        horizontal = PdfBookSpacing.screenHorizontalFolded,
                        vertical = PdfBookSpacing.md,
                    ),
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState(if (scrollToEnd) Int.MAX_VALUE else 0))
                .padding(bottom = PdfBookSpacing.lg),
        ) {
            SettingsSection(stringResource(R.string.settings_section_general))
            SettingsGroup {
                ChoiceBlock(
                    title = stringResource(R.string.settings_start_screen),
                    options = listOf(
                        StartScreen.Folders to stringResource(R.string.settings_start_folders),
                        StartScreen.LastBook to stringResource(R.string.settings_start_last_book),
                    ),
                    selected = settings.startScreen,
                    onSelect = { value -> onSettingsChange { it.copy(startScreen = value) } },
                )
                GroupDivider()
                ThemeSegment(
                    selected = settings.themeMode,
                    onSelect = { value -> onSettingsChange { it.copy(themeMode = value) } },
                )
                GroupDivider()
                ChoiceBlock(
                    title = stringResource(R.string.settings_language),
                    options = listOf(
                        AppLanguage.System to stringResource(R.string.settings_language_system),
                        AppLanguage.Korean to stringResource(R.string.settings_language_korean),
                        AppLanguage.English to stringResource(R.string.settings_language_english),
                    ),
                    selected = settings.language,
                    onSelect = { value -> onSettingsChange { it.copy(language = value) } },
                )
            }

            SettingsSection(stringResource(R.string.settings_section_reading))
            SettingsGroup {
                ChoiceBlock(
                    title = stringResource(R.string.settings_default_direction),
                    options = listOf(
                        ReadingDirection.RTL to stringResource(R.string.settings_direction_rtl),
                        ReadingDirection.LTR to stringResource(R.string.settings_direction_ltr),
                    ),
                    selected = settings.defaultReadingDirection,
                    onSelect = { value -> onSettingsChange { it.copy(defaultReadingDirection = value) } },
                )
                GroupDivider()
                ChoiceBlock(
                    title = stringResource(R.string.settings_two_page),
                    options = listOf(
                        TwoPageMode.Auto to stringResource(R.string.settings_two_page_auto),
                        TwoPageMode.AlwaysSingle to stringResource(R.string.settings_two_page_single),
                        TwoPageMode.AlwaysDouble to stringResource(R.string.settings_two_page_double),
                    ),
                    selected = settings.twoPageMode,
                    onSelect = { value -> onSettingsChange { it.copy(twoPageMode = value) } },
                )
                GroupDivider()
                SwitchRow(
                    title = stringResource(R.string.settings_cover_alone),
                    checked = settings.coverAlone,
                    onCheckedChange = { checked -> onSettingsChange { it.copy(coverAlone = checked) } },
                )
                GroupDivider()
                ChoiceBlock(
                    title = stringResource(R.string.settings_page_turn_animation),
                    options = listOf(
                        PageTurnAnimation.Slide to stringResource(R.string.settings_animation_slide),
                        PageTurnAnimation.None to stringResource(R.string.settings_animation_none),
                    ),
                    selected = settings.pageTurnAnimation,
                    onSelect = { value -> onSettingsChange { it.copy(pageTurnAnimation = value) } },
                )
                GroupDivider()
                SwitchRow(
                    title = stringResource(R.string.settings_volume_key_paging),
                    checked = settings.volumeKeyPaging,
                    onCheckedChange = { checked -> onSettingsChange { it.copy(volumeKeyPaging = checked) } },
                )
                GroupDivider()
                SwitchRow(
                    title = stringResource(R.string.settings_keep_screen_on),
                    checked = settings.keepScreenOn,
                    onCheckedChange = { checked -> onSettingsChange { it.copy(keepScreenOn = checked) } },
                )
                GroupDivider()
                SwitchRow(
                    title = stringResource(R.string.settings_tap_zone_guide),
                    checked = settings.showTapZoneGuide,
                    onCheckedChange = { checked -> onSettingsChange { it.copy(showTapZoneGuide = checked) } },
                )
            }

            StorageSettings(
                hiddenCount = hiddenCount,
                showDotFiles = settings.showDotFiles,
                onOpenHiddenItems = onOpenHiddenItems,
                onShowDotFilesChange = { checked -> onSettingsChange { it.copy(showDotFiles = checked) } },
            )

            SettingsSection(stringResource(R.string.settings_section_about))
            SettingsGroup {
                ListItem(
                    headlineContent = {
                        Text(
                            text = withNumericSpans(
                                stringResource(R.string.about_version, versionName),
                                SpanStyle(
                                    fontFamily = PdfBookNumeric.fontFamily,
                                    fontSize = 15.sp,
                                    letterSpacing = (-0.2).sp,
                                ),
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
internal fun SettingsSection(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(
            start = PdfBookSpacing.screenHorizontalFolded,
            end = PdfBookSpacing.screenHorizontalFolded,
            top = PdfBookSpacing.lg,
            bottom = PdfBookSpacing.xs,
        ),
    )
}

@Composable
internal fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = CardCorner,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.extendedColors.hairline),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PdfBookSpacing.screenHorizontalFolded),
    ) {
        Column(modifier = Modifier.fillMaxWidth(), content = content)
    }
}

@Composable
internal fun GroupDivider() {
    HorizontalDivider(color = MaterialTheme.extendedColors.hairline)
}

@Composable
private fun ThemeSegment(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
) {
    val options = listOf(
        ThemeMode.System to stringResource(R.string.settings_theme_system),
        ThemeMode.Light to stringResource(R.string.settings_theme_light),
        ThemeMode.Dark to stringResource(R.string.settings_theme_dark),
    )
    val ink = MaterialTheme.extendedColors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = PdfBookSpacing.xs),
    ) {
        Text(
            text = stringResource(R.string.settings_theme),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(
                horizontal = PdfBookSpacing.md,
                vertical = PdfBookSpacing.xs,
            ),
        )
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PdfBookSpacing.md, vertical = PdfBookSpacing.xs),
        ) {
            options.forEachIndexed { index, (value, label) ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                    onClick = { onSelect(value) },
                    selected = selected == value,
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = ink.inkButton,
                        activeContentColor = ink.onInkButton,
                        inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        inactiveContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    border = SegmentedButtonDefaults.borderStroke(ink.hairlineStrong),
                ) {
                    Text(label, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun <T> ChoiceBlock(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup()
            .padding(vertical = PdfBookSpacing.xxs),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(
                horizontal = PdfBookSpacing.md,
                vertical = PdfBookSpacing.xs,
            ),
        )
        options.forEach { (value, label) ->
            ListItem(
                headlineContent = {
                    Text(label, style = MaterialTheme.typography.bodyLarge)
                },
                leadingContent = {
                    RadioButton(
                        selected = selected == value,
                        onClick = null,
                        colors = pdfBookRadioButtonColors(),
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = selected == value,
                        onClick = { onSelect(value) },
                        role = Role.RadioButton,
                    ),
            )
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent = {
            Text(title, style = MaterialTheme.typography.bodyLarge)
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    uncheckedBorderColor = MaterialTheme.extendedColors.hairlineStrong,
                ),
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                onValueChange = onCheckedChange,
                role = Role.Switch,
            ),
    )
}
