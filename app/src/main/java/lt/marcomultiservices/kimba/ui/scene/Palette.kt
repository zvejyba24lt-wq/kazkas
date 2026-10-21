package lt.marcomultiservices.kimba.ui.scene

import androidx.compose.ui.graphics.Color
import lt.marcomultiservices.kimba.core.FloatStyle

/**
 * Every fixed colour of the lake scene, by name. Change a colour here and it changes
 * everywhere it is used. These are daylight base colours: the layers pass them through
 * [SceneColors.lit] (darker at night, warmer at sunset) or [SceneColors.far] (fades into
 * the horizon haze), so do not pre-darken them. Sky and water colours that follow the sun
 * live in [SceneColors].
 */
object Palette {
    // ---- Sky ----------------------------------------------------------------------------
    val SunDisc = Color(0xFFFFF7E6)
    val MoonHalo = Color(0xFFDCE6FF)
    val MoonDark = Color(0xFF2A3348)
    val MoonLit = Color(0xFFF1F0E6)
    val CloudNight = Color(0xFF262B36)
    val CloudDay = Color(0xFFF4F6F8)
    val CloudShade = Color(0xFF8A93A0)
    val CloudStorm = Color(0xFF5A6370)
    val CloudSunset = Color(0xFFF6B080)
    val Lightning = Color(0xFFE8EEFF)
    val LightningGlow = Color(0x66BFD0FF)
    val Star = Color.White

    // ---- Land ---------------------------------------------------------------------------
    /** Far hills on the horizon. */
    val HillFar = Color(0xFF2D4A3A)
    /** Forest line across the water. */
    val ForestFar = Color(0xFF1B3626)
    /** Near shore forest, its shaded foot. */
    val ForestNear = Color(0xFF14291C)
    /** Sunlit tree tops of the near shore. */
    val ForestNearLit = Color(0xFF2E5236)
    /** River bank: bare earth and the grass on top of it. */
    val BankEarth = Color(0xFF6B5034)
    val BankEarthDark = Color(0xFF3E2C1C)
    val BankGrass = Color(0xFF4C7A34)
    val Willow = WillowColors(
        trunk = Color(0xFF2E2418),
        far = Color(0xFF2D4A3A),
        dark = Color(0xFF2C4A22),
        mid = Color(0xFF6E9A44),
        light = Color(0xFF9CC46A),
    )

    class WillowColors(val trunk: Color, val far: Color, val dark: Color, val mid: Color, val light: Color)

    // ---- Sea coast ----------------------------------------------------------------------
    val Dunes = Color(0xFF4A5A50)
    val LighthouseWall = Color(0xFFE8E4DC)
    val LighthouseBand = Color(0xFFB83A2A)
    val LighthouseLantern = Color(0xFF2A2A2A)
    val LighthouseBeam = Color(0xCCFFF2C0)
    val Ship = Color(0xFF2A3038)
    val BuoyRed = Color(0xFFD8402E)
    val BuoyWhite = Color(0xFFF2F0EA)

    // ---- On the water -------------------------------------------------------------------
    val MistNight = Color(0xFF3A4048)
    val MistDay = Color(0xFFE6E9EC)
    val LilyLight = Color(0xFF4F8A3A)
    val LilyDark = Color(0xFF2D5A26)
    val LilyRim = Color(0xFF7DB05A)
    val LilyPetal = Color(0xFFFFF4F6)
    val LilyHeart = Color(0xFFF7D24A)
    val StoneLight = Color(0xFF8A8A80)
    val StoneDark = Color(0xFF3A3A34)
    val StoneMoss = Color(0xFF4E6A3A)
    val LeafOrange = Color(0xFFC88A2A)
    val LeafBrown = Color(0xFF9A5A22)
    val GullNight = Color(0xFF20242A)
    val GullDay = Color(0xFFF4F4F0)
    val Foam = Color.White

    // ---- Pier (lake, pond, river): dark stained boards ----------------------------------
    val Pier = PierColors(
        boardFar = Color(0xFF6E5136),
        boardNear = Color(0xFF9A7650),
        seam = Color(0xFF2A1E14),
        edgeLight = Color(0xFFC8A274),
        grain = Color(0xFF4A3524),
        side = Color(0xFF35261A),
        post = Color(0xFF3B2A1C),
        nail = Color(0xFF2A2420),
        rail = Color(0xFF3B2A1C),
    )

