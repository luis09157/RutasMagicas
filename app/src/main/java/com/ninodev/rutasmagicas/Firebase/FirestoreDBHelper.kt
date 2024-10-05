package com.ninodev.rutasmagicas.Firebase

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.ninodev.rutasmagicas.Fragment.Home.HomeFragment
import com.ninodev.rutasmagicas.Fragment.Municipios.PueblosMagicosFragment
import com.ninodev.rutasmagicas.Fragment.PuebloMagico.PuebloMagicoDetalleFragment
import com.ninodev.rutasmagicas.Helper.HelperUser
import com.ninodev.rutasmagicas.Model.EstadoModel
import com.ninodev.rutasmagicas.Model.MunicipioModel
import com.ninodev.rutasmagicas.Model.PuebloMagicoModel
import java.io.ByteArrayOutputStream
import java.util.UUID

class FirestoreDBHelper {

    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    companion object{
        val _URL_STORAGE_FIREBASE = "https://firebasestorage.googleapis.com/v0/b/rutasmagicas-2514a.appspot.com/o/"
    }

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

                // Iterar sobre los documentos y contar aquellos donde 'visita' o 'verificado' es true
                visitasSnapshot.documents.forEach { document ->
                    Log.d("FirestoreDBHelper", "Documento: ${document.id}, Datos: ${document.data}")

                    // Verificar si el campo 'visita' o 'verificado' es true
                    val visita = document.getBoolean("visita") == true
                    val verificado = document.getBoolean("verificado") == true

                    if (visita || verificado) {
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
                        // Actualizar los campos 'visita', 'verificado' y 'imagenVerificada' en Firestore
                        document.reference.update(
                            mapOf(
                                "visita" to newVisita,
                                "verificado" to false, // Asegurar que 'verificado' esté siempre en false
                                "imagenVerificada" to if (newVisita) "" else document.getString("imagenVerificada") // Establecer imagenVerificada a "" si visita es true
                            )
                        ).addOnSuccessListener {
                            onSuccess(newVisita) // Notificar el éxito
                        }.addOnFailureListener { e ->
                            Log.e("FirestoreDBHelper", "Error actualizando la visita: ${e.message}")
                            onFailure(e) // Notificar el fallo
                        }
                    }
                } else {
                    // Si no se encuentra ningún documento, agregar uno nuevo
                    val newVisitaData = mapOf(
                        "nombreEstado" to estado,
                        "nombreMunicipio" to municipio,
                        "visita" to true,
                        "verificado" to false, // Forzar verificado a false en la creación
                        "imagenVerificada" to "" // Inicializar imagenVerificada como "" al crear
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
        onVisitFound: (visita: Boolean, verificado: Boolean, imagenVerificada: String) -> Unit, // Callback actualizado para devolver tres valores
        onVisitNotFound: () -> Unit, // Callback si no hay visitas encontradas
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
                        // Obtener los valores de "visita", "verificado", y "imagenVerificada"
                        val visita = document.getBoolean("visita") ?: false
                        val verificado = document.getBoolean("verificado") ?: false
                        val imagenVerificada = document.getString("imagenVerificada") ?: "" // Obtener la URL de la imagen

                        // Devolver los valores mediante el callback
                        onVisitFound(visita, verificado, imagenVerificada)
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
    fun getNombreUsuario(
        idUsuario: String,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        // Referencia a la colección de usuarios
        val userDocRef = firestore.collection("RutasMagicas")
            .document("RegistroUsuarios")
            .collection("Usuarios")
            .document(idUsuario)

        // Obtener el documento del usuario
        userDocRef.get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    // Obtener el campo "nombreUsuario"
                    val nombreUsuario = documentSnapshot.getString("nombreUsuario")
                    if (nombreUsuario != null) {
                        // Notificar éxito pasando el nombre del usuario
                        onSuccess(nombreUsuario)
                    } else {
                        // Si el campo no existe, lanzar una excepción controlada
                        onFailure(Exception("El campo 'nombreUsuario' no se encuentra en el documento"))
                    }
                } else {
                    // Si el documento no existe, lanzar una excepción
                    onFailure(Exception("El usuario con ID $idUsuario no existe"))
                }
            }
            .addOnFailureListener { exception ->
                // Manejar cualquier error que ocurra durante la obtención del documento
                Log.e("FirestoreDBHelper", "Error obteniendo nombreUsuario: ${exception.message}")
                onFailure(exception)
            }
    }
    fun toggleVisitaCertificado(
        userId: String,
        estado: String,
        municipio: String,
        puebloMagico: PuebloMagicoModel,
        imageBitmap: Bitmap, // Imagen que se va a subir
        onSuccess: (Boolean) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        // Establecer visita en false
        val visita = false
        puebloMagico.visita = visita

        // Referencia a la colección "Visitas" dentro del usuario
        val collectionRef = firestore.collection("RutasMagicas")
            .document("VisitasPueblosMagicos")
            .collection("Usuarios")
            .document(userId)
            .collection("Visitas")

        // Subir la imagen a Firebase antes de cualquier actualización en Firestore
        uploadImageToFirebase(imageBitmap, puebloMagico.imagenVerificada, { imageUrl ->
            // Guardar la URL de la imagen verificada en el modelo
            puebloMagico.imagenVerificada = imageUrl

            // Consulta para buscar el documento que coincide con el estado y municipio
            val query = collectionRef
                .whereEqualTo("nombreEstado", estado)
                .whereEqualTo("nombreMunicipio", municipio)

            // Ejecutar la consulta para actualizar o crear el documento en Firestore
            query.get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) {
                        // Recorrer los resultados de la consulta
                        for (document in querySnapshot) {
                            // Toggle de la variable 'verificado' y establecer 'visita' en false
                            val currentVerificado = document.getBoolean("verificado") ?: false
                            val newVerificado = !currentVerificado

                            // Actualizar el documento en Firestore
                            document.reference.update(
                                "visita", visita,
                                "verificado", newVerificado,
                                "imagenVerificada", if (newVerificado) imageUrl else ""
                            ).addOnSuccessListener {
                                onSuccess(newVerificado) // Notificar el éxito
                            }.addOnFailureListener { e ->
                                Log.e("FirestoreDBHelper", "Error actualizando la visita: ${e.message}")
                                onFailure(e) // Notificar el fallo
                            }
                        }
                    } else {
                        // Si no se encuentra ningún documento, agregar uno nuevo
                        val newVisitaData = mapOf(
                            "nombreEstado" to estado,
                            "nombreMunicipio" to municipio,
                            "visita" to visita, // Establecer visita en false para nuevo registro
                            "verificado" to true, // Inicialmente como true para nuevo registro
                            "imagenVerificada" to imageUrl // Guardar la URL de la imagen verificada
                        )

                        collectionRef.add(newVisitaData)
                            .addOnSuccessListener {
                                puebloMagico.visita = false // Actualizar el modelo
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
        }, onFailure) // Aquí se pasa correctamente el onFailure
    }


    fun uploadImageToFirebase(
        imageBitmap: Bitmap,
        previousImageUrl: String?, // URL de la imagen anterior
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (imageBitmap == null) {
            Log.e("FirestoreDBHelper", "El Bitmap es nulo.")
            onFailure(Exception("El Bitmap es nulo."))
            return
        }

        val storageRef = FirebaseStorage.getInstance().reference
        val uniqueID = UUID.randomUUID().toString()
        val fileName = "Certificaciones/${HelperUser.getUserId()}/${PueblosMagicosFragment._ESTADO.nombreEstado}/${PuebloMagicoDetalleFragment._PUEBLO_MAGICO.nombrePueblo}/$uniqueID.jpg"
        val imageRef = storageRef.child(fileName)

        // Si hay una URL anterior, intentamos eliminar la imagen anterior
        previousImageUrl?.let { url ->
            if (url.isNotEmpty()) {
                val previousImageRef = storageRef.storage.getReferenceFromUrl(url)
                Log.d("FirestoreDBHelper", "Intentando eliminar la imagen anterior en: $url")

                previousImageRef.delete()
                    .addOnSuccessListener {
                        Log.d("FirestoreDBHelper", "Imagen anterior eliminada correctamente.")
                        uploadNewImage(imageRef, imageBitmap, onSuccess, onFailure) // Subir la nueva imagen
                    }
                    .addOnFailureListener { exception ->
                        Log.e("FirestoreDBHelper", "Error eliminando la imagen anterior: ${exception.message}", exception)
                        onFailure(exception) // Llamar al onFailure aquí si no se puede eliminar
                    }
            } else {
                Log.e("FirestoreDBHelper", "La URL de la imagen anterior está vacía.")
                uploadNewImage(imageRef, imageBitmap, onSuccess, onFailure) // Subir la nueva imagen si no hay URL
            }
        } ?: run {
            Log.d("FirestoreDBHelper", "No hay imagen anterior, subiendo una nueva.")
            uploadNewImage(imageRef, imageBitmap, onSuccess, onFailure) // Subir la nueva imagen si no hay URL
        }
    }

    // Función para subir una nueva imagen
    private fun uploadNewImage(
        imageRef: StorageReference,
        imageBitmap: Bitmap,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val baos = ByteArrayOutputStream()
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        imageRef.putBytes(data)
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    onSuccess(uri.toString()) // Notificar el éxito
                }.addOnFailureListener { exception ->
                    Log.e("FirestoreDBHelper", "Error obteniendo la URL de la imagen subida: ${exception.message}")
                    onFailure(exception)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("FirestoreDBHelper", "Error subiendo la imagen: ${exception.message}")
                onFailure(exception)
            }
    }

