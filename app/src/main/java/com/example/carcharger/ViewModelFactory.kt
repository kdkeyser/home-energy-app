package com.example.carcharger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ViewModelFactory(private val appContainer: AppContainer) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(LoginViewModel::class.java) -> {
                LoginViewModel(appContainer.webSocketClient, appContainer.credentialsManager) as T
            }
            modelClass.isAssignableFrom(ChargerViewModel::class.java) -> {
                ChargerViewModel(appContainer.webSocketClient) as T
            }
            modelClass.isAssignableFrom(OverviewViewModel::class.java) -> {
                OverviewViewModel(appContainer.webSocketClient) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
