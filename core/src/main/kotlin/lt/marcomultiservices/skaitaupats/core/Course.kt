package lt.marcomultiservices.skaitaupats.core

import kotlin.random.Random

enum class LessonKind { LETTER, READING, STORY }

data class Lesson(
    val id: Int,
    val kind: LessonKind,
    val title: String,
    /** What the lesson bubble on the map shows: a letter pair like "Mm", or an emoji. */
    val badge: String,
    val exercises: List<Exercise>,
    /** Letters the child can read after this lesson. */
    val known: Set<Char>,
    val letter: LetterDef? = null,
)

sealed interface Exercise {
    /** What is said when the exercise appears. */
    val prompt: List<String>

    data class LetterIntro(val letter: LetterDef) : Exercise {
        override val prompt get() = listOf(Phrases.intro(letter))
    }

    data class FindLetter(val letter: LetterDef, val upper: Boolean, val options: List<String>) : Exercise {
        val answer: String get() = if (upper) letter.letter.uppercase() else letter.letter
        override val prompt get() = listOf(Phrases.find(letter))
    }

    data class BuildSyllable(val first: String, val second: String) : Exercise {
        val syllable: String get() = first + second
        override val prompt get() = listOf(Phrases.BUILD)
    }

    data class HearSyllable(val target: String, val options: List<String>) : Exercise {
        override val prompt get() = listOf(Phrases.HEAR, target)
    }

    data class ReadWord(val word: WordDef) : Exercise {
        override val prompt get() = listOf(Phrases.READ_WORD)
    }

    data class WordPicture(val word: WordDef, val options: List<WordDef>) : Exercise {
        override val prompt get() = listOf(Phrases.WORD_PICTURE)
    }

    data class PictureWord(val word: WordDef, val options: List<WordDef>) : Exercise {
        override val prompt get() = listOf(Phrases.PICTURE_WORD)
    }

    data class ReadSentence(val text: String) : Exercise {
        override val prompt get() = listOf(Phrases.READ_SENTENCE)
    }

    data class StoryPage(val story: StoryDef, val index: Int) : Exercise {
        val page: PageDef get() = story.pages[index]
        override val prompt get() = if (index == 0) listOf(story.title) else emptyList()
    }
}

/** Fixed spoken phrases. Each is recorded once, like any other text. */
object Phrases {
    const val BUILD = "Sujunk raides."
    const val HEAR = "Kurį skiemenį girdi?"
    const val READ_WORD = "Perskaityk žodį."
    const val WORD_PICTURE = "Perskaityk ir surask paveikslėlį."
    const val PICTURE_WORD = "Kuris žodis tinka paveikslėliui?"
    const val READ_SENTENCE = "Perskaityk sakinį."
    const val TRY_AGAIN = "Pabandyk dar kartą."
    const val LESSON_DONE = "Pamoka baigta! Puikiai padirbėjai!"
    const val WELCOME = "Labas! Skaitome kartu?"
    const val LOCKED = "Šią pamoką atrakins tėveliai."
    const val TOMORROW = "Šiandien jau daug išmokai! Naujos pamokos bus rytoj. O senas gali kartoti."
    val PRAISE = listOf("Puiku!", "Šaunu!", "Teisingai!", "Labai gerai!")

    fun intro(l: LetterDef): String =
        if (l.word.endsWith(".")) "Raidė ${l.name}. ${l.word}" else "Raidė ${l.name}, kaip ${l.word}."

    fun find(l: LetterDef) = "Surask raidę ${l.name}."

    val fixed: List<String> get() = listOf(BUILD, HEAR, READ_WORD, WORD_PICTURE, PICTURE_WORD, READ_SENTENCE, TRY_AGAIN, LESSON_DONE, WELCOME, LOCKED, TOMORROW) + PRAISE
}

class Course(val lessons: List<Lesson>) {
    /** Every text the app can say; each needs a recording. */
    fun spokenTexts(): Set<String> {
        val out = linkedSetOf<String>()
        out += Phrases.fixed
        for (lesson in lessons) for (e in lesson.exercises) {
            out += e.prompt
            when (e) {
                is Exercise.LetterIntro -> Unit
                is Exercise.FindLetter -> Unit
                is Exercise.BuildSyllable -> out += e.syllable
                is Exercise.HearSyllable -> out += e.options
                is Exercise.ReadWord -> { out += e.word.syllables.map { it.lowercase() }; out += e.word.text }
                is Exercise.WordPicture -> out += e.word.text
                is Exercise.PictureWord -> out += e.word.text
                is Exercise.ReadSentence -> { out += e.text; out += wordsOf(e.text) }
                is Exercise.StoryPage -> { out += e.page.text; out += wordsOf(e.page.text) }
            }
        }
        return out.filter { it.isNotBlank() }.toSet()
    }

    companion object {
        /** The individual words of a sentence as they are spoken when tapped. */
        fun wordsOf(sentence: String): List<String> = Lt.tokens(sentence).map { Lt.bare(it) }.filter { it.isNotEmpty() }
    }
}

/**
 * Turns the content into an ordered course. Letters come one per lesson; a word, sentence or
 * story appears only once every letter in it has been taught, so the child never meets an
 * unknown letter.
 */
object CourseBuilder {
    private const val WORDS_IN_LETTER_LESSON = 2

