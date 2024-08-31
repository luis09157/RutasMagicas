package com.ninodev.rutasmagicas.Helper

import android.content.Context
import androidx.fragment.app.Fragment
import com.ninodev.rutasmagicas.MainActivity
import com.ninodev.rutasmagicas.R

class UtilFragment {
    companion object{
        fun changeFragment(context: Context,fragment: Fragment,name:String) {
            val transition = (context as MainActivity).supportFragmentManager.beginTransaction()
            transition.replace(R.id.nav_host_fragment_content_main, fragment)
                .addToBackStack(null).commit()
        }
    }

}