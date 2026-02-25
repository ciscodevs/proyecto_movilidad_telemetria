package com.example.proyecto_movilidad

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import java.util.Date

class MainViewModel : ViewModel() {
    private val db by lazy { FirebaseFirestore.getInstance() }

    private val _vehiculo = mutableStateOf(Vehiculo())
    val vehiculo: State<Vehiculo> = _vehiculo

    private val _velocidadMaxima = mutableStateOf(0)
    val velocidadMaxima: State<Int> = _velocidadMaxima

    // --- ESTADO PARA ANIMACIÓN DE CONDUCCIÓN ---
    // Calculamos la intensidad de 0.0 a 1.0 para que la UI sepa cuánto vibrar
    private val _intensidadVibracion = mutableStateOf(0f)
    val intensidadVibracion: State<Float> = _intensidadVibracion

    val historialVelocidad = mutableStateListOf<Float>()
    val historialBateria = mutableStateListOf<Float>()
    val historialTemperatura = mutableStateListOf<Float>()

    private val _alertasActivas = mutableStateListOf<Alerta>()
    val alertasActivas: List<Alerta> = _alertasActivas

    // Candado para no registrar el mismo fallo repetidamente en la BD
    private var ultimoComponenteFallo: String = ""

    fun escucharVehiculo() {
        db.collection("vehiculos").document("BYD_DOLPHIN_01")
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null && snapshot.exists()) {
                    val nuevoVehiculo = snapshot.toObject<Vehiculo>() ?: Vehiculo()
                    _vehiculo.value = nuevoVehiculo

                    actualizarHistoriales(nuevoVehiculo)
                    actualizarIntensidadAnimacion(nuevoVehiculo.telemetria.velocidad)
                    verificarAlertas(nuevoVehiculo)
                }
            }
    }

    private fun actualizarIntensidadAnimacion(velocidad: Int) {
        // Mapeamos la velocidad (0-100) a un rango de vibración (0f-5f)
        _intensidadVibracion.value = if (velocidad > 0) (velocidad / 20f) else 0f
    }

    private fun actualizarHistoriales(v: Vehiculo) {
        val nuevaVelocidad = v.telemetria.velocidad.toFloat()
        historialVelocidad.add(nuevaVelocidad)
        if (nuevaVelocidad > _velocidadMaxima.value) {
            _velocidadMaxima.value = nuevaVelocidad.toInt()
        }
        if (historialVelocidad.size > 20) historialVelocidad.removeAt(0)

        historialBateria.add(v.telemetria.bateria_porcentaje.toFloat())
        if (historialBateria.size > 20) historialBateria.removeAt(0)

        historialTemperatura.add(v.telemetria.temp_motor.toFloat())
        if (historialTemperatura.size > 20) historialTemperatura.removeAt(0)
    }

    private fun verificarAlertas(v: Vehiculo) {
        val t = v.telemetria
        _alertasActivas.clear()
        var hayAlgunaAlertaCritica = false

        // MOTOR
        if (t.temp_motor > 90.0) {
            val msg = "¡SOBRECALENTAMIENTO MOTOR!"
            _alertasActivas.add(Alerta(msg, true))
            registrarFalloEnFirebase("Motor", "${t.temp_motor}°C", "CRÍTICO")
            hayAlgunaAlertaCritica = true
        }

        // BATERÍA
        if (t.temp_bateria > 55.0) {
            val msg = "TEMPERATURA BATERÍA ALTA"
            _alertasActivas.add(Alerta(msg, true))
            registrarFalloEnFirebase("Batería", "${t.temp_bateria}°C", "CRÍTICO")
            hayAlgunaAlertaCritica = true
        }

        // INVERSOR
        if (t.temp_inversor > 75.0) {
            _alertasActivas.add(Alerta("ADVERTENCIA: INVERSOR CALIENTE", true))
            hayAlgunaAlertaCritica = true
        }

        // BATERÍA BAJA
        if (t.bateria_porcentaje < 15.0) {
            _alertasActivas.add(Alerta("BATERÍA MUY BAJA", false))
            hayAlgunaAlertaCritica = true
        }

        // Si ya no hay fallos, liberamos el nombre del componente para permitir nuevos registros
        if (!hayAlgunaAlertaCritica) {
            ultimoComponenteFallo = ""
        }
    }

    private fun registrarFalloEnFirebase(componente: String, valor: String, nivel: String) {
        // Evitamos duplicados: si el último fallo registrado es el mismo, no hacemos nada
        if (ultimoComponenteFallo == componente) return

        val nuevoFallo = hashMapOf(
            "componente" to componente,
            "valorAlcanzado" to valor,
            "nivel" to nivel,
            "fecha" to Date()
        )

        db.collection("historial_fallos")
            .add(nuevoFallo)
            .addOnSuccessListener {
                ultimoComponenteFallo = componente
            }
    }

    fun alternarSeguros(actual: Boolean) {
        db.collection("vehiculos").document("BYD_DOLPHIN_01")
            .update("controles.seguros_desbloqueados", !actual)
    }

    fun abrirCajuela() {
        val docRef = db.collection("vehiculos").document("BYD_DOLPHIN_01")
        docRef.update("controles.cajuela_abierta", true)
            .addOnSuccessListener {
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    docRef.update("controles.cajuela_abierta", false)
                }, 2000)
            }
    }
}