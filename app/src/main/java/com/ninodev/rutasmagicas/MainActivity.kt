package com.ninodev.rutasmagicas

import LoginFragment
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.navigation.ui.AppBarConfiguration
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.ninodev.rutasmagicas.Helper.UtilFragment
import com.ninodev.rutasmagicas.databinding.ActivityMainBinding
import com.ninodev.rutasmagicas.ui.FirestoreDBHelper

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var firestoreDBHelper: FirestoreDBHelper
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView = binding.navView

        // Obtén el NavController usando el ID correcto

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow
            ), drawerLayout
        )




        // Inicializar Firebase
        FirebaseApp.initializeApp(this)

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d(TAG, "Token: $token")
            } else {
                Log.w(TAG, "Error al obtener el token", task.exception)
            }
        }

        // Inicializar FirestoreDBHelper después de Firebase
        firestoreDBHelper = FirestoreDBHelper()

        // Usar FirestoreDBHelper para obtener municipios del estado "Aguascalientes"
        firestoreDBHelper.getMunicipios(
            estado = "Aguascalientes",
            onSuccess = { municipios ->
                for (municipio in municipios) {
                    Log.d(TAG, "Municipio: $municipio")
                }
            },
            onFailure = { error ->
                Toast.makeText(this, "Error al obtener los municipios: ${error.message}", Toast.LENGTH_LONG).show()
                Log.e(TAG, "Error al obtener los municipios", error.toException())
            }
        )

        // Cambiar al fragmento de inicio
       // UtilFragment.changeFragment(this, HomeFragment(), TAG)
        UtilFragment.changeFragment(this, LoginFragment(), TAG)
    }

}
