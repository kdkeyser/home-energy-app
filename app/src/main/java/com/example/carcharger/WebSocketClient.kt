package com.example.carcharger

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BasicAuthCredentials
import io.ktor.client.plugins.auth.providers.basic
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
    data class Error(val message: String) : ConnectionStatus()
}

class WebSocketClient {

    private val client = HttpClient(CIO) {
        install(WebSockets)
    }

    private var session: DefaultClientWebSocketSession? = null
    private var job: Job? = null
    private var credentials: Pair<String, String>? = null

    private val _messages = MutableSharedFlow<Message>()
    val messages = _messages.asSharedFlow()

    private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Idle)
    val connectionStatus = _connectionStatus.asStateFlow()

    fun connect(username: String, password: String) {
        if (job?.isActive == true) {
            return 
        }
        credentials = username to password
        _connectionStatus.value = ConnectionStatus.Idle
        job = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    Log.d("WebSocketClient", "Attempting to connect...")
                    val authedClient = client.config {
                        install(Auth) {
                            basic {
                                credentials { BasicAuthCredentials(username = credentials!!.first, password = credentials!!.second) }
                            }
                        }
                    }
                    authedClient.webSocket(method = HttpMethod.Get, host = "192.168.129.60", port = 8080, path = "/ws") {
                        session = this
                        Log.d("WebSocketClient", "Connection successful.")
                        if (_connectionStatus.value !is ConnectionStatus.Connected) {
                            _connectionStatus.value = ConnectionStatus.Connected
                        }

                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                val json = frame.readText()
                                _messages.emit(deserializeMessage(json))
                            }
                        }
                    }
                } catch (e: CancellationException) {
                    Log.d("WebSocketClient", "Connection job cancelled.")
                    break
                } catch (e: Exception) {
                    Log.e("WebSocketClient", "Connection failed.", e)
                    if (e is io.ktor.client.plugins.ClientRequestException && e.response.status.value == 401) {
                        _connectionStatus.value = ConnectionStatus.Error("Invalid username or password")
                        disconnect()
                        break
                    }
                } finally {
                    session?.close()
                    session = null
                    Log.d("WebSocketClient", "Connection closed.")
                    if (isActive) {
                        Log.d("WebSocketClient", "Reconnecting in 5 seconds...")
                        delay(5000)
                    }
                }
            }
        }
    }

    suspend fun sendMessage(message: Message) {
        if (session?.isActive != true) {
            Log.w("WebSocketClient", "No active session to send message.")
            return
        }
        try {
            val json = Json.encodeToString(Message.serializer(), message)
            session?.send(Frame.Text(json))
        } catch (e: Exception) {
            Log.e("WebSocketClient", "Failed to send message", e)
        }
    }

    fun disconnect() {
        Log.d("WebSocketClient", "Disconnecting...")
        job?.cancel()
        job = null
        credentials = null
        _connectionStatus.value = ConnectionStatus.Idle
    }
}
