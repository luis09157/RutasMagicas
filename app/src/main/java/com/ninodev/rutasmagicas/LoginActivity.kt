package com.ninodev.rutasmagicas

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.ninodev.rutasmagicas.Config.AppConfig
import com.ninodev.rutasmagicas.Helper.GPSHelper
import com.ninodev.rutasmagicas.Helper.UtilFragment
import com.ninodev.rutasmagicas.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private val TAG = "LoginActivity"
    private lateinit var gpsHelper: GPSHelper


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar Firebase
        FirebaseApp.initializeApp(this)
        gpsHelper = GPSHelper(this)
        solicitarPermisosUbicacion()


        auth = FirebaseAuth.getInstance()

        // Verificar si el usuario ya está autenticado
        if (auth.currentUser != null) {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        } else {
            UtilFragment.changeFragment(supportFragmentManager, LoginFragment(), TAG)
        }
    }

    fun solicitarPermisosUbicacion() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Solicitar los permisos si no han sido concedidos
            ActivityCompat.requestPermissions(
                this, // Asegúrate de que "this" es una Activity
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                AppConfig.CODE_UBICACION // Código de solicitud de permiso
            )
        } else {
            // Permisos ya concedidos
            gpsHelper.iniciarActualizacionGPS() // Inicia el GPS si ya tienes los permisos
        }
    }
}