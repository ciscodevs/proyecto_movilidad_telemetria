import serial
import paho.mqtt.client as mqtt
import firebase_admin
from firebase_admin import credentials, firestore
import json
import time

# --- 1. CONFIGURACIÓN DE FIREBASE ---
cred = credentials.Certificate("serviceAccount.json")
if not firebase_admin._apps:
    firebase_admin.initialize_app(cred)
db = firestore.client()
vehiculo_ref = db.collection('vehiculos').document('BYD_DOLPHIN_01')

# Variables de memoria para no saturar el serial
ultimo_estado_seguros = None
ultimo_estado_cajuela = None

# --- 2. CONFIGURACIÓN MQTT ---
MQTT_BROKER = "192.168.100.120" 
MQTT_TOPIC = "movilidad/vehiculo1"
client = mqtt.Client()

try:
    client.connect(MQTT_BROKER, 1883, 60)
    client.loop_start() # Importante: loop_start permite que MQTT corra en segundo plano
    print(f"✅ Conectado al Broker MQTT: {MQTT_BROKER}")
except Exception as e:
    print(f"❌ Error MQTT: {e}")

# --- 3. CONFIGURACIÓN DEL ARDUINO ---
ser = None
puertos = ['/dev/ttyACM0', '/dev/ttyUSB0']

for p in puertos:
    try:
        ser = serial.Serial(p, 9600, timeout=1)
        print(f"✅ Arduino detectado en {p}")
        break
    except:
        continue

if not ser:
    print("❌ Error: No se encontró el Arduino.")
    exit()

# --- 4. FUNCIÓN LISTENER (CONTROL DE BOTONES) ---
def on_snapshot(doc_snapshot, changes, read_time):
    global ultimo_estado_seguros, ultimo_estado_cajuela
    
    for doc in doc_snapshot:
        data = doc.to_dict()
        if not data: continue

        controles = data.get('controles', {})
        seguros_actual = controles.get('seguros_desbloqueados')
        cajuela_actual = controles.get('cajuela_abierta')

        # Control de Seguros (Solo si cambió)
        if seguros_actual != ultimo_estado_seguros:
            ser.write(b'S' if seguros_actual else b's')
            ultimo_estado_seguros = seguros_actual
            print(f"\n🔓 Seguros: {seguros_actual}")

        # Control de Cajuela (Solo si cambió)
        if cajuela_actual != ultimo_estado_cajuela:
            ser.write(b'A' if cajuela_actual else b'a')
            ultimo_estado_cajuela = cajuela_actual
            print(f"📦 Cajuela: {cajuela_actual}")

# Activamos el escucha de Firebase
doc_watch = vehiculo_ref.on_snapshot(on_snapshot)

print("🚀 Sistema Completo: Telemetría (MQTT/App) y Controles activos.")

# --- 5. CICLO PRINCIPAL (TELEMETRÍA) ---
try:
    while True:
        if ser.in_waiting > 0:
            try:
                linea = ser.readline().decode('utf-8', errors='ignore').strip()
                if linea.startswith('{'):
                    data = json.loads(linea)
                    
                    if "telemetria" in data:
                        l = data["telemetria"]
                        estado = l.get("estado", "OPERATIVO")
                        
                        # A. Publicación MQTT para el Dashboard
                        client.publish(MQTT_TOPIC, json.dumps(l))
                        
                        # B. Actualización Firebase para la App Móvil
                        vehiculo_ref.update({
                            "telemetria.estado": estado,
                            "telemetria.velocidad": l["velocidad"],
                            "telemetria.rpm": l["rpm"],
                            "telemetria.temp_motor": l["temp_motor"],
                            "telemetria.temp_inversor": l["temp_inversor"],
                            "telemetria.temp_bateria": l["temp_bateria"],
                            "telemetria.bateria_porcentaje": l["bateria_porcentaje"],
                            "ultima_actualizacion": firestore.SERVER_TIMESTAMP
                        })
                        
                        # C. Lógica de Colisión (Sincronización forzada)
                        if estado == "COLISION_DETECTADA":
                            vehiculo_ref.update({"controles.seguros_desbloqueados": True})
                            print("\n" + "!" * 40)
                            print(" ⚠️  COLISIÓN DETECTADA - SEGUROS ABIERTOS ⚠️ ")
                            print("!" * 40 + "\n")
                        else:
                            print(f"✅ [SYNC] {estado:10} | Vel: {l['velocidad']:3} km/h | "
                              f"Bat: {l['bateria_porcentaje']:2}% | "
                              f"M: {l['temp_motor']:5.1f}°C | "
                              f"I: {l['temp_inversor']:5.1f}°C | "
                              f"B: {l['temp_bateria']:5.1f}°C")
                
            except Exception as e:
                pass
                
        time.sleep(0.01)

except KeyboardInterrupt:
    print("\n🛑 Deteniendo sistema...")
    doc_watch.unsubscribe()
    ser.close()
    client.loop_stop()
    client.disconnect()