package com.automapoko.app.domain.model

/**
 * Configuração específica de cada tipo de gatilho.
 * Sealed class garante que novos tipos sejam tratados em todos os when() do projeto.
 */
sealed class TriggerConfig {

    /**
     * Gatilho Bluetooth.
     * @param deviceAddress Endereço MAC do dispositivo (identificador único e estável)
     * @param deviceName Nome do dispositivo (exibição apenas)
     * @param event CONNECTED ou DISCONNECTED
     */
    data class BluetoothConfig(
        val deviceAddress: String,
        val deviceName: String,
        val event: TriggerEvent
    ) : TriggerConfig()

    /**
     * Gatilho Wi-Fi.
     * @param ssid Nome da rede (SSID)
     * @param event CONNECTED ou DISCONNECTED
     */
    data class WifiConfig(
        val ssid: String,
        val event: TriggerEvent
    ) : TriggerConfig()

    /**
     * Gatilho de localização via Geofencing.
     * @param latitude Latitude do centro da área
     * @param longitude Longitude do centro da área
     * @param radiusMeters Raio em metros (mínimo 100m recomendado pela API)
     * @param locationName Nome amigável do local (ex: "Casa", "Trabalho")
     * @param event ENTER ou EXIT
     */
    data class LocationConfig(
        val latitude: Double,
        val longitude: Double,
        val radiusMeters: Float,
        val locationName: String,
        val event: TriggerEvent
    ) : TriggerConfig()
}
