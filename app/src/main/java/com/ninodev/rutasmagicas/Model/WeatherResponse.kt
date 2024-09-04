// WeatherResponse.kt
package com.ninodev.rutasmagicas.Model

data class WeatherResponse(
    val timelines: Timelines
)

data class Timelines(
    val minutely: List<MinuteData>
)

data class MinuteData(
    val time: String,
    val values: WeatherValues
)

data class WeatherValues(
    val temperature: Double,
    val weatherCode: Int?  // Añadido para obtener la condición climática
)
