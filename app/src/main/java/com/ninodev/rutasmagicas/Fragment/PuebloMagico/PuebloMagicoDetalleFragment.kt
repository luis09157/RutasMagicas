package com.ninodev.rutasmagicas.Fragment.PuebloMagico

import ClimaService
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.app.ActivityCompat
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.Glide
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.ninodev.rutasmagicas.Config.AppConfig
import com.ninodev.rutasmagicas.Fragment.Municipios.PueblosMagicosFragment
import com.ninodev.rutasmagicas.Helper.HelperUser
import com.ninodev.rutasmagicas.Helper.UtilFragment
import com.ninodev.rutasmagicas.LoginFragment
import com.ninodev.rutasmagicas.Model.ClimaModel
import com.ninodev.rutasmagicas.Model.PuebloMagicoModel
import com.ninodev.rutasmagicas.R
import com.ninodev.rutasmagicas.databinding.FragmentPuebloMagicoDetalleBinding
import com.ninodev.rutasmagicas.Firebase.FirestoreDBHelper

class PuebloMagicoDetalleFragment : Fragment() {
    private val TAG = "PuebloMagicoDetalleFragment"
    private var _binding: FragmentPuebloMagicoDetalleBinding? = null
    private lateinit var animacionClima: LottieAnimationView
    val firestoreHelper = FirestoreDBHelper()

    private lateinit var climaService: ClimaService
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val binding get() = _binding!!

