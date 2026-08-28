package com.automapoko.app.domain.executor

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.automapoko.app.domain.model.ActionConfig
import com.automapoko.app.domain.model.Automation
import com.automapoko.app.domain.model.AutomationStatus
import com.automapoko.app.domain.model.ExecutionLog
import com.automapoko.app.domain.model.ExecutionStatus
import com.automapoko.app.data.repository.AutomationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Executa a ação de uma automação e registra o resultado no histórico.
 *
 * Responsabilidades:
 * 1. Verificar pré-condições (SYSTEM_ALERT_WINDOW, app instalado)
 * 2. Executar a ação
 * 3. Registrar o resultado (sucesso ou falha com motivo)
 * 4. Atualizar o diagnóstico da automação se necessário
 */
@Singleton
class AutomationExecutor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: AutomationRepository
) {

    /**
     * Ponto de entrada principal — recebe a automação que disparou.
     * Suspending: deve ser chamado dentro de uma coroutine (ex: dentro do receiver via scope).
     */
    suspend fun execute(automation: Automation) {
        when (val action = automation.actionConfig) {
            is ActionConfig.OpenAppConfig -> executeOpenApp(automation, action)
        }
    }

    private suspend fun executeOpenApp(
        automation: Automation,
        action: ActionConfig.OpenAppConfig
    ) {
        // Verificação 1: permissão SYSTEM_ALERT_WINDOW
        if (!Settings.canDrawOverlays(context)) {
            recordFailure(
                automation = automation,
                reason = "Permissão 'Exibir sobre outros apps' não concedida. Abra o AutomaPoko para corrigir."
            )
            repository.updateStatus(
                id = automation.id,
                status = AutomationStatus.INACTIVE,
                message = "Permissão para abrir apps negada. Toque para corrigir."
            )
            return
        }

        // Verificação 2: app alvo instalado
        val launchIntent = context.packageManager.getLaunchIntentForPackage(action.packageName)
        if (launchIntent == null) {
            recordFailure(
                automation = automation,
                reason = "Aplicativo '${action.appName}' não está instalado."
            )
            repository.updateStatus(
                id = automation.id,
                status = AutomationStatus.INACTIVE,
                message = "Aplicativo '${action.appName}' não está instalado."
            )
            return
        }

        // Verificação 3: app alvo já está em primeiro plano — não faz nada
        if (isAppInForeground(action.packageName)) {
            // Registra sucesso silencioso (app já estava aberto)
            recordSuccess(automation)
            return
        }

        // Executa: abre o app
        try {
            launchIntent.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            }
            context.startActivity(launchIntent)
            recordSuccess(automation)
        } catch (e: Exception) {
            recordFailure(
                automation = automation,
                reason = "Falha ao abrir '${action.appName}': ${e.message}"
            )
        }
    }

    private fun isAppInForeground(packageName: String): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningProcesses = activityManager.runningAppProcesses ?: return false
        return runningProcesses.any { process ->
            process.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                && process.processName == packageName
        }
    }

    private suspend fun recordSuccess(automation: Automation) {
        repository.saveLog(
            ExecutionLog(
                automationId = automation.id,
                automationName = automation.name,
                status = ExecutionStatus.SUCCESS
            )
        )
    }

    private suspend fun recordFailure(automation: Automation, reason: String) {
        repository.saveLog(
            ExecutionLog(
                automationId = automation.id,
                automationName = automation.name,
                status = ExecutionStatus.FAILURE,
                failureReason = reason
            )
        )
    }
}
