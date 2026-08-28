package com.automapoko.app.data.local.converter

import com.automapoko.app.domain.model.ActionConfig
import org.json.JSONObject

object ActionConfigConverter {

    fun toJson(config: ActionConfig): String {
        val json = JSONObject()
        when (config) {
            is ActionConfig.OpenAppConfig -> {
                json.put("type", "OPEN_APP")
                json.put("packageName", config.packageName)
                json.put("appName", config.appName)
            }
        }
        return json.toString()
    }

    fun fromJson(jsonString: String): ActionConfig {
        val json = JSONObject(jsonString)
        return when (val type = json.getString("type")) {
            "OPEN_APP" -> ActionConfig.OpenAppConfig(
                packageName = json.getString("packageName"),
                appName = json.getString("appName")
            )
            else -> throw IllegalArgumentException("Tipo de ação desconhecido: $type")
        }
    }
}
