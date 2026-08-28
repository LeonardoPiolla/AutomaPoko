package com.automapoko.app.service

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkManagerScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val HEALTH_CHECK_WORK_NAME = "automapoko_health_check"
    }

    /**
     * Agenda o worker de saúde para rodar a cada 15 minutos.
     * Usa KEEP para não recriar se já estiver agendado.
     *
     * Chamado na inicialização do app (MainActivity.onCreate).
     */
    fun scheduleHealthCheck() {
        val constraints = Constraints.Builder()
            .build() // Sem restrições — precisa rodar independente de rede/bateria

        val request = PeriodicWorkRequestBuilder<HealthCheckWorker>(
            repeatInterval = 15,
            repeatIntervalTimeUnit = TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            HEALTH_CHECK_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
