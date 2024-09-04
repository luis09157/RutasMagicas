// ClimaService.kt
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ninodev.rutasmagicas.Model.WeatherResponse
import okhttp3.*
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

private const val BASE_URL = "https://api.tomorrow.io/v4/weather/forecast"

class ClimaService {

    private val client = OkHttpClient()

    enum class WeatherCondition(val description: String) {
        CLEAR("Soleado"),
        CLOUDY("Nublado"),
        RAINY("Lluvioso"),
        SNOWY("Nevado"),
        OTHER("Otro");

        companion object {
            fun fromCode(code: Int): WeatherCondition {
                return when (code) {
                    1000 -> CLEAR
                    1001 -> CLOUDY
                    1100, 1101 -> RAINY
                    1200, 1201 -> SNOWY
                    else -> OTHER
                }
            }
        }
    }

    // Método para obtener la temperatura y la condición climática más cercana
    fun getLatestWeather(location: String, apiKey: String, callback: (Double?, WeatherCondition?) -> Unit) {
        val url = "$BASE_URL?location=$location&apikey=$apiKey"

        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(null, null)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val jsonData = response.body?.string()
                    val weatherResponse = parseWeatherResponse(jsonData)
                    val (temperature, weatherCondition) = getClosestWeather(weatherResponse)
                    callback(temperature, weatherCondition)
                } else {
                    callback(null, null)
                }
            }
        })
    }

    // Función para parsear la respuesta JSON usando Gson
    private fun parseWeatherResponse(jsonData: String?): WeatherResponse {
        val gson = Gson()
        val type = object : TypeToken<WeatherResponse>() {}.type
        return gson.fromJson(jsonData, type)
    }

    // Función para obtener la temperatura y la condición más cercana en el tiempo
    private fun getClosestWeather(response: WeatherResponse): Pair<Double?, WeatherCondition?> {
        // Obtener la hora actual en formato ISO 8601
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        val currentTime = dateFormat.parse(dateFormat.format(Date()))

        // Filtrar los datos minutely para obtener la temperatura y el clima más cercano
        val closestData = response.timelines.minutely
            .minByOrNull {
                val dataTime = dateFormat.parse(it.time)
                Math.abs(currentTime.time - dataTime.time)
            }

        val temperature = closestData?.values?.temperature
        val weatherCode = closestData?.values?.weatherCode
        val weatherCondition = weatherCode?.let { WeatherCondition.fromCode(it) }

        return Pair(temperature, weatherCondition)
    }
}
