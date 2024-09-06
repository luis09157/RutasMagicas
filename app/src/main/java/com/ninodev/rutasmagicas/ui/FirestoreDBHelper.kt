package com.ninodev.rutasmagicas.ui

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
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
                for (document in documents) {
                    val estadoModel = parseEstado(document.id, document.data)
                    estados.add(estadoModel)
                }
                onSuccess(estados)
            }
            .addOnFailureListener { exception ->
                Log.e("FirestoreDBHelper", "Error obteniendo estados: ${exception.message}")
                onFailure(exception)
            }
    }

    private fun parseEstado(estadoId: String, data: Map<String, Any>): EstadoModel {
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

        // Ahora obtenemos la colección de municipios anidada dentro del estado
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
            }
            .addOnFailureListener { exception ->
                Log.e("FirestoreDBHelper", "Error obteniendo municipios: ${exception.message}")
            }

        return estadoModel
    }

    private fun parseMunicipio(data: Map<String, Any>): MunicipioModel {
        val nombreMunicipio = data["nombreMunicipio"] as? String ?: ""
        val municipioImagen = data["imagen"] as? String ?: ""
        val municipioDescripcion = data["descripcion"] as? String ?: ""

        Log.d("FirestoreDBHelper", "Municipio: $nombreMunicipio")

        return MunicipioModel(
            nombreMunicipio = nombreMunicipio,
            imagen = municipioImagen,
            descripcion = municipioDescripcion
        )
    }
}
