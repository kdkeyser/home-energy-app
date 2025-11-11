package com.example.carcharger

import android.content.Context

interface AppContainer {
    val webSocketClient: WebSocketClient
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    override val webSocketClient: WebSocketClient by lazy {
        WebSocketClient()
    }
}
