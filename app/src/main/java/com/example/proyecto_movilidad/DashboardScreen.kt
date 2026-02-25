package com.example.proyecto_movilidad

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DashboardScreen(
    vehiculo: Vehiculo,
    historial: List<Float>,
    velocidadMaxima: Int,
    alertas: List<Alerta>,
    authViewModel: AuthViewModel,
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onVerHistorial: () -> Unit,
    onLogout: () -> Unit,
    onAbrirCajuela: () -> Unit = {},
    onAlternarSeguros: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val accentRed = Color(0xFFE82127)
    val teslaDark = Color(0xFF111111)

    // --- LÓGICA DE ANIMACIÓN AVANZADA ---
    val velocidadActual = vehiculo.telemetria.velocidad
    val intensidad by mainViewModel.intensidadVibracion
    val infiniteTransition = rememberInfiniteTransition(label = "KinetixEngine")

    // 1. Vibración dinámica
    val offsetAnimado by infiniteTransition.animateFloat(
        initialValue = -intensidad,
        targetValue = intensidad,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 50, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Vibration"
    )

    // 2. Escala Elástica (Efecto de empuje al acelerar)
    val escalaVelocidad by animateFloatAsState(
        targetValue = 1f + (velocidadActual / 600f),
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "ScalePush"
    )

    // 3. Motion Blur (Desenfoque de movimiento para Android 12+)
    val desenfoqueDinamico by animateFloatAsState(
        targetValue = if (velocidadActual > 60) (velocidadActual / 25f) else 0f,
        animationSpec = tween(400),
        label = "BlurMotion"
    )

    val hayAlertaCritica = alertas.any { it.esCritica }
    val glowColor by animateColorAsState(
        targetValue = if (hayAlertaCritica) accentRed.copy(alpha = 0.25f) else Color.Transparent,
        animationSpec = tween(1000),
        label = "AlertGlow"
    )

    Surface(color = teslaDark, modifier = Modifier.fillMaxSize()) {
        Column(modifier = modifier.fillMaxSize().padding(horizontal = 20.dp)) {

            // --- TOP STATUS BAR (BATERÍA) ---
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.BatteryFull, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.weight(1f).height(4.dp).background(Color.DarkGray, RoundedCornerShape(2.dp))) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(((vehiculo.telemetria.bateria_porcentaje / 100f).toFloat()))
                            .fillMaxHeight()
                            .background(
                                if (vehiculo.telemetria.bateria_porcentaje < 20) accentRed else Color(0xFF32CD32),
                                RoundedCornerShape(2.dp)
                            )
                    )
                }
                Text("  ${vehiculo.telemetria.bateria_porcentaje}%", color = Color.White, style = MaterialTheme.typography.labelSmall)
            }

            // --- HEADER ---
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = vehiculo.nombre.uppercase().ifEmpty { "BYD DOLPHIN" }, letterSpacing = 2.sp, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("CONNECTED", color = Color(0xFF32CD32), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onVerHistorial, modifier = Modifier.background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(50))) {
                        Icon(Icons.Default.Analytics, contentDescription = "Historial", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))

                    // BOTÓN CERRAR SESIÓN ACTUALIZADO
                    IconButton(
                        onClick = {
                            // 1. Limpieza de credenciales en el ViewModel
                            authViewModel.email.value = ""
                            authViewModel.password.value = ""
                            authViewModel.errorMessage.value = null

                            // 2. Notificar a la navegación
                            onLogout()
                        },
                        modifier = Modifier.background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(50))
                    ) {
                        Icon(Icons.Default.PowerSettingsNew, null, tint = accentRed)
                    }
                }
            }

            // --- ALERTS ---
            alertas.take(1).forEach { alerta ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = if (alerta.esCritica) accentRed else Color(0xFFF4B400)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(alerta.mensaje, color = Color.White, modifier = Modifier.padding(12.dp).align(Alignment.CenterHorizontally), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            // --- VELOCÍMETRO CINÉTICO ---
            Box(
                modifier = Modifier.fillMaxWidth().weight(1.5f),
                contentAlignment = Alignment.Center
            ) {
                if (hayAlertaCritica) {
                    Box(modifier = Modifier.size(280.dp).blur(45.dp).background(glowColor, CircleShape))
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.graphicsLayer {
                        scaleX = escalaVelocidad
                        scaleY = escalaVelocidad
                        translationY = offsetAnimado

                        // Aplicar Blur solo en Android 12+ (API 31+)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && desenfoqueDinamico > 0) {
                            renderEffect = android.graphics.RenderEffect.createBlurEffect(
                                desenfoqueDinamico, 0f, android.graphics.Shader.TileMode.CLAMP
                            ).asComposeRenderEffect()
                        }
                    }
                ) {
                    Text("${vehiculo.telemetria.velocidad}", fontSize = 110.sp, fontWeight = FontWeight.ExtraLight, color = Color.White)
                    Text("km/h", fontSize = 16.sp, color = Color.Gray.copy(alpha = 0.5f), letterSpacing = 6.sp)

                    // Estelas de viento animadas (Partículas de velocidad)
                    if (velocidadActual > 30) {
                        Box(Modifier.height(20.dp).width(120.dp), contentAlignment = Alignment.Center) {
                            repeat(4) { i ->
                                val xPos by infiniteTransition.animateFloat(
                                    initialValue = -60f,
                                    targetValue = 60f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(durationMillis = 500, delayMillis = i * 120),
                                        repeatMode = RepeatMode.Restart
                                    ),
                                    label = "WindParticle"
                                )
                                Box(
                                    Modifier.offset(x = xPos.dp)
                                        .width(15.dp).height(1.dp)
                                        .background(Color.White.copy(alpha = 0.1f))
                                )
                            }
                        }
                    }
                }
            }

            // --- REJILLA DE SENSORES ---
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { TeslaSensorItem("MOTOR", "${vehiculo.telemetria.temp_motor}°", vehiculo.telemetria.temp_motor > 90) }
                item { TeslaSensorItem("BATTERY", "${vehiculo.telemetria.temp_bateria}°", vehiculo.telemetria.temp_bateria > 55) }
                item { TeslaSensorItem("INVERTER", "${vehiculo.telemetria.temp_inversor}°", vehiculo.telemetria.temp_inversor > 75) }
                item { TeslaSensorItem("RPM", vehiculo.telemetria.rpm.toString(), false) }
            }

            // --- CONTROLES INFERIORES ---
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                TeslaControlBtn(
                    icon = if (vehiculo.controles.seguros_desbloqueados) Icons.Default.LockOpen else Icons.Default.Lock,
                    label = "SEGUROS",
                    active = vehiculo.controles.seguros_desbloqueados,
                    onClick = { onAlternarSeguros(vehiculo.controles.seguros_desbloqueados) }
                )
                TeslaControlBtn(icon = Icons.Default.DirectionsCar, label = "CAJUELA", active = vehiculo.controles.cajuela_abierta, onClick = onAbrirCajuela)
                TeslaControlBtn(icon = Icons.Default.Map, label = "MAPA", active = false, onClick = {
                    val uri = "geo:${vehiculo.ubicacion.latitud},${vehiculo.ubicacion.longitud}?q=${vehiculo.ubicacion.latitud},${vehiculo.ubicacion.longitud}"
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
                })
            }
        }
    }
}

@Composable
fun TeslaSensorItem(label: String, value: String, isAlert: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Spacer(Modifier.height(4.dp))
        Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Medium, color = if (isAlert) Color(0xFFE82127) else Color.White)
    }
}

@Composable
fun TeslaControlBtn(icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    val activeGreen = Color(0xFF32CD32)
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Box(
            modifier = Modifier.size(60.dp).background(if (active) activeGreen else Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = if (active) Color.White else Color.White.copy(alpha = 0.6f), modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = if (active) activeGreen else Color.Gray)
    }
}