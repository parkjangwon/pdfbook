package org.parkjw.apps.pdfbook.update

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.parkjw.apps.pdfbook.R
import org.parkjw.apps.pdfbook.ui.theme.ButtonCorner
import org.parkjw.apps.pdfbook.ui.theme.PdfBookProgressBar
import org.parkjw.apps.pdfbook.ui.theme.PdfBookSpacing
import org.parkjw.apps.pdfbook.ui.theme.extendedColors
import org.parkjw.apps.pdfbook.ui.theme.pdfBookOutlinedButtonColors
import org.parkjw.apps.pdfbook.ui.theme.pdfBookTextButtonColors

enum class UpdateAction(@StringRes val label: Int) {
    Check(R.string.update_check),
    Download(R.string.update_download),
    Later(R.string.update_later),
    OpenSettings(R.string.update_open_settings),
    OpenReleasePage(R.string.update_open_release),
}

data class UpdateStatus(@StringRes val text: Int, val arg: Any? = null, val isError: Boolean = false)

data class UpdateRow(
    val status: UpdateStatus?,
    val progress: Float?,
    val actions: List<UpdateAction>,
    val checkEnabled: Boolean,
)

fun updateRow(state: UpdateUiState): UpdateRow = when (state) {
    UpdateUiState.Idle -> UpdateRow(null, null, emptyList(), checkEnabled = true)
    UpdateUiState.Checking -> UpdateRow(UpdateStatus(R.string.update_checking), null, emptyList(), checkEnabled = false)
    UpdateUiState.UpToDate -> UpdateRow(UpdateStatus(R.string.update_latest), null, emptyList(), checkEnabled = true)
    is UpdateUiState.Available -> UpdateRow(
        UpdateStatus(R.string.update_available, state.release.version?.toString() ?: state.release.tagName),
        null,
        listOf(UpdateAction.Download),
        checkEnabled = true,
    )
    is UpdateUiState.NeedsInstallPermission -> UpdateRow(
        UpdateStatus(R.string.update_need_permission),
        null,
        listOf(UpdateAction.OpenSettings),
        checkEnabled = false,
    )
    is UpdateUiState.Downloading -> UpdateRow(
        UpdateStatus(R.string.update_downloading, state.percent),
        state.percent / 100f,
        emptyList(),
        checkEnabled = false,
    )
    is UpdateUiState.ReadyToInstall -> UpdateRow(
        UpdateStatus(R.string.update_downloading, 100),
        1f,
        emptyList(),
        checkEnabled = false,
    )
    is UpdateUiState.Failed -> UpdateRow(
        UpdateStatus(
            when (state.problem) {
                UpdateProblem.Check -> R.string.update_check_failed
                UpdateProblem.Download -> R.string.update_download_failed
                UpdateProblem.Verify -> R.string.update_verify_failed
            },
            isError = true,
        ),
        null,
        listOf(UpdateAction.OpenReleasePage),
        checkEnabled = true,
    )
}

@Composable
private fun UpdateStatus.render(): String =
    if (arg == null) stringResource(text) else stringResource(text, arg)

@Composable
fun UpdateCheckBlock(
    state: UpdateUiState,
    onAction: (UpdateAction) -> Unit,
) {
    val row = updateRow(state)
    Column(modifier = Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = {
                Text(stringResource(R.string.update_check), style = MaterialTheme.typography.bodyLarge)
            },
            supportingContent = row.status?.let { status ->
                {
                    Text(
                        status.render(),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (status.isError) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    enabled = row.checkEnabled,
                    role = Role.Button,
                    onClick = { onAction(UpdateAction.Check) },
                ),
        )
        row.progress?.let { progress ->
            PdfBookProgressBar(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PdfBookSpacing.md)
                    .padding(bottom = PdfBookSpacing.sm),
            )
        }
        if (row.actions.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(PdfBookSpacing.xs),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PdfBookSpacing.md)
                    .padding(bottom = PdfBookSpacing.sm),
            ) {
                row.actions.forEach { action ->
                    if (action == UpdateAction.Download) {
                        InkButton(stringResource(action.label)) { onAction(action) }
                    } else {
                        OutlinedButton(
                            onClick = { onAction(action) },
                            shape = ButtonCorner,
                            colors = pdfBookOutlinedButtonColors(),
                        ) {
                            Text(stringResource(action.label))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UpdatePromptDialog(
    state: UpdateUiState.Available,
    onDownload: () -> Unit,
    onLater: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onLater,
        title = {
            Text(
                stringResource(
                    R.string.update_available,
                    state.release.version?.toString() ?: state.release.tagName,
                ),
            )
        },
        confirmButton = { InkButton(stringResource(R.string.update_download), onDownload) },
        dismissButton = {
            TextButton(onClick = onLater, colors = pdfBookTextButtonColors()) {
                Text(stringResource(R.string.update_later))
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp,
    )
}

@Composable
private fun InkButton(label: String, onClick: () -> Unit) {
    val ink = MaterialTheme.extendedColors
    Button(
        onClick = onClick,
        shape = ButtonCorner,
        colors = ButtonDefaults.buttonColors(
            containerColor = ink.inkButton,
            contentColor = ink.onInkButton,
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            focusedElevation = 0.dp,
            hoveredElevation = 0.dp,
            disabledElevation = 0.dp,
        ),
    ) {
        Text(label)
    }
}