    fun toggleVisitaCertificadoSinImagen(
        userId: String,
        estado: String,
        municipio: String,
        puebloMagico: PuebloMagicoModel,
        onSuccess: (Boolean) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        // Establecer visita en false
        val visita = false
        puebloMagico.visita = visita

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

        query.get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    for (document in querySnapshot) {
                        // Toggle de la variable 'verificado' y establecer 'visita' en false
                        val currentVerificado = document.getBoolean("verificado") ?: false
                        val newVerificado = !currentVerificado

                        // Actualizar el documento en Firestore eliminando la imagen anterior si existe
                        document.reference.update(
                            "visita", visita,
                            "verificado", newVerificado,
                            "imagenVerificada", "" // Borrar la imagen si no hay una nueva
                        ).addOnSuccessListener {
                            onSuccess(newVerificado) // Notificar el éxito
                        }.addOnFailureListener { e ->
                            Log.e("FirestoreDBHelper", "Error actualizando la visita: ${e.message}")
                            onFailure(e) // Notificar el fallo
                        }
                    }
                } else {
                    // Si no se encuentra ningún documento, agregar uno nuevo sin imagen
                    val newVisitaData = mapOf(
                        "nombreEstado" to estado,
                        "nombreMunicipio" to municipio,
                        "visita" to visita, // Establecer visita en false para nuevo registro
                        "verificado" to true, // Inicialmente como true para nuevo registro
                        "imagenVerificada" to "" // Sin imagen
                    )

                    collectionRef.add(newVisitaData)
                        .addOnSuccessListener {
                            puebloMagico.visita = false // Actualizar el modelo
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



}
