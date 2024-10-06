package com.ninodev.rutasmagicas.Fragment.Perfil

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import com.ninodev.rutasmagicas.Firebase.FirestoreDBHelper
import com.ninodev.rutasmagicas.Helper.HelperUser
import com.ninodev.rutasmagicas.R
import com.ninodev.rutasmagicas.databinding.FragmentMiPerfilBinding
import java.io.ByteArrayOutputStream
import java.util.UUID // Importar para generar un ID aleatorio

class MiPerfil : Fragment() {
    private val TAG = "MiPerfil"
    private var _binding: FragmentMiPerfilBinding? = null
    private val firestoreHelper = FirestoreDBHelper()
    private val REQUEST_IMAGE_PICK = 1001
    private val REQUEST_IMAGE_CAPTURE = 1002

    private val binding get() = _binding!!
    private val storage: FirebaseStorage by lazy { FirebaseStorage.getInstance() }
    // Obtén la referencia del usuario
    val userId = HelperUser.getUserId() // Obtener ID del usuario actual
    val randomId = UUID.randomUUID().toString() // Generar ID aleatorio
    val storageRef: StorageReference = storage.reference.child("Usuarios/FotoPerfil/$userId/$randomId.jpg")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMiPerfilBinding.inflate(inflater, container, false)
        val root: View = binding.root

        requireActivity().title = getString(R.string.menu_home)

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listeners()
    }

    private fun listeners() {
        binding.fabChangeImage.setOnClickListener {
            showImageSourceDialog()
        }
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("Galería", "Cámara")
        val builder = MaterialAlertDialogBuilder(requireContext())
        builder.setTitle("Selecciona la fuente de la imagen")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> checkStoragePermissions() // Galería
                1 -> checkCameraPermissions()  // Cámara
            }
        }
        builder.show()
    }

    private fun checkStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                openGallery()
            } else {
                showManageStoragePermissionDialog()
            }
        } else {
            Dexter.withContext(requireContext())
                .withPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                .withListener(object : PermissionListener {
                    override fun onPermissionGranted(response: PermissionGrantedResponse) {
                        openGallery()
                    }

                    override fun onPermissionDenied(response: PermissionDeniedResponse) {
                        handlePermissionDenied(response, "acceso al almacenamiento")
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        permission: PermissionRequest,
                        token: PermissionToken
                    ) {
                        showPermissionRationaleDialog(
                            title = "Permiso Necesario",
                            message = "Por favor otorga el permiso de acceso al almacenamiento para usar la galería.",
                            onPositiveAction = { token.continuePermissionRequest() }
                        )
                    }
                }).check()
        }
    }

    private fun checkCameraPermissions() {
        Dexter.withContext(requireContext())
            .withPermission(Manifest.permission.CAMERA)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse) {
                    openCamera()
                }

                override fun onPermissionDenied(response: PermissionDeniedResponse) {
                    handlePermissionDenied(response, "acceso a la cámara")
                }

                override fun onPermissionRationaleShouldBeShown(
                    permission: PermissionRequest,
                    token: PermissionToken
                ) {
                    showPermissionRationaleDialog(
                        title = "Permiso Necesario",
                        message = "Por favor otorga el permiso de acceso a la cámara para tomar fotos.",
                        onPositiveAction = { token.continuePermissionRequest() }
                    )
                }
            }).check()
    }

    private fun showManageStoragePermissionDialog() {
        showSettingsDialog(
            title = "Permiso Requerido",
            message = "El acceso al almacenamiento completo es necesario. Ve a configuración para otorgar el permiso.",
            onPositiveAction = { openManageStorageSettings() }
        )
    }

    private fun openManageStorageSettings() {
        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
        intent.data = Uri.parse("package:" + requireActivity().packageName)
        startActivity(intent)
    }

    private fun handlePermissionDenied(response: PermissionDeniedResponse, permissionType: String) {
        if (response.isPermanentlyDenied) {
            showSettingsDialog(
                title = "Permiso Requerido",
                message = "El acceso a $permissionType ha sido denegado permanentemente. Por favor, ve a configuraciones para otorgar los permisos.",
                onPositiveAction = { openAppSettings() }
            )
        } else {
            showPermissionRationaleDialog(
                title = "Permiso Requerido",
                message = "La aplicación necesita acceso a $permissionType.",
                onPositiveAction = { checkStoragePermissions() }
            )
        }
    }

    private fun showPermissionRationaleDialog(title: String, message: String, onPositiveAction: () -> Unit) {
        val builder = MaterialAlertDialogBuilder(requireContext())
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("Aceptar") { _, _ ->
                onPositiveAction()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showSettingsDialog(title: String, message: String, onPositiveAction: () -> Unit) {
        val builder = MaterialAlertDialogBuilder(requireContext())
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("Ir a Configuración") { _, _ ->
                onPositiveAction()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun openGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        if (galleryIntent.resolveActivity(requireActivity().packageManager) != null) {
            startActivityForResult(galleryIntent, REQUEST_IMAGE_PICK)
        } else {
            Toast.makeText(requireContext(), "No se encontró una aplicación para abrir la galería.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (cameraIntent.resolveActivity(requireActivity().packageManager) != null) {
            startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE)
        } else {
            Toast.makeText(requireContext(), "No se encontró una aplicación para abrir la cámara.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", requireActivity().packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> {
                    val imageBitmap = data?.extras?.get("data") as Bitmap
                    binding.imgProfile.setImageBitmap(imageBitmap)
                    uploadImageToFirebase(imageBitmap)
                }
                REQUEST_IMAGE_PICK -> {
                    val selectedImageUri: Uri? = data?.data
                    binding.imgProfile.setImageURI(selectedImageUri)
                    selectedImageUri?.let { uploadImageToFirebase(it) }
                }
            }
        }
    }

    private fun uploadImageToFirebase(imageBitmap: Bitmap) {
        // Convierte el Bitmap a un byte array
        val baos = ByteArrayOutputStream()
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()
        // Sube la imagen
        storageRef.putBytes(data)
            .addOnSuccessListener {
                Log.d(TAG, "Imagen subida con éxito")
                Toast.makeText(requireContext(), "Imagen subida con éxito", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error al subir la imagen: ${exception.message}")
                Toast.makeText(requireContext(), "Error al subir la imagen", Toast.LENGTH_SHORT).show()
            }
    }

    private fun uploadImageToFirebase(imageUri: Uri) {
        // Sube la imagen
        storageRef.putFile(imageUri)
            .addOnSuccessListener {
                Log.d(TAG, "Imagen subida con éxito")
                Toast.makeText(requireContext(), "Imagen subida con éxito", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error al subir la imagen: ${exception.message}")
                Toast.makeText(requireContext(), "Error al subir la imagen", Toast.LENGTH_SHORT).show()
            }
    }
}
