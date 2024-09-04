package com.ninodev.rutasmagicas.Fragment.Home

import LoginFragment
import android.graphics.Typeface
import android.nfc.Tag
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.search.SearchBar
import com.google.android.material.snackbar.Snackbar
import com.ninodev.rutasmagicas.Adapter.EstadosAdapter
import com.ninodev.rutasmagicas.Fragment.Municipios.PueblosMagicosFragment
import com.ninodev.rutasmagicas.Helper.HelperUser
import com.ninodev.rutasmagicas.Helper.UtilFragment
import com.ninodev.rutasmagicas.Model.EstadoModel
import com.ninodev.rutasmagicas.R
import com.ninodev.rutasmagicas.databinding.FragmentHomeBinding
import com.ninodev.rutasmagicas.ui.FirestoreDBHelper

class HomeFragment : Fragment() {
    private val TAG = "FragmentEstados"
    private var _binding: FragmentHomeBinding? = null
    private lateinit var firestoreDBHelper: FirestoreDBHelper
    private lateinit var estadosAdapter: EstadosAdapter
    private lateinit var estadosList: MutableList<EstadoModel>
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        requireActivity().title = getString(R.string.menu_home)

        initData()
        listeners()

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    fun init() {
        try {
            if (HelperUser.isUserLoggedIn()) {
                val userId = HelperUser.getUserId()
                if (!userId.isNullOrEmpty()) {
                    HelperUser._ID_USER = userId
                    Snackbar.make(requireView(), userId, Snackbar.LENGTH_LONG).show()
                } else {
                    // Manejar el caso en que userId es nulo o vacío
                    Log.e(TAG, "User ID is null or empty")
                    Snackbar.make(requireView(), "User ID is null or empty", Snackbar.LENGTH_LONG).show()
                    // Opcional: Puedes redirigir al usuario a una pantalla de error o mostrar un mensaje
                }
            } else {
                UtilFragment.changeFragment(requireContext(), LoginFragment(), TAG)
            }
        }catch (e : Exception){
            Snackbar.make(requireView(), "Error: ${e.message}", Snackbar.LENGTH_LONG).show()
        }

    }

    private fun listeners() {
        binding.listaEstados.setOnItemClickListener { adapterView, view, i, l ->
            PueblosMagicosFragment._ESTADO = estadosList[i]
            UtilFragment.changeFragment(requireContext(),PueblosMagicosFragment(),TAG)

        }
    }

    private fun initData() {
        firestoreDBHelper = FirestoreDBHelper()

        estadosList = mutableListOf()
        estadosAdapter = EstadosAdapter(requireContext(), estadosList)
        binding.listaEstados.adapter = estadosAdapter

        firestoreDBHelper.getEstados(
            onSuccess = { estados ->
                estadosList.addAll(estados)
                estadosAdapter.notifyDataSetChanged()
            },
            onFailure = { error ->
                Toast.makeText(requireContext(), "Error al obtener los municipios: ${error.message}", Toast.LENGTH_LONG).show()
                Log.e(TAG, "Error al obtener los municipios", error.toException())
            }
        )
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
                    Snackbar.make(requireView(), "Presione de nuevo para salir", Snackbar.LENGTH_LONG)
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
