import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
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
        hideLoading()
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
                Snackbar.make(
                    requireView(),
                    getString(R.string.error_terms_not_accepted),
                    Snackbar.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            showLoading()
            // Verificar disponibilidad del nombre de usuario y del correo
            checkUserEmailAndNameAvailability(email, name, password)
        }
    }
    private fun checkUserEmailAndNameAvailability(correo: String, nombreUsuario: String, password: String) {
        val firestore = FirebaseFirestore.getInstance()
        val usersCollection = firestore.collection("RutasMagicas/RegistroUsuarios/Usuarios")

        // Verificar si el nombre de usuario ya existe
        usersCollection
            .whereEqualTo("nombreUsuario", nombreUsuario)
            .get()
            .addOnCompleteListener { nameCheckTask ->
                if (nameCheckTask.isSuccessful) {
                    if (!nameCheckTask.result.isEmpty) {
                        hideLoading()
                        // Nombre de usuario ya existe
                        binding.txtNombreUsuario.error = getString(R.string.error_username_taken)
                        binding.txtNombreUsuario.requestFocus()
                        Snackbar.make(requireView(), getString(R.string.error_username_taken), Snackbar.LENGTH_LONG).show()
                        Log.e(TAG, "Nombre de usuario ya existente")
                    } else {
                        // Nombre de usuario disponible, proceder a crear el usuario
                        createUserWithEmailAndPassword(correo, password, nombreUsuario)
                    }
                } else {
                    hideLoading()
                    Log.e(TAG, "Error al verificar el nombre de usuario: ${nameCheckTask.exception?.message}")
                    Snackbar.make(requireView(), "Error al verificar el nombre de usuario: ${nameCheckTask.exception?.message}", Snackbar.LENGTH_LONG).show()
                    // Manejo de errores, podrías decidir si quieres continuar o manejar el error de otra manera
                }
            }
    }
    private fun createUserWithEmailAndPassword(email: String, password: String, nombreUsuario: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Usuario creado exitosamente, ahora guardar los detalles en Firestore
                    createAccount(nombreUsuario, email)
                } else {
                    hideLoading()
                    val exception = task.exception
                    if (exception != null && exception.message?.contains("email address is already in use") == true) {
                        binding.txtCorreo.error = getString(R.string.error_email_already_registered)
                        binding.txtCorreo.requestFocus()
                        Snackbar.make(requireView(), getString(R.string.error_email_already_registered), Snackbar.LENGTH_LONG).show()
                        Log.e(TAG, "Error: correo ya registrado")
                    } else {
                        Log.e(TAG, "Error al crear usuario: ${exception?.message}")
                        Snackbar.make(requireView(), getString(R.string.error_registration_failed), Snackbar.LENGTH_LONG).show()
                    }
                }
            }
    }
    private fun createAccount(nombreUsuario: String, correo: String) {
        val userId = auth.currentUser?.uid
        val user = User(nombreUsuario, correo)

        if (userId != null) {
            val firestore = FirebaseFirestore.getInstance()
            val userRef = firestore.collection("RutasMagicas")
                .document("RegistroUsuarios")
                .collection("Usuarios")
                .document(userId)

            userRef.set(user)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        signOut()
                        LoginFragment._FLAG_IS_REGISTRO = true
                        UtilFragment.changeFragment(requireActivity().supportFragmentManager, LoginFragment(), TAG)
                    } else {
                        hideLoading()
                        Snackbar.make(requireView(), getString(R.string.error_registration_failed), Snackbar.LENGTH_LONG).show()

                        // Intentar eliminar el usuario recién creado en caso de error
                        auth.currentUser?.let { currentUser ->
                            currentUser.delete().addOnCompleteListener { deleteTask ->
                                if (deleteTask.isSuccessful) {
                                    Log.e(TAG, "Usuario eliminado debido a fallo en la creación en Firestore")
                                } else {
                                    Log.e(TAG, "Error eliminando usuario: ${deleteTask.exception?.message}")
                                }
                            }
                        }
                    }
                }
        } else {
            hideLoading()
            Log.e(TAG, "ID de usuario no encontrado")
            Snackbar.make(requireView(), getString(R.string.error_registration_failed), Snackbar.LENGTH_LONG).show()
        }
    }
    private fun signOut() {
        auth.signOut()
    }
    private fun showLoading() {
        binding.lottieLoading.visibility = View.VISIBLE
        binding.contenedor.visibility = View.GONE
    }

    private fun hideLoading() {
        binding.lottieLoading.visibility = View.GONE
        binding.contenedor.visibility = View.VISIBLE
    }
}
