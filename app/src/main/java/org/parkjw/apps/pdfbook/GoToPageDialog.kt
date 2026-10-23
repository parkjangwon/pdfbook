package org.parkjw.apps.pdfbook

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.parkjw.apps.pdfbook.ui.theme.ButtonCorner
import org.parkjw.apps.pdfbook.ui.theme.DialogCorner
import org.parkjw.apps.pdfbook.ui.theme.PdfBookNumeric
import org.parkjw.apps.pdfbook.ui.theme.ReaderOnOverlay
import org.parkjw.apps.pdfbook.ui.theme.ReaderOnOverlayVariant
import org.parkjw.apps.pdfbook.ui.theme.ReaderOverlay
import org.parkjw.apps.pdfbook.ui.theme.ReaderTrack
import org.parkjw.apps.pdfbook.ui.theme.pdfBookTextButtonColors
import org.parkjw.apps.pdfbook.ui.theme.withNumericSpans

const val GO_TO_PAGE_DIALOG_TAG = "go_to_page_dialog"
const val GO_TO_PAGE_FIELD_TAG = "go_to_page_field"

fun parsePageInput(input: String, pageCount: Int): Int? {
    if (pageCount <= 0) return null
    val text = input.trim()
    if (text.isEmpty() || text.any { !it.isDigit() }) return null
    return text.toIntOrNull()?.takeIf { it in 1..pageCount }
}

@Composable
fun GoToPageDialog(
    pageCount: Int,
    onDismiss: () -> Unit,
    onGo: (Int) -> Unit,
) {
    var input by remember { mutableStateOf("") }
    val page = parsePageInput(input, pageCount)
    Surface(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .widthIn(max = 360.dp)
            .fillMaxWidth()
            .testTag(GO_TO_PAGE_DIALOG_TAG)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            ),
        shape = DialogCorner,
        color = ReaderOverlay,
        border = BorderStroke(1.dp, ReaderTrack),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
    ) {
        Column(Modifier.padding(24.dp)) {
            Text(
                text = stringResource(R.string.go_to_page_title),
                style = MaterialTheme.typography.titleLarge,
                color = ReaderOnOverlay,
            )
            OutlinedTextField(
                value = input,
                onValueChange = { value -> input = value.filter { it.isDigit() } },
                singleLine = true,
                isError = page == null,
                textStyle = PdfBookNumeric.copy(fontSize = 16.sp, color = ReaderOnOverlay),
                placeholder = {
                    Text(
                        text = withNumericSpans(
                            stringResource(R.string.go_to_page_hint, pageCount),
                            SpanStyle(fontFamily = PdfBookNumeric.fontFamily),
                        ),
                        style = PdfBookNumeric.copy(fontSize = 14.sp, color = ReaderOnOverlayVariant),
                    )
                },
                supportingText = if (page == null) {
                    {
                        Text(
                            text = stringResource(R.string.go_to_page_error, pageCount),
                            color = ReaderOnOverlayVariant,
                        )
                    }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Go,
                ),
                keyboardActions = KeyboardActions(onGo = { page?.let(onGo) }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = ReaderOnOverlay,
                    unfocusedTextColor = ReaderOnOverlay,
                    errorTextColor = ReaderOnOverlay,
                    cursorColor = ReaderOnOverlay,
                    errorCursorColor = ReaderOnOverlay,
                    focusedBorderColor = ReaderOnOverlay,
                    unfocusedBorderColor = ReaderTrack,
                    errorBorderColor = ReaderOnOverlay,
                    focusedPlaceholderColor = ReaderOnOverlayVariant,
                    unfocusedPlaceholderColor = ReaderOnOverlayVariant,
                    errorPlaceholderColor = ReaderOnOverlayVariant,
                    errorSupportingTextColor = ReaderOnOverlayVariant,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .testTag(GO_TO_PAGE_FIELD_TAG),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = onDismiss,
                    colors = pdfBookTextButtonColors(contentColor = ReaderOnOverlay),
                ) {
                    Text(stringResource(R.string.go_to_page_cancel))
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { page?.let(onGo) },
                    enabled = page != null,
                    shape = ButtonCorner,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ReaderOnOverlay,
                        contentColor = ReaderOverlay,
                        disabledContainerColor = ReaderTrack,
                        disabledContentColor = ReaderOnOverlayVariant,
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp,
                        focusedElevation = 0.dp,
                        hoveredElevation = 0.dp,
                        disabledElevation = 0.dp,
                    ),
                ) {
                    Text(stringResource(R.string.go_to_page_go))
                }
            }
        }
    }
}