    fun build(content: ContentFile): Course {
        val lessons = mutableListOf<Lesson>()
        val known = linkedSetOf<Char>()
        val pendingWords = ArrayDeque<WordDef>()
        val pendingSentences = ArrayDeque<String>()
        val remainingWords = content.words.toMutableList()
        val remainingSentences = content.sentences.toMutableList()
        val remainingStories = content.stories.toMutableList()
        val readWords = mutableListOf<WordDef>()
        val syllablePool = mutableListOf<String>()

        fun readable(text: String) = Lt.lettersOf(text).all { it in known }
        fun nextId() = lessons.size + 1

        fun readingLesson(force: Boolean) {
            if (pendingWords.isEmpty() && pendingSentences.isEmpty()) return
            if (!force && pendingWords.size < 3 && pendingSentences.size < 2) return
            val id = nextId()
            val rnd = Random(id * 7919)
            val words = buildList { repeat(minOf(4, pendingWords.size)) { add(pendingWords.removeFirst()) } }
            val sentences = buildList { repeat(minOf(2, pendingSentences.size)) { add(pendingSentences.removeFirst()) } }
            val ex = mutableListOf<Exercise>()
            words.forEachIndexed { i, w ->
                ex += Exercise.ReadWord(w)
                pictureExercise(w, readWords, rnd, preferWordChoice = i % 2 == 1)?.let { ex += it }
            }
            // Revisit an older picture word so earlier lessons keep being practised.
            readWords.filter { it.emoji != null && it !in words }.shuffled(rnd).firstOrNull()?.let { old ->
                pictureExercise(old, readWords, rnd, preferWordChoice = true)?.let { ex += it }
            }
            sentences.forEach { ex += Exercise.ReadSentence(it) }
            lessons += Lesson(id, LessonKind.READING, "Skaitome", "📖", ex, known.toSet())
        }

        content.letters.forEachIndexed { index, def ->
            val ch = def.letter.single()
            known += ch
            val id = nextId()
            val rnd = Random(id * 7919)
            val ex = mutableListOf<Exercise>()
            ex += Exercise.LetterIntro(def)
            val others = content.letters.map { it.letter }.filter { it != def.letter }
            val lookAlikes = content.letters.take(index).map { it.letter }.filter { it != def.letter }
            fun options(upper: Boolean): List<String> {
                val pool = (lookAlikes.shuffled(rnd) + others.shuffled(rnd)).distinct().take(3)
                return (pool + def.letter).map { if (upper) it.uppercase() else it }.shuffled(rnd)
            }
            ex += Exercise.FindLetter(def, upper = false, options = options(false))
            ex += Exercise.FindLetter(def, upper = true, options = options(true))

            val newSyllables = syllablesWith(ch, known).shuffled(rnd).take(3)
            newSyllables.forEach { ex += Exercise.BuildSyllable(it.first, it.second) }
            syllablePool += newSyllables.map { it.first + it.second }
            newSyllables.firstOrNull()?.let { (a, b) ->
                val target = a + b
                val distractors = syllablePool.filter { it != target }.distinct().shuffled(rnd).take(2)
                if (distractors.size == 2) ex += Exercise.HearSyllable(target, (distractors + target).shuffled(rnd))
            }

            val unlocked = remainingWords.filter { readable(it.text) }
            remainingWords -= unlocked.toSet()
            unlocked.take(WORDS_IN_LETTER_LESSON).forEach { w ->
                ex += Exercise.ReadWord(w)
                readWords += w
                pictureExercise(w, readWords, rnd, preferWordChoice = false)?.let { ex += it }
            }
            unlocked.drop(WORDS_IN_LETTER_LESSON).forEach { pendingWords += it; readWords += it }
            remainingSentences.filter { readable(it) }.also { remainingSentences -= it.toSet(); pendingSentences += it }

            val pair = def.letter.uppercase() + def.letter
            lessons += Lesson(id, LessonKind.LETTER, "Raidė ${def.letter.uppercase()}", pair, ex, known.toSet(), def)

            readingLesson(force = false)
            remainingStories.filter { s -> s.pages.all { readable(it.text) } && readable(s.title) }.forEach { story ->
                readingLesson(force = true)
                remainingStories -= story
                val sid = nextId()
                lessons += Lesson(sid, LessonKind.STORY, story.title, story.emoji, story.pages.indices.map { Exercise.StoryPage(story, it) }, known.toSet())
            }
        }
        while (pendingWords.isNotEmpty() || pendingSentences.isNotEmpty()) readingLesson(force = true)
        check(remainingWords.isEmpty()) { "Words never readable: ${remainingWords.map { it.text }}" }
        check(remainingSentences.isEmpty()) { "Sentences never readable: $remainingSentences" }
        check(remainingStories.isEmpty()) { "Stories never readable: ${remainingStories.map { it.title }}" }
        return Course(lessons)
    }

    /** Consonant-vowel syllables that use the new letter and only known letters. */
    private fun syllablesWith(ch: Char, known: Set<Char>): List<Pair<String, String>> {
        val vowels = Lt.SYLLABLE_VOWELS.filter { it in known }
        val consonants = known.filter { !Lt.isVowel(it) }
        return if (Lt.isVowel(ch)) {
            if (ch !in Lt.SYLLABLE_VOWELS) emptyList() else consonants.map { it.toString() to ch.toString() }
        } else {
            vowels.map { ch.toString() to it.toString() }
        }
    }

    private fun pictureExercise(word: WordDef, pool: List<WordDef>, rnd: Random, preferWordChoice: Boolean): Exercise? {
        if (word.emoji == null) return null
        val others = pool.filter { it.emoji != null && it.emoji != word.emoji && it.text != word.text }.distinctBy { it.emoji }.shuffled(rnd).take(2)
        if (others.size < 2) return null
        val options = (others + word).shuffled(rnd)
        return if (preferWordChoice) Exercise.PictureWord(word, options) else Exercise.WordPicture(word, options)
    }
}

object Stars {
    fun forMistakes(mistakes: Int) = when {
        mistakes == 0 -> 3
        mistakes <= 2 -> 2
        else -> 1
    }
}
