package com.automapoko.app.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.automapoko.app.data.repository.AutomationRepository
import com.automapoko.app.domain.usecase.GeofenceManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Worker periódico que garante a saúde das automações em background.
 *
 * Executado a cada 15 minutos pelo WorkManager.
 *
 * Responsabilidades:
 * 1. Reregistrar geofences ativos (resistência ao HyperOS matar processos)
 * 2. Limpar logs com mais de 7 dias
 *
 * Por que WorkManager e não AlarmManager?
 * WorkManager é a solução recomendada pelo Android para tarefas periódicas — ele
 * respeita o estado da bateria (Doze mode), persiste entre reinicializações
 * e é compatível com o HyperOS.
 */
@HiltWorker
class HealthCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val geofenceManager: GeofenceManager,
    private val repository: AutomationRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            // 1. Reregistra geofences (sem efeito se já estiverem registrados)
            geofenceManager.reregisterAllActiveGeofences()

            // 2. Remove logs com mais de 7 dias
            repository.cleanOldLogs()

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
