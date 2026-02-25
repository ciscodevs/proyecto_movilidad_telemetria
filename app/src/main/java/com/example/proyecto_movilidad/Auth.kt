package com.example.proyecto_movilidad

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()

    var email = mutableStateOf("")
    var password = mutableStateOf("")
    var isLoading = mutableStateOf(false)
    var errorMessage = mutableStateOf<String?>(null)

    fun loginUsuario(onSuccess: () -> Unit) {
        if (email.value.isEmpty() || password.value.isEmpty()) {
            errorMessage.value = "Por favor, llena todos los campos"
            return
        }

        isLoading.value = true
        auth.signInWithEmailAndPassword(email.value, password.value)
            .addOnCompleteListener { task ->
                isLoading.value = false
                if (task.isSuccessful) {
                    onSuccess()
                } else {
                    errorMessage.value = "Error: ${task.exception?.message}"
                }
            }
    }
}