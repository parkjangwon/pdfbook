package org.parkjw.apps.pdfbook.ui.theme

import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun pdfBookTextButtonColors(
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
) = ButtonDefaults.textButtonColors(contentColor = contentColor)

@Composable
fun pdfBookOutlinedButtonColors(
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
) = ButtonDefaults.outlinedButtonColors(contentColor = contentColor)

@Composable
fun pdfBookIconButtonColors(
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
) = IconButtonDefaults.iconButtonColors(contentColor = contentColor)
