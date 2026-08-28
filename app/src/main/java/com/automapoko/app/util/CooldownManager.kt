package com.automapoko.app.util

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Controla o cooldown de 30 segundos entre execuções da mesma automação.
 *
 * Armazenado em memória: se o processo morrer, o cooldown é resetado.
 * Isso é intencional — se o processo foi encerrado, uma nova conexão
 * Bluetooth é genuinamente um novo evento.
 */
@Singleton
class CooldownManager @Inject constructor() {

    companion object {
        const val COOLDOWN_MS = 30_000L // 30 segundos
    }

    // Mapa de automationId -> timestamp da última execução (ms)
    private val lastExecutionMap = mutableMapOf<String, Long>()

    /**
     * Verifica se a automação pode ser executada agora.
     * @return true se o cooldown expirou (ou nunca foi executada), false caso contrário
     */
    fun canExecute(automationId: String): Boolean {
        val lastExecution = lastExecutionMap[automationId] ?: return true
        return System.currentTimeMillis() - lastExecution >= COOLDOWN_MS
    }

    /**
     * Registra que a automação foi executada agora.
     * Deve ser chamado imediatamente após decidir executar.
     */
    fun markExecuted(automationId: String) {
        lastExecutionMap[automationId] = System.currentTimeMillis()
    }
}
