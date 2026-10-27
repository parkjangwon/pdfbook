package org.parkjw.apps.pdfbook.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import org.parkjw.apps.pdfbook.R

val PdfBookSansFamily = FontFamily(
    Font(R.font.pdfbook_sans_regular, FontWeight.Normal),
    Font(R.font.pdfbook_sans_semibold, FontWeight.SemiBold),
)

val PdfBookMonoFamily = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_semibold, FontWeight.SemiBold),
)

val PdfBookNumeric = TextStyle(
    fontFamily = PdfBookMonoFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    letterSpacing = (-0.2).sp,
)

private fun pretendard(
    size: TextUnit,
    weight: FontWeight,
    letterSpacing: TextUnit = 0.sp,
    lineHeight: TextUnit,
) = TextStyle(
    fontFamily = PdfBookSansFamily,
    fontWeight = weight,
    fontSize = size,
    lineHeight = lineHeight,
    letterSpacing = letterSpacing,
)

val PdfBookTypography = Typography(
    displayLarge = pretendard(57.sp, FontWeight.Normal, lineHeight = 64.sp),
    displayMedium = pretendard(45.sp, FontWeight.Normal, lineHeight = 52.sp),
    displaySmall = pretendard(36.sp, FontWeight.Normal, lineHeight = 44.sp),
    headlineLarge = pretendard(32.sp, FontWeight.Normal, lineHeight = 40.sp),
    headlineMedium = pretendard(28.sp, FontWeight.Normal, lineHeight = 36.sp),
    headlineSmall = pretendard(24.sp, FontWeight.Normal, (-0.5).sp, 32.sp),
    titleLarge = pretendard(20.sp, FontWeight.Normal, (-0.3).sp, 28.sp),
    titleMedium = pretendard(16.sp, FontWeight.SemiBold, lineHeight = 24.sp),
    titleSmall = pretendard(14.sp, FontWeight.SemiBold, lineHeight = 20.sp),
    bodyLarge = pretendard(15.sp, FontWeight.Normal, lineHeight = 22.sp),
    bodyMedium = pretendard(14.sp, FontWeight.Normal, lineHeight = 20.sp),
    bodySmall = pretendard(12.sp, FontWeight.Normal, lineHeight = 16.sp),
    labelLarge = pretendard(14.sp, FontWeight.SemiBold, lineHeight = 20.sp),
    labelMedium = pretendard(12.sp, FontWeight.SemiBold, lineHeight = 16.sp),
    labelSmall = pretendard(11.sp, FontWeight.SemiBold, 0.4.sp, 16.sp),
)
