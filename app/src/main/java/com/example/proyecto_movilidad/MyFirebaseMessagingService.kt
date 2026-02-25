package com.example.proyecto_movilidad

import android.content.Intent
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

// Al extender FirebaseMessagingService, automáticamente extiendes android.app.Service
class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "Nuevo token generado: $token")
        // Aquí podrías enviar el token a tu base de datos si fuera necesario
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Si el mensaje trae datos, aquí podrías procesarlos
        Log.d("FCM", "Mensaje recibido de: ${remoteMessage.from}")

        remoteMessage.notification?.let {
            Log.d("FCM", "Cuerpo de la notificación: ${it.body}")
        }
    }
}