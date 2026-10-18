package org.parkjw.apps.pdfbook.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.parkjw.apps.pdfbook.R
import org.parkjw.apps.pdfbook.ui.theme.PdfBookNumeric
import org.parkjw.apps.pdfbook.ui.theme.PdfBookSpacing
import org.parkjw.apps.pdfbook.ui.theme.withNumericSpans

enum class PinPadLeft { Blank, Fingerprint, Confirm }

@Composable
fun PinPadScreen(
    title: String,
    subtitle: String,
    subtitleIsError: Boolean,
    warning: String?,
    note: String?,
    slotCount: Int,
    filled: Int,
    inputEnabled: Boolean,
    showLockIcon: Boolean,
    left: PinPadLeft,
    leftEnabled: Boolean,
    onDigit: (Char) -> Unit,
    onDelete: () -> Unit,
    onLeft: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val numericStyle = remember {
        SpanStyle(
            fontFamily = PdfBookNumeric.fontFamily,
            fontWeight = PdfBookNumeric.fontWeight,
            fontSize = PdfBookNumeric.fontSize,
            letterSpacing = PdfBookNumeric.letterSpacing,
        )
    }
    val backdrop = remember { MutableInteractionSource() }
    Box(
        modifier
            .fillMaxSize()
            .background(scheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .matchParentSize()
                .clickable(interactionSource = backdrop, indication = null, onClick = {}),
        )
        Column(
            modifier = Modifier
                .widthIn(max = 360.dp)
                .fillMaxWidth()
                .padding(horizontal = PdfBookSpacing.lg, vertical = PdfBookSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (showLockIcon) {
                Box(
                    modifier = Modifier
                        .padding(bottom = PdfBookSpacing.lg)
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(scheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_lock),
                        contentDescription = null,
                        tint = scheme.onSurface,
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = scheme.onSurface,
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = withNumericSpans(subtitle, numericStyle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (subtitleIsError) scheme.error else scheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = PdfBookSpacing.xs),
                )
            }
            if (!note.isNullOrEmpty()) {
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurface,
                    modifier = Modifier.padding(top = PdfBookSpacing.md),
                )
            }
            if (!warning.isNullOrEmpty()) {
                Text(
                    text = warning,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = PdfBookSpacing.md),
                )
            }
            Row(
                modifier = Modifier.padding(top = PdfBookSpacing.xl, bottom = PdfBookSpacing.xl),
                horizontalArrangement = Arrangement.spacedBy(PdfBookSpacing.md),
            ) {
                repeat(slotCount) { index ->
                    val dot = if (index < filled) {
                        Modifier.background(scheme.onSurface, CircleShape)
                    } else {
                        Modifier.border(1.5.dp, scheme.outline, CircleShape)
                    }
                    Box(Modifier.size(12.dp).then(dot))
                }
            }
            Keypad(
                enabled = inputEnabled,
                left = left,
                leftEnabled = leftEnabled && inputEnabled,
                onDigit = onDigit,
                onDelete = onDelete,
                onLeft = onLeft,
            )
        }
    }
}

@Composable
private fun Keypad(
    enabled: Boolean,
    left: PinPadLeft,
    leftEnabled: Boolean,
    onDigit: (Char) -> Unit,
    onDelete: () -> Unit,
    onLeft: () -> Unit,
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("L", "0", "D"),
    )
    Column(verticalArrangement = Arrangement.spacedBy(PdfBookSpacing.md)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(PdfBookSpacing.lg)) {
                row.forEach { key ->
                    when (key) {
                        "L" -> LeftKey(left, leftEnabled, onLeft)
                        "D" -> IconKey(
                            icon = R.drawable.ic_backspace,
                            description = stringResource(R.string.pin_delete),
                            enabled = enabled,
                            onClick = onDelete,
                        )
                        else -> DigitKey(key, enabled) { onDigit(key[0]) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DigitKey(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = PdfBookNumeric.copy(fontSize = 22.sp, lineHeight = 28.sp),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun IconKey(icon: Int, description: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .semantics { contentDescription = description }
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun LeftKey(left: PinPadLeft, enabled: Boolean, onClick: () -> Unit) {
    when (left) {
        PinPadLeft.Blank -> Box(Modifier.size(64.dp))
        PinPadLeft.Fingerprint -> IconKey(
            icon = R.drawable.ic_fingerprint,
            description = stringResource(R.string.app_lock_biometric),
            enabled = enabled,
            onClick = onClick,
        )
        PinPadLeft.Confirm -> IconKey(
            icon = R.drawable.ic_check,
            description = stringResource(R.string.pin_done),
            enabled = enabled,
            onClick = onClick,
        )
    }
}
