package com.ninodev.rutasmagicas.Fragment.Home

import android.animation.ValueAnimator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.appcompat.widget.SearchView
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
    private val TAG = "HomeFragment"
    private var _binding: FragmentHomeBinding? = null
    private lateinit var firestoreDBHelper: FirestoreDBHelper
    private lateinit var estadosAdapter: EstadosAdapter
    private lateinit var estadosList: MutableList<EstadoModel>

    companion object {
        var TOTAL_PUEBLOS_MAGICOS = 0
        var TOTAL_PUEBLOS_VISITAS = 0
    }

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return _binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        estadosList = mutableListOf()
        estadosAdapter = EstadosAdapter(requireContext(), estadosList)
        _binding?.listaEstados?.adapter = estadosAdapter

        init()
        initData()
        listeners()
        handleBackPress()
    }

    private fun init() {
        try {
            TOTAL_PUEBLOS_MAGICOS = 0
            if (HelperUser.isUserLoggedIn()) {
                val userId = HelperUser.getUserId()
                if (!userId.isNullOrEmpty()) {
                    HelperUser._ID_USER = userId
                    Snackbar.make(requireView(), "Usuario ID: $userId", Snackbar.LENGTH_LONG).show()
                } else {
                    Snackbar.make(requireView(), "El ID de usuario es nulo o vacío", Snackbar.LENGTH_LONG).show()
                }
            } else {
                UtilFragment.changeFragment(requireContext(), LoginFragment(), TAG)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message}", e)
            Snackbar.make(requireView(), "Error: ${e.message}", Snackbar.LENGTH_LONG).show()
        }
    }

    private fun listeners() {
        _binding?.listaEstados?.setOnItemClickListener { _, _, i, _ ->
            PueblosMagicosFragment._ESTADO = estadosList[i]
            UtilFragment.changeFragment(requireContext(), PueblosMagicosFragment(), TAG)
        }

        // Configurar el SearchView para búsqueda
        val searchView = _binding?.searchView
        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Opcionalmente, maneja el evento cuando el usuario envía la búsqueda
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterList(newText.orEmpty())
                return true
            }
        })
    }

    private fun initData() {
        firestoreDBHelper = FirestoreDBHelper()

        firestoreDBHelper.getEstados(
            onSuccess = { estados ->
                estadosList.clear()
                estadosList.addAll(estados)
                estadosAdapter.notifyDataSetChanged()
                fetchUserVisits()
            },
            onFailure = { error ->
                Snackbar.make(requireView(), "Error al obtener los municipios: ${error.message}", Snackbar.LENGTH_LONG).show()
            }
        )
    }

    private fun fetchUserVisits() {
        val userId = HelperUser.getUserId()
        if (!userId.isNullOrEmpty()) {
            firestoreDBHelper.getAllDataFromUser(
                userId,
                onComplete = { totalVisits ->
                    TOTAL_PUEBLOS_VISITAS = totalVisits
                    _binding?.txtVisitadosPueblos?.text = "($totalVisits/$TOTAL_PUEBLOS_MAGICOS)"
                    promedioVisitas()
                },
                onFailure = { error ->
                    Snackbar.make(requireView(), "Error al contar visitas: ${error.message}", Snackbar.LENGTH_LONG).show()
                }
            )
        }
    }

    private fun handleBackPress() {
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

    private fun promedioVisitas() {
        if (TOTAL_PUEBLOS_MAGICOS == 0) {
            _binding?.txtVisitadosPueblos?.text = "(0/$TOTAL_PUEBLOS_MAGICOS)"
            _binding?.txtPorcentaje?.text = "0%"
            _binding?.progressCircular?.progress = 0
            return
        }

        val porcentajeVisitados = (TOTAL_PUEBLOS_VISITAS.toDouble() / TOTAL_PUEBLOS_MAGICOS.toDouble()) * 100

        ValueAnimator.ofInt(0, porcentajeVisitados.toInt()).apply {
            duration = 1000
            addUpdateListener { animator ->
                val porcentajeAnimado = animator.animatedValue as Int
                _binding?.txtPorcentaje?.text = "$porcentajeAnimado%"
                _binding?.progressCircular?.progress = porcentajeAnimado
            }
            start()
        }
    }

    private fun filterList(query: String) {
        val filtered = estadosList.filter { estado ->
            estado.nombreEstado.contains(query, ignoreCase = true)
        }
        estadosAdapter.updateList(filtered)

        // Mostrar u ocultar el mensaje de no resultados
        if (filtered.isEmpty()) {
            _binding?.txtNoResults?.visibility = View.VISIBLE
            _binding?.listaEstados?.visibility = View.GONE
        } else {
            _binding?.txtNoResults?.visibility = View.GONE
            _binding?.listaEstados?.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
