@file:JvmName("Downloader")
package net.measurementlab.ndt7.android

import com.google.gson.Gson
import net.measurementlab.ndt7.android.utils.DataConverter.currentTimeInMicroseconds
import net.measurementlab.ndt7.android.utils.DataConverter.generateResponse
import net.measurementlab.ndt7.android.utils.NDT7Constants.MEASUREMENT_INTERVAL
import net.measurementlab.ndt7.android.NDTTest.TestType.*
import net.measurementlab.ndt7.android.models.CallbackRegistry
import net.measurementlab.ndt7.android.models.Measurement
import net.measurementlab.ndt7.android.utils.SocketFactory
import okhttp3.*
import okio.ByteString
import java.lang.Error
import java.net.URI
import java.util.concurrent.ExecutorService
import java.util.concurrent.Semaphore

class Downloader(
        private val cbRegistry: CallbackRegistry,
        private val executorService: ExecutorService,
        private val speedtestLock: Semaphore
): WebSocketListener() {

    private var startTime: Long = 0
    private var previous: Long = 0
    private var numBytes = 0.0
    private val gson = Gson()


    override fun onOpen(ws: WebSocket, response: Response) {
        startTime = currentTimeInMicroseconds()
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        numBytes += text.length.toDouble()
        tryToUpdateClient()

        try {
            val measurement = gson.fromJson(text, Measurement::class.java)
            cbRegistry.measurementProgressCbk(measurement)
        } catch (e: Exception) {
            return
        }
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        numBytes += bytes.size().toDouble()
        tryToUpdateClient()
    }

    override fun onClosing(ws: WebSocket, code: Int, reason: String) {

        val clientResponse = generateResponse(startTime, numBytes, DOWNLOAD)
        when (code) {
            1000 -> {
                cbRegistry.onFinishedCbk(clientResponse, null, DOWNLOAD)
            }
            else -> {
                cbRegistry.onFinishedCbk(clientResponse, Error(reason), DOWNLOAD)
            }
        }

        releaseResources()
        ws.close(1000, null)
    }

    override fun onFailure(ws: WebSocket, throwable: Throwable, response: Response?) {
        cbRegistry.onFinishedCbk(generateResponse(startTime, numBytes, DOWNLOAD), throwable, DOWNLOAD)
        releaseResources()
        ws.close(1001, null)
    }

    fun beginDownload(uri: URI, httpClient: OkHttpClient?) {
        SocketFactory.establishSocketConnection(uri, httpClient, this)
    }

    private fun tryToUpdateClient() {
        val now = currentTimeInMicroseconds()

        //if we haven't sent an update in 250ms, lets send one
        if (now - previous > MEASUREMENT_INTERVAL) {
            cbRegistry.speedtestProgressCbk(generateResponse(startTime, numBytes, DOWNLOAD))
            previous = now
        }
    }

    private fun releaseResources() {
        speedtestLock.release()
        executorService.shutdown()
    }
}