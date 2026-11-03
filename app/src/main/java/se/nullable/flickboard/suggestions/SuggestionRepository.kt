package se.nullable.flickboard.suggestions

import android.content.Context
import android.util.AtomicFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/** All model work and disk access runs off the input/UI thread, serialized by [mutex]. */
class SuggestionRepository private constructor(context: Context) {
    private val app = context.applicationContext
    private fun file(language: SuggestionLanguage) = AtomicFile(File(app.noBackupFilesDir, "learned-words-${language.code}.json"))
    private val mutex = Mutex()
    private val models = mutableMapOf<SuggestionLanguage, SuggestionEngine>()

    private fun engine(language: SuggestionLanguage): SuggestionEngine = models[language] ?: run {
        val file = file(language)
        val seed = app.assets.open("suggestions-${language.code}.txt").bufferedReader().use { it.readText() }
            .split(Regex("\\s+")).filter { it.isNotBlank() }
        val learned = try {
            Json.decodeFromString<LearnedWords>(file.openRead().bufferedReader().use { it.readText() })
        } catch (_: java.io.IOException) {
            LearnedWords()
        } catch (_: IllegalArgumentException) {
            LearnedWords()
        }
        SuggestionEngine(seed, learned).also { models[language] = it }
    }

    suspend fun suggest(language: SuggestionLanguage, context: WordContext): List<String> = withContext(Dispatchers.IO) {
        mutex.withLock { engine(language).suggest(context) }
    }

    suspend fun learn(language: SuggestionLanguage, word: String, previous: List<String>) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val file = file(language)
            engine(language).learn(word, previous)
            var stream: java.io.FileOutputStream? = null
            try {
                stream = file.startWrite()
                stream.write(Json.encodeToString(engine(language).learned).toByteArray(Charsets.UTF_8))
                file.finishWrite(stream)
            } catch (_: java.io.IOException) {
                file.failWrite(stream)
            }
        }
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        mutex.withLock {
            models.clear()
            SuggestionLanguage.entries.forEach { file(it).delete() }
        }
    }

    companion object {
        @Volatile private var instance: SuggestionRepository? = null
        fun get(context: Context): SuggestionRepository = instance ?: synchronized(this) {
            instance ?: SuggestionRepository(context).also { instance = it }
        }
    }
}
