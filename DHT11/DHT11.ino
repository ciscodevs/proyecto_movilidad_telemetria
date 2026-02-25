#include <Servo.h>

// --- CONFIGURACIÓN DE PINES ---
const int pinPot = A0;
const int pinSensorChoque = 2;
const int pinVibrador = 3;
const int pinBuzzer = 4;
const int pinServo = 10; 

// --- VARIABLES DE CONTROL ---
float nivelBateria = 100.0;
unsigned long tiempoUltimoConsumo = 0;
unsigned long tiempoInicioAlerta = 0;
bool sistemaBloqueado = false;
String mensajeEstado = "OPERATIVO";

Servo miServo;

void(* resetFunc) (void) = 0;

// --- FUNCIÓN PARA MOVIMIENTO SUAVE (EVITA REINICIOS POR USB) ---
void moverSuave(int fin) {
  int inicio = miServo.read();
  if (inicio < fin) {
    for (int pos = inicio; pos <= fin; pos++) {
      miServo.write(pos);
      delay(15); 
    }
  } else {
    for (int pos = inicio; pos >= fin; pos--) {
      miServo.write(pos);
      delay(15);
    }
  }
}

void setup() {
  Serial.begin(9600);
  pinMode(pinSensorChoque, INPUT_PULLUP);
  pinMode(pinVibrador, OUTPUT);
  pinMode(pinBuzzer, OUTPUT);

  miServo.attach(pinServo);
  miServo.write(0); 
  delay(500);
}

void loop() {
  unsigned long tiempoActual = millis();

  // --- ESCUCHAR COMANDOS DESDE PC (LÓGICA DE ESTADO PERSISTENTE) ---
  if (Serial.available() > 0) {
    char comando = Serial.read();

    // Lógica para Cajuela
    if (comando == 'A') {         // Switch activado en App
      moverSuave(180); 
      digitalWrite(pinBuzzer, HIGH); delay(100); digitalWrite(pinBuzzer, LOW);
    }
    else if (comando == 'a') {    // Switch desactivado en App (REGRESA)
      moverSuave(0); 
    }

    // Lógica para Seguros
    else if (comando == 'S') {    // Switch activado en App
      moverSuave(90); 
      digitalWrite(pinBuzzer, HIGH); delay(50); digitalWrite(pinBuzzer, LOW);
      delay(50);
      digitalWrite(pinBuzzer, HIGH); delay(50); digitalWrite(pinBuzzer, LOW);
    }
    else if (comando == 's') {    // Switch desactivado en App (REGRESA)
      moverSuave(0); 
    }

    // Comando de cierre total
    else if (comando == 'C') { 
      moverSuave(0);
    }

    while (Serial.available() > 0) Serial.read();
  }

  // 1. GESTIÓN DE BATERÍA
  int lecturaPot = analogRead(pinPot);
  if (tiempoActual - tiempoUltimoConsumo >= 1000) {
    int velAux = map(lecturaPot, 0, 1023, 0, 180);
    nivelBateria -= (velAux * 0.008) + 0.02;
    if (nivelBateria < 0) nivelBateria = 0;
    tiempoUltimoConsumo = tiempoActual;
  }

  // 2. VARIABLES DE TELEMETRÍA
  int velocidad = 0, rpm = 0;
  float tMotor = 20.0, tInv = 20.0, tBat = 20.0;

  // 3. DETECCIÓN DE IMPACTO
  if (digitalRead(pinSensorChoque) == LOW && !sistemaBloqueado && nivelBateria > 0) {
    sistemaBloqueado = true;
    mensajeEstado = "COLISION_DETECTADA";
    tiempoInicioAlerta = tiempoActual;
    moverSuave(90); 
  }

  // 4. LÓGICA DE OPERACIÓN
  if (nivelBateria > 0) {
    if (!sistemaBloqueado) {
      velocidad = map(lecturaPot, 0, 1023, 0, 180);
      rpm = velocidad * 45;
      tMotor = 25.0 + (velocidad * 0.42);
      tInv = 25.0 + (velocidad * 0.30);
      tBat = 25.0 + (velocidad * 0.20);
      mensajeEstado = "OPERATIVO";

      if (velocidad > 120 || tMotor > 90.0) {
        digitalWrite(pinBuzzer, HIGH);
        delay(50);
        digitalWrite(pinBuzzer, LOW);
      }
    }
    else {
      velocidad = 0; 
      rpm = 0;
      tMotor = 20.0; tInv = 20.0; tBat = 20.0;
      digitalWrite(pinVibrador, HIGH);
      digitalWrite(pinBuzzer, HIGH);

      if (tiempoActual - tiempoInicioAlerta >= 4000) {
        digitalWrite(pinVibrador, LOW);
        digitalWrite(pinBuzzer, LOW);
        moverSuave(0);
        delay(1000);
        resetFunc();
      }
    }
  } 
  else {
    mensajeEstado = "SIN_BATERIA";
    velocidad = 0;
  }

  // 5. ENVÍO DE JSON
  Serial.print("{\"telemetria\": {");
  Serial.print("\"estado\":\""); Serial.print(mensajeEstado); Serial.print("\",");
  Serial.print("\"velocidad\":"); Serial.print(velocidad);
  Serial.print(", \"rpm\":"); Serial.print(rpm);
  Serial.print(", \"temp_motor\":"); Serial.print(tMotor);
  Serial.print(", \"temp_inversor\":"); Serial.print(tInv);
  Serial.print(", \"temp_bateria\":"); Serial.print(tBat);
  Serial.print(", \"bateria_porcentaje\":"); Serial.print((int)nivelBateria);
  Serial.println("}}");

  delay(100);
}