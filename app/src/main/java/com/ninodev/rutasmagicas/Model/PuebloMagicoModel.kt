package com.ninodev.rutasmagicas.Model

import com.google.protobuf.Internal.BooleanList

class PuebloMagicoModel (
    var nombrePueblo: String = "",
    var imagen: String = "",
    var descripcion: String = "",
    var latitud : String = "",
    var longitud : String = "",
    var visita : Boolean = false,
    var visitaCertificada : Boolean = false
)
