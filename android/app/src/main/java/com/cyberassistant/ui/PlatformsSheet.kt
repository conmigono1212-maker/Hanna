package com.cyberassistant.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlatformsSheet(
    destination: String,
    onDestinationChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val address = destination.trim()
    var openError by remember { mutableStateOf(false) }
    val openPlatform: (Uri) -> Unit = { uri ->
        openError = false
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: ActivityNotFoundException) {
            openError = true
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("💼 Plataformas", style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(
                value = destination,
                onValueChange = onDestinationChange,
                label = { Text("Dirección o lugar de destino") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                enabled = address.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    openPlatform(
                        Uri.parse("https://www.google.com/maps/dir/").buildUpon()
                            .appendQueryParameter("api", "1")
                            .appendQueryParameter("destination", address)
                            .appendQueryParameter("travelmode", "driving")
                            .build()
                    )
                }
            ) { Text("Navegador") }
            if (openError) {
                Text(
                    "No hay una aplicación disponible para abrir el enlace.",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
