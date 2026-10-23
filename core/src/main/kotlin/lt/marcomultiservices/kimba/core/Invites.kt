package lt.marcomultiservices.kimba.core

import kotlin.random.Random

/**
 * Friend codes that work without a server.
 *
 * Every player has a six-letter code. A newcomer who enters a friend's code (or installs from
 * the friend's link, which carries it) gets a starter gift once. Their game then shows a
 * thank-you code: their own code plus four check letters tied to the friend's code, so only
 * that friend can redeem it, once per newcomer. Rewards are game items only, never money.
 */
object Invites {
    const val NEWCOMER_COINS = 250
    const val NEWCOMER_CASTS = 10
    const val THANKS_COINS = 300
    const val THANKS_CASTS = 5
    /** Friends one player can be thanked for. */
    const val MAX_THANKS = 30
    /** Casts for the first share of a catch each day. */
    const val SHARE_CASTS = 3

    /** No 0/O or 1/I, so a code read aloud or retyped from a picture is not mistaken. */
    private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    private const val SALT = "kimba-ezeras-2026"

    fun newCode(rng: Random): String = String(CharArray(6) { ALPHABET[rng.nextInt(ALPHABET.length)] })

    /** Upper case, without spaces, dashes or anything else that is not part of a code. */
    fun normalize(s: String): String = s.uppercase().filter { it in ALPHABET }

    fun isCode(s: String): Boolean = s.length == 6 && s.all { it in ALPHABET }

    /** The thank-you code a newcomer gives back to the friend who invited them. */
    fun thanks(inviter: String, newcomer: String): String = newcomer + check(inviter, newcomer)

    /** The newcomer behind a thank-you code meant for [mine], or null if it is not one. */
    fun redeem(mine: String, code: String): String? {
        val t = normalize(code)
        if (t.length != 10) return null
        val newcomer = t.take(6)
        if (newcomer == mine || !isCode(newcomer)) return null
        return newcomer.takeIf { check(mine, it) == t.drop(6) }
    }

    /** Four letters from an FNV-1a hash: enough that a guessed code almost never fits. */
    private fun check(inviter: String, newcomer: String): String {
        var h = -0x340d631b7bdddcdbL
        for (c in "$SALT|$inviter|$newcomer") {
            h = h xor c.code.toLong()
            h *= 0x100000001b3L
        }
        return String(CharArray(4) { i -> ALPHABET[((h ushr (i * 5 + 7)) and 31).toInt()] })
    }
}

/** Takes a friend's code once; null if one was taken already or the code is not valid. */
fun Progress.acceptInvite(code: String): Progress? {
    val c = Invites.normalize(code)
    if (invitedBy != null || !Invites.isCode(c) || c == inviteCode) return null
    return copy(invitedBy = c, coins = coins + Invites.NEWCOMER_COINS)
}

/** Pays for a friend's thank-you code; null if it is not for this player or was used. */
fun Progress.thank(code: String): Progress? {
    val newcomer = Invites.redeem(inviteCode, code) ?: return null
    if (newcomer in thanked || thanked.size >= Invites.MAX_THANKS) return null
    return copy(thanked = thanked + newcomer, coins = coins + Invites.THANKS_COINS)
}

/** Marks today's first share; null if the player has shared today already. */
fun Progress.shared(epochDay: Long): Progress? = if (shareDay == epochDay) null else copy(shareDay = epochDay)
