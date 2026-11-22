package com.cyberassistant.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class VoiceEngine(context:Context):TextToSpeech.OnInitListener {
 private val tts=TextToSpeech(context.applicationContext,this)
 private var ready=false
 override fun onInit(status:Int){ready=status==TextToSpeech.SUCCESS}
 fun speak(text:String,localeTag:String="es-419",pitch:Float=1.05f,rate:Float=1f){
   if(!ready)return
   tts.language=Locale.forLanguageTag(localeTag)
   tts.setPitch(pitch); tts.setSpeechRate(rate)
   tts.speak(text,TextToSpeech.QUEUE_FLUSH,null,"cyber_reply")
 }
 fun stop(){tts.stop()}
 fun close(){tts.shutdown()}
}
