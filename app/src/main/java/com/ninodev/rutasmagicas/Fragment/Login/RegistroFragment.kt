package com.ninodev.rutasmagicas.Fragment.Login

import LoginFragment
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.bumptech.glide.util.Util
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.ninodev.rutasmagicas.Fragment.Home.HomeFragment
import com.ninodev.rutasmagicas.Fragment.Municipios.PueblosMagicosFragment
import com.ninodev.rutasmagicas.Helper.UtilFragment
import com.ninodev.rutasmagicas.R
import com.ninodev.rutasmagicas.databinding.FragmentRegistroBinding

class RegistroFragment : Fragment() {
    private val TAG = "RegistroFragment"
    private var _binding: FragmentRegistroBinding? = null
    private lateinit var auth: FirebaseAuth

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegistroBinding.inflate(inflater, container, false)
        val root: View = binding.root

        auth = Firebase.auth
        requireActivity().title = getString(R.string.menu_home)
        listeners()

        return root
    }

    private fun listeners() {
        binding.btnRegistro.setOnClickListener {
            val email = binding.txtCorreo.toString().trim()
            val password = binding.txtContraseA.toString().trim()

            if (email.isEmpty()) {
                binding.txtCorreo.error = getString(R.string.error_empty_email)
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.txtCorreo.error = getString(R.string.error_invalid_email)
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                binding.txtContraseA.error = getString(R.string.error_empty_password)
                return@setOnClickListener
            }

            if (password.length < 6) {
                binding.txtContraseA.error = getString(R.string.error_short_password)
                return@setOnClickListener
            }

            createAccount(email, password)
        }
    }

    private fun createAccount(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    Toast.makeText(requireContext(), getString(R.string.success_registration), Toast.LENGTH_SHORT).show()
                    UtilFragment.changeFragment(requireContext(), HomeFragment(), TAG)
                } else {
                    Toast.makeText(requireContext(), getString(R.string.error_registration_failed), Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onResume() {
        super.onResume()
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    UtilFragment.changeFragment(requireContext(), LoginFragment(), TAG)
                }
            })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
