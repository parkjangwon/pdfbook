package org.parkjw.apps.pdfbook.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.parkjw.apps.pdfbook.R
import org.parkjw.apps.pdfbook.ReadingDirection
import org.parkjw.apps.pdfbook.TapZoneWidthSheet
import org.parkjw.apps.pdfbook.TapZoneWidthSheetDefaults
import org.parkjw.apps.pdfbook.lock.AppLockMode
import org.parkjw.apps.pdfbook.lock.SecurityValueRow
import org.parkjw.apps.pdfbook.ui.SafeTopBar
import org.parkjw.apps.pdfbook.ui.theme.CardCorner
import org.parkjw.apps.pdfbook.ui.theme.PdfBookNumeric
import org.parkjw.apps.pdfbook.ui.theme.PdfBookSpacing
import org.parkjw.apps.pdfbook.ui.theme.extendedColors
import org.parkjw.apps.pdfbook.ui.theme.pdfBookIconButtonColors
import org.parkjw.apps.pdfbook.ui.theme.pdfBookRadioButtonColors
import org.parkjw.apps.pdfbook.ui.theme.pdfBookTextButtonColors
import org.parkjw.apps.pdfbook.ui.theme.withNumericSpans
import org.parkjw.apps.pdfbook.update.UpdateAction
import org.parkjw.apps.pdfbook.update.UpdateCheckBlock
import org.parkjw.apps.pdfbook.update.UpdateUiState
import org.parkjw.apps.pdfbook.update.openBrowsableUrl

enum class SettingsSubScreen {
    Main,
    Reading,
    OpenSource,
}

private enum class SettingsChoice {
    Direction,
    TwoPage,
    Animation,
}

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onSettingsChange: ((AppSettings) -> AppSettings) -> Unit,
    appLockMode: AppLockMode = AppLockMode.None,
    onOpenAppLock: () -> Unit = {},
    versionName: String,
    versionCode: Long = 0L,
    modifier: Modifier = Modifier,
    hiddenCount: Int = 0,
    onOpenHiddenItems: () -> Unit = {},
    scrollToEnd: Boolean = false,
    updateState: UpdateUiState = UpdateUiState.Idle,
    onUpdateAction: (UpdateAction) -> Unit = {},
    initialSubScreen: SettingsSubScreen = SettingsSubScreen.Main,
    initiallyShowStartChoice: Boolean = false,
    initiallyShowTapWidthSheet: Boolean = false,
) {
    var subScreen by rememberSaveable { mutableStateOf(initialSubScreen.name) }
    var showStartChoice by rememberSaveable { mutableStateOf(initiallyShowStartChoice) }
    var showLanguageChoice by rememberSaveable { mutableStateOf(false) }
    when (subScreen) {
        SettingsSubScreen.Reading.name -> ReadingSettingsScreen(
            settings = settings,
            onSettingsChange = onSettingsChange,
            onBack = { subScreen = SettingsSubScreen.Main.name },
            initiallyShowTapWidthSheet = initiallyShowTapWidthSheet,
            modifier = modifier,
        )
        SettingsSubScreen.OpenSource.name -> OpenSourceLicensesScreen(
            onBack = { subScreen = SettingsSubScreen.Main.name },
            modifier = modifier,
        )
        else -> SettingsMainScreen(
            settings = settings,
            onSettingsChange = onSettingsChange,
            appLockMode = appLockMode,
            onOpenAppLock = onOpenAppLock,
            versionName = versionName,
            versionCode = versionCode,
            hiddenCount = hiddenCount,
            onOpenHiddenItems = onOpenHiddenItems,
            scrollToEnd = scrollToEnd,
            updateState = updateState,
            onUpdateAction = onUpdateAction,
            showStartChoice = showStartChoice,
            onShowStartChoiceChange = { showStartChoice = it },
            showLanguageChoice = showLanguageChoice,
            onShowLanguageChoiceChange = { showLanguageChoice = it },
            onOpenReading = { subScreen = SettingsSubScreen.Reading.name },
            onOpenOpenSource = { subScreen = SettingsSubScreen.OpenSource.name },
            modifier = modifier,
        )
    }
}

