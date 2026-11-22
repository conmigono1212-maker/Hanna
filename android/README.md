# HANNAH para Android

Desde esta carpeta, con Java 17 y Android SDK 36 instalados, ejecuta `bash ./gradlew assembleDebug`. El APK se genera en `app/build/outputs/apk/debug/app-debug.apk`.

En la app, abre **Conectar** e introduce una clave de la API de OpenAI con acceso a `gpt-4o-mini`. La clave se mantiene en memoria mientras la pantalla del chat está activa. Los mensajes del chat se envían a OpenAI para generar las respuestas.
