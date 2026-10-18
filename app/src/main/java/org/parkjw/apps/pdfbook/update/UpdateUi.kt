package org.parkjw.apps.pdfbook.update

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.parkjw.apps.pdfbook.R
import org.parkjw.apps.pdfbook.ui.theme.ButtonCorner
import org.parkjw.apps.pdfbook.ui.theme.PdfBookNumeric
import org.parkjw.apps.pdfbook.ui.theme.PdfBookProgressBar
import org.parkjw.apps.pdfbook.ui.theme.PdfBookSpacing
import org.parkjw.apps.pdfbook.ui.theme.extendedColors
import org.parkjw.apps.pdfbook.ui.theme.pdfBookOutlinedButtonColors
import org.parkjw.apps.pdfbook.ui.theme.pdfBookTextButtonColors
import org.parkjw.apps.pdfbook.ui.theme.withNumericSpans

private val UpdateBannerCorner = RoundedCornerShape(10.dp)

enum class UpdateAction(@StringRes val label: Int) {
    Check(R.string.update_check_action),
    Download(R.string.update_download),
    Later(R.string.update_later),
    OpenSettings(R.string.update_open_settings),
    OpenReleasePage(R.string.update_open_release),
}

sealed interface UpdateText {
    data class Res(@StringRes val res: Int, val arg: Any? = null) : UpdateText
    data class WithCurrentVersion(@StringRes val res: Int?) : UpdateText
    data object CurrentVersionLatest : UpdateText
}

enum class UpdateTrailing {
    None,
    Spinner,
    Check,
    Download,
}

data class UpdateRow(
    val title: UpdateText,
    val subtitle: UpdateText?,
    val trailing: UpdateTrailing = UpdateTrailing.None,
    val progress: Float? = null,
    val hint: UpdateText? = null,
    val bottomAction: UpdateAction? = null,
)

fun displayVersion(release: Release): String = "v" + (release.version?.toString() ?: release.tagName.removePrefix("v"))

fun displayVersion(versionName: String): String = if (versionName.isEmpty()) "" else "v" + versionName.removePrefix("v")

private val CheckTitle = UpdateText.Res(R.string.update_check)

private fun availableTitle(release: Release) = UpdateText.Res(R.string.update_available, displayVersion(release))

fun updateRow(state: UpdateUiState): UpdateRow = when (state) {
    UpdateUiState.Idle -> UpdateRow(CheckTitle, UpdateText.WithCurrentVersion(null), UpdateTrailing.Check)
    UpdateUiState.Checking -> UpdateRow(CheckTitle, UpdateText.Res(R.string.update_checking), UpdateTrailing.Spinner)
    UpdateUiState.UpToDate -> UpdateRow(CheckTitle, UpdateText.CurrentVersionLatest, UpdateTrailing.Check)
    is UpdateUiState.Available -> UpdateRow(
        availableTitle(state.release),
        UpdateText.WithCurrentVersion(R.string.update_current_version),
        UpdateTrailing.Download,
    )
    is UpdateUiState.NeedsInstallPermission -> UpdateRow(
        availableTitle(state.release),
        UpdateText.Res(R.string.update_need_permission),
        bottomAction = UpdateAction.OpenSettings,
    )
    is UpdateUiState.Downloading -> downloadingRow(state.percent)
    is UpdateUiState.ReadyToInstall -> downloadingRow(100)
    is UpdateUiState.Failed -> when (state.problem) {
        UpdateProblem.Check -> UpdateRow(CheckTitle, UpdateText.Res(R.string.update_check_failed), UpdateTrailing.Check)
        else -> UpdateRow(
            state.release?.let(::availableTitle) ?: CheckTitle,
            UpdateText.Res(
                when (state.problem) {
                    UpdateProblem.Download -> R.string.update_download_failed
                    UpdateProblem.Verify -> R.string.update_verify_failed
                    else -> R.string.update_install_failed
                },
            ),
            bottomAction = UpdateAction.OpenReleasePage,
        )
    }
}

