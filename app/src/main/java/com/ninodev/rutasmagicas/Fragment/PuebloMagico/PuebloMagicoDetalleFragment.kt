package com.ninodev.rutasmagicas.Fragment.PuebloMagico

import ClimaService
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
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
import com.ninodev.rutasmagicas.Helper.UtilHelper
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        private val _CAMERA_REQUEST_CODE = 1074
        private lateinit var _IMAGEN_URI: Uri
        private val LOCATION_PERMISSION_REQUEST_CODE = 1001

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

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            // La imagen fue capturada, puedes manejarla aquí
            showLoading()
            try {
                val bitmap = BitmapFactory.decodeStream(requireActivity().contentResolver.openInputStream(_IMAGEN_URI))
                uploadImage(bitmap)  // Llama a tu función para subir la imagen
            } catch (e: Exception) {
                hideLoading()
                UtilHelper.mostrarSnackbar(requireView(), "Error al cargar la imagen: ${e.message}")
            }
        } else {
            UtilHelper.mostrarSnackbar(requireView(), "No se pudo capturar la imagen.")
        }
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
                    leerVisitas()
                } else {
                    Snackbar.make(requireView(), "User ID is null or empty", Snackbar.LENGTH_LONG).show()
                }
            } else {
                UtilFragment.changeFragment(requireActivity().supportFragmentManager, LoginFragment(), TAG)
            }
        } catch (e: Exception) {
            Snackbar.make(requireView(), "Error: ${e.message}", Snackbar.LENGTH_LONG).show()
        }
    }
    fun listeners() {


        binding.btnPuebloCertificado.setOnClickListener {
            if (_PUEBLO_MAGICO.visita) {
                mostrarConfirmacionCertificacion()
            } else {
                if(_PUEBLO_MAGICO.visitaCertificada){
                    mostrarDialogoEliminarCertificacion()
                }else{
                    if(UtilHelper.isLocationEnabled(requireContext())){
                        checkLocationPermission()
                    }else{
                       mostrarDialogoActivarGPS()
                    }

                }
            }

        }
        binding.btnUbicacion.setOnClickListener {
           goToUbicationGoogleMaps()
        }
        binding.btnPuebloSeleccionado.setOnClickListener {
            val title: String
            val message: String
            val positiveButtonText: String

            if (_PUEBLO_MAGICO.visitaCertificada) {
                title = "Quitar Certificación"
                message = "Al quitar la certificación de tu visita al pueblo mágico ${_PUEBLO_MAGICO.nombrePueblo}, podrás validar tu visita sin certificación. ¿Estás seguro de que deseas proceder con esta opción?"
                positiveButtonText = "Continuar"
            } else {
                if (_PUEBLO_MAGICO.visita) {
                    title = "Quitar visita"
                    message = "¿Deseas quitar tu visita al pueblo mágico ${_PUEBLO_MAGICO.nombrePueblo}?"
                    positiveButtonText = "Quitar"
                } else {
                    title = "Añadir visita"
                    message = "¿Deseas añadir tu visita al pueblo mágico ${_PUEBLO_MAGICO.nombrePueblo}?"
                    positiveButtonText = "Añadir"
                }
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
    private fun mostrarDialogoActivarGPS(){
        // Mostrar un diálogo para pedir que active la ubicación
        MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialogTheme)
            .setTitle("Habilitar ubicación")
            .setMessage("Para certificar tu visita al Pueblo Mágico, es necesario activar la ubicación. ¿Deseas habilitarla ahora?")
            .setPositiveButton("Activar") { _, _ ->
                // Si el usuario acepta, abre la configuración de ubicación
                val intent = Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                // Si el usuario no quiere activar la ubicación, simplemente cierra el diálogo
                dialog.dismiss()
            }
            .show()
    }
    private fun mostrarDialogoEliminarCertificacion() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Confirmación de eliminación")
            .setMessage("¿Está seguro de que desea quitar la certificación de su visita al pueblo mágico? " +
                    "Esto eliminará la validación, su foto de asistencia y desaparecerá de su progreso.")
            .setPositiveButton("Eliminar") { dialog, _ ->
                // Aquí llamas a la función para eliminar la certificación
                certificarVisitaPuebloMagico()// O la función que uses para manejar el toggle
                dialog.dismiss() // Cerrar el diálogo
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss() // Cerrar el diálogo sin hacer nada
            }
            .show()
    }
    fun initClima() {
        climaService = ClimaService() // Use the property if it's not already initialized
        climaService.getLatestWeather(_PUEBLO_MAGICO.latitud.toDouble(), _PUEBLO_MAGICO.longitud.toDouble(), "cthKbWdY4MQgKJZxqn0AcasAF8yqXAng") { temperature, condition ->

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
    fun toggleVisita() {
        firestoreHelper.toggleVisita(
            userId = HelperUser._ID_USER,
            estado = PueblosMagicosFragment._ESTADO.nombreEstado,
            municipio = _PUEBLO_MAGICO.nombrePueblo,
            puebloMagico = _PUEBLO_MAGICO,
            onSuccess = { newVisita ->
                _PUEBLO_MAGICO.visita = newVisita
                _PUEBLO_MAGICO.visitaCertificada = false
                if (newVisita) {
                    setBtnVisitaTrue()
                } else {
                    setBtnVisitaFalse()
                }
            },
            onFailure = { exception ->
                Snackbar.make(requireView(), "Error al actualizar la visita: ${exception.message}", Snackbar.LENGTH_LONG).show()
            }
        )
    }
    fun leerVisitas() {
        firestoreHelper.leerVisitas(
            idUsuario = HelperUser._ID_USER,
            nombreEstado = PueblosMagicosFragment._ESTADO.nombreEstado,
            nombreMunicipio = _PUEBLO_MAGICO.nombrePueblo,
            onVisitFound = { visita, verificado ->
                _PUEBLO_MAGICO.visita = visita
                _PUEBLO_MAGICO.visitaCertificada = verificado
                if (visita) {
                    setBtnVisitaTrue()
                    Log.d("Verificado", "Visita confirmada a ${_PUEBLO_MAGICO.nombrePueblo} en ${PueblosMagicosFragment._ESTADO.nombreEstado}")
                } else {
                    setBtnVisitaFalse()
                    Log.d("Verificado", "Visita no realizada a ${_PUEBLO_MAGICO.nombrePueblo} en ${PueblosMagicosFragment._ESTADO.nombreEstado}")
                }

                if (verificado) {
                    setBtnCertificadoTrue()
                    Log.d("Verificado", "El pueblo mágico está verificado.")
                } else {
                    setBtnCertificadoFalse()
                    Log.d("Verificado", "El pueblo mágico no está verificado.")
                }
                visibleImagenVerificacion()
            },
            onVisitNotFound = {
                // Acción cuando no se encuentra la visita
                setBtn2True()
                _PUEBLO_MAGICO.visita = false
                _PUEBLO_MAGICO.visitaCertificada = false
                Log.d(TAG, "No se encontraron visitas a ${_PUEBLO_MAGICO.nombrePueblo} en ${PueblosMagicosFragment._ESTADO.nombreEstado}")
            },
            onFailure = { exception ->
                // Manejo de errores
                UtilHelper.mostrarSnackbar(requireView(), "Error al obtener los documentos: ${exception.message}")
            }
        )
    }
    private fun setBtn2True(){
        binding.btnPuebloSeleccionado.setImageResource(R.drawable.imagen_visita_blanco)
        binding.btnPuebloCertificado.setImageResource(R.drawable.img_verificar_blanco)
        binding.fondoPuebloVisitado.visibility = View.GONE
        binding.fondoPuebloCertificado.visibility = View.GONE
    }
    private fun setBtnVisitaTrue(){
        binding.btnPuebloSeleccionado.setImageResource(R.drawable.imagen_visitado_azul)
        binding.fondoPuebloVisitado.visibility = View.VISIBLE
        setBtnCertificadoFalse()
    }
    private fun setBtnVisitaFalse(){
        binding.btnPuebloSeleccionado.setImageResource(R.drawable.imagen_visita_blanco)
        binding.fondoPuebloVisitado.visibility = View.GONE
    }
    private fun setBtnCertificadoTrue(){
        binding.btnPuebloCertificado.setImageResource(R.drawable.img_verificar_azul)
        binding.fondoPuebloCertificado.visibility = View.VISIBLE
        setBtnVisitaFalse()
    }
    private fun setBtnCertificadoFalse(){
        binding.btnPuebloCertificado.setImageResource(R.drawable.img_verificar_blanco)
        binding.fondoPuebloCertificado.visibility = View.GONE

    }
    private fun mostrarInformarRechazoPermiso() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Permiso de Ubicación Denegado")
            .setMessage("Has denegado el acceso a la ubicación varias veces. Para otorgar el permiso, ve a Configuración de la aplicación.")
            .setPositiveButton("Configuración") { dialog, which ->
                // Enviar al usuario a la configuración de la aplicación
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri: Uri = Uri.fromParts("package", requireActivity().packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton("Cancelar", null)
            .setIcon(R.drawable.ic_home) // Opcional: añade un ícono
            .show()
    }
    private fun mostrarInformarRechazoPermisoCamara() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Permiso de Cámara Denegado")
            .setMessage("Has denegado el acceso a la cámara varias veces. Para otorgar el permiso, ve a Configuración de la aplicación.")
            .setPositiveButton("Configuración") { dialog, which ->
                // Enviar al usuario a la configuración de la aplicación
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri: Uri = Uri.fromParts("package", requireActivity().packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton("Cancelar", null)
            .setIcon(R.drawable.ic_menu_camera) // Opcional: añade un ícono relacionado con la cámara
            .show()
    }
    private fun mostrarAlertActiveLocationConfig() {
        MaterialAlertDialogBuilder(requireActivity())
            .setTitle("Permiso de Ubicación Requerido")
            .setMessage("Esta aplicación necesita acceso a tu ubicación para proporcionar la funcionalidad solicitada. Ve a Configuración para habilitarla.")
            .setPositiveButton("Configuración") { dialog, which ->
                // Enviar al usuario a la configuración de la aplicación
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri: Uri = Uri.fromParts("package", requireActivity().packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton("Cancelar", null)
            .setIcon(R.drawable.ubicacion) // Opcional: añade un ícono
            .show()
    }
    private fun mostrarAlertActiveCameraConfig() {
        MaterialAlertDialogBuilder(requireActivity())
            .setTitle("Permiso de Cámara Requerido")
            .setMessage("Esta aplicación necesita acceso a tu cámara para tomar fotos. Ve a Configuración para habilitar el permiso.")
            .setPositiveButton("Configuración") { dialog, which ->
                // Enviar al usuario a la configuración de la aplicación
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri: Uri = Uri.fromParts("package", requireActivity().packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton("Cancelar", null)
            .setIcon(R.drawable.ic_menu_camera) // Opcional: añade un ícono relacionado con la cámara
            .show()
    }
    private fun mostrarConfirmacionCertificacion() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Confirmación")
            .setMessage("Ya has indicado que has visitado este pueblo mágico. ¿Deseas certificar tu visita?")
            .setPositiveButton("Sí") { dialog, _ ->
                if(UtilHelper.isLocationEnabled(requireContext())){
                    checkLocationPermission()
                }else{
                    mostrarDialogoActivarGPS()
                }
                dialog.dismiss()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    private fun goToUbicationGoogleMaps(){
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
    private fun getLocationAndCalculateDistance() {
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    location?.let {
                        // Obtener la ubicación del usuario
                        val userLat = it.latitude
                        val userLng = it.longitude

                        // Obtener la ubicación del pueblo mágico
                         //val puebloLat = _PUEBLO_MAGICO.latitud.toDouble()
                         //val puebloLng = _PUEBLO_MAGICO.longitud.toDouble()

                        // Obtener la ubicación del pueblo mágico
                        val puebloLat = "25.7327111".toDouble()
                        val puebloLng = "-100.1844254".toDouble()

                        // Calcular la distancia
                        val results = FloatArray(1)
                        Location.distanceBetween(userLat, userLng, puebloLat, puebloLng, results)
                        val distanceInMeters = results[0]

                        // Validar si la distancia es menor o igual a 300 metros
                        if (distanceInMeters <= 300) {
                            Snackbar.make(requireView(), "Estás dentro del rango de 300 metros.", Snackbar.LENGTH_LONG).show()
                            checkCameraPermission()
                        } else {
                            MaterialAlertDialogBuilder(requireContext())
                                .setTitle("Certifica tu visita")
                                .setMessage("Para certificar tu visita, tómate una foto en las letras del Pueblo Mágico. Presiona el botón 'Abrir mapa' para obtener indicaciones y llegar al lugar.")
                                .setPositiveButton("Abrir mapa") { dialog, which ->
                                    goToUbicationGoogleMaps()
                                }
                                .setNegativeButton("Cancelar", null)
                                .show()
                        }
                    } ?: run {
                        Snackbar.make(requireView(), "No se pudo obtener la ubicación", Snackbar.LENGTH_LONG).show()
                    }
                }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir: File? = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }
    private fun openCamera() {
        // Crea un archivo para la imagen
        val imageFile = createImageFile()
        _IMAGEN_URI = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", imageFile)

        // Inicia el launcher de la cámara
        takePictureLauncher.launch(_IMAGEN_URI)
    }
    private fun certificarVisitaPuebloMagico() {
        firestoreHelper.toggleVisitaCertificado(
            HelperUser.getUserId()!!,
            PueblosMagicosFragment._ESTADO.nombreEstado,
            _PUEBLO_MAGICO.nombrePueblo,
            _PUEBLO_MAGICO,
            onSuccess = { isVerificado ->
                hideLoading()
                _PUEBLO_MAGICO.visita = false
                _PUEBLO_MAGICO.visitaCertificada = isVerificado

                // Verificar el estado de isVerificado
                if (isVerificado) {
                    setBtnCertificadoTrue()
                    UtilHelper.mostrarSnackbar(requireView(), "¡Se certificó la visita al pueblo mágico con éxito!")
                } else {
                    setBtnCertificadoFalse()
                    UtilHelper.mostrarSnackbar(requireView(), "La visita al pueblo mágico no se certificó.")
                }

                Log.d("Success", "Estado verificado: $isVerificado")
            },
            onFailure = { exception ->
                hideLoading()
                setBtnCertificadoFalse()
                UtilHelper.mostrarSnackbar(requireView(), "La visita al pueblo mágico no se certificó.")
                Log.e("Error", "Ocurrió un error: ${exception.message}")
            }
        )
    }
    private fun uploadImage(bitmap: Bitmap) {
        // Usar FirestoreDBHelper para subir la imagen
        firestoreHelper.uploadImageToFirebase(bitmap,
            onSuccess = { imageUrl ->
                certificarVisitaPuebloMagico()
            },
            onFailure = { exception ->
                hideLoading()
                UtilHelper.mostrarSnackbar(requireView(), "Error al subir la imagen: ${exception.message}")
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

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {

            // Verificar si se debe mostrar una explicación al usuario
            if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(),
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Mostrar un diálogo explicando por qué se necesita el permiso
                mostrarAlertActiveLocationConfig()
            } else {
                // Aquí el usuario ha rechazado el permiso anteriormente
                Log.d("PermissionRequest", "El usuario ha rechazado el permiso varias veces y no se puede solicitar nuevamente.")
                mostrarInformarRechazoPermiso()
            }
        } else {
            // El permiso ya está concedido
            getLocationAndCalculateDistance()
        }
    }
    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {

            // Verificar si se debe mostrar una explicación al usuario
            if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.CAMERA)) {

                // Mostrar un diálogo explicando por qué se necesita el permiso de cámara
                mostrarAlertActiveCameraConfig()
            } else {
                // El usuario ha rechazado el permiso anteriormente
                Log.d("PermissionRequest", "El usuario ha rechazado el permiso de cámara varias veces y no se puede solicitar nuevamente.")
                mostrarInformarRechazoPermisoCamara()
            }
        } else {
            // Si ya se tienen permisos, abrir la cámara
            openCamera()
        }
    }

    override fun onResume() {
        super.onResume()
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    UtilFragment.changeFragment(requireActivity().supportFragmentManager, PueblosMagicosFragment(), TAG)
                }
            })
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == _CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            val imageUri = data.data

            // Verificar que la URI no sea nula
            imageUri?.let { uri ->
                // Convertir la URI a Bitmap
                try {
                    showLoading()
                    val inputStream = requireActivity().contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)

                    // Usar FirestoreDBHelper para subir la imagen
                    val firestoreDBHelper = FirestoreDBHelper()
                    firestoreDBHelper.uploadImageToFirebase(bitmap,
                        onSuccess = { imageUrl ->
                            hideLoading()
                            UtilHelper.mostrarSnackbar(requireView(), "Imagen subida exitosamente: $imageUrl")
                            // Aquí puedes guardar la URL en Firestore o hacer algo más
                        },
                        onFailure = { exception ->
                            hideLoading()
                            UtilHelper.mostrarSnackbar(requireView(), "Error al subir la imagen: ${exception.message}")
                        }
                    )
                } catch (e: Exception) {
                    hideLoading()
                    UtilHelper.mostrarSnackbar(requireView(), "Error al convertir la URI a Bitmap: ${e.message}")
                }
            } ?: run {
                hideLoading()
                UtilHelper.mostrarSnackbar(requireView(), "La URI de la imagen es nula.")
            }
        }
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // El permiso de ubicación fue concedido
                    getLocationAndCalculateDistance()
                } else {
                    // El permiso de ubicación fue denegado
                    Log.d("PermissionRequest", "El usuario ha denegado el permiso de ubicación.")
                    mostrarAlertActiveLocationConfig()
                }
            }
            _CAMERA_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // El permiso de cámara fue concedido
                    // Aquí puedes ejecutar la acción que requiere el permiso de cámara
                    openCamera()
                } else {
                    // El permiso de cámara fue denegado
                    Log.d("PermissionRequest", "El usuario ha denegado el permiso de cámara.")
                    mostrarInformarRechazoPermisoCamara()
                }
            }
        }
    }

    private fun visibleImagenVerificacion(){
        if(_PUEBLO_MAGICO.visitaCertificada){
            binding.contenedorImagenVerificacion.visibility = View.VISIBLE
            val url = "${FirestoreDBHelper._URL_STORAGE_FIREBASE}/Certificaciones/${HelperUser.getUserId()}/${PueblosMagicosFragment._ESTADO.nombreEstado}/${_PUEBLO_MAGICO.nombrePueblo}/${HelperUser.getUserId()}.jpg?alt=media"

            val encodedUrl = "${FirestoreDBHelper._URL_STORAGE_FIREBASE}" +
                    Uri.encode("Certificaciones/${HelperUser.getUserId()}/${PueblosMagicosFragment._ESTADO.nombreEstado}/${_PUEBLO_MAGICO.nombrePueblo}/${HelperUser.getUserId()}.jpg") +
                    "?alt=media"

            Log.e("YoloSwuag",encodedUrl)
            Glide.with(requireContext())
                .load(encodedUrl)
                .placeholder(R.drawable.img_carga_viaje) // Imagen de marcador de posición
                .error(R.drawable.img_not_found) // Imagen de error en caso de fallo
                .into(binding.imagenVerificacion!!)
        }else{
            binding.contenedorImagenVerificacion.visibility = View.GONE
        }

    }
    private fun hideImagenVerificacion(){
        binding.contenedorImagenVerificacion.visibility = View.GONE
    }

}
