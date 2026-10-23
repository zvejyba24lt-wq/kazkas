package lt.marcomultiservices.kimba.core

import kotlin.random.Random

/**
 * Things other than fish that now and then come up on the line, as every angler knows: an old
 * boot, a crayfish, a sunken rod with a lure still on it, and very rarely a bottle with a letter
 * and a map to a secret cove.
 */
enum class Surprise(val label: L, val note: L, val chance: Double, val coins: Int, val xp: Int) {
    BOOT(
        L("Senas batas", "Alter Stiefel", "Old boot"),
        L("Pilnas dumblo ir su vandens žole. Bent jau ne tuščias užmetimas!", "Voller Schlamm und Kraut. Immerhin kein leerer Wurf!", "Full of mud and weed. At least the cast wasn't empty!"),
        0.012, 0, 5,
    ),
    CRAYFISH(
        L("Plačiažnyplis vėžys", "Edelkrebs", "Noble crayfish"),
        L("Įsikibo į masalą žnyplėmis. Vėžiai mėgsta šiltą vandenį ir naktį.", "Hat sich mit den Scheren am Köder festgehalten. Krebse mögen warmes Wasser und die Nacht.", "Hung on to the bait with its claws. Crayfish like warm water and the night."),
        0.015, 40, 10,
    ),
    SUNKEN_ROD(
        L("Nuskendusi meškerė", "Versunkene Rute", "Sunken rod"),
        L("Kažkas ją pametė seniai, bet ant valo dar kabo masalas.", "Jemand hat sie vor langer Zeit verloren, aber am Schnurende hängt noch ein Köder.", "Someone lost it long ago, but a lure is still tied on."),
        0.006, 400, 20,
    ),
    BOTTLE(
        L("Butelis su laišku", "Flaschenpost", "Message in a bottle"),
        L("Viduje senas žemėlapis su kryželiu prie slaptos įlankos.", "Darin eine alte Karte mit einem Kreuz an einer geheimen Bucht.", "Inside, an old map with a cross by a secret cove."),
        0.005, 0, 50,
    );

    val key: String get() = "surprise:$name"

    companion object {
        /** Lures a sunken rod may carry. */
        val rodLures = listOf(Lure.WOBBLER, Lure.SPINNER, Lure.PILK, Lure.BOILIE)

        /** What else bites on this cast, or null for a fish. Never through the ice. */
        fun roll(c: Conditions, spot: Spot, onIce: Boolean, rng: Random): Surprise? {
            if (onIce) return null
            for (s in entries) {
                val odds = when (s) {
                    CRAYFISH -> if (spot.water == Water.SEA || c.waterTempC < 12) 0.0 else s.chance * (if (c.phase == DayPhase.NIGHT) 2.0 else 1.0)
                    BOTTLE -> if (spot.water == Water.POND) 0.0 else s.chance
                    else -> s.chance
                }
                if (rng.nextDouble() < odds) return s
            }
            return null
        }

        /**
         * Applies [s] to [p]: coins and XP; the sunken rod gives a lure not yet owned (else its
         * coins), the bottle opens the secret cove (a second bottle pays coins instead).
         * Returns the new progress and the lure found, if any.
         */
        fun apply(p: Progress, s: Surprise, rng: Random): Pair<Progress, Lure?> {
            val count = p.surprises[s.name] ?: 0
            var next = p.copy(xp = p.xp + s.xp, surprises = p.surprises + (s.name to count + 1))
            var lure: Lure? = null
            when (s) {
                SUNKEN_ROD -> {
                    lure = rodLures.filter { it.key !in p.owned }.randomOrNull(rng)
                    next = if (lure != null) next.copy(owned = next.owned + lure.key) else next.copy(coins = next.coins + s.coins)
                }
                BOTTLE -> next = if (Destinations.secret.id !in p.trips) next.copy(trips = next.trips + Destinations.secret.id) else next.copy(coins = next.coins + 600)
                else -> next = next.copy(coins = next.coins + s.coins)
            }
            return next to lure
        }
    }
}
