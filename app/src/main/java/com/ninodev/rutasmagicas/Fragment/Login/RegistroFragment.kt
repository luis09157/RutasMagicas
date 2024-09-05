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
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import com.ninodev.rutasmagicas.Fragment.Home.HomeFragment
import com.ninodev.rutasmagicas.Helper.UtilFragment
import com.ninodev.rutasmagicas.LoginFragment
import com.ninodev.rutasmagicas.Model.User
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


        requireActivity().title = getString(R.string.menu_home)
        listeners()

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = Firebase.auth
    }
    private fun listeners() {
        binding.btnRegistro.setOnClickListener {
            // Limpiar errores anteriores
            binding.txtNombreUsuario.error = null
            binding.txtCorreo.error = null
            binding.txtContraseA.error = null
            binding.txtConfirmarContraseA.error = null

            val name = binding.txtNombreUsuario.editText?.text.toString().trim()
            val email = binding.txtCorreo.editText?.text.toString().trim()
            val password = binding.txtContraseA.editText?.text.toString().trim()
            val confirmPassword = binding.txtConfirmarContraseA.editText?.text.toString().trim()
            val termsAccepted = binding.checkboxTerminosCondiciones.isChecked

            if (name.isEmpty()) {
                binding.txtNombreUsuario.error = getString(R.string.error_empty_name)
                return@setOnClickListener
            }

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

            checkUserEmailAndNameAvailability(email, name, password)
        }
    }

    private fun checkUserEmailAndNameAvailability(email: String, name: String, password: String) {
        val database = FirebaseDatabase.getInstance()
        val usersRef = database.getReference("users")

        // Try to create a user with the given email
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Email is not taken, now check username availability
                    usersRef.orderByChild("name").equalTo(name).get().addOnCompleteListener { nameCheckTask ->
                        if (nameCheckTask.isSuccessful) {
                            if (nameCheckTask.result.exists()) {
                                // Username is already taken
                                binding.txtNombreUsuario.error = getString(R.string.error_username_taken)
                                auth.currentUser?.delete()
                            } else {
                                // Both email and username are available
                                createAccount(name, email)
                            }
                        } else {
                            // Error checking username
                            createAccount(name, email)
                        }
                    }
                } else {
                    // Check if the failure is due to the email already being in use
                    val exception = task.exception
                    if (exception != null && exception.message?.contains("email address is already in use") == true) {
                        binding.txtCorreo.error = getString(R.string.error_email_already_registered)
                    } else {
                        Snackbar.make(requireView(), getString(R.string.error_registration_failed), Snackbar.LENGTH_LONG).show()
                    }
                }
            }
    }

    private fun signOut() {
        auth.signOut()
    }

    private fun createAccount(name: String, email: String) {
        val userId = auth.currentUser?.uid
        val user = User(name, email)

        if (userId != null) {
            val database = FirebaseDatabase.getInstance()
            val myRef = database.getReference("users").child(userId)
            myRef.setValue(user)
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        signOut()
                        LoginFragment._FLAG_IS_REGISTRO = true
                        UtilFragment.changeFragment(requireContext(), LoginFragment(), TAG)
                    } else {
                        // Error setting user data
                        Snackbar.make(requireView(), getString(R.string.error_registration_failed), Snackbar.LENGTH_LONG).show()
                    }
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
