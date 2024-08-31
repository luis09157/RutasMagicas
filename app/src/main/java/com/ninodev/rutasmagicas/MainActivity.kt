package com.ninodev.rutasmagicas

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.ninodev.rutasmagicas.Fragment.Estados.EstadosFragment
import com.ninodev.rutasmagicas.Helper.UtilFragment
import com.ninodev.rutasmagicas.databinding.ActivityMainBinding
import com.ninodev.rutasmagicas.ui.FirestoreDBHelper

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var firestoreDBHelper: FirestoreDBHelper
    var TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        binding.appBarMain.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null)
                .setAnchorView(R.id.fab).show()
        }
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // Inicializar Firebase
        FirebaseApp.initializeApp(this)


        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d("MainActivity", "Token: $token")
            } else {
                Log.w("MainActivity", "Error al obtener el token", task.exception)
            }
        }

        // Inicializar FirestoreDBHelper después de Firebase
        firestoreDBHelper = FirestoreDBHelper()

        // Usar FirestoreDBHelper para obtener municipios del estado "Aguascalientes"
        firestoreDBHelper.getMunicipios(
            estado = "Aguascalientes",
            onSuccess = { municipios ->
                for (municipio in municipios) {
                    Log.d("MainActivity", "Municipio: $municipio")
                }
            },
            onFailure = { error ->
                Toast.makeText(this, "Error al obtener los municipios: ${error.message}", Toast.LENGTH_LONG).show()
                Log.e("MainActivity", "Error al obtener los municipios", error.toException())
            }
        )



        UtilFragment.changeFragment(this, EstadosFragment(), TAG)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
