package com.automapoko.app.data.local.converter

import com.automapoko.app.domain.model.TriggerConfig
import com.automapoko.app.domain.model.TriggerEvent
import org.json.JSONObject

/**
 * Converte TriggerConfig para JSON e vice-versa.
 * Usamos JSONObject nativo do Android para não adicionar dependência de Gson/Moshi.
 */
object TriggerConfigConverter {

    fun toJson(config: TriggerConfig): String {
        val json = JSONObject()
        when (config) {
            is TriggerConfig.BluetoothConfig -> {
                json.put("type", "BLUETOOTH")
                json.put("deviceAddress", config.deviceAddress)
                json.put("deviceName", config.deviceName)
                json.put("event", config.event.name)
            }
            is TriggerConfig.WifiConfig -> {
                json.put("type", "WIFI")
                json.put("ssid", config.ssid)
                json.put("event", config.event.name)
            }
            is TriggerConfig.LocationConfig -> {
                json.put("type", "LOCATION")
                json.put("latitude", config.latitude)
                json.put("longitude", config.longitude)
                json.put("radiusMeters", config.radiusMeters)
                json.put("locationName", config.locationName)
                json.put("event", config.event.name)
            }
        }
        return json.toString()
    }

    fun fromJson(jsonString: String): TriggerConfig {
        val json = JSONObject(jsonString)
        return when (val type = json.getString("type")) {
            "BLUETOOTH" -> TriggerConfig.BluetoothConfig(
                deviceAddress = json.getString("deviceAddress"),
                deviceName = json.getString("deviceName"),
                event = TriggerEvent.valueOf(json.getString("event"))
            )
            "WIFI" -> TriggerConfig.WifiConfig(
                ssid = json.getString("ssid"),
                event = TriggerEvent.valueOf(json.getString("event"))
            )
            "LOCATION" -> TriggerConfig.LocationConfig(
                latitude = json.getDouble("latitude"),
                longitude = json.getDouble("longitude"),
                radiusMeters = json.getDouble("radiusMeters").toFloat(),
                locationName = json.getString("locationName"),
                event = TriggerEvent.valueOf(json.getString("event"))
            )
            else -> throw IllegalArgumentException("Tipo de gatilho desconhecido: $type")
        }
    }
}
