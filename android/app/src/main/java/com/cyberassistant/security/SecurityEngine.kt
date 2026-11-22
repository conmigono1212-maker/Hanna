package com.cyberassistant.security

import android.content.Context
import android.os.Build
import com.cyberassistant.core.*
import java.io.File
import java.security.MessageDigest

class SecurityEngine(private val context:Context) {
 fun scan():List<Finding> {
   val out=mutableListOf<Finding>()
   out += Finding("android",Severity.INFO,"Versión Android",
     "API ${Build.VERSION.SDK_INT}","Mantén Android actualizado.")
   if(context.filesDir.usableSpace < 500L*1024*1024)
     out += Finding("storage",Severity.MEDIUM,"Almacenamiento bajo",
       "Menos de 500 MB libres en almacenamiento privado.",
       "Libera espacio y conserva copias de seguridad.")
   return out
 }
 fun sha256(file:File):String {
   val d=MessageDigest.getInstance("SHA-256")
   file.inputStream().use { input ->
     val b=ByteArray(16384)
     while(true){val n=input.read(b); if(n<0) break; d.update(b,0,n)}
   }
   return d.digest().joinToString(""){"%02x".format(it)}
 }
}
