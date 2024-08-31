package com.ninodev.rutasmagicas.Fragment.Estados

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.ninodev.rutasmagicas.Adapter.EstadosAdapter
import com.ninodev.rutasmagicas.Model.EstadoModel
import com.ninodev.rutasmagicas.R
import com.ninodev.rutasmagicas.databinding.FragmentEstadosBinding
import com.ninodev.rutasmagicas.ui.FirestoreDBHelper

class EstadosFragment : Fragment() {
    val TAG = "FragmentEstados"
    private var _binding: FragmentEstadosBinding? = null
    private lateinit var firestoreDBHelper: FirestoreDBHelper
    private lateinit var estadosAdapter: EstadosAdapter
    private lateinit var estadosList: MutableList<EstadoModel>
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEstadosBinding.inflate(inflater, container, false)
        val root: View = binding.root

        requireActivity().title = getString(R.string.menu_estados)

        firestoreDBHelper = FirestoreDBHelper()

        estadosList = mutableListOf()
        estadosAdapter = EstadosAdapter(requireContext(),estadosList)
        binding.gridEstados.adapter = estadosAdapter

        firestoreDBHelper.getEstados(
            onSuccess = { estados ->
                estadosList.addAll(estados)
                estadosAdapter.notifyDataSetChanged()
            },
            onFailure = { error ->
                Toast.makeText(requireContext(), "Error al obtener los municipios: ${error.message}", Toast.LENGTH_LONG).show()
                Log.e("MainActivity", "Error al obtener los municipios", error.toException())
            }
        )


        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    override fun onResume() {
        super.onResume()
        if (view == null) {
            return
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                   // UtilFragment.changeFragment(requireContext(), MenuFragment(), TAG)
                }
            })
    }
}