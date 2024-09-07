package com.ninodev.rutasmagicas.Fragment.PuebloMagico

import ClimaService
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.ninodev.rutasmagicas.Fragment.Municipios.PueblosMagicosFragment
import com.ninodev.rutasmagicas.Helper.UtilFragment
import com.ninodev.rutasmagicas.Model.ClimaModel
import com.ninodev.rutasmagicas.Model.PuebloMagicoModel
import com.ninodev.rutasmagicas.R
import com.ninodev.rutasmagicas.databinding.FragmentPuebloMagicoDetalleBinding

class PuebloMagicoDetalleFragment : Fragment() {
    private val TAG = "PuebloMagicoDetalleFragment"
    private var _binding: FragmentPuebloMagicoDetalleBinding? = null

    private lateinit var climaService: ClimaService

    private val binding get() = _binding!!


    companion object{
        var _PUEBLO_MAGICO : PuebloMagicoModel = PuebloMagicoModel()
        var _CLIMA : ClimaModel = ClimaModel()
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPuebloMagicoDetalleBinding.inflate(inflater, container, false)
        val root: View = binding.root

        requireActivity().title = getString(R.string.menu_home)

        initData()
        initClima()
        listeners()

        return root
    }

    fun listeners() {
        binding.btnUbicacion.setOnClickListener {
            val latitud = _PUEBLO_MAGICO.latitud
            val longitud = _PUEBLO_MAGICO.longitud

            // Crear la URI para abrir la ubicación directamente en Google Maps
            val gmmIntentUri = Uri.parse("geo:$latitud,$longitud?q=$latitud,$longitud")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)

            // Verifica si hay alguna aplicación que pueda manejar el Intent
            if (mapIntent.resolveActivity(requireActivity().packageManager) != null) {
                startActivity(mapIntent)
            } else {
                // Si no hay Google Maps instalado, opcionalmente, abre en un navegador
                val url = "https://www.google.com/maps?q=$latitud,$longitud"
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(browserIntent)
            }
        }
    }




    fun initClima(){
        val climaService = ClimaService()
        climaService.getLatestWeather("Apodaca, Nuevo Leon, Mexico", "cthKbWdY4MQgKJZxqn0AcasAF8yqXAng") { temperature, condition ->
            println("Temperatura: $temperature°C")
            println("Condición: ${condition?.description ?: "No disponible"}")
            _CLIMA.Temperatura = "$temperature°C"
            _CLIMA.Condicion = "${condition?.description ?: "No disponible"}"
        }

    }
    fun initData(){
        Glide.with(requireContext())
            .load(_PUEBLO_MAGICO.imagen)
            .placeholder(R.drawable.ic_launcher_background) // Imagen de marcador de posición
            .error(R.drawable.estado_nuevo_leon) // Imagen de error en caso de fallo
            .into(binding.imagenMunicipio)

        binding.txtTitulo.text = _PUEBLO_MAGICO.nombrePueblo
        binding.txtDescripcion.text = _PUEBLO_MAGICO.descripcion

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
