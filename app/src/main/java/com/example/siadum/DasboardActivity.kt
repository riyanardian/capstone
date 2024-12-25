package com.example.siadum

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.eclipse.paho.client.mqttv3.*

class DasboardActivity : AppCompatActivity() {

    // Broker configuration
    private val brokerUrl = "tcp://broker.hivemq.com:1883"
    private lateinit var mqttClient: MqttClient
    private lateinit var statusText: TextView

    // MQTT Topics
    private val seasonTopic = "control/season"
    private val soilMoisture1Topic = "sensor/soilMoisture1"
    private val soilMoisture2Topic = "sensor/soilMoisture2"
    private val waterLevelTopic = "sensor/waterLevel"
    private val gate1StatusTopic = "actuator/gate1Status"
    private val gate2StatusTopic = "actuator/gate2Status"
    private val pumpStatusTopic = "actuator/pumpStatus"
    private val manualControlTopic = "control/manual"

    // UI Elements
    private lateinit var spinnerSeason: Spinner
    private lateinit var tvSoilMoisture: TextView
    private lateinit var tvWaterLevel: TextView
    private lateinit var tvGateStatus1: TextView
    private lateinit var tvGateStatus2: TextView
    private lateinit var tvPumpStatus: TextView
    private lateinit var btnManualControl: Button
    private lateinit var btnViewHistoricalData: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dasboard)

        // Bind UI elements
        spinnerSeason = findViewById(R.id.spinnerSeason)
        tvSoilMoisture = findViewById(R.id.tvSoilMoisture)
        tvWaterLevel = findViewById(R.id.tvWaterLevel)
        tvGateStatus1 = findViewById(R.id.tvGateStatus1)
        tvGateStatus2 = findViewById(R.id.tvGateStatus2)
        tvPumpStatus = findViewById(R.id.tvPumpStatus)
        statusText = findViewById(R.id.tvBrokerStatus)
        btnManualControl = findViewById(R.id.btnManualControl)
        btnViewHistoricalData = findViewById(R.id.btnViewHistory)

        setupSpinner()
        setupButtons()
        initializeMqtt()
    }

    private fun setupSpinner() {
        val seasons = arrayOf("Musim Hujan", "Musim Kemarau")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, seasons)
        spinnerSeason.adapter = adapter

        spinnerSeason.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val season = parent?.getItemAtPosition(position).toString()
                publishToMqtt(seasonTopic, season)
                updateVisibilityBasedOnSeason(season)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Default season if nothing is selected
                publishToMqtt(seasonTopic, "Musim Hujan")
                updateVisibilityBasedOnSeason("Musim Hujan")
            }
        }
    }

    private fun updateVisibilityBasedOnSeason(season: String) {
        tvWaterLevel.visibility = if (season == "Musim Hujan") View.VISIBLE else View.GONE
    }

    private fun setupButtons() {
        btnManualControl.setOnClickListener {
            onManualControlPressed()
            val intent = Intent(this, KontrolActivity::class.java)
            startActivity(intent)
        }

        btnViewHistoricalData.setOnClickListener {
            val intent = Intent(this, HistorisActivity::class.java)
            startActivity(intent)
        }
    }

    private fun onManualControlPressed() {
        // Mengirimkan pesan ke MQTT untuk mengaktifkan kontrol manual
        val mqttMessage = "on"
        publishToMqtt(manualControlTopic, mqttMessage)
    }

    override fun onResume() {
        super.onResume()

        // Kirim pesan ke MQTT untuk menonaktifkan kontrol manual saat kembali ke dashboard
        val mqttMessage = "off"
        publishToMqtt(manualControlTopic, mqttMessage)
    }

    private fun initializeMqtt() {
        try {
            mqttClient = MqttClient(brokerUrl, MqttClient.generateClientId(), null)
            val connectOptions = MqttConnectOptions().apply {
                connectionTimeout = 10
                keepAliveInterval = 60
            }

            mqttClient.setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable?) {
                    runOnUiThread {
                        statusText.text = "Connection lost: ${cause?.message}"
                    }
                    attemptReconnect()
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    runOnUiThread {
                        handleIncomingMessage(topic, message?.toString())
                    }
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {
                    // Message delivered callback
                }
            })

            mqttClient.connect(connectOptions)
            runOnUiThread {
                statusText.text = "Connected"
            }

            subscribeToTopics()
        } catch (e: MqttException) {
            runOnUiThread {
                statusText.text = "Connection Error: ${e.message}"
            }
        }
    }

    private fun attemptReconnect() {
        Thread {
            var connected = false
            while (!connected) {
                try {
                    if (isInternetAvailable()) {
                        mqttClient.reconnect()
                        connected = true
                        runOnUiThread {
                            statusText.text = "Reconnected"
                        }
                    } else {
                        runOnUiThread {
                            statusText.text = "Tunggu"
                        }
                    }
                    Thread.sleep(2000)
                } catch (e: MqttException) {
                    e.printStackTrace()
                    runOnUiThread {
                        statusText.text = "Reconnect attempt failed: ${e.message}"
                    }
                    Thread.sleep(2000)
                }
            }
        }.start()
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    private fun handleIncomingMessage(topic: String?, message: String?) {
        when (topic) {
            soilMoisture1Topic -> tvSoilMoisture.text = "$message"
            soilMoisture2Topic -> tvSoilMoisture.text = "$message"
            waterLevelTopic -> tvWaterLevel.text = "$message"

            gate1StatusTopic -> tvGateStatus1.text = if ("$message" == "1") "Terbuka" else "Tertutup"
            gate2StatusTopic -> tvGateStatus2.text = if ("$message" == "1") "Terbuka" else "Tertutup"

            pumpStatusTopic -> tvPumpStatus.text = if ("$message" == "1") "Nyala" else "Mati"
        }
    }

    private fun subscribeToTopics() {
        val topics = arrayOf(
            soilMoisture1Topic,
            soilMoisture2Topic,
            waterLevelTopic,
            gate1StatusTopic,
            gate2StatusTopic,
            pumpStatusTopic
        )
        try {
            for (topic in topics) {
                mqttClient.subscribe(topic, 1)
            }
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    private fun publishToMqtt(topic: String, message: String) {
        try {
            mqttClient.publish(topic, MqttMessage(message.toByteArray()))
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            mqttClient.disconnect()
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }
}