private fun downloadingRow(percent: Int) = UpdateRow(
    title = UpdateText.Res(R.string.update_downloading, percent),
    subtitle = null,
    progress = percent / 100f,
    hint = UpdateText.Res(R.string.update_download_hint),
)

@Composable
private fun UpdateText.render(currentVersion: String): String = when (this) {
    is UpdateText.Res -> if (arg == null) stringResource(res) else stringResource(res, arg)
    is UpdateText.WithCurrentVersion ->
        if (res == null) displayVersion(currentVersion) else stringResource(res, displayVersion(currentVersion))
    UpdateText.CurrentVersionLatest -> displayVersion(currentVersion) + " · " + stringResource(R.string.update_latest)
}

private fun numeric(size: Int) = SpanStyle(
    fontFamily = PdfBookNumeric.fontFamily,
    fontSize = size.sp,
    letterSpacing = (-0.2).sp,
)

@Composable
fun UpdateCheckBlock(
    state: UpdateUiState,
    currentVersion: String,
    onAction: (UpdateAction) -> Unit,
) {
    val row = updateRow(state)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PdfBookSpacing.md, vertical = PdfBookSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(PdfBookSpacing.xs),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = withNumericSpans(row.title.render(currentVersion), numeric(15)),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                row.subtitle?.let { subtitle ->
                    Text(
                        text = withNumericSpans(subtitle.render(currentVersion), numeric(12)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            when (row.trailing) {
                UpdateTrailing.None -> Unit
                UpdateTrailing.Spinner -> CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    strokeWidth = 2.dp,
                )
                UpdateTrailing.Check -> OutlineAction(UpdateAction.Check, onAction)
                UpdateTrailing.Download -> InkButton(stringResource(UpdateAction.Download.label)) {
                    onAction(UpdateAction.Download)
                }
            }
        }
        row.progress?.let { progress ->
            PdfBookProgressBar(progress = progress, modifier = Modifier.fillMaxWidth())
        }
        row.hint?.let { hint ->
            Text(
                text = hint.render(currentVersion),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        row.bottomAction?.let { action ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (action == UpdateAction.OpenSettings) {
                    InkButton(stringResource(action.label)) { onAction(action) }
                } else {
                    OutlineAction(action, onAction)
                }
            }
        }
    }
}

@Composable
private fun OutlineAction(action: UpdateAction, onAction: (UpdateAction) -> Unit) {
    OutlinedButton(
        onClick = { onAction(action) },
        shape = ButtonCorner,
        colors = pdfBookOutlinedButtonColors(),
        border = BorderStroke(1.dp, MaterialTheme.extendedColors.hairlineStrong),
    ) {
        Text(stringResource(action.label), style = MaterialTheme.typography.labelLarge)
        if (action == UpdateAction.OpenReleasePage) {
            Icon(
                painter = painterResource(R.drawable.ic_open_external),
                contentDescription = null,
                modifier = Modifier
                    .padding(start = PdfBookSpacing.xxs)
                    .size(16.dp),
            )
        }
    }
}

@Composable
fun UpdateBanner(
    version: String,
    onLater: () -> Unit,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val horizontal = if (maxWidth < 600.dp) {
            PdfBookSpacing.screenHorizontalFolded
        } else {
            PdfBookSpacing.screenHorizontalUnfolded
        }
        Surface(
            shape = UpdateBannerCorner,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontal, vertical = PdfBookSpacing.xs),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = PdfBookSpacing.sm, end = PdfBookSpacing.xxs, top = PdfBookSpacing.sm),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PdfBookSpacing.xs),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_download),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = withNumericSpans(stringResource(R.string.update_available, version), numeric(13)),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    BannerButton(R.string.update_later, onLater)
                    BannerButton(R.string.update_download, onDownload)
                }
            }
        }
    }
}

@Composable
private fun BannerButton(@StringRes label: Int, onClick: () -> Unit) {
    TextButton(onClick = onClick, colors = pdfBookTextButtonColors()) {
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
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
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}
