package se.nullable.flickboard.suggestions

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class SuggestionEngineTest {
    private fun bundled(language: String) = SuggestionEngine(
        File("src/main/assets/suggestions-$language.txt").readLines().filter { it.isNotBlank() },
    )

    @Test fun polishCorrectionsUseBundledDictionary() {
        val engine = bundled("pl")
        for ((typed, expected) in listOf("gura" to "góra", "zolw" to "żółw", "ksiazka" to "książka")) {
            val result = engine.suggest(WordContext.from(typed))
            assertTrue("$typed → $result should include $expected", expected in result)
            assertTrue(result.size in 1..3)
        }
    }

    @Test fun englishCompletionAndTypo() {
        val engine = bundled("en")
        assertTrue("hello" in engine.suggest(WordContext.from("hell")))
        assertTrue("hello" in engine.suggest(WordContext.from("hlelo")))
        assertEquals("don't", WordContext.from("I don't ").previous.last())
        assertEquals("don't", WordContext.from("I don’t ").previous.last())
    }

    @Test fun learnsThreeMostLikelyNextWords() {
        val engine = SuggestionEngine(listOf("nie", "tak", "to"))
        repeat(6) { engine.learn("domu", listOf("do")) }
        repeat(4) { engine.learn("pracy", listOf("do")) }
        repeat(2) { engine.learn("szkoły", listOf("do")) }
        engine.learn("sklepu", listOf("do"))
        assertEquals(listOf("domu", "pracy", "szkoły"), engine.suggest(WordContext.from("do ")))
    }

    @Test fun twoWordContextOutranksSingleWordContext() {
        val engine = SuggestionEngine(listOf("nie", "tak"))
        repeat(10) { engine.learn("pracy", listOf("do")) }
        engine.learn("domu", listOf("wracam", "do"))
        assertEquals("domu", engine.suggest(WordContext.from("wracam do ")).first())
    }

    @Test fun learnedWordsCompleteAndSurviveSerialization() {
        val engine = SuggestionEngine(emptyList())
        engine.learn("Flickboard", emptyList())
        val saved = Json.encodeToString(engine.learned)
        val restored = SuggestionEngine(emptyList(), Json.decodeFromString<LearnedWords>(saved))
        assertEquals(listOf("Flickboard"), restored.suggest(WordContext.from("Fli")))
    }

    @Test fun preservesCapitalization() {
        val engine = SuggestionEngine(listOf("góra"))
        assertEquals(listOf("Góra"), engine.suggest(WordContext.from("Gura")))
        assertEquals(listOf("GÓRA"), engine.suggest(WordContext.from("GURA")))
    }

    @Test fun sentenceBoundariesAndCompletedWordDetection() {
        assertEquals(WordContext("gura", listOf("to", "jest")), WordContext.from("To jest gura"))
        assertEquals(WordContext("", listOf("to", "jest")), WordContext.from("To jest "))
        assertEquals(WordContext("", emptyList()), WordContext.from("To jest. "))
        assertEquals(WordContext("", emptyList()), WordContext.from("To jest\n"))
    }

    @Test fun ignoresNonWordsAndCapsHistory() {
        val engine = SuggestionEngine(emptyList())
        engine.learn("12345", emptyList())
        engine.learn("x".repeat(41), emptyList())
        assertTrue(engine.learned.words.isEmpty())
        for (i in 0..5100) {
            val letters = i.toString().map { 'a' + (it - '0') }.joinToString("")
            engine.learn("word$letters", emptyList())
        }
        assertEquals(5000, engine.learned.words.size)
    }

    @Test fun transposedLettersCountAsOneTypo() {
        assertEquals(1, editDistance("hlelo", "hello"))
        assertEquals(1, editDistance("gura", "gora"))
    }
}
