package com.example.proyecto_movilidad

import java.util.Date

data class Fallo(
    val componente: String = "",
    val valorAlcanzado: String = "",
    val fecha: Date = Date(),
    val nivel: String = "CRÍTICO"
)