package com.urbanguide

import android.util.Log
import com.google.android.gms.maps.model.LatLng
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
    MoveMap,
    Unmanaged;

    companion object {
        fun fromTopic(topic: String?): Topics {
            return when {
                topic?.contains("drawpoint", ignoreCase = true) == true -> DrawPoint
                topic?.contains("inappalert", ignoreCase = true) == true -> InAppAlert
                topic?.contains("inappnotification", ignoreCase = true) == true -> InAppNotification
                topic?.contains("movemap", ignoreCase = true) == true -> MoveMap
                else -> Unmanaged
            }
        }
    }
}


sealed class MqttEvent {
    data class DrawPointEvent(val title: String, val position: LatLng, val topic: String, val timestamp_sent: String) : MqttEvent()
    data class MoveMapEvent(val position: LatLng, val topic: String, val timestamp_sent: String) : MqttEvent()
    data class InAppAlertEvent(val text: String, val topic: String, val timestamp_sent: String) : MqttEvent()
    data class InAppNotificationEvent(val title: String, val text: String, val topic: String, val timestamp_sent: String) : MqttEvent()
}

class MQTTManager(mqttCallback: MqttCallback) {
    companion object {
        const val TAG = "MQTTManager"
    }

    private val serverURI = "tcp://10.0.2.2:1883"
    private val clientId: String = MqttClient.generateClientId()
    private var mqttClient = MqttClient(serverURI, clientId, MemoryPersistence())

    init {
        mqttClient.setCallback(mqttCallback)
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