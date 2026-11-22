package com.cyberassistant.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cyberassistant.security.SecurityEngine

class SecurityWorker(ctx:Context,params:WorkerParameters):CoroutineWorker(ctx,params){
 override suspend fun doWork():Result {
   SecurityEngine(applicationContext).scan()
   return Result.success()
 }
}
