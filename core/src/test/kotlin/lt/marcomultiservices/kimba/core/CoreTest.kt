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
        repeat(Baits.MAX) { b = b.use(t0)!! }
        assertEquals(0, b.count)
        assertEquals(null, b.use(t0))
        assertEquals(1, b.now(t0 + Baits.REFILL_MS).count)
        assertEquals(Baits.MAX, b.now(t0 + 2 * Baits.MAX * Baits.REFILL_MS).count)
        assertEquals(Baits.MAX + 3, Baits(Baits.MAX, t0).add(3, t0).count)
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
        val corn = List(2000) { BiteEngine.roll(c, 0.4, rng, gear = Gear(lure = Lure.CORN)).species.id }
        val spin = List(2000) { BiteEngine.roll(c, 0.8, rng, gear = Gear(lure = Lure.SPINNER)).species.id }
        assertTrue(corn.count { it == "carp" } > spin.count { it == "carp" } * 5)
        assertTrue(spin.count { it == "pike" } > corn.count { it == "pike" } * 5)
    }

    @Test
    fun eachWaterHasItsOwnFish() {
        val c = cond(at(2026, 9, 10, 19), Sky.CLOUDY, cloud = 0.8)
        val rng = Random(5)
        val sea = List(1500) { BiteEngine.roll(c, 0.6, rng, Spot(Water.SEA)).species }.toSet()
        val lake = List(1500) { BiteEngine.roll(c, 0.6, rng, Spot.HOME_LAKE).species }.toSet()
        assertTrue(Fish.cod in sea && Fish.cod !in lake)
        assertTrue(Fish.tench in lake && Fish.tench !in sea)
        assertTrue(Fish.whitefish !in lake)
        val bodensee = List(1500) { BiteEngine.roll(c, 0.8, rng, Destinations.byId("bodensee")!!.spot).species }.toSet()
        assertTrue(Fish.whitefish in bodensee)
        for (w in Water.entries) println("$w: ${Fish.all.count { it.livesAt(Spot(w)) }} species, index ${Fish.biteIndex(c, Spot(w))}")
        // Every golden fish lives somewhere the player can fish.
        assertTrue(Fish.goldens.all { g -> Water.entries.any { g.base.livesAt(Spot(it)) } })
    }

    @Test
    fun strongerGearHoldsBigFish() {
        val pull = Fish.catfish.power * 1.4
        fun landed(strength: Double): Int = (1..40).count { seed ->
            val f = Fight(pull, Random(seed), strength)
            var t = 0.0
            while (f.result == null && t < 120) {
                // Careful player: reels unless the line is close to the limit.
                f.step(0.016, f.tension < 0.85)
                t += 0.016
            }
            f.result == FightResult.LANDED
        }
        val basic = landed(Rod.BASIC.strength)
        val legend = landed(Rod.LEGEND.strength + Line.THICK.strength)
        println("big catfish landed: basic $basic/40, legend $legend/40")
        assertTrue(legend >= basic)
    }

    @Test
    fun levelsCoinsAndShop() {
        assertEquals(1, Levels.levelOf(0))
        assertEquals(2, Levels.levelOf(100))
        assertEquals(10, Levels.levelOf(Levels.xpFor(10)))
        var p = Progress()
        assertEquals(null, p.buy(Lure.SPINNER)) // level 2 needed
        p = p.copy(xp = Levels.xpFor(2), coins = 1000)
        p = p.buy(Lure.SPINNER)!!
        // A spinner does not go on a float rod.
        assertEquals(Lure.WORM, p.lure)
        assertEquals(Lure.WORM, p.gear.lure)
        assertEquals(600, p.coins)
        assertEquals(null, p.buy(Lure.SPINNER))
        p = p.copy(coins = 1500).buy(Tackle.SPIN)!!
        assertEquals(Tackle.SPIN, p.tackle)
        assertEquals(Lure.SPINNER, p.gear.lure)
        p = p.equip(Lure.WORM)
        assertEquals(Tackle.FLOAT, p.tackle)
        assertEquals(Lure.WORM, p.gear.lure)
        p = p.copy(coins = 1000)
        assertTrue(Economy.coins(Fish.pike, null, 4000) > Economy.coins(Fish.roach, null, 200) * 10)
        val nemunas = Destinations.byId("nemunas")!!
        assertEquals(null, p.travel(nemunas))
        p = p.copy(xp = Levels.xpFor(3), coins = 2000)
        p = p.travel(nemunas)!!
        assertEquals(500, p.coins)
        assertEquals(Water.RIVER, p.spot.water)
        p = p.goHome(Water.SEA) // level 6 needed for the sea at home
        assertEquals(Spot(Water.LAKE), p.spot)
        assertEquals(nemunas.id, p.travel(nemunas)!!.trip) // second visit is free
        assertEquals(500, p.travel(nemunas)!!.coins)
    }

    @Test
    fun questsAndStreak() {
        val q = Quests.generate(20000)
        assertEquals(q, Quests.generate(20000))
        assertEquals(3, q.list.size)
        var p = Progress()
        var day = 20000L
        var done = 0
        repeat(12) {
            val c = Catch("roach", null, 30.0, 1500, 0, Sky.CLEAR, 10.0, "Vilnius", Water.LAKE)
            val (next, reward) = Economy.land(p, c, false, day)
            p = next
            done += reward.quests.size
        }
        assertTrue(done >= 1)
        assertTrue(p.coins > 150)
        val (p1, b1) = Streak.visit(p, day)
        assertEquals(1, b1!!.day)
        assertEquals(null, Streak.visit(p1, day).second)
        val (p2, b2) = Streak.visit(p1, day + 1)
        assertEquals(2, b2!!.day)
        assertEquals(1, Streak.visit(p2, day + 5).second!!.day)
    }

    @Test
    fun everySpeciesHasQuestPlural() {
        for (sp in Fish.all) assertTrue(Fish.plural(sp.id) != null, sp.id)
        val q = Quest(QuestKind.SPECIES, 3, species = "rudd", coins = 150)
        assertEquals("Sugauk 3 raudes", q.text(Lang.LT))
    }

    @Test
    fun trophiesAreRare() {
        val rng = Random(7)
        val sizes = List(20000) { BiteEngine.sizeFraction(rng.nextDouble(), 5.5) }
        val median = sizes.sorted()[sizes.size / 2]
        val trophies = sizes.count { it > 0.8 } / sizes.size.toDouble()
        assertTrue(median in 0.09..0.18, "median $median")
        assertTrue(trophies in 0.005..0.025, "trophies $trophies")
        assertTrue(sizes.all { it in 0.0..1.0 })
    }

    @Test
    fun weightsMatchRealFish() {
        // Typical weights at a given length from angling length-weight tables (grams).
        val table = listOf(
            Triple(Fish.roach, 25.0, 160..260), Triple(Fish.perch, 30.0, 300..460), Triple(Fish.pike, 80.0, 3400..4600),
            Triple(Fish.pike, 100.0, 7000..9000), Triple(Fish.bream, 50.0, 1500..2200), Triple(Fish.carp, 70.0, 5000..7200),
            Triple(Fish.catfish, 150.0, 21000..30000), Triple(Fish.zander, 70.0, 2700..3700), Triple(Fish.tench, 40.0, 850..1250),
            Triple(Fish.eel, 80.0, 750..1150), Triple(Fish.cod, 60.0, 1900..2700), Triple(Fish.trout, 40.0, 620..880),
            Triple(Fish.crucian, 25.0, 330..480), Triple(Fish.burbot, 50.0, 800..1100), Triple(Fish.herring, 25.0, 100..160),
        )
        for ((s, cm, range) in table) assertTrue(s.grams(cm) in range, "${s.id} ${cm} cm = ${s.grams(cm)} g, expected $range")
    }

    @Test
    fun fishLeftAloneGetsOff() {
        val f = Fight(Fish.pike.power * 1.2, Random(3))
        var t = 0.0
        while (f.result == null && t < 60) { f.step(1 / 60.0, false); t += 1 / 60.0 }
        assertEquals(FightResult.ESCAPED, f.result)
        assertTrue(t <= IDLE_LIMIT + 0.1)
    }

    @Test
    fun fishOnlyBiteWhereTheyLive() {
        val seaOnly = setOf(Fish.cod, Fish.flounder, Fish.herring, Fish.seaTrout, Fish.garfish, Fish.turbot)
        val spots = Water.entries.map { Spot(it) } + Destinations.all.map { it.spot }
        val skies = listOf(Sky.CLEAR, Sky.RAIN, Sky.THUNDER, Sky.SNOW, Sky.FOG)
        val rng = Random(11)
        for (spot in spots) for ((i, sky) in skies.withIndex()) {
            val c = cond(at(2026, 1 + i * 2, 15, 4 + i * 4), sky, temp = -2.0 + i * 7, cloud = 0.2 * i)
            for (lure in Lure.entries) repeat(150) {
                val b = BiteEngine.roll(c, rng.nextDouble(), rng, spot, Gear(lure = lure, boat = rng.nextBoolean()))
                assertTrue(b.species.livesAt(spot), "${b.species.id} bit at $spot")
                assertTrue(b.golden == null || b.golden!!.base == b.species)
                if (spot.water != Water.SEA) assertTrue(b.species !in seaOnly, "${b.species.id} at $spot")
                if (spot.water == Water.SEA) assertTrue(b.species in seaOnly, "${b.species.id} at sea")
            }
        }
    }

    @Test
    fun questsCanBeDoneWithOpenWaters() {
        for (level in 1..20) for (day in 20000L..20060L) {
            val open = Quests.watersFor(level)
            for (q in Quests.generate(day, level).list) {
                q.water?.let { assertTrue(it in open, "level $level quest in $it") }
                q.species?.let { id ->
                    val s = Fish.byId(id)!!
                    assertTrue(open.any { s.livesAt(Spot(it)) }, "level $level quest for $id")
                }
            }
        }
    }

    @Test
    fun achievementsPayOnceAndUnlocksMoveOn() {
        val c = Catch("pike", null, 70.0, 2500, 0L, Sky.THUNDER, 15.0, "x", Water.RIVER)
        val p0 = Progress(coins = 0)
        val (p1, got) = Achievements.check(p0, listOf(c))
        assertTrue(Achievement.FIRST_FISH in got && Achievement.STORM in got && Achievement.RAIN in got && Achievement.KILO in got)
        assertTrue(Achievement.FIVE_KILO !in got)
        assertEquals(got.sumOf { it.coins }, p1.coins)
        assertTrue(Achievements.check(p1, listOf(c)).second.isEmpty())
        var level = 1
        while (true) {
            val n = Unlocks.next(level) ?: break
            assertTrue(n.level > level && n.names.isNotEmpty())
            level = n.level
        }
        assertTrue(level >= 15)
    }

    @Test
    fun oldSavesWithSpinnersGetASpinningRod() {
        val old = Progress(owned = setOf("lure:WORM", "lure:SPINNER", "rod:BASIC"), lure = Lure.SPINNER)
        val m = old.migrated()
        assertTrue(m.owns(Tackle.SPIN) && m.owns(Tackle.FLOAT))
        assertEquals(Tackle.SPIN, m.tackle)
        assertEquals(Lure.SPINNER, m.gear.lure)
        assertEquals(m, m.migrated())
        assertEquals(Progress(), Progress().migrated())
    }

    @Test
    fun aFishHookedCloseNeedsLessWinding() {
        val far = Fight(0.3, Random(1))
        val near = Fight(0.3, Random(1), start = 0.35)
        assertEquals(1.0, far.distance, 1e-9)
        assertEquals(0.35, near.distance, 1e-9)
        var steps = 0
        while (near.result == null && steps < 10_000) { near.step(0.05, reeling = near.tension < 0.85); steps++ }
        var farSteps = 0
        while (far.result == null && farSteps < 10_000) { far.step(0.05, reeling = far.tension < 0.85); farSteps++ }
        assertTrue(steps < farSteps)
    }

    @Test
    fun steeringAgainstTheFishTiresIt() {
        fun play(steerAgainst: Boolean): Fight {
            val f = Fight(0.6, Random(7))
            repeat(200) { f.step(0.05, reeling = f.tension < 0.8, rod = if (steerAgainst) -f.heading else f.heading) }
            return f
        }
        val good = play(true)
        val bad = play(false)
        assertTrue(good.stamina < bad.stamina)
        assertTrue(good.result == FightResult.LANDED || good.distance < bad.distance)
    }

    @Test
    fun theNetTakesOnlyACalmFishInFront() {
        val f = Fight(0.2, Random(5), start = 0.05)
        var steps = 0
        while (f.result == null && steps < 20_000) { f.step(0.05, reeling = f.tension < 0.8, rod = -f.heading); steps++ }
        assertEquals(FightResult.LANDED, f.result)
        assertTrue(f.netReady)
    }

    @Test
    fun friendCodesWorkOnceAndOnlyForTheirOwner() {
        val rng = Random(7)
        val rob = Progress(inviteCode = Invites.newCode(rng))
        val friend = Progress(inviteCode = Invites.newCode(rng))
        assertTrue(Invites.isCode(rob.inviteCode))
        // The newcomer takes the code once, typed loosely, but never their own.
        val joined = friend.acceptInvite(rob.inviteCode.lowercase().chunked(3).joinToString("-"))!!
        assertEquals(friend.coins + Invites.NEWCOMER_COINS, joined.coins)
        assertEquals(null, joined.acceptInvite(rob.inviteCode))
        assertEquals(null, friend.acceptInvite(friend.inviteCode))
        assertEquals(null, friend.acceptInvite("ABC"))
        // The thank-you code pays the inviter once, and nobody else.
        val thanks = Invites.thanks(rob.inviteCode, friend.inviteCode)
        val paid = rob.thank(thanks)!!
        assertEquals(rob.coins + Invites.THANKS_COINS, paid.coins)
        assertEquals(null, paid.thank(thanks))
        assertEquals(null, friend.thank(thanks))
        val forged = thanks.dropLast(1) + (if (thanks.last() == 'A') 'B' else 'A')
        assertEquals(null, rob.thank(forged))
        // Sharing pays once a day.
        val day = 20_000L
        val s = rob.shared(day)!!
        assertEquals(null, s.shared(day))
        assertTrue(s.shared(day + 1) != null)
    }

    @Test
    fun iceGrowsInFrostAndMeltsInThaw() {
        var log = IceLog(0.0, 100)
        // A week at -5 makes walkable ice on a lake; a pond freezes sooner, a river never.
        log = Ice.update(log, 107, -5.0, 0.0, 1)
        assertTrue(log.cm in 13.0..17.0, "cm=${log.cm}")
        assertTrue(Ice.frozen(Water.LAKE, log))
        assertTrue(!Ice.frozen(Water.RIVER, log))
        assertTrue(Ice.frozen(Water.POND, IceLog(8.5, 1)) && !Ice.frozen(Water.LAKE, IceLog(8.5, 1)))
        // The same day changes nothing; thaw and rain take it away.
        assertEquals(log, Ice.update(log, 107, 20.0, 9.0, 1))
        val thaw = Ice.update(log, 110, 4.0, 3.0, 3)
        assertTrue(thaw.cm < 1.0, "cm=${thaw.cm}")
        // A first start in deep midwinter frost begins on ice, in autumn on open water.
        assertTrue(Ice.update(IceLog(), 5, -8.0, 0.0, 1).cm >= Ice.SAFE_CM)
        assertEquals(0.0, Ice.update(IceLog(), 5, 6.0, 0.0, 10).cm)
        // On the ice the rod is a float rod and carp hardly bite.
        val p = Progress(ice = IceLog(20.0, 1), lure = Lure.BOILIE, owned = Shop.starter + Lure.BOILIE.key + Tackle.FEEDER.key, tackle = Tackle.FEEDER)
        assertTrue(p.onIce)
        assertEquals(Tackle.FLOAT, p.gearHere.tackle)
        assertEquals(Lure.WORM, p.gearHere.lure)
        assertTrue(Ice.affinity(Fish.byId("carp")!!) < 0.1)
    }

    @Test
    fun weatherStampsComeFromTheRealWeather() {
        val zone = ZoneId.of("Europe/Vilnius")
        val noon = LocalDateTime.of(2026, 1, 10, 12, 0).atZone(zone).toInstant().toEpochMilli()
        val midnight = LocalDateTime.of(2026, 1, 10, 23, 30).atZone(zone).toInstant().toEpochMilli()
        fun c(sky: Sky, at: Long, ice: Boolean = false) = Catch("perch", null, 20.0, 100, at, sky, -5.0, "", ice = ice)
        assertEquals(setOf(Stamp.SUN), Stamp.of(c(Sky.CLEAR, noon), zone))
        assertEquals(setOf(Stamp.NIGHT), Stamp.of(c(Sky.CLEAR, midnight), zone))
        assertEquals(setOf(Stamp.SNOW, Stamp.ICE), Stamp.of(c(Sky.SNOW, noon, ice = true), zone))
        assertEquals(setOf(Stamp.RAIN), Stamp.of(c(Sky.THUNDER, noon), zone))
    }

    @Test
    fun dailyChallengeKeepsTheBestFishOfTheDay() {
        val day = LocalDateTime.of(2026, 7, 4, 12, 0).toLocalDate().toEpochDay()
        val s = Challenge.species(day, 7)
        assertEquals(s, Challenge.species(day, 7))
        fun fish(cm: Double) = Catch(s.id, null, cm, s.grams(cm), 0, Sky.CLEAR, 18.0, "")
        val first = Challenge.record(ChallengeLog(), day, 7, fish(s.minCm + 2.0))!!
        assertEquals(null, Challenge.record(first, day, 7, fish(s.minCm + 1.0)))
        val better = Challenge.record(first, day, 7, fish(s.maxCm.toDouble()))!!
        assertEquals(5, Challenge.boxes(better, s))
        assertEquals("🟩🟩🟩⬜⬜", Challenge.bar(3))
        // The first fish of the day pays the bonus once.
        val (p1, r1) = Economy.land(Progress(), fish(s.minCm + 2.0), true, day)
        val (_, r2) = Economy.land(p1, fish(s.maxCm.toDouble()), false, day)
        assertTrue(r1.challenge && r2.challenge)
        assertTrue(r1.coins - Economy.coins(s, null, s.grams(s.minCm + 2.0)) >= Challenge.COINS)
        // In winter the kind is one that bites under the ice.
        for (d in 0L until 40L) assertTrue(Ice.affinity(Challenge.species(day + d, 1)) >= 0.8)
    }

    @Test
    fun aLegendBitesOnlyWhenItsConditionsMeetAndOnce() {
        val pike = Fish.legends.first { it.id == "legend_pike" }
        // Find an evening in Vilnius at dusk.
        var dusk = LocalDateTime.of(2026, 10, 20, 16, 0).atZone(vilnius).toInstant().toEpochMilli()
        while (cond(dusk).phase != DayPhase.DUSK) dusk += 10 * 60_000L
        val right = cond(dusk, Sky.CLOUDY, trend = -2.0, cloud = 0.9)
        val wrong = cond(dusk, Sky.CLEAR, trend = -2.0, cloud = 0.1)
        assertTrue(pike.active(right) && !pike.active(wrong))
        val rng = Random(3)
        val bites = List(2000) { BiteEngine.roll(right, 0.5, rng, Spot.HOME_LAKE) }
        val legends = bites.count { it.golden == pike }
        assertTrue(legends in 60..200, "legends=$legends")
        assertTrue(bites.first { it.golden == pike }.cm > Fish.pike.maxCm)
        // Once caught, never again; never on a trip or another water.
        assertTrue(List(2000) { BiteEngine.roll(right, 0.5, rng, Spot.HOME_LAKE, legendsCaught = setOf(pike.id)) }.none { it.golden == pike })
        assertTrue(List(2000) { BiteEngine.roll(right, 0.5, rng, Spot(Water.POND)) }.none { it.golden?.legend == true && it.golden != Fish.legends.first { l -> l.legendOf == Water.POND } })
        assertTrue(!Fish.gold(pike.id) && Fish.gold("moon_pike"))
        // Clues come one a day.
        val p = Progress(cluesFrom = 100)
        assertEquals(1, p.clues(100)); assertEquals(3, p.clues(105))
    }

    @Test
    fun campPiecesAreBoughtOnce() {
        val p = Progress(coins = 1000, xp = Levels.xpFor(3))
        val bought = p.buyCamp(CampItem.FIRE)!!
        assertEquals(600, bought.coins)
        assertEquals(null, bought.buyCamp(CampItem.FIRE))
        assertEquals(null, bought.buyCamp(CampItem.CABIN))
        assertEquals(1, bought.campCount())
    }

    @Test
    fun stormIsWarnedAndPaysDouble() {
        val start = 1_800_000_000_000L
        val hours = List(12) { i -> Forecast.Hour(start + i * 3_600_000L, 20.0, 1005.0, 0.9, 5.0, 1.0, if (i == 3) Sky.THUNDER else Sky.CLOUDY) }
        val f = Forecast(hours)
        assertEquals(start + 3 * 3_600_000L, Storm.ahead(f, start))
        assertEquals(null, Storm.ahead(f, start, hours = 2))
        assertEquals(null, Storm.ahead(f, start + 4 * 3_600_000L))
        val c = Catch("perch", null, 30.0, 400, start, Sky.CLOUDY, 20.0, "x")
        val day = 20_000L
        val (_, calm) = Economy.land(Progress(), c, false, day)
        val (_, storm) = Economy.land(Progress(), c.copy(sky = Sky.THUNDER), false, day)
        assertTrue(storm.storm && !calm.storm)
        assertTrue(storm.coins >= calm.coins + Economy.coins(Fish.byId("perch")!!, null, 400))
    }
}
