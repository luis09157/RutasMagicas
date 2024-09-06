package com.ninodev.rutasmagicas.Helper

import com.google.firebase.auth.FirebaseAuth
import android.util.Log

class AuthHelper {

    fun checkUserAuthentication() {
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        if (currentUser != null) {
            // El usuario está autenticado
            Log.d("AuthHelper", "Usuario autenticado: ${currentUser.uid}")
        } else {
            // El usuario no está autenticado
            Log.d("AuthHelper", "No hay usuario autenticado")
        }
    }

    fun observeAuthChanges() {
        val auth = FirebaseAuth.getInstance()
        val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                // Usuario autenticado
                Log.d("AuthHelper", "Usuario autenticado: ${user.uid}")
            } else {
                // Usuario no autenticado
                Log.d("AuthHelper", "No hay usuario autenticado")
            }
        }

        // Añadir el listener para detectar cambios
        auth.addAuthStateListener(authListener)

        // Recuerda eliminar el listener cuando ya no lo necesites
        // auth.removeAuthStateListener(authListener)
    }
}
