package com.ninodev.rutasmagicas.Model

data class WeatherData(
    val data: Data
)

data class Data(
    val current: Current,
    val location: Location
)

data class Current(
    val temperature: Double
)

data class Location(
    val city: String,
    val state: String,
    val country: String
)
