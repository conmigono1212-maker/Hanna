package com.cyberassistant.ui

import java.io.IOException
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

internal data class ChatMessage(val role: String, val text: String)

internal class ChatClient {
    suspend fun reply(apiKey: String, history: List<ChatMessage>): String =
        withContext(Dispatchers.IO) {
            val messages = JSONArray().put(
                JSONObject().put("role", "system").put(
                    "content", "Te llamas HANNAH. Responde en español claro y conciso salvo que se pida otro idioma."
                )
            )
            history.takeLast(16).forEach { message ->
                messages.put(JSONObject().put("role", message.role).put("content", message.text))
            }
            val request = JSONObject()
                .put("model", "gpt-4o-mini")
                .put("messages", messages)
                .put("max_completion_tokens", 700)
                .toString()

            val connection = URL("https://api.openai.com/v1/chat/completions")
                .openConnection() as HttpsURLConnection
            try {
                connection.requestMethod = "POST"
                connection.connectTimeout = 15_000
                connection.readTimeout = 45_000
                connection.instanceFollowRedirects = false
                connection.doOutput = true
                connection.setRequestProperty("Authorization", "Bearer $apiKey")
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.use { it.write(request.toByteArray(Charsets.UTF_8)) }

                val code = connection.responseCode
                if (code !in 200..299) {
                    throw IOException(
                        when (code) {
                            401, 403 -> "Revisa la clave de OpenAI y sus permisos."
                            429 -> "OpenAI alcanzó el límite de solicitudes o de saldo."
                            else -> "OpenAI devolvió el error HTTP $code."
                        }
                    )
                }
                val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val reply = JSONObject(body).optJSONArray("choices")?.optJSONObject(0)
                    ?.optJSONObject("message")?.optString("content").orEmpty().trim()
                reply.ifBlank { throw IOException("OpenAI devolvió una respuesta vacía.") }
            } finally {
                connection.disconnect()
            }
        }
}
