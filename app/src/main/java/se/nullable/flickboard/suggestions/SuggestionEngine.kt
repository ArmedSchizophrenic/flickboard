package se.nullable.flickboard.suggestions

import kotlinx.serialization.Serializable
import java.text.Normalizer
import java.util.Locale
import kotlin.math.min

/** Pure local model: word counts plus one- and two-word contexts. */
@Serializable
data class LearnedWords(
    val words: MutableMap<String, Int> = mutableMapOf(),
    val pairs: MutableMap<String, Int> = mutableMapOf(),
    val triples: MutableMap<String, Int> = mutableMapOf(),
    val savedWords: MutableMap<String, String> = mutableMapOf(),
)

data class WordContext(val prefix: String, val previous: List<String>) {
    companion object {
        fun from(beforeCursor: String): WordContext {
            val prefix = beforeCursor.takeLastWhile(::isWordCharacter)
            // Sentence boundaries must not create associations between unrelated sentences.
            val sentence = beforeCursor.dropLast(prefix.length)
                .takeLastWhile { it !in ".!?\n\r;" }
            return WordContext(prefix, WORD.findAll(sentence).map { normalize(it.value) }.toList().takeLast(2))
        }
    }
}

internal fun isWordCharacter(char: Char) = char.isLetter() || char == '\'' || char == '’' ||
    Character.getType(char) == Character.NON_SPACING_MARK.toInt()
private val WORD = Regex("[\\p{L}\\p{M}]+(?:['’][\\p{L}\\p{M}]+)*")
internal fun normalize(word: String) = Normalizer.normalize(word, Normalizer.Form.NFC)
    .lowercase(Locale.ROOT).replace('’', '\'')
private fun folded(word: String) = Normalizer.normalize(normalize(word).replace('ł', 'l'), Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "")

class SuggestionEngine(seedWords: List<String>, val learned: LearnedWords = LearnedWords()) {
    private val dictionary = seedWords.map(::normalize).distinct()
        .withIndex().associate { it.value to ((seedWords.size - it.index) * 1000 / seedWords.size.coerceAtLeast(1)).coerceAtLeast(1) }
    private val foldedDictionary = dictionary.keys.associateWith(::folded)
    private val phoneticDictionary = dictionary.keys.associateWith { folded(it.replace('ó', 'u')) }

    fun addWord(word: String): Boolean {
        val normalized = normalize(word)
        if (!WORD.matches(normalized)) return false
        return learned.savedWords.put(normalized, word) != word
    }

    fun removeWord(word: String): Boolean {
        val normalized = normalize(word)
        if (learned.savedWords.remove(normalized) == null) return false
        learned.words.remove(normalized)
        learned.pairs.keys.removeAll { normalized in it.split(' ') }
        learned.triples.keys.removeAll { normalized in it.split(' ') }
        return true
    }

    fun learn(completedWord: String, previous: List<String>) {
        val word = normalize(completedWord)
        if (word.length !in 1..40 || !WORD.matches(word)) return
        fun increment(map: MutableMap<String, Int>, key: String, limit: Int) {
            map[key] = ((map[key] ?: 0) + 1).coerceAtMost(1_000_000)
            if (map.size > limit) map.remove(map.minBy { it.value }.key)
        }
        increment(learned.words, word, 5000)
        val context = previous.takeLast(2).map(::normalize)
        context.lastOrNull()?.let { increment(learned.pairs, "$it $word", 10000) }
        if (context.size == 2) increment(learned.triples, "${context.joinToString(" ")} $word", 15000)
    }

    fun suggest(context: WordContext, currentWord: String = context.prefix): List<String> {
        val literal = currentWord.takeIf { context.prefix.isNotEmpty() && WORD.matches(normalize(it)) }
        if (context.prefix.length > 40) return listOfNotNull(literal)
        val prefix = normalize(context.prefix)
        val foldedPrefix = folded(prefix)
        val phoneticPrefix = folded(prefix.replace('ó', 'u'))
        val previous = context.previous.map(::normalize)
        fun contextScore(word: String): Long {
            val pair = previous.lastOrNull()?.let { learned.pairs["$it $word"] } ?: 0
            val triple = if (previous.size == 2) learned.triples["${previous.joinToString(" ")} $word"] ?: 0 else 0
            fun weight(count: Int, scale: Int) = (scale * count.toDouble() / (count + 1.0)).toLong()
            return (if (triple > 0) 500_000L + weight(triple, 100_000) else 0L) +
                (if (pair > 0) 100_000L + weight(pair, 50_000) else 0L) +
                weight(learned.words[word] ?: 0, 10_000)
        }
        val ranked = (dictionary.keys + learned.words.keys + learned.savedWords.keys).mapNotNull { word ->
            val foldedWord = foldedDictionary[word] ?: folded(word)
            val matchScore = when {
                prefix.isEmpty() -> 0
                word.startsWith(prefix) -> 4_000_000L
                // Polish ó/u confusion, e.g. gura → góra.
                (phoneticDictionary[word] ?: folded(word.replace('ó', 'u'))) == phoneticPrefix -> 3_500_000L
                foldedWord.startsWith(foldedPrefix) -> 3_000_000L
                prefix.length < 3 -> return@mapNotNull null
                else -> {
                    val distance = editDistance(foldedPrefix, foldedWord)
                    val limit = if (prefix.length >= 6) 2 else 1
                    if (distance > limit) return@mapNotNull null
                    2_000_000L - distance * 100_000L
                }
            }
            word to (matchScore + contextScore(word).coerceAtMost(900_000) +
                (if (prefix.isNotEmpty() && word in learned.savedWords) 10_000 else 0) + (dictionary[word] ?: 0))
        }.sortedWith(compareByDescending<Pair<String, Long>> { it.second }.thenBy { it.first })
            .map { applyCase(learned.savedWords[it.first] ?: it.first, context.prefix) }
            .filter { literal == null || normalize(it) != normalize(literal) }
            .distinct().take(if (literal != null) 2 else 3)
        return listOfNotNull(literal) + ranked
    }

    private fun applyCase(word: String, prefix: String): String = when {
        prefix.length > 1 && prefix.all { it.isUpperCase() } -> word.uppercase(Locale.ROOT)
        prefix.firstOrNull()?.isUpperCase() == true -> word.replaceFirstChar { it.titlecase(Locale.ROOT) }
        word == "i" -> "I"
        else -> word
    }
}

/** Optimal string alignment distance, including adjacent transpositions. */
internal fun editDistance(a: String, b: String): Int {
    if (kotlin.math.abs(a.length - b.length) > 2) return 3
    val rows = Array(a.length + 1) { IntArray(b.length + 1) }
    for (i in 0..a.length) rows[i][0] = i
    for (j in 0..b.length) rows[0][j] = j
    for (i in 1..a.length) for (j in 1..b.length) {
        rows[i][j] = min(min(rows[i - 1][j] + 1, rows[i][j - 1] + 1),
            rows[i - 1][j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1)
        if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) {
            rows[i][j] = min(rows[i][j], rows[i - 2][j - 2] + 1)
        }
    }
    return rows[a.length][b.length]
}
