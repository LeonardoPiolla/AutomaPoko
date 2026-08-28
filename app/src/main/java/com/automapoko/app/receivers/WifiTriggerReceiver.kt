package com.automapoko.app.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import com.automapoko.app.data.repository.AutomationRepository
import com.automapoko.app.domain.executor.AutomationExecutor
import com.automapoko.app.domain.model.TriggerConfig
import com.automapoko.app.domain.model.TriggerEvent
import com.automapoko.app.domain.model.TriggerType
import com.automapoko.app.util.CooldownManager
import com.automapoko.app.util.ReceiverCoroutineScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Recebe broadcasts de mudança de estado de rede Wi-Fi.
 *
 * Limitação do Android 12+: para ler o SSID, é necessário ACCESS_FINE_LOCATION
 * e ACCESS_BACKGROUND_LOCATION. Sem essas permissões, getConnectionInfo() retorna SSID oculto.
 *
 * Rastreia o estado anterior da conexão para detectar transições
 * (desconectado→conectado e conectado→desconectado) com precisão.
 */
@AndroidEntryPoint
class WifiTriggerReceiver : BroadcastReceiver() {

    @Inject lateinit var repository: AutomationRepository
    @Inject lateinit var executor: AutomationExecutor
    @Inject lateinit var cooldownManager: CooldownManager

    companion object {
        // Controle de estado anterior para detectar transições reais
        private var lastConnectedSsid: String? = null
        private var wasConnected: Boolean = false
    }

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()

        ReceiverCoroutineScope.scope.launch {
            try {
                processWifiChange(context)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun processWifiChange(context: Context) {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val wifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        val isWifiConnected = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true

        if (isWifiConnected) {
            // Obtém SSID da rede conectada
            // Requer ACCESS_FINE_LOCATION + ACCESS_BACKGROUND_LOCATION no Android 12+
            @Suppress("DEPRECATION")
            val wifiInfo = wifiManager.connectionInfo
            val rawSsid = wifiInfo?.ssid ?: return

            // O Android envolve o SSID em aspas — remover para comparação limpa
            val currentSsid = rawSsid.removeSurrounding("\"")

            // Ignora SSID desconhecido ou oculto (permissão negada)
            if (currentSsid.isBlank() || currentSsid == "<unknown ssid>") return

            // Detecta transição: estava desconectado e agora conectou
            if (!wasConnected || lastConnectedSsid != currentSsid) {
                wasConnected = true
                lastConnectedSsid = currentSsid
                processEvent(currentSsid, TriggerEvent.CONNECTED)
            }
        } else {
            // Detecta transição: estava conectado e agora desconectou
            if (wasConnected && lastConnectedSsid != null) {
                val ssidQueDesconectou = lastConnectedSsid!!
                wasConnected = false
                lastConnectedSsid = null
                processEvent(ssidQueDesconectou, TriggerEvent.DISCONNECTED)
            }
        }
    }

    private suspend fun processEvent(ssid: String, event: TriggerEvent) {
        val activeAutomations = repository.getActiveByTriggerType(TriggerType.WIFI)

        for (automation in activeAutomations) {
            val config = automation.triggerConfig as? TriggerConfig.WifiConfig ?: continue

            if (config.ssid != ssid || config.event != event) continue
            if (!cooldownManager.canExecute(automation.id)) continue

            cooldownManager.markExecuted(automation.id)
            executor.execute(automation)
        }
    }
}
