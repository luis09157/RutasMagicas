package com.ninodev.rutasmagicas.Helper

import com.google.firebase.auth.FirebaseAuth

class HelperUser {
    companion object {
        var _ID_USER = ""
        /**
         * Verifica si el usuario está autenticado y devuelve su ID.
         * @return El ID del usuario si está autenticado, o null si no lo está.
         */
        fun getUserId(): String? {
            val auth = FirebaseAuth.getInstance()
            val user = auth.currentUser

            return if (user != null) {
                user.uid // Retorna el ID del usuario
            } else {
                ""
            }
        }

        /**
         * Verifica si el usuario está autenticado.
         * @return true si el usuario está autenticado, false en caso contrario.
         */
        fun isUserLoggedIn(): Boolean {
            val auth = FirebaseAuth.getInstance()
            return auth.currentUser != null
        }
    }
}
