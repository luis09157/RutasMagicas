package com.ninodev.rutasmagicas.Helper

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.core.app.ActivityCompat

class GPSHelper(private val context: Context) {

    // LocationManager es el servicio de Android para obtener la ubicación
    private var locationManager: LocationManager? = null

    // Escuchar las actualizaciones del GPS
    fun iniciarActualizacionGPS() {
        // Inicializamos el LocationManager
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // Comprobamos que los permisos de ubicación están concedidos
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Si no se han concedido los permisos, los solicitamos (esto debe hacerse desde una Activity)
            // Aquí debería manejarse la solicitud de permisos (ver explicación más abajo)
            return
        }

        // Obtener actualizaciones de ubicación desde el GPS
        locationManager?.requestLocationUpdates(
            LocationManager.GPS_PROVIDER, // Usa el proveedor GPS
            2000L, // Intervalo de actualización en milisegundos (2 segundos en este ejemplo)
            10f, // Distancia mínima en metros para recibir actualizaciones (10 metros en este ejemplo)
            locationListener // Listener para obtener la ubicación
        )
    }

    // LocationListener que recibe las actualizaciones de GPS
    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            // Aquí se recibe la nueva ubicación (latitud y longitud)
            val latitud = location.latitude
            val longitud = location.longitude
            println("Ubicación actual: Latitud: $latitud, Longitud: $longitud")

            // Puedes implementar la lógica para validar si el usuario está en el lugar
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            // Puedes manejar los cambios de estado del GPS aquí
        }

        override fun onProviderEnabled(provider: String) {
            // Aquí manejas cuando el GPS se habilita
        }

        override fun onProviderDisabled(provider: String) {
            // Aquí manejas cuando el GPS se deshabilita
        }
    }

    // Detener las actualizaciones de ubicación cuando no sean necesarias
    fun detenerActualizacionGPS() {
        locationManager?.removeUpdates(locationListener)
    }
}
