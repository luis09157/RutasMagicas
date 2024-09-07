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
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import com.ninodev.rutasmagicas.Fragment.Municipios.PueblosMagicosFragment
import com.ninodev.rutasmagicas.Helper.HelperUser
import com.ninodev.rutasmagicas.Helper.UtilFragment
import com.ninodev.rutasmagicas.LoginFragment
import com.ninodev.rutasmagicas.Model.ClimaModel
import com.ninodev.rutasmagicas.Model.PuebloMagicoModel
import com.ninodev.rutasmagicas.R
import com.ninodev.rutasmagicas.databinding.FragmentPuebloMagicoDetalleBinding

class PuebloMagicoDetalleFragment : Fragment() {
    private val TAG = "PuebloMagicoDetalleFragment"
    private var _binding: FragmentPuebloMagicoDetalleBinding? = null
    private lateinit var animacionClima: LottieAnimationView


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
                    Snackbar.make(requireView(), userId, Snackbar.LENGTH_LONG).show()

                    leerVisitas(HelperUser._ID_USER, PueblosMagicosFragment._ESTADO.nombreEstado ,_PUEBLO_MAGICO.nombrePueblo)
                } else {
                    Snackbar.make(requireView(), "User ID is null or empty", Snackbar.LENGTH_LONG).show()
                }
            } else {
                UtilFragment.changeFragment(requireContext(), LoginFragment(), TAG)
            }
        }catch (e : Exception){
            Snackbar.make(requireView(), "Error: ${e.message}", Snackbar.LENGTH_LONG).show()
        }

    }
    fun listeners() {
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

    fun toggleVisita() {
        val newVisita = !_PUEBLO_MAGICO.visita // Invertir el estado actual
        _PUEBLO_MAGICO.visita = newVisita

        // Actualizar Firestore con el nuevo estado de la visita
        val db = FirebaseFirestore.getInstance()
        val docRef = db.collection("RutasMagicas")
            .document("VisitasPueblosMagicos")
            .collection("Usuarios")
            .document(HelperUser._ID_USER)
            .collection(PueblosMagicosFragment._ESTADO.nombreEstado)
            .document(_PUEBLO_MAGICO.nombrePueblo)

        // Actualiza el campo 'visita' en Firestore
        docRef.update("visita", newVisita)
            .addOnSuccessListener {
                // Cambiar la imagen del botón según el nuevo estado
                if (newVisita) {
                    binding.btnPuebloSeleccionado.setImageResource(R.drawable.paloma_verde)
                    Snackbar.make(requireView(), "Has añadido tu visita", Snackbar.LENGTH_LONG).show()
                } else {
                    binding.btnPuebloSeleccionado.setImageResource(R.drawable.paloma_blanca)
                    Snackbar.make(requireView(), "Has quitado tu visita", Snackbar.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                Snackbar.make(requireView(), "Error al actualizar la visita: ${e.message}", Snackbar.LENGTH_LONG).show()
            }
    }

    fun initClima() {
        val climaService = ClimaService()
        climaService.getLatestWeather("Apodaca, Nuevo Leon, Mexico", "cthKbWdY4MQgKJZxqn0AcasAF8yqXAng") { temperature, condition ->
            println("Temperatura: $temperature°C")
            println("Condición: ${condition?.description ?: "No disponible"}")
            _CLIMA.Temperatura = "$temperature°C"
            _CLIMA.Condicion = "${condition?.description ?: "No disponible"}"

            // Cambiar la animación según la condición del clima en el hilo principal
            activity?.runOnUiThread {
                when (condition) {
                    ClimaService.WeatherCondition.CLEAR -> animacionClima.setAnimation(R.raw.animacion_soleado)
                    ClimaService.WeatherCondition.CLOUDY -> animacionClima.setAnimation(R.raw.animacion_nublado)
                    ClimaService.WeatherCondition.RAINY -> animacionClima.setAnimation(R.raw.animacion_lluvioso)
                    ClimaService.WeatherCondition.SNOWY -> animacionClima.setAnimation(R.raw.animacion_nevando)
                    else -> animacionClima.setAnimation(R.raw.animacion_soleado)
                }
                animacionClima.playAnimation() // Iniciar la animación
            }
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
    fun leerVisitas(idUsuario: String, nombreEstado: String, nombreMunicipio: String) {
        // Obtén una instancia de Firestore
        val db = FirebaseFirestore.getInstance()

        // Navega en la estructura de la colección
        val docRef = db.collection("RutasMagicas")
            .document("VisitasPueblosMagicos")
            .collection("Usuarios")
            .document(idUsuario)
            .collection(nombreEstado)
            .document(nombreMunicipio)

        // Accede al documento que contiene la propiedad "visita"
        docRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Verifica si el campo "visita" existe
                    val visita = document.getBoolean("visita")

                    if (visita == true) {
                        // Si la visita es verdadera, haz algo con la información
                        binding.btnPuebloSeleccionado.setImageResource(R.drawable.paloma_verde)
                        println("Visita confirmada a $nombreMunicipio en $nombreEstado")
                        _PUEBLO_MAGICO.visita = visita
                    } else {
                        println("No se ha realizado la visita.")
                        _PUEBLO_MAGICO.visita = false
                        binding.btnPuebloSeleccionado.setImageResource(R.drawable.paloma_blanca)
                    }
                } else {
                    _PUEBLO_MAGICO.visita = false
                    binding.btnPuebloSeleccionado.setImageResource(R.drawable.paloma_blanca)
                    println("No se encontró el documento.")
                }
            }
            .addOnFailureListener { exception ->
                // Manejo de errores
                _PUEBLO_MAGICO.visita = false
                binding.btnPuebloSeleccionado.setImageResource(R.drawable.paloma_blanca)
                println("Error al obtener el documento: $exception")
            }
    }

}
