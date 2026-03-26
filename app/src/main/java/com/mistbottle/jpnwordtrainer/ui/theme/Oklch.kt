package com.mistbottle.jpnwordtrainer.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * Compose Color helper for OKLCH values.
 *
 * - lightness: 0.0..1.0
 * - chroma: usually 0.0..0.4
 * - hueDegrees: 0.0..360.0
 */
fun oklch(
    lightness: Double,
    chroma: Double,
    hueDegrees: Double,
    alpha: Float = 1f,
): Color {
    val hueRadians = Math.toRadians(hueDegrees)
    val a = chroma * cos(hueRadians)
    val b = chroma * sin(hueRadians)

    val lPrime = lightness + 0.3963377774 * a + 0.2158037573 * b
    val mPrime = lightness - 0.1055613458 * a - 0.0638541728 * b
    val sPrime = lightness - 0.0894841775 * a - 1.2914855480 * b

    val l = lPrime.pow(3.0)
    val m = mPrime.pow(3.0)
    val s = sPrime.pow(3.0)

    val redLinear = +4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s
    val greenLinear = -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s
    val blueLinear = -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s

    return Color(
        red = linearToSrgb(redLinear).toFloat(),
        green = linearToSrgb(greenLinear).toFloat(),
        blue = linearToSrgb(blueLinear).toFloat(),
        alpha = alpha,
    )
}

private fun linearToSrgb(value: Double): Double {
    val clamped = value.coerceIn(0.0, 1.0)
    return if (clamped <= 0.0031308) {
        12.92 * clamped
    } else {
        1.055 * clamped.pow(1.0 / 2.4) - 0.055
    }
}
