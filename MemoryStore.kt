package com.cyberassistant.memory

import android.content.Context
import java.util.concurrent.ConcurrentHashMap

class MemoryStore(private val context:Context) {
 private val data=ConcurrentHashMap<String,String>()
 fun put(key:String,value:String){data[key]=value}
 fun get(key:String):String?=data[key]
 fun forget(key:String){data.remove(key)}
 fun keys():Set<String>=data.keys
}
