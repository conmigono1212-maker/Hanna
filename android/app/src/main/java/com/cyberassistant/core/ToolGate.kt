package com.cyberassistant.core

class ToolGate {
 private val readOnly=setOf("security_scan","market_snapshot","memory_search","system_status")
 fun authorize(request:ToolRequest, userConfirmed:Boolean):Boolean =
   request.tool in readOnly || userConfirmed
}
