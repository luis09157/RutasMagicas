package com.ninodev.rutasmagicas.Fragment.Municipios

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.ninodev.rutasmagicas.Adapter.PueblosMagicosAdapter
import com.ninodev.rutasmagicas.Fragment.Home.HomeFragment
import com.ninodev.rutasmagicas.Fragment.PuebloMagico.PuebloMagicoDetalleFragment
import com.ninodev.rutasmagicas.Helper.UtilFragment
import com.ninodev.rutasmagicas.Model.EstadoModel
import com.ninodev.rutasmagicas.R
import com.ninodev.rutasmagicas.databinding.FragmentPueblosMagicosBinding

class PueblosMagicosFragment : Fragment() {
    private val TAG = "PueblosMagicosFragment"
    private var _binding: FragmentPueblosMagicosBinding? = null
    private val binding get() = _binding!!

    companion object{
        var _ESTADO : EstadoModel = EstadoModel()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPueblosMagicosBinding.inflate(inflater, container, false)
        val root: View = binding.root

        requireActivity().title = getString(R.string.menu_home)

        initData()
        listeners()

        return root
    }

    fun initData(){
        Glide.with(requireContext())
            .load(_ESTADO.imagen)
            .placeholder(R.drawable.ic_launcher_background) // Imagen de marcador de posición
            .error(R.drawable.estado_nuevo_leon) // Imagen de error en caso de fallo
            .into(binding.imagenMunicipio)

        binding.txtTitulo.text = _ESTADO.nombreEstado
        binding.txtDescripcion.text = _ESTADO.descripcion

        val adapter = PueblosMagicosAdapter(requireContext(), _ESTADO.municipios)
        binding.gridPueblosMagicos.adapter = adapter
    }
    fun listeners(){
        binding.gridPueblosMagicos.setOnItemClickListener { adapterView, view, i, l ->
            PuebloMagicoDetalleFragment._PUEBLO_MAGICO.nombrePueblo = _ESTADO.municipios.get(i).nombreMunicipio
            PuebloMagicoDetalleFragment._PUEBLO_MAGICO.imagen = _ESTADO.municipios.get(i).imagen
            PuebloMagicoDetalleFragment._PUEBLO_MAGICO.descripcion = _ESTADO.municipios.get(i).descripcion
            PuebloMagicoDetalleFragment._PUEBLO_MAGICO.latitud = _ESTADO.municipios.get(i).latitud
            PuebloMagicoDetalleFragment._PUEBLO_MAGICO.longitud = _ESTADO.municipios.get(i).longitud

            UtilFragment.changeFragment(requireContext(),PuebloMagicoDetalleFragment(),TAG)
        }
    }
    override fun onResume() {
        super.onResume()
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    UtilFragment.changeFragment(requireContext(), HomeFragment(), TAG)
                }
            })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
