package com.automapoko.app.domain.model

import java.time.Instant
import java.util.UUID

/**
 * Registro de uma execução de automação.
 * Guardamos os 7 dias mais recentes por automação.
 */
data class ExecutionLog(
    val id: String = UUID.randomUUID().toString(),
    val automationId: String,
    val automationName: String,
    val executedAt: Instant = Instant.now(),
    val status: ExecutionStatus,
    /** Motivo da falha, quando status == FAILURE */
    val failureReason: String? = null
)
