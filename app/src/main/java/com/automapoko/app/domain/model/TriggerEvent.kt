package com.automapoko.app.domain.model

enum class TriggerEvent {
    CONNECTED,      // Bluetooth/Wi-Fi: dispositivo/rede conectou
    DISCONNECTED,   // Bluetooth/Wi-Fi: dispositivo/rede desconectou
    ENTER,          // Localização: entrou na área
    EXIT            // Localização: saiu da área
}
