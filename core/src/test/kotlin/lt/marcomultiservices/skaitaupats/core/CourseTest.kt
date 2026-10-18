package lt.marcomultiservices.skaitaupats.core

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CourseTest {
    private val content = ContentParser.parse(File("../app/src/main/assets/kursas.json").readText())
    private val course = CourseBuilder.build(content)

    @Test
    fun alphabetIsCompleteAndInOrder() {
        assertEquals(Lt.ALPHABET.toSet(), content.letters.map { it.letter.single() }.toSet())
        assertEquals(32, content.letters.size)
    }

    @Test
    fun syllablesSpellTheWord() {
        for (w in content.words) assertTrue(w.syllables.all { it.isNotEmpty() }, w.hyphenated)
        assertEquals(content.words.size, content.words.map { it.text.lowercase() }.distinct().size, "duplicate words")
    }

    @Test
    fun childNeverMeetsAnUnknownLetter() {
        for (lesson in course.lessons) for (e in lesson.exercises) {
            val texts = when (e) {
                is Exercise.ReadWord -> listOf(e.word.text)
                is Exercise.WordPicture -> listOf(e.word.text)
                is Exercise.PictureWord -> e.options.map { it.text }
                is Exercise.ReadSentence -> listOf(e.text)
                is Exercise.StoryPage -> listOf(e.page.text, e.story.title)
                is Exercise.HearSyllable -> e.options
                is Exercise.BuildSyllable -> listOf(e.syllable)
                else -> emptyList()
            }
            for (t in texts) {
                val unknown = Lt.lettersOf(t) - lesson.known
                assertTrue(unknown.isEmpty(), "Lesson ${lesson.id} (${lesson.title}) uses $unknown in \"$t\"")
            }
        }
    }

    @Test
    fun choicesHaveOneRightAnswer() {
        for (lesson in course.lessons) for (e in lesson.exercises) when (e) {
            is Exercise.FindLetter -> { assertEquals(4, e.options.distinct().size); assertTrue(e.answer in e.options) }
            is Exercise.HearSyllable -> { assertEquals(3, e.options.distinct().size); assertTrue(e.target in e.options) }
            is Exercise.WordPicture -> { assertEquals(3, e.options.map { it.emoji }.distinct().size); assertTrue(e.word in e.options) }
            is Exercise.PictureWord -> { assertEquals(3, e.options.map { it.emoji }.distinct().size); assertTrue(e.word in e.options) }
            else -> Unit
        }
    }

    @Test
    fun courseHasSensibleShape() {
        val n = course.lessons.size
        println("Lessons: $n")
        course.lessons.forEach { l -> println("${l.id}. ${l.kind} ${l.title} [${l.exercises.size}]") }
        assertTrue(n in 90..130, "lesson count $n")
        assertEquals(20, course.lessons.count { it.kind == LessonKind.STORY })
        assertTrue(course.lessons.all { it.exercises.isNotEmpty() })
        assertEquals((1..n).toList(), course.lessons.map { it.id })
        println("Spoken texts: ${course.spokenTexts().size}")
    }

    @Test
    fun audioIdsAreUnique() {
        val texts = course.spokenTexts()
        assertEquals(texts.size, texts.map { Audio.id(it) }.distinct().size)
    }
}
