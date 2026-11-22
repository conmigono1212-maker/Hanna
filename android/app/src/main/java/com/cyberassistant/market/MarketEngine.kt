package com.cyberassistant.market

import com.cyberassistant.core.*

data class MarketObservation(
 val symbol:String,val price:Double?,val currency:String,
 val timestampUtc:String,val source:String
)

data class MarketSignal(
 val type:String,val severity:Severity,
 val evidence:List<String>,val uncertainty:String
)

class MarketEngine {
 fun analyze(observations:List<MarketObservation>):List<MarketSignal> {
   // Solo análisis de datos ya validados. No ejecuta órdenes.
   return observations.map {
     MarketSignal("observation",Severity.INFO,
       listOf("${it.symbol}: dato recibido de ${it.source}"),
       "No constituye una recomendación de inversión.")
   }
 }
}
