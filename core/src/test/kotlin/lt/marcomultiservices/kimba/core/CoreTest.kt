package lt.marcomultiservices.kimba.core

import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CoreTest {
    private val vilnius = ZoneId.of("Europe/Vilnius")

    private fun at(y: Int, mo: Int, d: Int, h: Int, min: Int = 0) =
        LocalDateTime.of(y, mo, d, h, min).atZone(vilnius).toInstant().toEpochMilli()

    private fun cond(millis: Long, sky: Sky = Sky.PARTLY, temp: Double = 15.0, trend: Double = 0.0, cloud: Double = 0.4) =
        Conditions(Weather(temp, 1013.0, trend, cloud, 3.0, 0.0, sky), millis, 54.69, 25.28, vilnius)

    @Test
    fun sunIsUpAtNoonAndDownAtMidnight() {
        assertTrue(Astro.sunElevation(at(2026, 6, 21, 13, 15), 54.69, 25.28) > 55)
        assertTrue(Astro.sunElevation(at(2026, 6, 21, 1, 0), 54.69, 25.28) < 0)
        assertTrue(Astro.sunElevation(at(2026, 12, 21, 12, 30), 54.69, 25.28) in 8.0..14.0)
    }

    @Test
    fun sunriseInVilniusMatchesAlmanac() {
        // Vilnius, 2026-10-20: sunrise ~08:00, sunset ~18:05 local time.
        val (rise, set) = Astro.sunriseSunset(at(2026, 10, 20, 0), 54.69, 25.28)
        val riseLocal = java.time.Instant.ofEpochMilli(rise!!).atZone(vilnius)
        val setLocal = java.time.Instant.ofEpochMilli(set!!).atZone(vilnius)
        println("sunrise $riseLocal sunset $setLocal")
        assertTrue(riseLocal.hour in 7..8)
        assertTrue(setLocal.hour in 17..18)
    }

    @Test
    fun moonPhaseMatchesKnownFullMoon() {
        // Full moon on 2026-10-26 ~06:12 UTC.
        val full = java.time.Instant.parse("2026-10-26T06:12:00Z").toEpochMilli()
        assertTrue(Astro.moonIllumination(full) > 0.98, "illumination ${Astro.moonIllumination(full)}")
        val new = java.time.Instant.parse("2026-11-09T07:02:00Z").toEpochMilli()
        assertTrue(Astro.moonIllumination(new) < 0.02)
    }

    @Test
    fun phasesOfDay() {
        assertEquals(DayPhase.DAY, cond(at(2026, 7, 10, 13)).phase)
        assertEquals(DayPhase.NIGHT, cond(at(2026, 7, 10, 1, 30)).phase)
        assertEquals(DayPhase.DAWN, cond(at(2026, 10, 20, 7, 50)).phase)
        assertEquals(DayPhase.DUSK, cond(at(2026, 10, 20, 18, 0)).phase)
    }

    @Test
    fun predatorsPreferDuskRainAndFallingPressure() {
        val calm = cond(at(2026, 9, 10, 13), Sky.CLEAR, cloud = 0.0)
        val front = cond(at(2026, 9, 10, 19, 45), Sky.RAIN, trend = -3.0, cloud = 1.0)
        assertTrue(Fish.zander.activity(front) > 4 * Fish.zander.activity(calm))
        assertTrue(Fish.pike.activity(front) > Fish.pike.activity(calm))
        assertTrue(Fish.bleak.activity(calm) > Fish.bleak.activity(front))
    }

    @Test
    fun biteIndexVaries() {
        val summerDawn = cond(at(2026, 6, 15, 4, 30), Sky.PARTLY, temp = 18.0)
        val winterNoon = cond(at(2026, 1, 15, 12), Sky.CLEAR, temp = -6.0, cloud = 0.0)
        println("index summer dawn ${Fish.biteIndex(summerDawn)} winter noon ${Fish.biteIndex(winterNoon)}")
        assertTrue(Fish.biteIndex(summerDawn) > Fish.biteIndex(winterNoon) + 20)
    }

    @Test
    fun goldenFishOnlyUnderTheirCondition() {
        val rng = Random(1)
        val storm = cond(at(2026, 7, 20, 22), Sky.THUNDER, temp = 24.0, trend = -4.0, cloud = 1.0)
        val clear = cond(at(2026, 7, 20, 22), Sky.CLEAR, temp = 24.0, cloud = 0.0)
        val stormBites = List(3000) { BiteEngine.roll(storm, 1.0, rng) }
        val clearBites = List(3000) { BiteEngine.roll(clear, 1.0, rng) }
        assertTrue(stormBites.count { it.golden?.id == "storm_catfish" } > 20)
        assertEquals(0, clearBites.count { it.golden?.id == "storm_catfish" })
        assertTrue(clearBites.all { it.golden == null || it.golden.active(clear) })
    }

    @Test
    fun nearAndFarCastsCatchDifferentFish() {
        val rng = Random(2)
        val c = cond(at(2026, 7, 1, 20), Sky.CLOUDY, temp = 20.0, cloud = 0.9)
        val near = List(2000) { BiteEngine.roll(c, 0.0, rng).species.depth }.average()
        val far = List(2000) { BiteEngine.roll(c, 1.0, rng).species.depth }.average()
        assertTrue(far > near + 0.15, "near $near far $far")
    }

    /** A careful player reels while the line is safe and lets go when it gets tight. */
    private fun play(pull: Double, seed: Int, careful: Boolean): Pair<FightResult, Double> {
        val f = Fight(pull, Random(seed))
        var t = 0.0
        while (f.result == null && t < 120) {
            val reel = if (careful) f.tension < 0.78 && !f.running else true
            f.step(1 / 60.0, reel)
            t += 1 / 60.0
        }
        return (f.result ?: FightResult.ESCAPED) to t
    }

    @Test
    fun everyFishCanBeLandedByCarefulPlay() {
        for (s in Fish.all) {
            for (size in listOf(0.0, 1.0)) {
                val pull = s.power * (0.6 + 0.8 * size)
                val results = (1..40).map { play(pull, it, careful = true) }
                val landed = results.count { it.first == FightResult.LANDED }
                val avg = results.filter { it.first == FightResult.LANDED }.map { it.second }.average()
                println("${s.id} size $size: landed $landed/40 in %.1fs".format(avg))
                assertTrue(landed >= 36, "${s.id} size $size landed only $landed")
                assertTrue(avg < 60)
            }
        }
    }

    @Test
    fun bigStrongFishSnapsWhenReeledBlindly() {
        val results = (1..20).map { play(Fish.catfish.power * 1.4, it, careful = false).first }
        assertTrue(results.count { it == FightResult.SNAPPED } >= 18, results.toString())
        val small = (1..20).map { play(Fish.roach.power * 0.6, it, careful = false).first }
        assertTrue(small.all { it == FightResult.LANDED })
    }

    @Test
    fun baitsRefill() {
        val t0 = 1_000_000_000L
        var b = Baits(Baits.MAX, t0)
        repeat(5) { b = b.use(t0)!! }
        assertEquals(0, b.count)
        assertEquals(null, b.use(t0))
        assertEquals(1, b.now(t0 + Baits.REFILL_MS).count)
        assertEquals(Baits.MAX, b.now(t0 + 10 * Baits.REFILL_MS).count)
        assertEquals(8, Baits(Baits.MAX, t0).add(3, t0).count)
    }

    @Test
    fun parsesMetNorwayForecast() {
        val text = """
        {"type":"Feature","properties":{"timeseries":[
          {"time":"2026-10-20T18:00:00Z","data":{"instant":{"details":{"air_pressure_at_sea_level":1012.0,"air_temperature":7.5,"cloud_area_fraction":96.0,"wind_speed":4.1}},
            "next_1_hours":{"summary":{"symbol_code":"lightrain"},"details":{"precipitation_amount":0.4}}}},
          {"time":"2026-10-20T19:00:00Z","data":{"instant":{"details":{"air_pressure_at_sea_level":1011.0,"air_temperature":7.0,"cloud_area_fraction":100.0,"wind_speed":4.5}},
            "next_1_hours":{"summary":{"symbol_code":"heavyrainandthunder"},"details":{"precipitation_amount":3.1}}}},
          {"time":"2026-10-20T21:00:00Z","data":{"instant":{"details":{"air_pressure_at_sea_level":1009.0,"air_temperature":6.0,"cloud_area_fraction":100.0,"wind_speed":5.0}},
            "next_6_hours":{"summary":{"symbol_code":"fog"},"details":{"precipitation_amount":0.0}}}}
        ]}}
        """.trimIndent()
        val f = Forecast.parse(text)
        assertEquals(3, f.hours.size)
        val w = f.weatherAt(java.time.Instant.parse("2026-10-20T18:30:00Z").toEpochMilli())!!
        assertEquals(Sky.DRIZZLE, w.sky)
        assertEquals(-3.0, w.pressureTrend, 0.01)
        assertEquals(Sky.THUNDER, f.hours[1].sky)
        assertEquals(Sky.FOG, f.hours[2].sky)
    }

    @Test
    fun findsFullMoonWindow() {
        val start = java.time.Instant.parse("2026-10-25T12:00:00Z").toEpochMilli()
        val hours = (0..60).map { i ->
            Forecast.Hour(start + i * 3_600_000L, 8.0, 1013.0, 0.2, 2.0, 0.0, Sky.CLEAR)
        }
        val windows = Windows.golden(Forecast(hours), start, 54.69, 25.28, vilnius)
        windows.forEach { println("${it.golden.id} ${java.time.Instant.ofEpochMilli(it.startMillis)} - ${java.time.Instant.ofEpochMilli(it.endMillis)}") }
        assertTrue(windows.any { it.golden.id == "moon_pike" })
        assertTrue(windows.none { it.golden.id == "storm_catfish" })
    }

    @Test
    fun luresChangeTheCatch() {
        val rng = Random(3)
        val c = cond(at(2026, 7, 1, 5), Sky.CLOUDY, temp = 20.0, cloud = 0.9)
        val corn = List(2000) { BiteEngine.roll(c, 0.4, rng, Lure.CORN).species.id }
        val spin = List(2000) { BiteEngine.roll(c, 0.8, rng, Lure.SPINNER).species.id }
        assertTrue(corn.count { it == "carp" } > spin.count { it == "carp" } * 5)
        assertTrue(spin.count { it == "pike" } > corn.count { it == "pike" } * 5)
    }
}
