package com.ninodev.rutasmagicas.Fragment.Home

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.ninodev.rutasmagicas.Adapter.EstadosAdapter
import com.ninodev.rutasmagicas.Fragment.Municipios.PueblosMagicosFragment
import com.ninodev.rutasmagicas.Helper.HelperUser
import com.ninodev.rutasmagicas.Helper.UtilFragment
import com.ninodev.rutasmagicas.LoginFragment
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
    companion object {
        var _TOTAL_PUEBLOS_MAGICOS: Int = 0 // Variable global para el total de pueblos mágicos
    }

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        requireActivity().title = getString(R.string.menu_home)

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
        initData()
        listeners()
    }

    fun init() {
        try {
            _TOTAL_PUEBLOS_MAGICOS = 0
            if (HelperUser.isUserLoggedIn()) {
                val userId = HelperUser.getUserId()
                if (!userId.isNullOrEmpty()) {
                    HelperUser._ID_USER = userId
                    Snackbar.make(requireView(), userId, Snackbar.LENGTH_LONG).show()
                } else {
                    Snackbar.make(requireView(), "User ID is null or empty", Snackbar.LENGTH_LONG).show()
                }
            } else {
                UtilFragment.changeFragment(requireContext(), LoginFragment(), TAG)
            }
        } catch (e: Exception) {
            Snackbar.make(requireView(), "Error: ${e.message}", Snackbar.LENGTH_LONG).show()
        }
    }

    private fun listeners() {
        binding.listaEstados.setOnItemClickListener { adapterView, view, i, l ->
            PueblosMagicosFragment._ESTADO = estadosList[i]
            UtilFragment.changeFragment(requireContext(), PueblosMagicosFragment(), TAG)
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

                // Contar visitas true para el usuario
                val userId = HelperUser.getUserId()
                if (!userId.isNullOrEmpty()) {
                    firestoreDBHelper.getAllDataFromUser(
                        userId,
                        onComplete = { totalVisits ->
                            binding.txtVisitadosPueblos.text = "($totalVisits/${_TOTAL_PUEBLOS_MAGICOS})"
                            Log.d("HomeFragment", "Total de visitas: $totalVisits")
                        },
                        onFailure = { error ->
                            Snackbar.make(requireView(), "Error al contar visitas: ${error.message}", Snackbar.LENGTH_LONG).show()
                        }
                    )
                }
            },
            onFailure = { error ->
                Snackbar.make(requireView(), "Error al obtener los municipios: ${error.message}", Snackbar.LENGTH_LONG).show()
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
                        requireActivity().finish()
                        return
                    }

                    doubleBackToExitPressedOnce = true
                    Snackbar.make(requireView(), "Presione de nuevo para salir", Snackbar.LENGTH_LONG).show()

                    Handler(Looper.getMainLooper()).postDelayed({
                        doubleBackToExitPressedOnce = false
                    }, 2000)
                }
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
