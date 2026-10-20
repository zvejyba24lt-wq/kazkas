package lt.marcomultiservices.kimba.ui.scene

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import lt.marcomultiservices.kimba.core.Conditions
import lt.marcomultiservices.kimba.core.Sky

/** The light of the scene, derived from the real sun height, clouds and weather. */
class SceneColors(c: Conditions) {
    private class Stop(val elev: Double, val top: Color, val horizon: Color)

    private val stops = listOf(
        Stop(-18.0, Color(0xFF060A18), Color(0xFF0C142C)),
        Stop(-10.0, Color(0xFF0B1433), Color(0xFF232C55)),
        Stop(-5.0, Color(0xFF1C2B60), Color(0xFF9C5A72)),
        Stop(-1.0, Color(0xFF2F4A88), Color(0xFFEE8A4E)),
        Stop(3.0, Color(0xFF4770B2), Color(0xFFF5BE78)),
        Stop(10.0, Color(0xFF3D7DCC), Color(0xFFBCDBF0)),
        Stop(35.0, Color(0xFF2B6BC2), Color(0xFFA6D2F4)),
    )

    private fun at(e: Double): Pair<Color, Color> {
        if (e <= stops.first().elev) return stops.first().top to stops.first().horizon
        if (e >= stops.last().elev) return stops.last().top to stops.last().horizon
        val i = stops.indexOfLast { it.elev <= e }
        val a = stops[i]
        val b = stops[i + 1]
        val f = ((e - a.elev) / (b.elev - a.elev)).toFloat()
        return lerp(a.top, b.top, f) to lerp(a.horizon, b.horizon, f)
    }

    private fun smooth(e0: Double, e1: Double, x: Double): Float {
        val t = ((x - e0) / (e1 - e0)).coerceIn(0.0, 1.0)
        return (t * t * (3 - 2 * t)).toFloat()
    }

    val elev = c.sunElevation
    /** 0 at night, 1 in full daylight. */
    val daylight: Float = smooth(-10.0, 8.0, elev)
    /** Golden hour warmth, strongest with the sun at the horizon. */
    val warmth: Float = (1f - kotlin.math.abs(elev.toFloat() - 1f) / 9f).coerceIn(0f, 1f)
    val cloud: Float = c.weather.cloud.toFloat()
    val storm: Boolean = c.weather.sky == Sky.THUNDER
    val fog: Float = when (c.weather.sky) {
        Sky.FOG -> 1f
        Sky.DRIZZLE, Sky.RAIN -> 0.3f
        Sky.HEAVY_RAIN, Sky.THUNDER -> 0.45f
        Sky.SNOW, Sky.SLEET -> 0.5f
        else -> 0f
    }
    val rain: Float = c.rain.toFloat()
    val snow: Float = when (c.weather.sky) { Sky.SNOW -> 1f; Sky.SLEET -> 0.5f; else -> 0f }
    val wind: Float = c.weather.windMs.toFloat()

    private val overcast: Float = (cloud * cloud * 0.85f + rain * 0.15f).coerceIn(0f, 0.95f)

    val skyTop: Color
    val skyHorizon: Color

    init {
        val (top, hor) = at(elev)
        val greyTop = lerp(Color(0xFF12151C), Color(0xFF79838F), daylight)
        val greyHor = lerp(Color(0xFF1E222A), Color(0xFFAAB1B9), daylight)
        var t = lerp(top, greyTop, overcast)
        // A low sun still glows through a gap at the horizon under clouds.
        var h = lerp(hor, greyHor, overcast * (1f - warmth * 0.45f))
        if (storm) { t = lerp(t, Color(0xFF0E1218), 0.55f); h = lerp(h, Color(0xFF2A2F38), 0.45f) }
        val fogCol = lerp(Color(0xFF2E333B), Color(0xFFC8CDD2), daylight)
        h = lerp(h, fogCol, fog * 0.85f)
        t = lerp(t, fogCol, fog * 0.45f)
        skyTop = t
        skyHorizon = h
    }

    /** Deep water colour under the reflections. */
    val waterDeep: Color = lerp(lerp(Color(0xFF03070F), Color(0xFF173544), daylight), Color(0xFF1A2026), overcast * 0.4f)

    /** Sun or moon glitter on the water. */
    val glow: Color = when {
        elev > 6 -> Color(0xFFFFF4D6)
        elev > -3 -> Color(0xFFFFB066)
        else -> Color(0xFFCBD8F0)
    }
    val glowStrength: Float = when {
        elev > -3 -> (1f - overcast * 1.05f).coerceIn(0f, 1f) * (0.55f + warmth * 0.45f)
        else -> (c.moonIllumination.toFloat() * 0.6f * (1f - cloud)).coerceIn(0f, 1f)
    }

    /** Overall brightness for the foreground (trees, pier, reeds). */
    val light: Float = (0.12f + 0.88f * daylight) * (1f - overcast * 0.35f) * (if (storm) 0.7f else 1f)

    /** Tints a foreground colour for the current light: darker and bluer at night, warmer at sunset. */
    fun lit(base: Color, extraDark: Float = 0f): Color {
        val l = (light * (1f - extraDark)).coerceIn(0.05f, 1f)
        val night = Color(0xFF0A1220)
        var col = lerp(night, base, l)
        if (warmth > 0f && cloud < 0.8f) col = lerp(col, Color(0xFFB0603A), warmth * 0.18f * (1f - cloud))
        return col
    }

    /** Aerial perspective: distant things fade into the horizon colour. */
    fun far(base: Color, distance: Float): Color = lerp(lit(base), skyHorizon, distance.coerceIn(0f, 1f))

    val starAlpha: Float = ((1f - daylight) * (1f - cloud * 1.1f)).coerceIn(0f, 1f)
}
