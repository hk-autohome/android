package com.harshkanjariya.autohome.api

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.harshkanjariya.autohome.BuildConfig
import com.harshkanjariya.autohome.MainActivity
import com.harshkanjariya.autohome.R
import info.mqtt.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage


class Mqtt {
    private var mqttClient: MqttAndroidClient? = null
    private val serverURI = BuildConfig.MQTT_URL
    private val serverUsername = BuildConfig.MQTT_USERNAME
    private val serverPassword = BuildConfig.MQTT_PASSWORD

    companion object {
        const val TAG = "AndroidMqttClient"
    }

    fun connect(context: Context, clientId: String) {
        val notification = createForegroundNotification(context)

        mqttClient = MqttAndroidClient(context, serverURI, clientId).apply {
            setForegroundService(notification)
        }
        mqttClient?.setCallback(object : MqttCallback {
            override fun messageArrived(topic: String?, message: MqttMessage?) {
                Log.d(TAG, "Receive message: ${message.toString()} from topic: $topic")
            }

            override fun connectionLost(cause: Throwable?) {
                Log.d(TAG, "Connection lost ${cause.toString()}")
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {

            }
        })
        val options = MqttConnectOptions().apply {
            userName = serverUsername
            password = serverPassword.toCharArray()
            isCleanSession = true
            connectionTimeout = 10
            keepAliveInterval = 20
            isAutomaticReconnect = true
        }
        try {
            println("connect: Trying to Connect")
            mqttClient?.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    println("Connection success")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    println("Connection failure $exception\n\n\n\n${exception?.message}")
                }
            })
        } catch (e: MqttException) {
            println("connect: Connection failed")
            e.printStackTrace()
        }

    }

    private fun createForegroundNotification(context: Context): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "com.harshkanjariya.autohome",
                "MQTT Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, "com.harshkanjariya.autohome")
            .setContentTitle("MQTT Service")
            .setContentText("MQTT is running in the background")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .build()
    }

    fun sendMessage(s: String) {
        val msg = MqttMessage(s.toByteArray())
        mqttClient?.publish("esp32/output", msg)
    }
    fun disconnect() {
        if (mqttClient == null) return
        try {
            mqttClient!!.unregisterResources()
            mqttClient!!.close()
            mqttClient!!.disconnect(null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Disconnected")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.d(TAG, "Failed to disconnect")
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    fun subscribe(topic: String, qos: Int = 0) {
        if (mqttClient == null) return
        try {
            mqttClient!!.subscribe(topic, qos, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Subscribed to $topic")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.d(TAG, "Failed to subscribe $topic")
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    fun unsubscribe(topic: String) {
        if (mqttClient == null) return
        try {
            mqttClient!!.unsubscribe(topic, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Unsubscribed to $topic")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.d(TAG, "Failed to unsubscribe $topic")
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

}