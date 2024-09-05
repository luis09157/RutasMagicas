package com.ninodev.rutasmagicas.Helper

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
    }
}