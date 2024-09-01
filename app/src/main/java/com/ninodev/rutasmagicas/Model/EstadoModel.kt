package com.ninodev.rutasmagicas.Model

import java.util.UUID

data class EstadoModel(
    var id: String = UUID.randomUUID().toString(),
    var nombreEstado: String = "",
    var descripcion: String = "",
    var imagen: String = "",
    var numeroPueblos: String = "",
    var municipios: MutableList<MunicipioModel> = mutableListOf() // Lista mutable de municipios
)
