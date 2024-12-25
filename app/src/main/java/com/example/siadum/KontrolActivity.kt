package com.example.siadum

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.eclipse.paho.client.mqttv3.*

class KontrolActivity : AppCompatActivity() {

    private var mqttClient: MqttClient? = null
    private val brokerUrl = "tcp://broker.hivemq.com:1883"
    private val seasonTopic = "control/season"
    private val manualControlTopic = "control/comand"

    private lateinit var tvGate1Status: TextView
    private lateinit var tvGate2Status: TextView
    private lateinit var tvPompa: TextView
    private lateinit var tvMQTTStatus: TextView
    private lateinit var seekBarPintuAir1: SeekBar
    private lateinit var seekBarPintuAir2: SeekBar
    private lateinit var btnPompa: Button

    private var musim: String? = null
    private var pumpStatus: String = "Off"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kontrol)

        // Inisialisasi UI
        initUI()

        // Inisialisasi MQTT
        initMQTT()
    }

    private fun initUI() {
        tvGate1Status = findViewById(R.id.tvPintuAir1)
        tvGate2Status = findViewById(R.id.tvPintuAir2)
        tvPompa = findViewById(R.id.tvPompa)
        tvMQTTStatus = findViewById(R.id.tvMQTTStatus)
        seekBarPintuAir1 = findViewById(R.id.seekBarPintuAir1)
        seekBarPintuAir2 = findViewById(R.id.seekBarPintuAir2)
        btnPompa = findViewById(R.id.btnPompa)

        setupManualControl()
    }

    private fun setupManualControl() {
        seekBarPintuAir1.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val command = if (progress > 50) "open_gate1" else "close_gate1"
                    publishManualCommand(command)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekBarPintuAir2.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val command = if (progress > 50) "open_gate2" else "close_gate2"
                    publishManualCommand(command)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnPompa.setOnClickListener {
            val command = if (pumpStatus == "Off") "start_pump" else "stop_pump"
            publishManualCommand(command)
        }
    }

    private fun publishManualCommand(command: String) {
        try {
            mqttClient?.let {
                if (it.isConnected) {
                    val message = MqttMessage(command.toByteArray())
                    it.publish(manualControlTopic, message)
                    Log.d("MQTT", "Command Published: $command")

                    when (command) {
                        "start_pump" -> {
                            pumpStatus = "On"
                            tvPompa.text = "Pompa Air (On)"
                            btnPompa.text = "Matikan Pompa"
                        }
                        "stop_pump" -> {
                            pumpStatus = "Off"
                            tvPompa.text = "Pompa Air (Off)"
                            btnPompa.text = "Aktifkan Pompa"
                        }
                        "open_gate1" -> tvGate1Status.text = "Pintu Air 1: Terbuka"
                        "close_gate1" -> tvGate1Status.text = "Pintu Air 1: Tertutup"
                        "open_gate2" -> tvGate2Status.text = "Pintu Air 2: Terbuka"
                        "close_gate2" -> tvGate2Status.text = "Pintu Air 2: Tertutup"
                    }
                } else {
                    Log.e("MQTT", "MQTT client not connected")
                    tvMQTTStatus.text = "Tidak Terhubung ke MQTT"
                }
            }
        } catch (e: MqttException) {
            Log.e("MQTT", "Error publishing command: ${e.message}")
            tvMQTTStatus.text = "Kesalahan: ${e.message}"
        }
    }

    private fun initMQTT() {
        try {
            mqttClient = MqttClient(brokerUrl, MqttClient.generateClientId(), null)
            val connectOptions = MqttConnectOptions().apply {
                connectionTimeout = 10
                keepAliveInterval = 60
                isAutomaticReconnect = true
            }

            mqttClient?.setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable?) {
                    Log.e("MQTT", "Connection Lost: ${cause?.message}")
                    tvMQTTStatus.text = "Koneksi Terputus: ${cause?.message}"
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    val payload = message?.toString() ?: ""
                    Log.d("MQTT", "Message Arrived: $payload")
                    if (topic == seasonTopic) {
                        musim = payload
                        updateUIForSeason()
                    }
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {
                    Log.d("MQTT", "Delivery Complete")
                }
            })

            mqttClient?.connect(connectOptions)
            mqttClient?.subscribe(seasonTopic)
            tvMQTTStatus.text = "Terhubung ke MQTT"
            Log.d("MQTT", "Connected to MQTT Broker")
        } catch (e: MqttException) {
            Log.e("MQTT", "Error initializing MQTT: ${e.message}")
            tvMQTTStatus.text = "Gagal Terhubung: ${e.message}"
        }
    }

    private fun updateUIForSeason() {
        when (musim) {
            "Hujan" -> {
                seekBarPintuAir1.isEnabled = true
                seekBarPintuAir2.isEnabled = true
                btnPompa.isEnabled = false
                tvPompa.text = "Pompa Tidak Aktif"
            }
            "Kemarau" -> {
                seekBarPintuAir1.isEnabled = false
                seekBarPintuAir2.isEnabled = false
                btnPompa.isEnabled = true
                tvPompa.text = "Pompa Aktif"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mqttClient?.let {
            if (it.isConnected) {
                it.disconnect()
                Log.d("MQTT", "MQTT Disconnected")
            }
        }
    }
}
