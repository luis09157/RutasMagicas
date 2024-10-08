package com.ninodev.rutasmagicas.Fragment.Home

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.appcompat.widget.SearchView
import com.bumptech.glide.Glide
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.ninodev.rutasmagicas.Adapter.EstadosAdapter
import com.ninodev.rutasmagicas.Fragment.Municipios.PueblosMagicosFragment
import com.ninodev.rutasmagicas.Helper.HelperUser
import com.ninodev.rutasmagicas.Helper.UtilFragment
import com.ninodev.rutasmagicas.LoginFragment
import com.ninodev.rutasmagicas.Model.EstadoModel
import com.ninodev.rutasmagicas.databinding.FragmentHomeBinding
import com.ninodev.rutasmagicas.Firebase.FirestoreDBHelper
import com.ninodev.rutasmagicas.LoginActivity
import com.ninodev.rutasmagicas.MainActivity
import com.ninodev.rutasmagicas.R

class HomeFragment : Fragment() {
    private val TAG = "HomeFragment"
    private var _binding: FragmentHomeBinding? = null
    private lateinit var firestoreDBHelper: FirestoreDBHelper
    private lateinit var estadosAdapter: EstadosAdapter
    private lateinit var estadosList: MutableList<EstadoModel>
    private lateinit var navHeaderView: View // Vista del header del nav

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

        showLoading()
        estadosList = mutableListOf()
        estadosAdapter = EstadosAdapter(requireContext(), estadosList)
        _binding?.listaEstados?.adapter = estadosAdapter

        init()
        initData()
        listeners()
        handleBackPress()

        return _binding?.root
    }

    private fun setDataNavHeader() {
        val navView = requireActivity().findViewById<NavigationView>(R.id.nav_view)
        navHeaderView = navView.getHeaderView(0) // Obtiene la primera vista de header

        if (navHeaderView == null) {
            Log.e(TAG, "El header del NavigationView es nulo.")
            return
        }

        val imagenPerfil = navHeaderView.findViewById<ImageView>(R.id.imagen_perfil_nav_header)
        val txtNombreUsuarioNav = navHeaderView.findViewById<TextView>(R.id.txt_nombre_usuario_nav)
        val txtCorreoNav = navHeaderView.findViewById<TextView>(R.id.txt_correo_nav)

        if (MainActivity._INFO_USER != null) {
            Glide.with(requireContext())
                .load(MainActivity._INFO_USER.imagenPerfil)
                .placeholder(R.drawable.img_carga_viaje)
                .error(R.drawable.img_not_found)
                .into(imagenPerfil)

            txtNombreUsuarioNav.text = MainActivity._INFO_USER.nombreUsuario
            txtCorreoNav.text = MainActivity._INFO_USER.correo
        } else {
            Log.e(TAG, "INFO_USER es nulo.")
        }
    }

    private fun init() {
        try {
            firestoreDBHelper = FirestoreDBHelper()
            TOTAL_PUEBLOS_MAGICOS = 0
            if (HelperUser.isUserLoggedIn()) {
                val userId = HelperUser.getUserId()
                if (!userId.isNullOrEmpty()) {
                    HelperUser._ID_USER = userId

                    firestoreDBHelper.getUserDataFromFirestore(userId,
                        onSuccess = { user ->
                            // Actualizar los datos globales o la interfaz de usuario
                            MainActivity._INFO_USER = user
                            binding.txtNombreUsuario.text = "Hola ${MainActivity._INFO_USER.nombreUsuario},\n ¿Que ruta quieres realizar hoy?"
                            setDataNavHeader()
                            Log.d("FirestoreDBHelper", "Usuario obtenido: ${user.nombreUsuario}")
                        },
                        onFailure = { exception ->
                            // Manejo del fallo al obtener el usuario
                            Log.e("FirestoreDBHelper", "Error obteniendo usuario: ${exception.message}")
                            val intent = Intent(activity, LoginActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)

                            // Mostrar un mensaje de error o manejar la excepción
                        }
                    )

                   /* firestoreDBHelper.getNombreUsuario( HelperUser._ID_USER,
                        onSuccess = { nombreUsuario ->
                            Log.d("NombreUsuario", "El nombre del usuario es: $nombreUsuario")
                            binding.txtNombreUsuario.text = "Hola ${nombreUsuario},\n ¿Que ruta quieres realizar hoy?"
                        },
                        onFailure = { exception ->
                            Log.e("ErrorNombreUsuario", "Error: ${exception.message}")
                        }
                    )*/
                } else {
                    Snackbar.make(requireView(), "El ID de usuario es nulo o vacío", Snackbar.LENGTH_LONG).show()
                }
            } else {
                UtilFragment.changeFragment(requireActivity().supportFragmentManager, LoginFragment(), TAG)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message}", e)
            Snackbar.make(requireView(), "Error: ${e.message}", Snackbar.LENGTH_LONG).show()
        }
    }
    private fun listeners() {
        _binding?.listaEstados?.setOnItemClickListener { _, _, i, _ ->
            val adapter = _binding?.listaEstados?.adapter // Obtén el adapter asociado al ListView
            val estadoSeleccionado = adapter?.getItem(i) as EstadoModel // Obtén el item directamente del adapter
            PueblosMagicosFragment._ESTADO = estadoSeleccionado
            UtilFragment.changeFragment(requireActivity().supportFragmentManager, PueblosMagicosFragment(), TAG)
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
                    hideLoading()
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
    private fun showLoading() {
        binding.lottieLoading.visibility = View.VISIBLE
        binding.contenedor.visibility = View.GONE
    }
    private fun hideLoading() {
        binding.lottieLoading.visibility = View.GONE
        binding.contenedor.visibility = View.VISIBLE
    }
}