    /** Palanga style sea pier: wide, silver-grey weathered boards and a white railing. */
    val SeaPier = PierColors(
        boardFar = Color(0xFF7A746A),
        boardNear = Color(0xFFA89E8E),
        seam = Color(0xFF3A362F),
        edgeLight = Color(0xFFD8D0C2),
        grain = Color(0xFF5E584E),
        side = Color(0xFF4A453D),
        post = Color(0xFF3E3A34),
        nail = Color(0xFF34302A),
        rail = Color(0xFFE4E0D8),
    )

    class PierColors(
        val boardFar: Color, val boardNear: Color, val seam: Color, val edgeLight: Color, val grain: Color,
        val side: Color, val post: Color, val nail: Color, val rail: Color,
    )

    val LampPost = Color(0xFF2A2E33)
    val LampLight = Color(0xFFFFE2A8)

    // Props on the pier.
    val TinDark = Color(0xFF4E5E68)
    val TinMid = Color(0xFF5B6E7A)
    val TinLight = Color(0xFFB8C6CE)
    val TinSoil = Color(0xFF6A4A32)

    // ---- Reeds ---------------------------------------------------------------------------
    val ReedBack = Color(0xFF2E4A24)
    val ReedFront = Color(0xFF3F6230)
    val ReedShine = Color(0xFF8DB06A)
    val ReedHead = Color(0xFF5E3B22)
    val ReedHeadShine = Color(0xFF8A5C38)

    // ---- Tackle --------------------------------------------------------------------------
    val RodCork = Color(0xFF8E6A47)
    val RodBlank = Color(0xFF1E2228)
    val RodShine = Color(0xFF6D7A88)
    val RodGuide = Color(0xFFB9C2CA)
    val ReelBody = Color(0xFF2A2F36)
    val ReelRim = Color(0xFF9AA5B0)
    val ReelKnob = Color(0xFFD2B04A)
    val Line = Color(0xCCF2F0EA)
    val LineStrain = Color(0xFFFF6A50)
    /** Feeder rod: the bright quiver tip, the cage, the rod rest and the bite alarm. */
    val QuiverTip = Color(0xFFFF7A2E)
    val QuiverBand = Color(0xFFF4F1E8)
    val FeederCage = Color(0xFF6E7781)
    val FeederLead = Color(0xFF3A3F45)
    val RodRest = Color(0xFF5C4128)
    val AlarmBody = Color(0xFF1C1F24)
    val AlarmLed = Color(0xFFFF3B2E)
    /** Spinning lure. */
    val Spoon = Color(0xFFE8ECEF)
    val SpoonShade = Color(0xFF8E98A2)
    val SpoonRed = Color(0xFFD8402E)
    val Aim = Color(0xFFFFC56B)
    val GlowFloatHalo = Color(0xAAB8FF6A)

    /** Float colours per style: body light and shaded side, tip light and shaded side. */
    class FloatColors(val body: Color, val bodyShade: Color, val tip: Color, val tipShade: Color)

    val Floats = mapOf(
        FloatStyle.CLASSIC to FloatColors(Color(0xFFF7F3EA), Color(0xFFCFC8BA), Color(0xFFF2533F), Color(0xFFB72D24)),
        FloatStyle.FEATHER to FloatColors(Color(0xFFE8DCC0), Color(0xFFA89470), Color(0xFFF0A030), Color(0xFFB86A18)),
        FloatStyle.GLOW to FloatColors(Color(0xFF2A3A30), Color(0xFF141C18), Color(0xFFB8FF6A), Color(0xFF6ACC2A)),
        FloatStyle.GOLD to FloatColors(Color(0xFFFFE9A0), Color(0xFFC89030), Color(0xFFFFF4D0), Color(0xFFE0A830)),
    )

    // ---- Fish effects -------------------------------------------------------------------
    val FishShadow = Color(0xFF071010)
    val FightShadow = Color(0xFF0B1410)
    val Spray = Color.White

    // ---- Weather and insects --------------------------------------------------------------
    val Rain = Color.White
    val Snow = Color.White
    val FireflyGlow = Color(0xCCE8FF8A)
    val FireflyCore = Color(0xFFF6FFD0)
    val Dragonfly = Color(0xFF1E5A7A)
    val DragonflyWing = Color(0xFFDDEEFF)
    val StormFlash = Color(0xFFDDE6FF)
    val Vignette = Color(0x66000000)
}
