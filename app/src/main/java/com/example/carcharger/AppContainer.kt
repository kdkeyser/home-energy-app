package com.example.carcharger

import android.content.Context

interface AppContainer {
    val webSocketClient: WebSocketClient
    val credentialsManager: CredentialsManager
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    override val webSocketClient: WebSocketClient by lazy {
        WebSocketClient()
    }
    
    override val credentialsManager: CredentialsManager by lazy {
        CredentialsManager(context)
    }
}
