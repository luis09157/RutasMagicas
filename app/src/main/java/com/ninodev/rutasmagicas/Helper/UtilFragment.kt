package com.ninodev.rutasmagicas.Helper

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.ninodev.rutasmagicas.R

class UtilFragment {
    companion object {
        fun changeFragment(fragmentManager: FragmentManager, fragment: Fragment, tag: String) {
            val transition = fragmentManager.beginTransaction()
            transition.replace(R.id.nav_host_fragment_content_main, fragment, tag)
                .addToBackStack(tag)
                .commit()
        }
    }
}
