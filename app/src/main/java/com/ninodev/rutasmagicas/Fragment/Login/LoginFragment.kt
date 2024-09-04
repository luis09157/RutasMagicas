import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.ninodev.rutasmagicas.Fragment.Home.HomeFragment
import com.ninodev.rutasmagicas.Fragment.Municipios.PueblosMagicosFragment
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
        binding.btnLogin.setOnClickListener {
            UtilHelper.hideKeyboard(requireView())
            val email = binding.txtCorreo.editText?.text.toString().trim()
            val password = binding.txtContraseA .editText?.text.toString().trim()

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
                    UtilFragment.changeFragment(requireContext(),HomeFragment(),TAG)
                } else {
                    showAlert(getString(R.string.msg_login_failed))
                }
            }
    }

    private fun showAlert(message: String) {
        MaterialAlertDialogBuilder(requireContext())
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


    override fun onResume() {
        super.onResume()
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    UtilFragment.changeFragment(requireContext(), PueblosMagicosFragment(), TAG)
                }
            })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
