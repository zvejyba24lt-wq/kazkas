package lt.marcomultiservices.skaitaupats.core

import java.security.MessageDigest

object Lt {
    const val ALPHABET = "aąbcčdeęėfghiįyjklmnoprsštuųūvzž"
    const val VOWELS = "aąeęėiįyouųū"

    /** Vowels used to build practice syllables; nasal letters rarely start or end simple syllables. */
    const val SYLLABLE_VOWELS = "aoieuėyū"

    fun isVowel(c: Char) = c.lowercaseChar() in VOWELS

    /** The distinct Lithuanian letters a text uses, ignoring case and punctuation. */
    fun lettersOf(text: String): Set<Char> = text.lowercase().filter { it in ALPHABET }.toSet()

    /** Splits a sentence into its words, keeping punctuation attached so it can be shown as written. */
    fun tokens(sentence: String): List<String> = sentence.split(' ').filter { it.isNotBlank() }

    /** A token without surrounding punctuation, e.g. "„Miau!“" -> "Miau". */
    fun bare(token: String): String = token.trim { !it.isLetter() }
}

/** Every spoken text maps to one bundled recording: assets/audio/<id>.mp3. */
object Audio {
    fun id(text: String): String {
        val digest = MessageDigest.getInstance("SHA-1").digest(text.trim().toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }.take(16)
    }

    fun path(text: String) = "audio/${id(text)}.mp3"
}
