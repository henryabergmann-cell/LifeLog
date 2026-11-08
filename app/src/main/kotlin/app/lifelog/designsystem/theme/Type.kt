package app.lifelog.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * The M3 type scale (in sp, so it follows the user's font size), tuned for hierarchy:
 * - Large text gets negative tracking, because letters drift apart as they grow; body text
 *   stays close to zero.
 * - Titles are semibold: hierarchy comes from weight as much as from size.
 * - Labels use tabular figures, so times and counts don't jitter when they change.
 */
private val Base = Typography()

internal val LifeLogTypography = Typography(
    displayLarge = Base.displayLarge.tracked((-0.02).em),
    displayMedium = Base.displayMedium.tracked((-0.02).em),
    displaySmall = Base.displaySmall.tracked((-0.02).em),
    headlineLarge = Base.headlineLarge.tracked((-0.015).em, FontWeight.SemiBold),
    headlineMedium = Base.headlineMedium.tracked((-0.015).em, FontWeight.SemiBold),
    headlineSmall = Base.headlineSmall.tracked((-0.01).em, FontWeight.SemiBold),
    titleLarge = Base.titleLarge.tracked((-0.01).em, FontWeight.SemiBold),
    titleMedium = Base.titleMedium.tracked(0.sp, FontWeight.SemiBold),
    titleSmall = Base.titleSmall.tracked(0.sp, FontWeight.SemiBold),
    bodyLarge = Base.bodyLarge.tracked(0.15.sp),
    bodyMedium = Base.bodyMedium.tracked(0.1.sp),
    bodySmall = Base.bodySmall.tracked(0.2.sp),
    labelLarge = Base.labelLarge.tabular(),
    labelMedium = Base.labelMedium.tabular(),
    labelSmall = Base.labelSmall.tabular(),
)

/** Slightly rounder than the M3 defaults, for a softer, friendlier calendar. */
internal val LifeLogShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

private fun TextStyle.tracked(letterSpacing: TextUnit, weight: FontWeight? = null): TextStyle =
    copy(letterSpacing = letterSpacing, fontWeight = weight ?: fontWeight)

private fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = "tnum")