@Composable
private fun SettingsMainScreen(
    settings: AppSettings,
    onSettingsChange: ((AppSettings) -> AppSettings) -> Unit,
    appLockMode: AppLockMode,
    onOpenAppLock: () -> Unit,
    versionName: String,
    versionCode: Long,
    hiddenCount: Int,
    onOpenHiddenItems: () -> Unit,
    scrollToEnd: Boolean,
    updateState: UpdateUiState,
    onUpdateAction: (UpdateAction) -> Unit,
    showStartChoice: Boolean,
    onShowStartChoiceChange: (Boolean) -> Unit,
    showLanguageChoice: Boolean,
    onShowLanguageChoiceChange: (Boolean) -> Unit,
    onOpenReading: () -> Unit,
    onOpenOpenSource: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val startOptions = listOf(
        StartScreen.Folders to stringResource(R.string.settings_start_folders),
        StartScreen.LastBook to stringResource(R.string.settings_start_last_book),
    )
    val languageOptions = listOf(
        AppLanguage.System to stringResource(R.string.settings_language_system),
        AppLanguage.Korean to stringResource(R.string.settings_language_korean),
        AppLanguage.English to stringResource(R.string.settings_language_english),
    )
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
                .testTag("settings_screen")
                .padding(padding)
                .verticalScroll(rememberScrollState(if (scrollToEnd) Int.MAX_VALUE else 0))
                .padding(bottom = PdfBookSpacing.lg),
        ) {
            SettingsSection(stringResource(R.string.settings_section_general))
            SettingsGroup {
                SecurityValueRow(
                    label = stringResource(R.string.settings_start_screen),
                    value = startOptions.first { it.first == settings.startScreen }.second,
                    onClick = { onShowStartChoiceChange(true) },
                )
                GroupDivider()
                ThemeSegment(
                    selected = settings.themeMode,
                    onSelect = { value -> onSettingsChange { it.copy(themeMode = value) } },
                )
                GroupDivider()
                SecurityValueRow(
                    label = stringResource(R.string.settings_language),
                    value = languageOptions.first { it.first == settings.language }.second,
                    onClick = { onShowLanguageChoiceChange(true) },
                )
            }

            SettingsSection(stringResource(R.string.settings_section_reading))
            SettingsGroup {
                SettingsNavRow(
                    label = stringResource(R.string.settings_reading_entry),
                    onClick = onOpenReading,
                    modifier = Modifier.testTag("settings_reading_entry"),
                )
            }

            StorageSettings(
                hiddenCount = hiddenCount,
                showDotFiles = settings.showDotFiles,
                onOpenHiddenItems = onOpenHiddenItems,
                onShowDotFilesChange = { checked -> onSettingsChange { it.copy(showDotFiles = checked) } },
            )
            SecuritySettingsSection(
                appLockMode = appLockMode,
                onOpenAppLock = onOpenAppLock,
            )
            SettingsSection(stringResource(R.string.settings_section_update))
            SettingsGroup {
                UpdateCheckBlock(state = updateState, currentVersion = versionName, onAction = onUpdateAction)
                GroupDivider()
                SwitchRow(
                    title = stringResource(R.string.update_auto),
                    supporting = stringResource(R.string.update_auto_desc),
                    checked = settings.autoUpdateCheck,
                    onCheckedChange = { checked -> onSettingsChange { it.copy(autoUpdateCheck = checked) } },
                )
            }

            SettingsSection(stringResource(R.string.settings_section_about))
            SettingsGroup {
                ListItem(
                    headlineContent = {
                        Text(
                            text = stringResource(R.string.settings_version),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    },
                    supportingContent = {
                        Text(
                            text = withNumericSpans(
                                stringResource(R.string.settings_version_value, versionName, versionCode),
                                SpanStyle(
                                    fontFamily = PdfBookNumeric.fontFamily,
                                    fontSize = 13.sp,
                                    letterSpacing = (-0.2).sp,
                                ),
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.fillMaxWidth(),
                )
                GroupDivider()
                SettingsNavRow(
                    label = stringResource(R.string.settings_open_source),
                    onClick = onOpenOpenSource,
                    modifier = Modifier.testTag("settings_open_source"),
                )
                GroupDivider()
                ListItem(
                    headlineContent = {
                        Text(
                            text = stringResource(R.string.settings_github),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    },
                    trailingContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_open_external),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = false,
                            onClick = {
                                context.openBrowsableUrl(GITHUB_REPO_URL)
                            },
                        ),
                )
            }
        }
    }
    if (showStartChoice) {
        SettingsChoiceDialog(
            title = stringResource(R.string.settings_start_screen),
            options = startOptions,
            selected = settings.startScreen,
            onSelect = { value ->
                onSettingsChange { it.copy(startScreen = value) }
                onShowStartChoiceChange(false)
            },
            onDismiss = { onShowStartChoiceChange(false) },
        )
    }
    if (showLanguageChoice) {
        SettingsChoiceDialog(
            title = stringResource(R.string.settings_language),
            options = languageOptions,
            selected = settings.language,
            onSelect = { value ->
                onSettingsChange { it.copy(language = value) }
                onShowLanguageChoiceChange(false)
            },
            onDismiss = { onShowLanguageChoiceChange(false) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReadingSettingsScreen(
    settings: AppSettings,
    onSettingsChange: ((AppSettings) -> AppSettings) -> Unit,
    onBack: () -> Unit,
    initiallyShowTapWidthSheet: Boolean,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)
    var choice by rememberSaveable { mutableStateOf<String?>(null) }
    var showTapWidthSheet by rememberSaveable { mutableStateOf(initiallyShowTapWidthSheet) }
    val directionOptions = listOf(
        ReadingDirection.RTL to stringResource(R.string.settings_direction_rtl),
        ReadingDirection.LTR to stringResource(R.string.settings_direction_ltr),
    )
    val twoPageOptions = listOf(
        TwoPageMode.Auto to stringResource(R.string.settings_two_page_auto),
        TwoPageMode.AlwaysSingle to stringResource(R.string.settings_two_page_single),
        TwoPageMode.AlwaysDouble to stringResource(R.string.settings_two_page_double),
    )
    val animationOptions = listOf(
        PageTurnAnimation.Slide to stringResource(R.string.settings_animation_slide),
        PageTurnAnimation.None to stringResource(R.string.settings_animation_none),
    )
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            SafeTopBar {
                IconButton(
                    onClick = onBack,
                    colors = pdfBookIconButtonColors(),
                    modifier = Modifier.testTag("settings_reading_back"),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_back),
                        contentDescription = stringResource(R.string.reader_back),
                    )
                }
                Text(
                    text = stringResource(R.string.settings_section_reading),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .testTag("settings_reading_screen")
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = PdfBookSpacing.lg),
        ) {
            SettingsGroup {
                SecurityValueRow(
                    label = stringResource(R.string.settings_default_direction),
                    value = directionOptions.first { it.first == settings.defaultReadingDirection }.second,
                    onClick = { choice = SettingsChoice.Direction.name },
                )
                GroupDivider()
                SecurityValueRow(
                    label = stringResource(R.string.settings_two_page),
                    value = twoPageOptions.first { it.first == settings.twoPageMode }.second,
                    onClick = { choice = SettingsChoice.TwoPage.name },
                )
                GroupDivider()
                SwitchRow(
                    title = stringResource(R.string.settings_cover_alone),
                    checked = settings.coverAlone,
                    onCheckedChange = { checked -> onSettingsChange { it.copy(coverAlone = checked) } },
                )
                GroupDivider()
                SecurityValueRow(
                    label = stringResource(R.string.settings_page_turn_animation),
                    value = animationOptions.first { it.first == settings.pageTurnAnimation }.second,
                    onClick = { choice = SettingsChoice.Animation.name },
                )
                GroupDivider()
                SwitchRow(
                    title = stringResource(R.string.settings_keep_zoom),
                    supporting = stringResource(R.string.settings_keep_zoom_desc),
                    checked = settings.keepZoomOnTurn,
                    onCheckedChange = { checked -> onSettingsChange { it.copy(keepZoomOnTurn = checked) } },
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
                GroupDivider()
                ListItem(
                    headlineContent = {
                        Text(
                            text = stringResource(R.string.reader_tap_width_title),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = withNumericSpans(
                                    stringResource(R.string.reader_tap_width_percent, settings.tapZoneSidePercent),
                                    SpanStyle(fontFamily = PdfBookNumeric.fontFamily),
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Icon(
                                painter = painterResource(R.drawable.ic_chevron_right),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_tap_width_row")
                        .selectable(selected = false, onClick = { showTapWidthSheet = true }),
                )
            }
        }
    }
    when (choice) {
        SettingsChoice.Direction.name -> SettingsChoiceDialog(
            title = stringResource(R.string.settings_default_direction),
            options = directionOptions,
            selected = settings.defaultReadingDirection,
            onSelect = { value ->
                onSettingsChange { it.copy(defaultReadingDirection = value) }
                choice = null
            },
            onDismiss = { choice = null },
        )
        SettingsChoice.TwoPage.name -> SettingsChoiceDialog(
            title = stringResource(R.string.settings_two_page),
            options = twoPageOptions,
            selected = settings.twoPageMode,
            onSelect = { value ->
                onSettingsChange { it.copy(twoPageMode = value) }
                choice = null
            },
            onDismiss = { choice = null },
        )
        SettingsChoice.Animation.name -> SettingsChoiceDialog(
            title = stringResource(R.string.settings_page_turn_animation),
            options = animationOptions,
            selected = settings.pageTurnAnimation,
            onSelect = { value ->
                onSettingsChange { it.copy(pageTurnAnimation = value) }
                choice = null
            },
            onDismiss = { choice = null },
        )
    }
    if (showTapWidthSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showTapWidthSheet = false },
            sheetState = sheetState,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 0.dp,
            dragHandle = null,
        ) {
            TapZoneWidthSheet(
                percent = settings.tapZoneSidePercent,
                onPercentChange = { value -> onSettingsChange { it.copy(tapZoneSidePercent = value) } },
                onDone = { showTapWidthSheet = false },
                colors = TapZoneWidthSheetDefaults.appColors(),
                previewDirection = settings.defaultReadingDirection,
            )
        }
    }
}

@Composable
private fun <T> SettingsChoiceDialog(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.fillMaxWidth().selectableGroup()) {
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
        },
        confirmButton = {
            TextButton(onClick = onDismiss, colors = pdfBookTextButtonColors()) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

@Composable
internal fun SettingsNavRow(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = {
            Text(label, style = MaterialTheme.typography.bodyLarge)
        },
        trailingContent = {
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = modifier
            .fillMaxWidth()
            .selectable(selected = false, onClick = onClick),
    )
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
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
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
internal fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    supporting: String? = null,
) {
    ListItem(
        headlineContent = {
            Text(title, style = MaterialTheme.typography.bodyLarge)
        },
        supportingContent = supporting?.let {
            {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall.copy(
                        lineBreak = LineBreak.Paragraph.copy(wordBreak = LineBreak.WordBreak.Phrase),
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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

internal const val GITHUB_REPO_URL = "https://github.com/parkjangwon/pdfbook"
