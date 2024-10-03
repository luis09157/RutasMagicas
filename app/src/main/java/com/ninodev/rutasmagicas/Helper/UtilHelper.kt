package com.ninodev.rutasmagicas.Helper

import android.content.Context
import android.location.LocationManager
import android.provider.DocumentsContract.Root
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.ninodev.rutasmagicas.R

class UtilHelper {
    companion object{
        fun hideKeyboard(view: View) {
            val inputMethodManager = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
        }
        fun showAlert(context: Context,message: String) {
            MaterialAlertDialogBuilder(context, R.style.AlertDialogTheme)
                .setMessage(message)
                .setPositiveButton(context.getString(R.string.btn_ok)) { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
        fun mostrarSnackbar(view: View, mensaje: String) {
            Snackbar.make(view, mensaje, Snackbar.LENGTH_SHORT)
                .setAction("Aceptar") { /* Acciones si se requiere */ }
                .show()
        }
        fun isLocationEnabled(context: Context): Boolean {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }
    }
}