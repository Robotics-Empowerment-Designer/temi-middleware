package com.example.mqtt

import android.util.Log
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

/**
 * Diese Klasse stellt eine Schnittstelle für die Kommunikation über das MQTT-Protokoll bereit.
 * Sie verwendet die Eclipse Paho MQTT-Bibliothek für die Implementierung der MQTT-Funktionalitäten.
 */
class MqttHandler {
    // Die Instanz des MQTT-Clients
    private var client: MqttClient? = null
    private var TAG = "MQTTHandler"

    /**
     * Stellt eine Verbindung zu einem MQTT-Broker her.
     *
     * @param brokerUrl Die URL des MQTT-Brokers.
     * @param clientId  Die Client-ID für die Verbindung.
     * @param USERNAME  Der Benutzername für die Authentifizierung.
     * @param PASSWORD  Das Passwort für die Authentifizierung.
     * @return Gibt true zurück, wenn die Verbindung erfolgreich hergestellt wurde, ansonsten false.
     */
    fun connect(
        brokerUrl: String?,
        clientId: String?,
        USERNAME: String? = null,
        PASSWORD: String? = null
    ): Boolean {
        try {
            // Persistence-Schicht einrichten
            val persistence = MemoryPersistence()
            Log.d(TAG, "in connect")
            // MQTT-Client initialisieren
            client = MqttClient(brokerUrl, clientId, persistence)

            // Verbindungsoptionen konfigurieren
            val connectOptions = MqttConnectOptions()
            connectOptions.isCleanSession = true

            if (USERNAME != null && PASSWORD != null) {
            connectOptions.userName = USERNAME
            connectOptions.password = PASSWORD.toCharArray()
            }

            // Mit dem Broker verbinden
            client!!.connect(connectOptions)
        } catch (e: MqttException) {
            e.printStackTrace()
        }
        return false
    }

    /**
     * Trennt die Verbindung zum MQTT-Broker.
     */
    fun disconnect() {
        try {
            client!!.disconnect()
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    /**
     * Veröffentlicht eine Nachricht zu einem bestimmten MQTT-Thema.
     *
     * @param topic   Das MQTT-Thema, zu dem die Nachricht veröffentlicht werden soll.
     * @param message Der Inhalt der zu veröffentlichenden Nachricht.
     */
    fun publish(topic: String?, message: String) {
        try {
            val mqttMessage = MqttMessage(message.toByteArray())
            client!!.publish(topic, mqttMessage)
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    /**
     * Abonniert ein bestimmtes MQTT-Thema.
     *
     * @param topic Das MQTT-Thema, das abonniert werden soll.
     */
    fun subscribe(topic: String?) {
        try {
            client!!.subscribe(topic)
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    /**
     * Setzt einen Callback für eingehende MQTT-Nachrichten.
     *
     * @param callback Der MqttCallback, der für eingehende Nachrichten verwendet werden soll.
     */
    fun setCallback(callback: MqttCallback?) {
        Log.d("MqttHandler", client.toString())
        client!!.setCallback(callback)
    }

    val isConnected: Boolean
        /**
         * Überprüft, ob der MQTT-Client aktuell mit dem Broker verbunden ist.
         *
         * @return Gibt true zurück, wenn die Verbindung besteht, ansonsten false.
         */
        get() = client != null && client!!.isConnected
}