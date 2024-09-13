package com.ninodev.rutasmagicas.ui

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.ninodev.rutasmagicas.Fragment.Home.HomeFragment
import com.ninodev.rutasmagicas.Model.EstadoModel
import com.ninodev.rutasmagicas.Model.MunicipioModel
import com.ninodev.rutasmagicas.Model.PuebloMagicoModel

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
                HomeFragment.TOTAL_PUEBLOS_MAGICOS += estadoModel.municipios.size
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


    fun toggleVisita(
        userId: String,
        estado: String,
        municipio: String,
        puebloMagico: PuebloMagicoModel,
        onSuccess: (Boolean) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val newVisita = !puebloMagico.visita // Invertir el estado actual
        puebloMagico.visita = newVisita

        // Referencia a la colección "Visitas" dentro del usuario
        val collectionRef = firestore.collection("RutasMagicas")
            .document("VisitasPueblosMagicos")
            .collection("Usuarios")
            .document(userId)
            .collection("Visitas")

        // Consulta para buscar el documento que coincide con el estado y municipio
        val query = collectionRef
            .whereEqualTo("nombreEstado", estado)
            .whereEqualTo("nombreMunicipio", municipio)

        // Ejecutar la consulta
        query.get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    // Recorrer los resultados de la consulta
                    for (document in querySnapshot) {
                        // Actualizar el campo 'visita' en Firestore
                        document.reference.update("visita", newVisita)
                            .addOnSuccessListener {
                                onSuccess(newVisita) // Notificar el éxito
                            }
                            .addOnFailureListener { e ->
                                Log.e("FirestoreDBHelper", "Error actualizando la visita: ${e.message}")
                                onFailure(e) // Notificar el fallo
                            }
                    }
                } else {
                    // Si no se encuentra ningún documento, agregar uno nuevo
                    val newVisitaData = mapOf(
                        "nombreEstado" to estado,
                        "nombreMunicipio" to municipio,
                        "visita" to true // Establecer la visita como true por defecto
                    )

                    collectionRef.add(newVisitaData)
                        .addOnSuccessListener {
                            puebloMagico.visita = true // Actualizar el modelo
                            onSuccess(true) // Notificar el éxito
                        }
                        .addOnFailureListener { e ->
                            Log.e("FirestoreDBHelper", "Error agregando el nuevo registro: ${e.message}")
                            onFailure(e) // Notificar el fallo
                        }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("FirestoreDBHelper", "Error obteniendo los documentos: ${exception.message}")
                onFailure(exception) // Notificar el fallo
            }
    }


    fun leerVisitas(
        idUsuario: String,
        nombreEstado: String,
        nombreMunicipio: String,
        onVisitFound: () -> Unit, // Callback si la visita es encontrada (visita == true)
        onVisitNotFound: () -> Unit, // Callback si la visita no es encontrada (visita == false o no existe)
        onFailure: (Exception) -> Unit // Callback para manejar errores
    ) {
        // Navegar en la estructura de Firestore
        val collectionRef = firestore.collection("RutasMagicas")
            .document("VisitasPueblosMagicos")
            .collection("Usuarios")
            .document(idUsuario)
            .collection("Visitas") // Subcolección de visitas

        // Consulta para buscar el documento que coincide con el estado y municipio
        val query = collectionRef
            .whereEqualTo("nombreEstado", nombreEstado)
            .whereEqualTo("nombreMunicipio", nombreMunicipio)

        // Ejecutar la consulta
        query.get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    // Recorre los resultados de la consulta
                    for (document in querySnapshot) {
                        val visita = document.getBoolean("visita")

                        if (visita == true) {
                            // Si la visita es verdadera, ejecutar el callback
                            onVisitFound()
                        } else {
                            // Si no se ha realizado la visita
                            onVisitNotFound()
                        }
                    }
                } else {
                    // No se encontraron documentos, ejecutar callback de visita no encontrada
                    onVisitNotFound()
                }
            }
            .addOnFailureListener { exception ->
                // Manejar cualquier error y ejecutar el callback de error
                Log.e("FirestoreDBHelper", "Error obteniendo los documentos: ${exception.message}")
                onFailure(exception)
            }
    }

    fun contarPueblosVisitadosEnEstado(
        idUsuario: String,
        nombreEstado: String,
        onSuccess: (Int, Int) -> Unit, // (pueblosVisitados, totalPueblos)
        onFailure: (Exception) -> Unit
    ) {
        val collectionRef = firestore.collection("RutasMagicas")
            .document("VisitasPueblosMagicos")
            .collection("Usuarios")
            .document(idUsuario)
            .collection("Visitas")

        // Filtrar por nombre del estado
        val query = collectionRef
            .whereEqualTo("nombreEstado", nombreEstado)

        // Ejecutar la consulta
        query.get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    var totalPueblos = 0
                    var pueblosVisitados = 0

                    for (document in querySnapshot) {
                        val visita = document.getBoolean("visita") == true
                        totalPueblos++ // Contar todos los pueblos
                        if (visita) {
                            pueblosVisitados++ // Contar los que fueron visitados
                        }
                    }

                    // Llamar al callback con el número de pueblos visitados y el total
                    onSuccess(pueblosVisitados, totalPueblos)
                } else {
                    // No se encontraron visitas en el estado, retornar 0 en ambos
                    onSuccess(0, 0)
                }
            }
            .addOnFailureListener { exception ->
                // Manejar errores
                Log.e("FirestoreDBHelper", "Error obteniendo los pueblos: ${exception.message}")
                onFailure(exception)
            }
    }




}
