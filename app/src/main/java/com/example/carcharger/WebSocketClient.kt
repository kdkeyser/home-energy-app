package com.example.carcharger

import android.util.Log
import io.konektis.ClientMessage
import io.konektis.Message
import io.konektis.deserializeMessage
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.http.HttpMethod
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

sealed class ConnectionStatus {
    object Idle : ConnectionStatus()
    object Connected : ConnectionStatus()
    object Unauthorized : ConnectionStatus()
    data class Error(val message: String) : ConnectionStatus()
}

class WebSocketClient {

    private val client = HttpClient(CIO) {
        install(WebSockets)
    }

    private var session: DefaultClientWebSocketSession? = null
    private var job: Job? = null

    private val _messages = MutableSharedFlow<Message>()
    val messages = _messages.asSharedFlow()

    private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Idle)
    val connectionStatus = _connectionStatus.asStateFlow()

    fun connect(username: String, password: String) {
        if (job?.isActive == true) {
            return
        }
        _connectionStatus.value = ConnectionStatus.Idle
        job = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    Log.d("WebSocketClient", "Attempting to connect...")
                    client.webSocket(method = HttpMethod.Get, host = "192.168.129.60", port = 8080, path = "/ws") {
                        session = this

                        // Send authentication message
                        val authMessage = ClientMessage.Authenticate(username, password)
                        sendMessage(authMessage)

                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                val json = frame.readText()
                                Log.d("WebSocketClient", "Message received: $json")
                                val message = deserializeMessage(json)

                                when (message) {
                                    is Message.Authenticated -> {
                                        _connectionStatus.value = ConnectionStatus.Connected
                                        Log.d("WebSocketClient", "Authentication successful.")
                                    }
                                    is Message.Unauthorized -> {
                                        _connectionStatus.value = ConnectionStatus.Unauthorized
                                        disconnect()
                                        break
                                    }
                                    else -> {
                                        Log.w("WebSocketClient", "Received message while not authenticated: $message")
                                        _connectionStatus.value = ConnectionStatus.Error("Received message while not authenticated: $message")
                                        disconnect()
                                        break
                                    }
                                }
                            }
                        }
                    }
                } catch (e: CancellationException) {
                    Log.d("WebSocketClient", "Connection job cancelled.")
                    break
                } catch (e: Exception) {
                    _connectionStatus.value = ConnectionStatus.Error(e.message ?: "Unknown error")
                    Log.e("WebSocketClient", "Connection failed.", e)
                    delay(5000)
                } finally {
                    session?.close()
                    session = null
                    Log.d("WebSocketClient", "Connection closed.")
                }
            }
        }
    }

    suspend fun sendMessage(clientMessage: ClientMessage) {
        if (session?.isActive != true) {
            Log.w("WebSocketClient", "No active session to send message.")
            return
        }
        try {
            val json = Json.encodeToString( clientMessage)
            session?.send(Frame.Text(json))
        } catch (e: Exception) {
            Log.e("WebSocketClient", "Failed to send message", e)
        }
    }

    fun disconnect() {
        Log.d("WebSocketClient", "Disconnecting...")
        job?.cancel()
        job = null
        _connectionStatus.value = ConnectionStatus.Idle
    }
}
