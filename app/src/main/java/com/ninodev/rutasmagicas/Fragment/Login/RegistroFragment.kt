package com.ninodev.rutasmagicas.Fragment.Login

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.ninodev.rutasmagicas.Fragment.Home.HomeFragment
import com.ninodev.rutasmagicas.Helper.UtilFragment
import com.ninodev.rutasmagicas.LoginFragment
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
            // Limpiar errores anteriores
            binding.txtCorreo.error = null
            binding.txtContraseA.error = null
            binding.txtConfirmarContraseA.error = null

            val email = binding.txtCorreo.editText?.text.toString().trim()
            val password = binding.txtContraseA.editText?.text.toString().trim()
            val confirmPassword = binding.txtConfirmarContraseA.editText?.text.toString().trim()
            val termsAccepted = binding.checkboxTerminosCondiciones.isChecked

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

            if (password.length < 8) {
                binding.txtContraseA.error = getString(R.string.error_short_password)
                return@setOnClickListener
            }

            if (password.length > 20) {
                binding.txtContraseA.error = getString(R.string.error_long_password)
                return@setOnClickListener
            }

            if (!password.matches(".*[a-z].*".toRegex()) || !password.matches(".*[A-Z].*".toRegex())) {
                binding.txtContraseA.error = getString(R.string.error_no_case_variation)
                return@setOnClickListener
            }

            if (!password.matches(".*\\d.*".toRegex())) {
                binding.txtContraseA.error = getString(R.string.error_no_number)
                return@setOnClickListener
            }

            if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\|,.<>/?].*".toRegex())) {
                binding.txtContraseA.error = getString(R.string.error_no_special_characters)
                return@setOnClickListener
            }

            if (password.contains(email.split('@')[0], ignoreCase = true)) {
                binding.txtContraseA.error = getString(R.string.error_password_contains_email)
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                binding.txtConfirmarContraseA.error = getString(R.string.error_password_mismatch)
                return@setOnClickListener
            }

            if (!termsAccepted) {
                Snackbar.make(requireView(), getString(R.string.error_terms_not_accepted), Snackbar.LENGTH_LONG)
                    .show()
                return@setOnClickListener
            }

            createAccount(email, password)
        }
    }


    private fun createAccount(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    UtilFragment.changeFragment(requireContext(), HomeFragment(), TAG)
                } else {
                    val errorMessage = when (task.exception?.message) {
                        "The email address is already in use by another account." ->
                            getString(R.string.error_email_already_in_use)
                        else ->
                            getString(R.string.error_registration_failed)
                    }
                    Snackbar.make(requireView(), errorMessage, Snackbar.LENGTH_LONG).show()
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
