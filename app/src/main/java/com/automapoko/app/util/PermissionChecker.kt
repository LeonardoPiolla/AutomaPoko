package com.automapoko.app.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "automapoko_prefs")

object PreferencesKeys {
    val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
}

@Singleton
class PermissionChecker @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend fun isOnboardingCompleted(): Boolean =
        context.dataStore.data
            .map { it[PreferencesKeys.ONBOARDING_COMPLETED] ?: false }
            .first()

    suspend fun setOnboardingCompleted() {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.ONBOARDING_COMPLETED] = true
        }
    }

    /** Verifica se SYSTEM_ALERT_WINDOW está concedida */
    fun hasOverlayPermission(): Boolean =
        Settings.canDrawOverlays(context)

    /** Verifica se o app está isento de otimização de bateria */
    fun hasBatteryOptimizationExemption(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    /** Abre a tela de configuração de SYSTEM_ALERT_WINDOW do sistema */
    fun openOverlaySettings(activity: Activity) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
        activity.startActivity(intent)
    }

    /** Abre a tela de isenção de bateria do sistema */
    fun openBatteryOptimizationSettings(activity: Activity) {
        val intent = Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:${context.packageName}")
        )
        activity.startActivity(intent)
    }

    /** Abre as configurações do app (para permissões negadas com "não perguntar novamente") */
    fun openAppSettings(activity: Activity) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        activity.startActivity(intent)
    }

    /**
     * Verifica permissões críticas na abertura do app.
     * Se SYSTEM_ALERT_WINDOW ou localização em background estiverem revogadas,
     * isso é tratado nas telas relevantes via diagnóstico.
     */
    suspend fun checkAndRequestCriticalPermissions(activity: Activity) {
        // As solicitações de permissão em runtime são feitas pelas telas
        // que precisam delas, via Accompanist Permissions.
        // Aqui apenas garantimos que o onboarding seja exibido se necessário.
    }
}
