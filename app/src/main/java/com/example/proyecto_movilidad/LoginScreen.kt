package com.example.proyecto_movilidad

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LoginScreen(authViewModel: AuthViewModel, onLoginSuccess: () -> Unit) {
    // Paleta de colores Tesla
    val teslaDark = Color(0xFF111111)
    val accentRed = Color(0xFFE82127)
    val teslaGray = Color(0xFF222222)

    // --- LÓGICA DE LIMPIEZA Y ENFOQUE ---
    val focusRequester = remember { FocusRequester() }
    var visible by remember { mutableStateOf(false) }

    // Este efecto se dispara cada vez que la pantalla aparece
    LaunchedEffect(Unit) {
        // 1. Limpieza total de credenciales anteriores
        authViewModel.email.value = ""
        authViewModel.password.value = ""
        authViewModel.errorMessage.value = null

        // 2. Disparar animaciones
        visible = true

        // 3. Poner el cursor en el correo automáticamente
        focusRequester.requestFocus()
    }

    Surface(color = teslaDark, modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(1000)) +
                    slideInVertically(animationSpec = tween(1000), initialOffsetY = { it / 10 }),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // --- LOGOTIPO ---
                Image(
                    painter = painterResource(id = R.drawable.logo_app),
                    contentDescription = "Logo Proyecto Movilidad",
                    modifier = Modifier
                        .width(500.dp)
                        .height(150.dp),
                    contentScale = ContentScale.Fit
                )

                Text(
                    text = "PROJECT BYD DOLPHIN",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(top = 12.dp)
                )

                Spacer(modifier = Modifier.height(50.dp))

                // --- FORMULARIO ---
                Text(
                    text = "CORREO ELECTRÓNICO",
                    color = Color.Gray,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.Start).padding(start = 4.dp, bottom = 8.dp)
                )
                TextField(
                    value = authViewModel.email.value,
                    onValueChange = { authViewModel.email.value = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester), // Vincular el foco aquí
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = teslaGray,
                        unfocusedContainerColor = teslaGray,
                        focusedIndicatorColor = Color.White,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(4.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "CONTRASEÑA",
                    color = Color.Gray,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.Start).padding(start = 4.dp, bottom = 8.dp)
                )
                TextField(
                    value = authViewModel.password.value,
                    onValueChange = { authViewModel.password.value = it },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = teslaGray,
                        unfocusedContainerColor = teslaGray,
                        focusedIndicatorColor = Color.White,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(4.dp)
                )

                // Mensaje de error
                authViewModel.errorMessage.value?.let {
                    Text(
                        text = it.uppercase(),
                        color = accentRed,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))

                // --- BOTÓN DE ACCIÓN ---
                if (authViewModel.isLoading.value) {
                    CircularProgressIndicator(color = Color.White)
                } else {
                    Button(
                        onClick = { authViewModel.loginUsuario(onLoginSuccess) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "INICIAR SESIÓN",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}