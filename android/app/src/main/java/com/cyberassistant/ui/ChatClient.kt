package com.cyberassistant.ui

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.IOException
import java.net.URL
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

internal data class ChatMessage(val role: String, val text: String)

internal class ChatClient(context: Context) {
    private val model = File(context.noBackupFilesDir, "qwen-1.5b.gguf")
    val ready get() = model.length() == MODEL_SIZE
    private external fun generate(path: ByteArray, input: ByteArray): ByteArray

    suspend fun download(progress: (Int) -> Unit) = withContext(Dispatchers.IO) {
        downloadLock.withLock {
            if (ready) return@withLock
            if (model.parentFile!!.usableSpace < MODEL_SIZE + 32_000_000L)
                throw IOException("Necesitas al menos 1,2 GB libres.")
            val tmp = File.createTempFile("modelo-", ".part", model.parentFile)
            try {
                val conn = URL(MODEL_URL).openConnection()
                conn.connectTimeout = 20000
                conn.readTimeout = 60000
                val digest = MessageDigest.getInstance("SHA-256")
                conn.getInputStream().use { input ->
                    tmp.outputStream().use { output ->
                        val buffer = ByteArray(131072)
                        var total = 0L
                        var last = -1
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val n = input.read(buffer)
                            if (n < 0) break
                            total += n
                            if (total > MODEL_SIZE) throw IOException("Tamaño de descarga inesperado.")
                            output.write(buffer, 0, n)
                            digest.update(buffer, 0, n)
                            val percent = (100 * total / MODEL_SIZE).toInt()
                            if (last != percent) { last = percent; progress(percent) }
                        }
                    }
                }
                val hash = digest.digest().joinToString("") { "%02x".format(it) }
                if (tmp.length() != MODEL_SIZE || hash != MODEL_HASH)
                    throw IOException("La descarga está incompleta. Intenta de nuevo.")
                if (!tmp.renameTo(model)) throw IOException("No se pudo guardar el modelo.")
            } finally { tmp.delete() }
        }
    }

    suspend fun reply(history: List<ChatMessage>): String = withContext(Dispatchers.IO) {
        val latest = history.last().text
        val search = Regex("""^(?:/buscar|busca(?:r)?(?: en internet)?)(?:\s+(.*))?$""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)).matchEntire(latest)
        if (search != null) {
            val query = search.groupValues[1].trim()
            if (query.isEmpty()) return@withContext "Escribe «busca en internet» seguido del tema."
            val doc = Jsoup.connect("https://html.duckduckgo.com/html/").data("q", query)
                .userAgent("Mozilla/5.0 (compatible; HANNAH/1.0)").timeout(15000).get()
            val hits = doc.select("a.result__a").mapNotNull { a ->
                val uri = Uri.parse(a.absUrl("href"))
                val url = if (uri.host in setOf("duckduckgo.com", "www.duckduckgo.com"))
                    uri.getQueryParameter("uddg") else uri.toString()
                if (url.isNullOrBlank() || Uri.parse(url).scheme !in setOf("http","https")) null
                else a.text() + "\n" + url
            }.take(5)
            return@withContext hits.joinToString("\n\n").ifBlank {
                "Abre la búsqueda: https://duckduckgo.com/?q=" + Uri.encode(query)
            }
        }
        if (!ready) throw IOException("Pulsa «Descargar modelo» para conversar sin internet.")
        System.loadLibrary("hannah")
        val prompt = buildString {
            append("<|im_start|>system\nEres HANNAH. Responde en español de forma clara.<|im_end|>\n")
            for (m in history.takeLast(5)) {
                append("<|im_start|>").append(m.role).append('\n')
                append(m.text.replace("<|", "< |")).append("<|im_end|>\n")
            }
            append("<|im_start|>assistant\n")
        }
        generate(model.absolutePath.toByteArray(), prompt.toByteArray()).toString(Charsets.UTF_8).trim()
            .ifBlank { throw IOException("El modelo devolvió una respuesta vacía.") }
    }

    companion object {
        private val downloadLock = Mutex()
        private const val MODEL_SIZE = 1117320736L
        private const val MODEL_HASH = "6a1a2eb6d15622bf3c96857206351ba97e1af16c30d7a74ee38970e434e9407e"
        private const val MODEL_URL = "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q4_k_m.gguf"
    }
}
