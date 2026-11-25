package com.cyberassistant.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.cyberassistant.R
import com.cyberassistant.voice.VoiceEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.io.IOException

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        val client = ChatClient()
        val voice = VoiceEngine(this)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Color(0xFF9CCBFF),
                background = Color(0xFF0A1324),
                surface = Color(0xFF14233C)
            )) {
                DisposableEffect(voice) {
                    onDispose { voice.close() }
                }
                HannahChat(client, voice)
            }
        }
    }
}

@Composable
private fun HannahChat(client: ChatClient, voice: VoiceEngine) {
    var showPlatforms by rememberSaveable { mutableStateOf(false) }
    var destination by rememberSaveable { mutableStateOf("") }
    val history = remember { mutableStateListOf<ChatMessage>() }
    var apiKey by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(false) }
    var draft by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    LaunchedEffect(history.size) {
        if (history.isNotEmpty()) listState.animateScrollToItem(history.size - 1)
    }

    if (showSettings) {
        var enteredKey by remember { mutableStateOf(apiKey) }
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text("Conectar con OpenAI") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("La clave dura esta sesión. Tus mensajes se envían a OpenAI para generar respuestas.")
                    OutlinedTextField(
                        value = enteredKey,
                        onValueChange = { enteredKey = it },
                        label = { Text("Clave de la API") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    apiKey = enteredKey.trim()
                    showSettings = false
                }) { Text("Listo") }
            },
            dismissButton = { TextButton(onClick = { showSettings = false }) { Text("Cerrar") } }
        )
    }

    if (showPlatforms) {
        PlatformsSheet(
            destination = destination,
            onDestinationChange = { destination = it },
            onDismiss = { showPlatforms = false }
        )
    }

    Scaffold { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.hannah_avatar),
                    contentDescription = "Retrato de HANNAH junto a un lobo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(100.dp).clip(RoundedCornerShape(16.dp))
                )
                Text("HANNAH", Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
            }
            OutlinedButton(onClick = { showPlatforms = true }) {
                Text("💼 Plataformas")
            }
            if (history.isEmpty()) {
                Text("Hola, soy HANNAH. ¿En qué te ayudo?", style = MaterialTheme.typography.titleMedium)
            }
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(history) { message ->
                    val own = message.role == "user"
                    Box(Modifier.fillMaxWidth(), contentAlignment = if (own) Alignment.CenterEnd else Alignment.CenterStart) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(0.88f),
                            color = if (own) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(message.text)
                                if (!own) {
                                    TextButton(onClick = { voice.speak(message.text) }) {
                                        Text("Escuchar respuesta")
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Row(
                Modifier.fillMaxWidth().padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Escribe un mensaje") },
                    maxLines = 4,
                    enabled = !busy
                )
                Button(
                    enabled = draft.isNotBlank() && !busy,
                    onClick = {
                        if (apiKey.isBlank()) {
                            showSettings = true
                        } else {
                            val prompt = draft.trim()
                            val message = ChatMessage("user", prompt)
                            val key = apiKey
                            history.add(message)
                            draft = ""
                            error = null
                            busy = true
                            scope.launch {
                                try {
                                    val reply = client.reply(key, history.toList())
                                    history.add(ChatMessage("assistant", reply))
                                } catch (exception: CancellationException) {
                                    throw exception
                                } catch (exception: Exception) {
                                    history.removeAt(history.lastIndex)
                                    draft = prompt
                                    error = when (exception) {
                                        is java.net.SocketTimeoutException -> "La conexión tardó demasiado. Intenta de nuevo."
                                        is java.net.UnknownHostException -> "Revisa tu conexión a internet."
                                        is IOException -> exception.message ?: "No se pudo conectar con OpenAI."
                                        else -> "No se pudo leer la respuesta. Intenta de nuevo."
                                    }
                                } finally {
                                    busy = false
                                }
                            }
                        }
                    }
                ) { Text("Enviar") }
            }
        }
    }
}
