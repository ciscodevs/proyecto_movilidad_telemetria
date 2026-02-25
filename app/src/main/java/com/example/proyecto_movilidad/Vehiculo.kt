package com.example.proyecto_movilidad

data class Telemetria(
    val bateria_porcentaje: Double = 0.0,
    val velocidad: Int = 0,
    val rpm: Int = 0,
    val temp_motor: Double = 0.0,
    val temp_inversor: Double = 0.0,
    val temp_bateria: Double = 0.0
)

data class Estado(
    val conectado: Boolean = false,
    val marcha: String = "",
    val ultima_actualizacion: String = ""
)

data class Ubicacion(
    val latitud: Double = 0.0,
    val longitud: Double = 0.0
)

data class Vehiculo(
    val id_vehiculo: String = "",
    val nombre: String = "",
    val estado: Estado = Estado(),
    val telemetria: Telemetria = Telemetria(),
    val ubicacion: Ubicacion = Ubicacion(),
    val controles: Controles = Controles()
)

data class Alerta(
    val mensaje: String = "",
    val esCritica: Boolean = false
)

data class Controles(
    val cajuela_abierta: Boolean = false,
    val seguros_desbloqueados: Boolean = false,
    val luces_encendidas: Boolean = false
)
