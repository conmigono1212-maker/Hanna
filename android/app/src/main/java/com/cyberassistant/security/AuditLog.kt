package com.cyberassistant.security
import android.content.Context
import java.time.Instant

class AuditLog(private val context:Context) {
 private val file get()=context.filesDir.resolve("audit.log")
 @Synchronized fun write(event:String) {
   file.appendText("${Instant.now()} | $event\n")
 }
}
