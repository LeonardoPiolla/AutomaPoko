package com.automapoko.app.receivers

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
 * Recebe broadcasts de conexão e desconexão Bluetooth do sistema.
 *
 * Declarado no AndroidManifest — o sistema acorda o processo quando o evento ocorre,
 * mesmo que o app não esteja em execução.
 *
 * Fluxo:
 * 1. Sistema envia broadcast ACL_CONNECTED ou ACL_DISCONNECTED
 * 2. Receiver identifica o dispositivo pelo endereço MAC
 * 3. Busca automações ativas do tipo Bluetooth
 * 4. Filtra pelo endereço MAC e evento configurado
 * 5. Verifica cooldown de 30s
 * 6. Executa ação via AutomationExecutor
 */
@AndroidEntryPoint
class BluetoothTriggerReceiver : BroadcastReceiver() {

    @Inject lateinit var repository: AutomationRepository
    @Inject lateinit var executor: AutomationExecutor
    @Inject lateinit var cooldownManager: CooldownManager

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        // Identifica o evento
        val event = when (action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> TriggerEvent.CONNECTED
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> TriggerEvent.DISCONNECTED
            else -> return
        }

        // Extrai o dispositivo Bluetooth do intent
        val device: BluetoothDevice? = intent.getParcelableExtra(
            BluetoothDevice.EXTRA_DEVICE,
            BluetoothDevice::class.java
        )
        val deviceAddress = device?.address ?: return

        // Usa goAsync() para que o sistema aguarde a coroutine completar
        val pendingResult = goAsync()

        ReceiverCoroutineScope.scope.launch {
            try {
                processBluetoothEvent(deviceAddress, event)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun processBluetoothEvent(deviceAddress: String, event: TriggerEvent) {
        val activeAutomations = repository.getActiveByTriggerType(TriggerType.BLUETOOTH)

        for (automation in activeAutomations) {
            val config = automation.triggerConfig as? TriggerConfig.BluetoothConfig ?: continue

            // Verifica se é o dispositivo e evento correto
            if (config.deviceAddress != deviceAddress || config.event != event) continue

            // Verifica cooldown
            if (!cooldownManager.canExecute(automation.id)) continue

            // Marca como executando (antes de executar, para bloquear execuções paralelas)
            cooldownManager.markExecuted(automation.id)

            // Executa a ação
            executor.execute(automation)
        }
    }
}
