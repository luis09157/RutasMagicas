import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.ninodev.rutasmagicas.Fragment.Home.HomeFragment
import com.ninodev.rutasmagicas.Fragment.Login.RegistroFragment
import com.ninodev.rutasmagicas.Helper.UtilFragment
import com.ninodev.rutasmagicas.Helper.UtilHelper
import com.ninodev.rutasmagicas.R
import com.ninodev.rutasmagicas.databinding.FragmentLoginBinding

class LoginFragment : Fragment() {
    private val TAG = "LoginFragment"
    private var _binding: FragmentLoginBinding? = null
    private lateinit var auth: FirebaseAuth

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        val root: View = binding.root

        auth = FirebaseAuth.getInstance() // Inicializar Firebase Auth
        requireActivity().title = getString(R.string.menu_home)

        init()
        listeners()
        return root
    }

    fun init(){
        hideLoading()
    }

    private fun listeners() {
        binding.btnOlvidasteContraseA.setOnClickListener {
            showPasswordResetDialog()
        }
        binding.btnRegistro.setOnClickListener {
            UtilFragment.changeFragment(requireContext(), RegistroFragment(), TAG)
        }
        binding.btnLogin.setOnClickListener {
            UtilHelper.hideKeyboard(requireView())
            val email = binding.txtCorreo.editText?.text.toString().trim()
            val password = binding.txtContraseA.editText?.text.toString().trim()

            if (email.isEmpty()) {
                showAlert(getString(R.string.msg_login_empty_email))
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                showAlert(getString(R.string.msg_login_invalid_email))
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                showAlert(getString(R.string.msg_login_empty_password))
                return@setOnClickListener
            }

            if (password.length < 6) {
                showAlert(getString(R.string.msg_login_short_password))
                return@setOnClickListener
            }
            showLoading()
            signIn(email, password)
        }
    }

    private fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                UtilHelper.hideKeyboard(requireView())
                hideLoading()
                if (task.isSuccessful) {
                    UtilFragment.changeFragment(requireContext(), HomeFragment(), TAG)
                } else {
                    showAlert(getString(R.string.msg_login_failed))
                }
            }
    }

    private fun showAlert(message: String) {
        MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialogTheme)
            .setMessage(message)
            .setPositiveButton(getString(R.string.btn_ok)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showLoading() {
        binding.lottieLoading.visibility = View.VISIBLE
        binding.contenedor.visibility = View.GONE
    }

    private fun hideLoading() {
        binding.lottieLoading.visibility = View.GONE
        binding.contenedor.visibility = View.VISIBLE
    }

    private fun showPasswordResetDialog() {
        val builder = MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialogTheme)
        builder.setTitle(getString(R.string.dialog_reset_password_title))
        builder.setMessage(getString(R.string.dialog_reset_password_message))

        val input = EditText(requireContext())
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        input.hint = getString(R.string.input_hint_email)
        builder.setView(input)

        builder.setPositiveButton(getString(R.string.btn_send)) { dialog, _ ->
            val email = input.text.toString().trim()
            if (email.isEmpty()) {
                Snackbar.make(requireView(), getString(R.string.error_empty_email), Snackbar.LENGTH_SHORT)
                    .show()
            } else {
                sendPasswordResetEmail(email)
            }
            dialog.dismiss()
        }

        builder.setNegativeButton(getString(R.string.btn_cancel)) { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }

    private fun sendPasswordResetEmail(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Snackbar.make(requireView(), getString(R.string.msg_reset_email_sent), Snackbar.LENGTH_SHORT)
                        .show()
                } else {
                    Snackbar.make(requireView(), getString(R.string.msg_reset_email_error), Snackbar.LENGTH_SHORT)
                        .show()
                }
            }
    }

    override fun onResume() {
        super.onResume()

        var doubleBackToExitPressedOnce = false

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (doubleBackToExitPressedOnce) {
                        // Si se presionó dos veces, se sale de la aplicación
                        requireActivity().finish()
                        return
                    }

                    doubleBackToExitPressedOnce = true
                    Snackbar.make(requireView(), getString(R.string.snackbar_exit_prompt), Snackbar.LENGTH_LONG)
                        .show()
                    // Se establece el tiempo de espera para el segundo botón de retroceso
                    Handler(Looper.getMainLooper()).postDelayed({
                        doubleBackToExitPressedOnce = false
                    }, 2000) // 2 segundos
                }
            })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
