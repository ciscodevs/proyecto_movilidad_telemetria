package com.example.proyecto_movilidad

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.proyecto_movilidad.ui.theme.Proyecto_movilidadTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging // <--- Importante

class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        FirebaseApp.initializeApp(this)
        super.onCreate(savedInstanceState)

        // --- CONFIGURACIÓN DE NOTIFICACIONES ---
        crearCanalNotificaciones()
        solicitarPermisosNotificacion()
        configurarSuscripcionPush() // <--- Nueva función

        setContent {
            Proyecto_movilidadTheme {
                var showSplash by remember { mutableStateOf(true) }
                var usuarioLogueado by remember {
                    mutableStateOf(FirebaseAuth.getInstance().currentUser != null)
                }
                var mostrarHistorial by remember { mutableStateOf(false) }

                AnimatedContent(
                    targetState = showSplash,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(1000)) togetherWith
                                fadeOut(animationSpec = tween(1000))
                    },
                    label = "TransicionPrincipal"
                ) { targetShowSplash ->
                    if (targetShowSplash) {
                        SplashScreen(onTimeout = { showSplash = false })
                    } else {
                        if (!usuarioLogueado) {
                            LoginScreen(
                                authViewModel = authViewModel,
                                onLoginSuccess = { usuarioLogueado = true }
                            )
                        } else {
                            LaunchedEffect(Unit) {
                                viewModel.escucharVehiculo()
                            }

                            // --- LOGICA DE NOTIFICACIONES LOCALES ---
                            LaunchedEffect(viewModel.alertasActivas.size) {
                                if (viewModel.alertasActivas.any { it.esCritica }) {
                                    enviarNotificacionAlerta(viewModel.alertasActivas.first().mensaje)
                                }
                            }

                            AnimatedContent(
                                targetState = mostrarHistorial,
                                transitionSpec = {
                                    slideInHorizontally(animationSpec = tween(500)) { it } + fadeIn() togetherWith
                                            slideOutHorizontally(animationSpec = tween(500)) { -it } + fadeOut()
                                },
                                label = "NavegacionInterna"
                            ) { targetMostrarHistorial ->
                                if (targetMostrarHistorial) {
                                    HistoryScreen(
                                        historialVelocidad = viewModel.historialVelocidad,
                                        historialBateria = viewModel.historialBateria,
                                        historialTemp = viewModel.historialTemperatura,
                                        onBack = { mostrarHistorial = false }
                                    )
                                } else {
                                    val vehiculoEstado by viewModel.vehiculo
                                    val historialVelocidad = viewModel.historialVelocidad
                                    val maxVel = viewModel.velocidadMaxima.value
                                    val alertasActivas = viewModel.alertasActivas

                                    Scaffold { innerPadding ->
                                        DashboardScreen(
                                            vehiculo = vehiculoEstado,
                                            historial = historialVelocidad,
                                            velocidadMaxima = maxVel,
                                            alertas = alertasActivas,
                                            authViewModel = authViewModel,
                                            onVerHistorial = { mostrarHistorial = true },
                                            onLogout = {
                                                FirebaseAuth.getInstance().signOut()
                                                usuarioLogueado = false
                                            },
                                            onAbrirCajuela = { viewModel.abrirCajuela() },
                                            onAlternarSeguros = { actual -> viewModel.alternarSeguros(actual) },
                                            mainViewModel = viewModel, // <-- Pasar el ViewModel aquí
                                            modifier = Modifier.padding(innerPadding)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun configurarSuscripcionPush() {
        // Nos suscribimos al tema "alertas" para recibir mensajes grupales de la consola
        FirebaseMessaging.getInstance().subscribeToTopic("alertas")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("FCM", "Suscrito exitosamente al tema: alertas")
                } else {
                    Log.e("FCM", "Error al suscribirse", task.exception)
                }
            }
    }

    private fun crearCanalNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Alertas Vehículo"
            val descriptionText = "Notificaciones de telemetría crítica"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("ALERTA_VEHICULO", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun enviarNotificacionAlerta(mensaje: String) {
        val builder = NotificationCompat.Builder(this, "ALERTA_VEHICULO")
            .setSmallIcon(R.drawable.logo_proyecto)
            .setContentTitle("⚠️ Alerta BYD Dolphin")
            .setContentText(mensaje)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(this)) {
            if (ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                notify(1001, builder.build())
            }
        }
    }

    private fun solicitarPermisosNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }
}