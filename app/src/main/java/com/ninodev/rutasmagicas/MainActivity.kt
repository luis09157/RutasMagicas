package com.ninodev.rutasmagicas

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.ninodev.rutasmagicas.Config.AppConfig
import com.ninodev.rutasmagicas.Fragment.Home.HomeFragment
import com.ninodev.rutasmagicas.Helper.GPSHelper
import com.ninodev.rutasmagicas.Helper.UtilFragment
import com.ninodev.rutasmagicas.Helper.UtilHelper
import com.ninodev.rutasmagicas.databinding.ActivityMainBinding
import android.Manifest
import com.ninodev.rutasmagicas.Fragment.Perfil.MiPerfil

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private val TAG = "MainActivity"
    private lateinit var gpsHelper: GPSHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar Firebase
        FirebaseApp.initializeApp(this)
        gpsHelper = GPSHelper(this)
        solicitarPermisosUbicacion()


        auth = FirebaseAuth.getInstance()

        // Verificar si el usuario ya está autenticado
        if (auth.currentUser != null) {
            UtilFragment.changeFragment(supportFragmentManager, HomeFragment(), TAG)
        } else {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // Configurar el botón para abrir y cerrar el drawer
        binding.topAppBar.setNavigationOnClickListener {
            toggleDrawer() // Llamada a la función que abre/cierra el Drawer
        }
        binding.navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    UtilFragment.changeFragment(supportFragmentManager, HomeFragment(), TAG)
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_mi_perfil -> {
                    UtilFragment.changeFragment(supportFragmentManager, MiPerfil(), TAG)
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_logout -> {
                    logout()
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                    true
                }
                else -> false
            }
        }

    }
    private fun iniciarGPS() {
        gpsHelper.iniciarActualizacionGPS()
    }
    private fun logout() {
        auth.signOut() // Cierra sesión en Firebase

        // Si estás usando Google Sign-In, también cierra sesión en Google
        GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN).signOut()
            .addOnCompleteListener {
                // Redirige al fragmento de inicio de sesión
                Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
    }

    private fun toggleDrawer() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            binding.drawerLayout.openDrawer(GravityCompat.START)
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


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == AppConfig.CODE_UBICACION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso concedido, iniciar actualizaciones de ubicación
                gpsHelper.iniciarActualizacionGPS()
            } else {
                // Permiso denegado, manejar el caso
                println("Permiso de ubicación denegado")
            }
        }
    }
}
