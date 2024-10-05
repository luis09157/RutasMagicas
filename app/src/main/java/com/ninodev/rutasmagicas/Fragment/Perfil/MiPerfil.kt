package com.ninodev.rutasmagicas.Fragment.Perfil

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.ninodev.rutasmagicas.Firebase.FirestoreDBHelper
import com.ninodev.rutasmagicas.Fragment.Home.HomeFragment
import com.ninodev.rutasmagicas.Helper.UtilFragment
import com.ninodev.rutasmagicas.R
import com.ninodev.rutasmagicas.databinding.FragmentMiPerfilBinding
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener

class MiPerfil : Fragment() {
    private val TAG = "PuebloMagicoDetalleFragment"
    private var _binding: FragmentMiPerfilBinding? = null
    private val firestoreHelper = FirestoreDBHelper()
    private val REQUEST_IMAGE_PICK = 1001
    private val REQUEST_IMAGE_CAPTURE = 1002

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
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
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Selecciona la fuente de la imagen")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> checkGalleryPermissions() // Galería
                1 -> checkCameraPermissions() // Cámara
            }
        }
        builder.show()
    }

    private fun checkGalleryPermissions() {
        Dexter.withContext(requireContext())
            .withPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse) {
                    openGallery()
                }

                override fun onPermissionDenied(response: PermissionDeniedResponse) {
                    // Manejar el permiso denegado (puedes mostrar un mensaje)
                }

                override fun onPermissionRationaleShouldBeShown(
                    permission: PermissionRequest,
                    token: PermissionToken
                ) {
                    token.continuePermissionRequest()
                }
            }).check()
    }

    private fun openGallery() {
        // Asegúrate de que este código esté correctamente implementado
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        if (galleryIntent.resolveActivity(requireActivity().packageManager) != null) {
            startActivityForResult(galleryIntent, REQUEST_IMAGE_PICK)
        } else {
            Toast.makeText(requireContext(), "No se encontró una aplicación para abrir la galería.", Toast.LENGTH_SHORT).show()
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
                    // Manejar el permiso denegado (puedes mostrar un mensaje)
                }

                override fun onPermissionRationaleShouldBeShown(
                    permission: PermissionRequest,
                    token: PermissionToken
                ) {
                    token.continuePermissionRequest()
                }
            }).check()
    }

    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (cameraIntent.resolveActivity(requireActivity().packageManager) != null) {
            startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE)
        } else {
            // Manejar el caso donde no hay actividad disponible para manejar el intent
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> {
                    val imageBitmap = data?.extras?.get("data") as Bitmap
                    binding.imgProfile.setImageBitmap(imageBitmap)
                }
                REQUEST_IMAGE_PICK -> {
                    val selectedImageUri: Uri? = data?.data
                    binding.imgProfile.setImageURI(selectedImageUri)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    UtilFragment.changeFragment(requireActivity().supportFragmentManager, HomeFragment(), TAG)
                }
            })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showLoading() {
        binding.lottieLoading.visibility = View.VISIBLE
        binding.contenedor.visibility = View.GONE
    }

    private fun hideLoading() {
        binding.lottieLoading.visibility = View.GONE
        binding.contenedor.visibility = View.VISIBLE
    }
}
