package lt.marcomultiservices.skaitaupats.core

import java.io.File

/** Writes every spoken text with its recording id, one "id|text" per line, for the voice workflow. */
fun main(args: Array<String>) {
    val content = ContentParser.parse(File(args[0]).readText())
    val texts = CourseBuilder.build(content).spokenTexts().sorted()
    File(args[1]).parentFile?.mkdirs()
    File(args[1]).writeText(texts.joinToString("\n", postfix = "\n") { "${Audio.id(it)}|$it" })
    println("${texts.size} texts -> ${args[1]}")
}