    companion object {
        var _PUEBLO_MAGICO: PuebloMagicoModel = PuebloMagicoModel()
        var _CLIMA: ClimaModel = ClimaModel()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPuebloMagicoDetalleBinding.inflate(inflater, container, false)
        val root: View = binding.root

        requireActivity().title = getString(R.string.menu_home)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        showLoading()
        initClima()
        initData()
        listeners()

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        animacionClima = binding.animacionClima
        init()
    }
    fun init() {
        try {
            if (HelperUser.isUserLoggedIn()) {
                val userId = HelperUser.getUserId()
                if (!userId.isNullOrEmpty()) {
                    HelperUser._ID_USER = userId
                    //Snackbar.make(requireView(), userId, Snackbar.LENGTH_LONG).show()

                    leerVisitas(PueblosMagicosFragment._ESTADO.nombreEstado, _PUEBLO_MAGICO.nombrePueblo)
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
    fun listeners() {
        binding.btnPuebloCertificado.setOnClickListener {
            checkLocationAndCalculateDistance()
        }
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
        binding.btnPuebloSeleccionado.setOnClickListener {
            val currentVisita = _PUEBLO_MAGICO.visita // Asumiendo que tienes un campo en _PUEBLO_MAGICO para la visita

            val title: String
            val message: String
            val positiveButtonText: String

            // Configurar el mensaje y los botones según el estado actual de la visita
            if (currentVisita == true) {
                title = "Quitar visita"
                message = "¿Deseas quitar tu visita al pueblo mágico ${_PUEBLO_MAGICO.nombrePueblo}?"
                positiveButtonText = "Quitar"
            } else {
                title = "Añadir visita"
                message = "¿Deseas añadir tu visita al pueblo mágico ${_PUEBLO_MAGICO.nombrePueblo}?"
                positiveButtonText = "Añadir"
            }

            // Crear y mostrar el diálogo de confirmación
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveButtonText) { dialog, _ ->
                    // Acción a realizar al confirmar
                    toggleVisita()
                    dialog.dismiss()
                }
                .setNegativeButton("Cancelar") { dialog, _ ->
                    // Acción a realizar al cancelar
                    dialog.dismiss()
                }
                .show()
        }
    }
    fun initClima() {
        climaService = ClimaService() // Use the property if it's not already initialized
        climaService.getLatestWeather(_PUEBLO_MAGICO.latitud.toDouble(), _PUEBLO_MAGICO.latitud.toDouble(), "cthKbWdY4MQgKJZxqn0AcasAF8yqXAng") { temperature, condition ->

            _CLIMA.Temperatura = "$temperature °C"
            _CLIMA.Condicion = "${condition?.description ?: "No disponible"}"

            // Verificar si _binding no es null antes de acceder a sus propiedades
            activity?.runOnUiThread {
                _binding?.let { binding ->
                    binding.txtClimaCondicion.text = _CLIMA.Condicion
                    binding.txtClimaTemperatura.text = _CLIMA.Temperatura
                    when (condition) {
                        ClimaService.WeatherCondition.CLEAR -> animacionClima.setAnimation(R.raw.animacion_soleado)
                        ClimaService.WeatherCondition.CLOUDY -> animacionClima.setAnimation(R.raw.animacion_nublado)
                        ClimaService.WeatherCondition.RAINY -> animacionClima.setAnimation(R.raw.animacion_lluvioso)
                        ClimaService.WeatherCondition.SNOWY -> animacionClima.setAnimation(R.raw.animacion_nevando)
                        else -> animacionClima.setAnimation(R.raw.animacion_soleado)
                    }
                    animacionClima.playAnimation() // Iniciar la animación
                    hideLoading()
                }
            }
        }
    }
    fun initData() {
        Glide.with(requireContext())
            .load(_PUEBLO_MAGICO.imagen)
            .placeholder(R.drawable.img_carga_viaje) // Imagen de marcador de posición
            .error(R.drawable.img_not_found) // Imagen de error en caso de fallo
            .into(binding.imagenMunicipio)

        binding.txtEstado.text = PueblosMagicosFragment._ESTADO.nombreEstado
        binding.txtPuebloMagico.text = "${_PUEBLO_MAGICO.nombrePueblo}"
        binding.txtDescripcion.text = HtmlCompat.fromHtml(_PUEBLO_MAGICO.descripcion, HtmlCompat.FROM_HTML_MODE_LEGACY)
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
    fun toggleVisita() {
        firestoreHelper.toggleVisita(
            userId = HelperUser._ID_USER,
            estado = PueblosMagicosFragment._ESTADO.nombreEstado,
            municipio = _PUEBLO_MAGICO.nombrePueblo,
            puebloMagico = _PUEBLO_MAGICO,
            onSuccess = { newVisita ->
                if (newVisita) {
                    binding.btnPuebloSeleccionado.setImageResource(R.drawable.paloma_verde)
                    Snackbar.make(requireView(), "Has añadido tu visita", Snackbar.LENGTH_LONG).show()
                } else {
                    binding.btnPuebloSeleccionado.setImageResource(R.drawable.paloma_blanca)
                    Snackbar.make(requireView(), "Has quitado tu visita", Snackbar.LENGTH_LONG).show()
                }
            },
            onFailure = { exception ->
                Snackbar.make(requireView(), "Error al actualizar la visita: ${exception.message}", Snackbar.LENGTH_LONG).show()
            }
        )
    }
    fun leerVisitas(nombreEstado: String, nombreMunicipio: String) {
        firestoreHelper.leerVisitas(
            idUsuario = HelperUser._ID_USER,
            nombreEstado = PueblosMagicosFragment._ESTADO.nombreEstado,
            nombreMunicipio = _PUEBLO_MAGICO.nombrePueblo,
            onVisitFound = {
                // Acción cuando la visita es encontrada
                binding.btnPuebloSeleccionado.setImageResource(R.drawable.paloma_verde)
                Snackbar.make(requireView(), "Visita confirmada a $nombreMunicipio en $nombreEstado", Snackbar.LENGTH_LONG).show()
                _PUEBLO_MAGICO.visita = true
            },
            onVisitNotFound = {
                // Acción cuando no se encuentra la visita
                binding.btnPuebloSeleccionado.setImageResource(R.drawable.paloma_blanca)
                _PUEBLO_MAGICO.visita = false
            },
            onFailure = { exception ->
                // Manejo de errores
                Snackbar.make(requireView(), "Error al obtener los documentos: ${exception.message}", Snackbar.LENGTH_LONG).show()
            }
        )
    }
    private fun showLoading() {
        binding.lottieLoading.visibility = View.VISIBLE
        binding.contenedor.visibility = View.GONE
    }
    private fun hideLoading() {
        binding.lottieLoading.visibility = View.GONE
        binding.contenedor.visibility = View.VISIBLE
    }
    private fun checkLocationAndCalculateDistance() {
        // Verifica si se tienen permisos de ubicación
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Obtén la ubicación actual
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        // Calcula la distancia entre el pueblo mágico y la ubicación actual
                        val puebloLocation = Location("PuebloMagico")
                        //puebloLocation.latitude = _PUEBLO_MAGICO.latitud.toDouble()
                        puebloLocation.latitude = "25.7326905".toDouble()
                       // puebloLocation.longitude = _PUEBLO_MAGICO.longitud.toDouble()
                        puebloLocation.longitude = "-100.1844694".toDouble()

                        val distanciaEnMetros = location.distanceTo(puebloLocation)

                        // Verifica si la distancia es menor a 10 metros
                        if (distanciaEnMetros <= 20) {
                            Snackbar.make(
                                requireView(),
                                "Estás dentro del rango de 10 metros del pueblo mágico.",
                                Snackbar.LENGTH_LONG
                            ).show()
                        } else {
                            Snackbar.make(
                                requireView(),
                                "Estás fuera del rango de 10 metros.",
                                Snackbar.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        Snackbar.make(
                            requireView(),
                            "No se pudo obtener la ubicación actual.",
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }
        } else {
            // Si no se tienen permisos, solicitarlos
            requestLocationPermission()
        }
    }

    private fun requestLocationPermission() {
        requestPermissions(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            AppConfig.CODE_UBICACION
        )
    }

    // Manejo del resultado de la solicitud de permisos
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == AppConfig.CODE_UBICACION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkLocationAndCalculateDistance()
            } else {
                Snackbar.make(
                    requireView(),
                    "Permiso de ubicación denegado.",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }
}
