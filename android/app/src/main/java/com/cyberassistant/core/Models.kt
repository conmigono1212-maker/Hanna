package com.cyberassistant.core

enum class Severity { INFO, LOW, MEDIUM, HIGH, CRITICAL }

data class Finding(val id:String,val severity:Severity,val title:String,val evidence:String,val recommendation:String)

data class AssistantSettings(
 val name:String="Cyber",
 val language:String="es-419",
 val voice:String="latina_femenina",
 val speakingRate:Float=1f,
 val pitch:Float=1.05f
)

data class ToolRequest(val tool:String,val arguments:Map<String,String> = emptyMap())
