package com.example.proyecto_movilidad

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HistoryScreen(
    historialVelocidad: List<Float>,
    historialBateria: List<Float>,
    historialTemp: List<Float>,
    onBack: () -> Unit
) {
    val teslaDark = Color(0xFF111111)

    Surface(color = teslaDark, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            // --- HEADER ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(50))
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "ANÁLISIS DE TELEMETRÍA",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Tendencias históricas por componente",
                        color = Color.Gray,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            // --- LISTA DE GRÁFICAS ---
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                item {
                    HistoryCard(
                        title = "NIVEL DE CARGA (SOC)",
                        datos = historialBateria,
                        colorLine = Color(0xFF32CD32), // Verde Tesla
                        unit = "%"
                    )
                }
                item {
                    HistoryCard(
                        title = "TEMPERATURA DEL SISTEMA",
                        datos = historialTemp,
                        colorLine = Color(0xFFE82127), // Rojo Tesla
                        unit = "°C"
                    )
                }
                item {
                    HistoryCard(
                        title = "VELOCIDAD DE CRUCERO",
                        datos = historialVelocidad,
                        colorLine = Color(0xFF00BFFF), // Azul Eléctrico
                        unit = "km/h"
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryCard(title: String, datos: List<Float>, colorLine: Color, unit: String) {
    val teslaGray = Color(0xFF1E1E1E)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = teslaGray),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                color = Color.Gray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            val maxVal = if (datos.isNotEmpty()) datos.maxOrNull() ?: 0f else 0f
            Text(
                text = "${maxVal.toInt()} $unit",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Light
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Dibujo de la gráfica mejorada
            HistoryLineChart(datos = datos, colorLine = colorLine)

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("LUN", color = Color.DarkGray, fontSize = 10.sp)
                Text("MAR", color = Color.DarkGray, fontSize = 10.sp)
                Text("MIE", color = Color.DarkGray, fontSize = 10.sp)
                Text("JUE", color = Color.DarkGray, fontSize = 10.sp)
                Text("VIE", color = Color.DarkGray, fontSize = 10.sp)
                Text("SAB", color = Color.DarkGray, fontSize = 10.sp)
                Text("DOM", color = Color.DarkGray, fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun HistoryLineChart(datos: List<Float>, colorLine: Color) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
    ) {
        if (datos.size < 2) return@Canvas

        val xSpacing = size.width / (datos.size - 1)
        val maxVal = (datos.maxOrNull() ?: 1f).coerceAtLeast(1f)

        val strokePath = Path().apply {
            datos.forEachIndexed { index, value ->
                val x = index * xSpacing
                val y = size.height - (value / maxVal * size.height)
                if (index == 0) moveTo(x, y) else lineTo(x, y)
            }
        }

        // 1. Dibujar el degradado de fondo (Área bajo la curva)
        val fillPath = Path().apply {
            addPath(strokePath)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(colorLine.copy(alpha = 0.3f), Color.Transparent)
            )
        )

        // 2. Dibujar la línea principal (Brillante)
        drawPath(
            path = strokePath,
            color = colorLine,
            style = Stroke(
                width = 3.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // 3. Dibujar puntos en los nodos para efecto "Tech"
        datos.forEachIndexed { index, value ->
            val x = index * xSpacing
            val y = size.height - (value / maxVal * size.height)
            drawCircle(
                color = colorLine,
                radius = 2.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}