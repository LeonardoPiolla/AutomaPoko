package com.automapoko.app.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.automapoko.app.data.repository.AutomationRepository
import com.automapoko.app.domain.executor.AutomationExecutor
import com.automapoko.app.domain.model.TriggerConfig
import com.automapoko.app.domain.model.TriggerEvent
import com.automapoko.app.util.CooldownManager
import com.automapoko.app.util.ReceiverCoroutineScope
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Recebe eventos de Geofencing do Google Play Services via PendingIntent.
 *
 * O ID de cada geofence é o ID da automação (String UUID).
 * Isso permite relacionar diretamente o evento com a automação correta.
 *
 * O Google Play Services monitora as áreas de forma nativa e eficiente,
 * acordando este receiver apenas quando o dispositivo entra ou sai de uma área.
 */
@AndroidEntryPoint
class GeofencingTriggerReceiver : BroadcastReceiver() {

    @Inject lateinit var repository: AutomationRepository
    @Inject lateinit var executor: AutomationExecutor
    @Inject lateinit var cooldownManager: CooldownManager

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return

        if (geofencingEvent.hasError()) return

        val transition = geofencingEvent.geofenceTransition
        val event = when (transition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> TriggerEvent.ENTER
            Geofence.GEOFENCE_TRANSITION_EXIT -> TriggerEvent.EXIT
            else -> return
        }

        val triggeringGeofences = geofencingEvent.triggeringGeofences ?: return

        val pendingResult = goAsync()

        ReceiverCoroutineScope.scope.launch {
            try {
                for (geofence in triggeringGeofences) {
                    processGeofenceEvent(geofence.requestId, event)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun processGeofenceEvent(automationId: String, event: TriggerEvent) {
        val automation = repository.getById(automationId) ?: return
        if (automation.triggerConfig !is TriggerConfig.LocationConfig) return
        if (automation.triggerConfig.event != event) return
        if (!cooldownManager.canExecute(automationId)) return

        cooldownManager.markExecuted(automationId)
        executor.execute(automation)
    }
}
