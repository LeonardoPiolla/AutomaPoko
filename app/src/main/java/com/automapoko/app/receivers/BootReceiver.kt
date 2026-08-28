package com.automapoko.app.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.automapoko.app.domain.usecase.GeofenceManager
import com.automapoko.app.util.ReceiverCoroutineScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Executado após reinicialização do dispositivo.
 *
 * Responsabilidade: reregistrar todos os geofences ativos.
 *
 * Por quê? A API de Geofencing do Google Play Services não persiste os geofences
 * após reinicialização — eles precisam ser reregistrados pelo app.
 * Os BroadcastReceivers de Bluetooth e Wi-Fi, por outro lado, são automaticamente
 * re-habilitados pelo sistema pois estão declarados no Manifest.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var geofenceManager: GeofenceManager

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val isBootAction = action == Intent.ACTION_BOOT_COMPLETED
                || action == "android.intent.action.QUICKBOOT_POWERON"
                || action == "com.htc.intent.action.QUICKBOOT_POWERON"

        if (!isBootAction) return

        val pendingResult = goAsync()

        ReceiverCoroutineScope.scope.launch {
            try {
                geofenceManager.reregisterAllActiveGeofences()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
