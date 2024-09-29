package com.ninodev.rutasmagicas.Service

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat.startActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
import java.io.IOException

class VersionPlayStore() {
    private val TAG = "VersionPlayStore"

    // URL de tu aplicación en Google Play Store
    private val playStoreUrl = "https://play.google.com/store/apps/details?id="

    // Función para obtener la versión más reciente desde la Play Store
    fun getLatestVersionFromPlayStore(packageName: String, callback: (String?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Hace scraping de la página de la Play Store
                val doc = Jsoup.connect("$playStoreUrl$packageName").get()

                // Busca el elemento en el HTML que contiene la versión
                val versionElement = doc.select("div.hAyfc:nth-child(4) > span:nth-child(2) > div:nth-child(1) > span:nth-child(1)")
                val latestVersion = versionElement.text()

                // Llama al callback con la versión obtenida
                callback(latestVersion)
            } catch (e: IOException) {
                Log.e(TAG, "Error obteniendo la versión de la Play Store: ${e.message}")
                callback(null)
            }
        }
    }

    fun showUpdateDialog(activity: Activity) {
        val builder = AlertDialog.Builder(activity.baseContext)
        builder.setTitle("Actualización disponible")
        builder.setMessage("Hay una nueva versión de la aplicación disponible. Por favor, actualízala desde la Play Store.")

        // Botón para ir a la Play Store
        builder.setPositiveButton("Actualizar") { dialogInterface, _ ->
            openPlayStore(activity)
            dialogInterface.dismiss()
        }

        // Botón de cancelar
        builder.setNegativeButton("Cancelar") { dialogInterface, _ ->
            dialogInterface.dismiss()
        }

        // Mostrar el diálogo
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    fun openPlayStore(activity: Activity) {
        val appPackageName = activity.packageName
        try {
            // Intenta abrir la Play Store
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appPackageName"))
            activity.startActivity(intent)
        } catch (e: android.content.ActivityNotFoundException) {
            // Si la Play Store no está instalada, abre en el navegador
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName"))
            activity.startActivity(intent)
        }
    }

}
