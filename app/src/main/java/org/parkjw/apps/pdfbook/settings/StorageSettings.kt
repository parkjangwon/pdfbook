package org.parkjw.apps.pdfbook.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import org.parkjw.apps.pdfbook.R
import org.parkjw.apps.pdfbook.ui.theme.PdfBookNumeric
import org.parkjw.apps.pdfbook.ui.theme.extendedColors
import org.parkjw.apps.pdfbook.ui.theme.withNumericSpans

@Composable
fun StorageSettings(
    hiddenCount: Int,
    showDotFiles: Boolean,
    onOpenHiddenItems: () -> Unit,
    onShowDotFilesChange: (Boolean) -> Unit,
) {
    SettingsSection(stringResource(R.string.settings_section_storage))
    SettingsGroup {
        ListItem(
            headlineContent = {
                Text(
                    text = stringResource(R.string.hidden_items_title),
                    style = MaterialTheme.typography.bodyLarge,
                )
            },
            supportingContent = {
                val count = pluralStringResource(
                    R.plurals.hidden_items_count,
                    hiddenCount,
                    hiddenCount,
                )
                Text(
                    text = withNumericSpans(
                        count,
                        SpanStyle(fontFamily = PdfBookNumeric.fontFamily),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            trailingContent = {
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = null,
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_hidden_items")
                .clickable(onClick = onOpenHiddenItems),
        )
        GroupDivider()
        ListItem(
            headlineContent = {
                Text(
                    text = stringResource(R.string.show_dot_files),
                    style = MaterialTheme.typography.bodyLarge,
                )
            },
            supportingContent = {
                Text(
                    text = stringResource(R.string.show_dot_files_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            trailingContent = {
                Switch(
                    checked = showDotFiles,
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
                    value = showDotFiles,
                    onValueChange = onShowDotFilesChange,
                    role = Role.Switch,
                ),
        )
    }
}
