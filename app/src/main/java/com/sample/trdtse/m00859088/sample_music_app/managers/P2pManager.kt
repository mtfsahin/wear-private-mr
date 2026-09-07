package com.sample.trdtse.m00859088.sample_music_app.managers

import android.content.Context
import android.util.Log
import com.huawei.hmf.tasks.OnFailureListener
import com.huawei.hmf.tasks.OnSuccessListener
import com.huawei.wearengine.HiWear
import com.huawei.wearengine.device.Device
import com.huawei.wearengine.p2p.Message
import com.huawei.wearengine.p2p.P2pClient
import com.huawei.wearengine.p2p.Receiver
import com.huawei.wearengine.p2p.SendCallback
import java.io.File
import java.nio.charset.StandardCharsets

class P2pManager(context: Context) {

    private val p2pClient: P2pClient = HiWear.getP2pClient(context)
    private var messageReceiver: Receiver? = null
    private var messageListener: MessageListener? = null

    interface MessageListener {
        fun onMessageReceived(message: String?)
        fun onMessageSent(message: String?)
    }

    fun registerReceiver(device: Device?, listener: MessageListener?) {
        messageReceiver?.let { p2pClient.unregisterReceiver(it) }
        messageListener = listener

        messageReceiver = Receiver { message ->
            when (message.type) {
                Message.MESSAGE_TYPE_DATA -> {
                    val receivedText = String(message.data, StandardCharsets.UTF_8)
                    Log.d(TAG, "New message received: $receivedText")
                    messageListener?.onMessageReceived(receivedText)
                }

                Message.MESSAGE_TYPE_FILE -> {
                    val fileName = message.file.name
                    Log.d(TAG, "New file received: $fileName")
                    messageListener?.onMessageReceived(
                        "New file received: $fileName, path: ${message.file}"
                    )
                }
            }
        }

        p2pClient.registerReceiver(device, messageReceiver)
            .addOnFailureListener { e -> Log.e(TAG, "Register error: ", e) }
    }

    fun unregisterReceiver() {
        messageReceiver?.let {
            p2pClient.unregisterReceiver(it)
            messageReceiver = null
            messageListener = null
        }
    }

    fun setPeer(watchType: WatchType) {
        p2pClient.setPeerPkgName(watchType.peerPackage)
        p2pClient.setPeerFingerPrint(watchType.peerFingerprint)
        Log.i(TAG, "Peer set for ${watchType.label}: ${watchType.peerPackage}")
    }

    fun pingDevice(
        device: Device,
        peerPackageName: String?,
        successListener: OnSuccessListener<String?>,
        failureListener: OnFailureListener,
    ) {
        p2pClient.ping(device) { result ->
            successListener.onSuccess(
                """- Connected Device Name: ${device.name}
                   - Peer Package Name: $peerPackageName
                   - Ping Result: result:$result
                """.trimIndent()
            )
        }.addOnSuccessListener {
            successListener.onSuccess("${device.name}'s $peerPackageName")
        }.addOnFailureListener { e ->
            failureListener.onFailure(Exception("Ping Fail: task failure $e", e))
        }
    }

    fun sendMessage(
        device: Device?,
        message: String,
        successListener: OnSuccessListener<Void?>?,
        failureListener: OnFailureListener,
    ) {
        try {
            val msg = Message.Builder()
                .setPayload(message.toByteArray(StandardCharsets.UTF_8))
                .build()

            p2pClient.send(device, msg, resultCallback(successListener, failureListener))
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener)

            messageListener?.onMessageSent(message)
        } catch (e: Exception) {
            failureListener.onFailure(e)
        }
    }
    fun sendFile(
        device: Device,
        file: File,
        successListener: OnSuccessListener<Void?>?,
        failureListener: OnFailureListener,
    ) {
        try {
            val fileMessage = Message.Builder()
                .setPayload(file)
                .build()

            p2pClient.send(device, fileMessage, resultCallback(successListener, failureListener))
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener)

            messageListener?.onMessageSent("${file.name} have been sent.")
        } catch (e: Exception) {
            Log.e(TAG, "Exception sending file: ${e.message}")
            failureListener.onFailure(e)
        }
    }


    fun sendFile(
        context: Context,
        device: Device,
        successListener: OnSuccessListener<Void?>?,
        failureListener: OnFailureListener,
    ) {
        try {
            val tempFile = File(context.cacheDir, "default_message.txt")
            tempFile.writeText("Test file content!")

            val fileMessage = Message.Builder()
                .setPayload(tempFile)
                .build()

            p2pClient.send(device, fileMessage, resultCallback(successListener, failureListener))
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener)

            messageListener?.onMessageSent("${tempFile.name} have been sent.")
        } catch (e: Exception) {
            Log.e(TAG, "Exception sending file: ${e.message}")
            failureListener.onFailure(e)
        }
    }

    private fun resultCallback(
        successListener: OnSuccessListener<Void?>?,
        failureListener: OnFailureListener,
    ): SendCallback = object : SendCallback {
        override fun onSendResult(resultCode: Int) {
            if (resultCode == SEND_OK) {
                Log.d(TAG, "Send successful")
                successListener?.onSuccess(null)
            } else {
                Log.e(TAG, "Send failed. Error code: $resultCode")
                failureListener.onFailure(Exception("Send failed with code $resultCode"))
            }
        }

        override fun onSendProgress(progress: Long) {
            Log.d(TAG, "Progress: $progress")
        }
    }

    private companion object {
        const val TAG = "P2pManager"
        const val SEND_OK = 207
    }
}
