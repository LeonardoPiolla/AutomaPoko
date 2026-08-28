package com.automapoko.app.domain.usecase

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.automapoko.app.data.repository.AutomationRepository
import com.automapoko.app.domain.model.Automation
import com.automapoko.app.domain.model.TriggerConfig
import com.automapoko.app.domain.model.TriggerEvent
import com.automapoko.app.domain.model.TriggerType
import com.automapoko.app.receivers.GeofencingTriggerReceiver
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gerencia o ciclo de vida dos Geofences na API do Google Play Services.
 *
 * Geofences são registrados usando o ID da automação como requestId,
 * o que permite identificar qual automação disparou o evento no receiver.
 */
@Singleton
class GeofenceManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: AutomationRepository
) {

    private val geofencingClient: GeofencingClient by lazy {
        LocationServices.getGeofencingClient(context)
    }

    /** PendingIntent que aponta para o GeofencingTriggerReceiver */
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofencingTriggerReceiver::class.java)
        PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    /** Registra o geofence de uma automação de localização */
    @SuppressLint("MissingPermission")
    suspend fun registerGeofence(automation: Automation) {
        val config = automation.triggerConfig as? TriggerConfig.LocationConfig ?: return

        val transitionType = when (config.event) {
            TriggerEvent.ENTER -> Geofence.GEOFENCE_TRANSITION_ENTER
            TriggerEvent.EXIT -> Geofence.GEOFENCE_TRANSITION_EXIT
            else -> return
        }

        val geofence = Geofence.Builder()
            .setRequestId(automation.id)
            .setCircularRegion(config.latitude, config.longitude, config.radiusMeters)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(transitionType)
            .setLoiteringDelay(30_000) // 30s dentro da área antes de disparar ENTER
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        try {
            geofencingClient.addGeofences(request, geofencePendingIntent).await()
        } catch (e: Exception) {
            // Permissão negada ou serviço indisponível — ignora silenciosamente
            // O diagnóstico é tratado na camada de UI/ViewModel
        }
    }

    /** Cancela o geofence de uma automação */
    suspend fun removeGeofence(automationId: String) {
        try {
            geofencingClient.removeGeofences(listOf(automationId)).await()
        } catch (e: Exception) {
            // Ignora — geofence pode já não existir
        }
    }

    /** Reregistra todos os geofences ativos — chamado após reboot */
    suspend fun reregisterAllActiveGeofences() {
        val activeLocationAutomations = repository.getActiveByTriggerType(TriggerType.LOCATION)
        for (automation in activeLocationAutomations) {
            registerGeofence(automation)
        }
    }
}
