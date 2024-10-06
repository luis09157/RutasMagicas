package com.ninodev.rutasmagicas.Helper

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat.startActivity
import com.google.firebase.auth.FirebaseAuth
import com.ninodev.rutasmagicas.Firebase.FirestoreDBHelper
import com.ninodev.rutasmagicas.Fragment.Home.HomeFragment
import com.ninodev.rutasmagicas.LoginActivity
import com.ninodev.rutasmagicas.MainActivity

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
        fun getDataUserRefresh(activity: MainActivity) {
            val firestoreHelper = FirestoreDBHelper()
            val userId = getUserId()

            if (userId.isNullOrEmpty()) {
                // Si el userId es nulo o vacío, manejar el error
                Log.e("FirestoreDBHelper", "El userId es nulo o vacío. No se puede obtener el usuario.")
                val intent = Intent(activity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                activity.startActivity(intent)
                activity.finish()
                return
            }

            firestoreHelper.getUserDataFromFirestore(userId,
                onSuccess = { user ->
                    // Actualizar los datos globales o la interfaz de usuario
                    MainActivity._INFO_USER = user
                    Log.d("FirestoreDBHelper", "Usuario obtenido: ${user.nombreUsuario}")
                    // Aquí puedes actualizar la interfaz de usuario o realizar otras operaciones
                },
                onFailure = { exception ->
                    // Manejo del fallo al obtener el usuario
                    Log.e("FirestoreDBHelper", "Error obteniendo usuario: ${exception.message}")
                    val intent = Intent(activity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    activity.startActivity(intent)
                    activity.finish()
                    // Mostrar un mensaje de error o manejar la excepción
                }
            )
        }

    }
}
