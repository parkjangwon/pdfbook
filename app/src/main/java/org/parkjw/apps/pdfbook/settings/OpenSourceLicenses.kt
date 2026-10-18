package org.parkjw.apps.pdfbook.settings

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import org.parkjw.apps.pdfbook.R
import org.parkjw.apps.pdfbook.ui.SafeTopBar
import org.parkjw.apps.pdfbook.ui.theme.PdfBookSpacing
import org.parkjw.apps.pdfbook.ui.theme.pdfBookIconButtonColors

data class OpenSourceEntry(
    @StringRes val name: Int,
    @StringRes val license: Int,
    @StringRes val copyright: Int,
    @StringRes val url: Int,
)

val OpenSourceLibraries = listOf(
    OpenSourceEntry(
        R.string.oss_androidx_name,
        R.string.oss_androidx_license,
        R.string.oss_androidx_copyright,
        R.string.oss_androidx_url,
    ),
    OpenSourceEntry(
        R.string.oss_kotlin_name,
        R.string.oss_kotlin_license,
        R.string.oss_kotlin_copyright,
        R.string.oss_kotlin_url,
    ),
    OpenSourceEntry(
        R.string.oss_material_name,
        R.string.oss_material_license,
        R.string.oss_material_copyright,
        R.string.oss_material_url,
    ),
    OpenSourceEntry(
        R.string.oss_jetbrains_mono_name,
        R.string.oss_jetbrains_mono_license,
        R.string.oss_jetbrains_mono_copyright,
        R.string.oss_jetbrains_mono_url,
    ),
    OpenSourceEntry(
        R.string.oss_pdfbook_sans_name,
        R.string.oss_pdfbook_sans_license,
        R.string.oss_pdfbook_sans_copyright,
        R.string.oss_pdfbook_sans_url,
    ),
    OpenSourceEntry(
        R.string.oss_activity_name,
        R.string.oss_activity_license,
        R.string.oss_activity_copyright,
        R.string.oss_activity_url,
    ),
    OpenSourceEntry(
        R.string.oss_window_name,
        R.string.oss_window_license,
        R.string.oss_window_copyright,
        R.string.oss_window_url,
    ),
    OpenSourceEntry(
        R.string.oss_datastore_name,
        R.string.oss_datastore_license,
        R.string.oss_datastore_copyright,
        R.string.oss_datastore_url,
    ),
)

@Composable
fun OpenSourceLicensesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            SafeTopBar {
                IconButton(onClick = onBack, colors = pdfBookIconButtonColors()) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_back),
                        contentDescription = stringResource(R.string.reader_back),
                    )
                }
                Text(
                    text = stringResource(R.string.settings_open_source),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
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
            SettingsGroup {
                OpenSourceLibraries.forEachIndexed { index, entry ->
                    if (index > 0) GroupDivider()
                    ListItem(
                        headlineContent = {
                            Text(
                                text = stringResource(entry.name),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        },
                        supportingContent = {
                            Column {
                                Text(
                                    text = stringResource(entry.license),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = stringResource(entry.copyright),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = stringResource(entry.url),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
