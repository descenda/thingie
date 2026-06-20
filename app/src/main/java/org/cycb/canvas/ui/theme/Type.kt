package org.cycb.canvas.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.unit.sp
import org.cycb.canvas.R

@OptIn(ExperimentalTextApi::class)
val GoogleSansFamily = FontFamily(
    Font(
        resId = R.font.googlesans,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(
            FontVariation.Setting("wght", 400f),
            FontVariation.Setting("opsz", 24f),
            FontVariation.Setting("GRAD", 0f)
        )
    ),
    Font(
        resId = R.font.googlesans,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(
            FontVariation.Setting("wght", 500f),
            FontVariation.Setting("opsz", 24f),
            FontVariation.Setting("GRAD", 0f)
        )
    ),
    Font(
        resId = R.font.googlesans,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(
            FontVariation.Setting("wght", 600f),
            FontVariation.Setting("opsz", 24f),
            FontVariation.Setting("GRAD", 0f)
        )
    ),
    Font(
        resId = R.font.googlesans,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(
            FontVariation.Setting("wght", 700f),
            FontVariation.Setting("opsz", 24f),
            FontVariation.Setting("GRAD", 0f)
        )
    ),
    Font(
        resId = R.font.googlesansitalic,
        weight = FontWeight.Normal,
        style = FontStyle.Italic,
        variationSettings = FontVariation.Settings(
            FontVariation.Setting("wght", 400f),
            FontVariation.Setting("opsz", 24f),
            FontVariation.Setting("GRAD", 0f)
        )
    )
)

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = GoogleSansFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = GoogleSansFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = GoogleSansFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = GoogleSansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = GoogleSansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = GoogleSansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = GoogleSansFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = GoogleSansFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = GoogleSansFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = GoogleSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = GoogleSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = GoogleSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = GoogleSansFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = GoogleSansFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = GoogleSansFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

fun getLargeTypography(base: Typography): Typography {
    return Typography(
        displayLarge = base.displayLarge.copy(fontSize = 62.sp, lineHeight = 70.sp),
        displayMedium = base.displayMedium.copy(fontSize = 50.sp, lineHeight = 58.sp),
        displaySmall = base.displaySmall.copy(fontSize = 40.sp, lineHeight = 48.sp),
        headlineLarge = base.headlineLarge.copy(fontSize = 36.sp, lineHeight = 44.sp),
        headlineMedium = base.headlineMedium.copy(fontSize = 32.sp, lineHeight = 40.sp),
        headlineSmall = base.headlineSmall.copy(fontSize = 28.sp, lineHeight = 36.sp),
        titleLarge = base.titleLarge.copy(fontSize = 26.sp, lineHeight = 32.sp),
        titleMedium = base.titleMedium.copy(fontSize = 20.sp, lineHeight = 28.sp),
        titleSmall = base.titleSmall.copy(fontSize = 18.sp, lineHeight = 24.sp),
        bodyLarge = base.bodyLarge.copy(fontSize = 20.sp, lineHeight = 28.sp),
        bodyMedium = base.bodyMedium.copy(fontSize = 18.sp, lineHeight = 24.sp),
        bodySmall = base.bodySmall.copy(fontSize = 16.sp, lineHeight = 20.sp),
        labelLarge = base.labelLarge.copy(fontSize = 18.sp, lineHeight = 24.sp),
        labelMedium = base.labelMedium.copy(fontSize = 16.sp, lineHeight = 20.sp),
        labelSmall = base.labelSmall.copy(fontSize = 14.sp, lineHeight = 18.sp)
    )
}
