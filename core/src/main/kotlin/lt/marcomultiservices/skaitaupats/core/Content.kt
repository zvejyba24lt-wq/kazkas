package lt.marcomultiservices.skaitaupats.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** The hand-written course content in assets/kursas.json. */
@Serializable
data class ContentFile(
    val letters: List<LetterDef>,
    val words: List<WordDef>,
    val sentences: List<String>,
    val stories: List<StoryDef>,
)

@Serializable
data class LetterDef(
    @SerialName("l") val letter: String,
    /** How the letter is named aloud, e.g. "em" for M. */
    val name: String,
    /** Example word (or short phrase) shown and spoken when the letter is introduced. */
    val word: String,
    val emoji: String,
)

@Serializable
data class WordDef(
    /** The word split into syllables with hyphens, e.g. "ka-tė". */
    @SerialName("w") val hyphenated: String,
    val emoji: String? = null,
) {
    val syllables: List<String> get() = hyphenated.split("-")
    val text: String get() = hyphenated.replace("-", "")
}

@Serializable
data class StoryDef(val title: String, val emoji: String, val pages: List<PageDef>)

@Serializable
data class PageDef(@SerialName("t") val text: String, val emoji: String)

object ContentParser {
    private val json = Json { ignoreUnknownKeys = true }
    fun parse(text: String): ContentFile = json.decodeFromString(ContentFile.serializer(), text)
}
