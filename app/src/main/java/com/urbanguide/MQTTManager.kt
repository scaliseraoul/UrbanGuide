package com.urbanguide

import android.util.Log
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence


enum class Topics {
    DrawPoint,
    InAppAlert,
    InAppNotification,
    MoveMap
}


class MQTTManager() {
    companion object {
        const val TAG = "MQTTManager"
    }

    private val serverURI = "tcp://10.0.2.2:1883"
    private val clientId: String = MqttClient.generateClientId();
    private var mqttClient = MqttClient(serverURI, clientId, MemoryPersistence())

    init {
        mqttClient.setCallback(object : MqttCallback {
            override fun messageArrived(topic: String?, message: MqttMessage?) {
                val currentTimeMillis = System.currentTimeMillis()
                Log.d(TAG, "Receive message: ${message.toString()} from topic: $topic at time $currentTimeMillis")
            }

            override fun connectionLost(cause: Throwable?) {
                Log.d(TAG, "Connection lost ${cause.toString()}")
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                Log.d(TAG, "Delivery Complete")
            }

        })
        val options = MqttConnectOptions()
        options.isAutomaticReconnect = true
        options.isCleanSession = false
        mqttClient.connect(options)
    }
    fun subscribe(topic: String) {
        mqttClient.subscribe(topic, 2)
    }

    fun publish(topic: String, payload: String) {
        try {
            val message = MqttMessage(payload.toByteArray())
            message.qos = 2
            mqttClient.publish(topic, message)
            Log.d(TAG, "Message published to topic $topic")
        } catch (e: MqttException) {
            Log.e(TAG, "Error publishing message to topic $topic", e)
        }
    }
}