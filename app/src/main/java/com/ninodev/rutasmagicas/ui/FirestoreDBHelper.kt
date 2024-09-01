package com.ninodev.rutasmagicas.ui

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.ninodev.rutasmagicas.Model.EstadoModel
import com.ninodev.rutasmagicas.Model.MunicipioModel

class FirestoreDBHelper {
    private val db: DatabaseReference = FirebaseDatabase.getInstance().getReference("Mexico/")

    // Método para obtener los nombres de los municipios desde Realtime Database
    fun getMunicipios(
        estado: String,
        onSuccess: (List<MunicipioModel>) -> Unit,
        onFailure: (DatabaseError) -> Unit
    ) {
        val estadoRef = db.child("Estados").child(estado).child("Municipios")
        estadoRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val municipios = mutableListOf<MunicipioModel>()
                for (municipioSnapshot in snapshot.children) {
                    val nombreMunicipio = municipioSnapshot.child("nombreMunicipio").getValue(String::class.java)
                    val imagen = municipioSnapshot.child("imagen").getValue(String::class.java)
                    val descripcion = municipioSnapshot.child("descripcion").getValue(String::class.java)

                    if (nombreMunicipio != null) {
                        val municipio = MunicipioModel(
                            nombreMunicipio = nombreMunicipio,
                            imagen = imagen ?: "",
                            descripcion = descripcion ?: ""
                        )
                        municipios.add(municipio)
                    }
                }
                onSuccess(municipios)
            }

            override fun onCancelled(error: DatabaseError) {
                onFailure(error)
            }
        })
    }

    fun getEstados(
        onSuccess: (MutableList<EstadoModel>) -> Unit,
        onFailure: (DatabaseError) -> Unit
    ) {
        val estadoRef = db.child("Estados")
        estadoRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val estados = mutableListOf<EstadoModel>()
                for (estadoSnapshot in snapshot.children) {
                    val nombreEstado = estadoSnapshot.child("nombreEstado").getValue(String::class.java)
                    val imagen = estadoSnapshot.child("imagen").getValue(String::class.java)
                    val descripcion = estadoSnapshot.child("descripcion").getValue(String::class.java)

                    val estadoModel = EstadoModel(
                        nombreEstado = nombreEstado ?: "",
                        imagen = imagen ?: "",
                        descripcion = descripcion ?: "",
                        municipios = mutableListOf()  // Inicia la lista vacía de municipios
                    )

                    // Agrega los municipios al estado
                    val municipiosSnapshot = estadoSnapshot.child("Municipios")
                    for (municipioSnapshot in municipiosSnapshot.children) {
                        val nombreMunicipio = municipioSnapshot.child("nombreMunicipio").getValue(String::class.java)
                        val municipioImagen = municipioSnapshot.child("imagen").getValue(String::class.java)
                        val municipioDescripcion = municipioSnapshot.child("descripcion").getValue(String::class.java)

                        if (nombreMunicipio != null) {
                            val municipio = MunicipioModel(
                                nombreMunicipio = nombreMunicipio,
                                imagen = municipioImagen ?: "",
                                descripcion = municipioDescripcion ?: ""
                            )
                            estadoModel.municipios.add(municipio)
                        }
                    }

                    estadoModel.numeroPueblos = estadoModel.municipios.size.toString()
                    estados.add(estadoModel)
                }
                onSuccess(estados)
            }

            override fun onCancelled(error: DatabaseError) {
                onFailure(error)
            }
        })
    }
}
