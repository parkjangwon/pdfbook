package org.parkjw.apps.pdfbook.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import org.parkjw.apps.pdfbook.R
import org.parkjw.apps.pdfbook.ReadingDirection
import org.parkjw.apps.pdfbook.ui.SafeTopBar
import org.parkjw.apps.pdfbook.ui.theme.PdfBookSpacing

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onSettingsChange: ((AppSettings) -> AppSettings) -> Unit,
    onChangeFolder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
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
                .verticalScroll(rememberScrollState())
                .padding(bottom = PdfBookSpacing.lg),
        ) {
            SettingsSection(stringResource(R.string.settings_section_general))
            ChoiceRow(
                title = stringResource(R.string.settings_start_screen),
                options = listOf(
                    StartScreen.Library to stringResource(R.string.settings_start_library),
                    StartScreen.LastBook to stringResource(R.string.settings_start_last_book),
                ),
                selected = settings.startScreen,
                onSelect = { value -> onSettingsChange { it.copy(startScreen = value) } },
            )
            ActionRow(
                title = stringResource(R.string.settings_change_folder),
                subtitle = stringResource(R.string.settings_change_folder_hint),
                onClick = onChangeFolder,
            )
            ChoiceRow(
                title = stringResource(R.string.settings_theme),
                options = listOf(
                    ThemeMode.System to stringResource(R.string.settings_theme_system),
                    ThemeMode.Dark to stringResource(R.string.settings_theme_dark),
                    ThemeMode.Light to stringResource(R.string.settings_theme_light),
                ),
                selected = settings.themeMode,
                onSelect = { value -> onSettingsChange { it.copy(themeMode = value) } },
            )
            ChoiceRow(
                title = stringResource(R.string.settings_language),
                options = listOf(
                    AppLanguage.System to stringResource(R.string.settings_language_system),
                    AppLanguage.Korean to stringResource(R.string.settings_language_korean),
                    AppLanguage.English to stringResource(R.string.settings_language_english),
                ),
                selected = settings.language,
                onSelect = { value -> onSettingsChange { it.copy(language = value) } },
            )

            SettingsSection(stringResource(R.string.settings_section_reading))
            ChoiceRow(
                title = stringResource(R.string.settings_default_direction),
                options = listOf(
                    ReadingDirection.RTL to stringResource(R.string.settings_direction_rtl),
                    ReadingDirection.LTR to stringResource(R.string.settings_direction_ltr),
                ),
                selected = settings.defaultReadingDirection,
                onSelect = { value -> onSettingsChange { it.copy(defaultReadingDirection = value) } },
            )
            ChoiceRow(
                title = stringResource(R.string.settings_two_page),
                options = listOf(
                    TwoPageMode.Auto to stringResource(R.string.settings_two_page_auto),
                    TwoPageMode.AlwaysSingle to stringResource(R.string.settings_two_page_single),
                    TwoPageMode.AlwaysDouble to stringResource(R.string.settings_two_page_double),
                ),
                selected = settings.twoPageMode,
                onSelect = { value -> onSettingsChange { it.copy(twoPageMode = value) } },
            )
            SwitchRow(
                title = stringResource(R.string.settings_cover_alone),
                checked = settings.coverAlone,
                onCheckedChange = { checked -> onSettingsChange { it.copy(coverAlone = checked) } },
            )
            ChoiceRow(
                title = stringResource(R.string.settings_page_turn_animation),
                options = listOf(
                    PageTurnAnimation.Slide to stringResource(R.string.settings_animation_slide),
                    PageTurnAnimation.None to stringResource(R.string.settings_animation_none),
                ),
                selected = settings.pageTurnAnimation,
                onSelect = { value -> onSettingsChange { it.copy(pageTurnAnimation = value) } },
            )
            SwitchRow(
                title = stringResource(R.string.settings_volume_key_paging),
                checked = settings.volumeKeyPaging,
                onCheckedChange = { checked -> onSettingsChange { it.copy(volumeKeyPaging = checked) } },
            )
            SwitchRow(
                title = stringResource(R.string.settings_keep_screen_on),
                checked = settings.keepScreenOn,
                onCheckedChange = { checked -> onSettingsChange { it.copy(keepScreenOn = checked) } },
            )
            SwitchRow(
                title = stringResource(R.string.settings_tap_zone_guide),
                checked = settings.showTapZoneGuide,
                onCheckedChange = { checked -> onSettingsChange { it.copy(showTapZoneGuide = checked) } },
            )
        }
    }
}

@Composable
private fun SettingsSection(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(
            start = PdfBookSpacing.screenHorizontalFolded,
            end = PdfBookSpacing.screenHorizontalFolded,
            top = PdfBookSpacing.lg,
            bottom = PdfBookSpacing.xs,
        ),
    )
}

@Composable
private fun <T> ChoiceRow(
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
                horizontal = PdfBookSpacing.screenHorizontalFolded,
                vertical = PdfBookSpacing.xs,
            ),
        )
        options.forEach { (value, label) ->
            ListItem(
                headlineContent = {
                    Text(label, style = MaterialTheme.typography.bodyMedium)
                },
                leadingContent = {
                    RadioButton(
                        selected = selected == value,
                        onClick = null,
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = selected == value,
                        onClick = { onSelect(value) },
                        role = Role.RadioButton,
                    )
                    .padding(horizontal = PdfBookSpacing.xs),
            )
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(horizontal = PdfBookSpacing.screenHorizontalFolded),
        )
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
            Text(title, style = MaterialTheme.typography.bodyMedium)
        },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = null)
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                onValueChange = onCheckedChange,
                role = Role.Switch,
            )
            .padding(horizontal = PdfBookSpacing.xs),
    )
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.padding(horizontal = PdfBookSpacing.screenHorizontalFolded),
    )
}

@Composable
private fun ActionRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = {
            Text(title, style = MaterialTheme.typography.bodyMedium)
        },
        supportingContent = {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.ic_folder),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = {
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = PdfBookSpacing.xs),
    )
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.padding(horizontal = PdfBookSpacing.screenHorizontalFolded),
    )
}
