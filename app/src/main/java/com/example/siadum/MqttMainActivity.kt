package com.example.siadum

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import kotlinx.coroutines.*

class MqttMainActivity : AppCompatActivity() {

    // Pecah Broker, Port, dan Topik menjadi variabel terpisah
    private val brokerHost = "broker.hivemq.com"
    private val brokerPort = 1883
    private val topic = "relay/control"

    private lateinit var mqttClient: MqttClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mqtt_main)

        val statusText = findViewById<TextView>(R.id.statusText)
        val relayButton = findViewById<Button>(R.id.relayButton)

        // Inisialisasi MQTT
        try {
            // Gabungkan broker host dan port untuk URL broker
            val brokerUrl = "tcp://$brokerHost:$brokerPort"
            mqttClient = MqttClient(brokerUrl, MqttClient.generateClientId(), null)

            // Opsi koneksi MQTT
            val connectOptions = MqttConnectOptions()
            connectOptions.connectionTimeout = 10 // Timeout 10 detik
            connectOptions.keepAliveInterval = 60 // Keep Alive 60 detik

            // Menambahkan callback untuk koneksi dan pesan
            mqttClient.setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable?) {
                    runOnUiThread {
                        statusText.text = "Disconnected"
                    }
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    // Jika Anda ingin memproses pesan yang diterima
                    runOnUiThread {
                        statusText.text = "Message: ${message?.toString()}"
                    }
                }

                override fun deliveryComplete(token: org.eclipse.paho.client.mqttv3.IMqttDeliveryToken?) {
                    // Mengonfirmasi pesan terkirim
                }
            })

            // Koneksi ke broker dengan opsi
            mqttClient.connect(connectOptions)
            statusText.text = "Connected to $brokerUrl"

        } catch (e: MqttException) {
            // Menampilkan error MQTT yang lebih jelas
            statusText.text = "MQTT Initialization Error: ${e.message}"
            e.printStackTrace()
        }

        // Logika untuk tombol relay
        relayButton.setOnClickListener {
            val currentStatus = if (statusText.text.contains("OFF")) "ON" else "OFF"
            try {
                mqttClient.publish(topic, MqttMessage(currentStatus.toByteArray()))
                // Menampilkan status terkini setelah publish
                statusText.text = "Relay Status: $currentStatus"
            } catch (e: Exception) {
                statusText.text = "Failed to Publish Message"
                e.printStackTrace()
            }
        }

        // Menyesuaikan padding dan insets untuk kompatibilitas tampilan
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    // Pastikan untuk menutup koneksi MQTT saat aktivitas dihentikan
    override fun onDestroy() {
        super.onDestroy()
        try {
            if (::mqttClient.isInitialized && mqttClient.isConnected) {
                mqttClient.disconnect()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
