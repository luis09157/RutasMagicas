package com.ninodev.rutasmagicas.ui

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.ninodev.rutasmagicas.Fragment.Home.HomeFragment
import com.ninodev.rutasmagicas.Model.EstadoModel
import com.ninodev.rutasmagicas.Model.MunicipioModel

class FirestoreDBHelper {

    private val firestore = FirebaseFirestore.getInstance()

    fun getEstados(
        onSuccess: (MutableList<EstadoModel>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        fetchEstadosFromFirestore(onSuccess, onFailure)
    }

    private fun fetchEstadosFromFirestore(
        onSuccess: (MutableList<EstadoModel>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        firestore.collection("RutasMagicas")
            .document("Paises")
            .collection("Mexico")
            .get()
            .addOnSuccessListener { documents ->
                val estados = mutableListOf<EstadoModel>()
                val totalEstados = documents.size() // Usar size() para obtener el número de documentos
                var completedEstados = 0 // Contador de estados completados

                for (document in documents) {
                    val estadoModel = parseEstado(document.id, document.data) {
                        // Callback que se ejecuta cuando los municipios se han cargado
                        completedEstados++
                        if (completedEstados == totalEstados) {
                            // Cuando todos los estados han cargado sus municipios, notificar el éxito
                            onSuccess(estados)
                        }
                    }
                    estados.add(estadoModel)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("FirestoreDBHelper", "Error obteniendo estados: ${exception.message}")
                onFailure(exception)
            }
    }

    private fun parseEstado(estadoId: String, data: Map<String, Any>, onComplete: () -> Unit): EstadoModel {
        val nombreEstado = data["nombreEstado"] as? String ?: ""
        val imagen = data["imagen"] as? String ?: ""
        val descripcion = data["descripcion"] as? String ?: ""

        val estadoModel = EstadoModel(
            nombreEstado = nombreEstado,
            imagen = imagen,
            descripcion = descripcion,
            municipios = mutableListOf()
        )

        Log.d("FirestoreDBHelper", "Estado: $nombreEstado")

        // Obtener la colección de municipios
        firestore.collection("RutasMagicas")
            .document("Paises")
            .collection("Mexico")
            .document(estadoId)
            .collection("Municipios")
            .get()
            .addOnSuccessListener { municipiosDocuments ->
                for (municipioDocument in municipiosDocuments) {
                    val municipio = parseMunicipio(municipioDocument.data)
                    estadoModel.municipios.add(municipio)
                }
                estadoModel.numeroPueblos = estadoModel.municipios.size.toString()
                HomeFragment._TOTAL_PUEBLOS_MAGICOS += estadoModel.municipios.size
                onComplete() // Llamar al callback cuando se hayan cargado los municipios
            }
            .addOnFailureListener { exception ->
                Log.e("FirestoreDBHelper", "Error obteniendo municipios: ${exception.message}")
                onComplete() // Llamar al callback aunque ocurra un fallo
            }

        return estadoModel
    }

    private fun parseMunicipio(data: Map<String, Any>): MunicipioModel {
        val nombreMunicipio = data["nombreMunicipio"] as? String ?: ""
        val municipioImagen = data["imagen"] as? String ?: ""
        val municipioDescripcion = data["descripcion"] as? String ?: ""
        val latitud = data["latitud"] as? String ?: ""
        val longitud = data["longitud"] as? String ?: ""
        Log.d("FirestoreDBHelper", "Municipio: $nombreMunicipio")

        return MunicipioModel(
            nombreMunicipio = nombreMunicipio,
            imagen = municipioImagen,
            descripcion = municipioDescripcion,
            latitud = latitud,
            longitud = longitud
        )
    }

    fun getAllDataFromUser(
        userId: String,
        onComplete: (Int) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val firestore = FirebaseFirestore.getInstance()
        val userCollectionRef = firestore.collection("RutasMagicas")
            .document("VisitasPueblosMagicos")
            .collection("Usuarios")
            .document(userId)
            .collection("Visitas")

        // Obtener todos los documentos de la colección "Visitas"
        userCollectionRef.get()
            .addOnSuccessListener { visitasSnapshot ->
                if (visitasSnapshot.isEmpty) {
                    // No hay documentos en la colección "Visitas", retornar 0 visitas
                    onComplete(0)
                    return@addOnSuccessListener
                }

                var totalVisits = 0

                // Iterar sobre los documentos y contar aquellos donde 'visita' es true
                visitasSnapshot.documents.forEach { document ->
                    Log.d("FirestoreDBHelper", "Documento: ${document.id}, Datos: ${document.data}")

                    // Verificar si el campo 'visita' es true
                    if (document.getBoolean("visita") == true) {
                        totalVisits++
                    }
                }

                // Retornar el total de visitas
                onComplete(totalVisits)
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreDBHelper", "Error obteniendo documentos de la colección Visitas: ${e.message}")
                onFailure(e)
            }
    }








}
