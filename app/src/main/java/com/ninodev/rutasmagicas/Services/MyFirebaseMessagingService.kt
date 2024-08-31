package com.ninodev.rutasmagicas.Services

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Aquí puedes manejar el token (enviar a tu servidor, guardarlo, etc.)
        Log.d("MyFirebaseMessagingService", "Nuevo token: $token")
    }
}