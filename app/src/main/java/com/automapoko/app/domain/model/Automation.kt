package com.automapoko.app.domain.model

import java.time.Instant
import java.util.UUID

/**
 * Modelo de domínio de uma automação.
 * Este é o objeto central do app — representa uma regra QUANDO → ENTÃO.
 */
data class Automation(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val status: AutomationStatus = AutomationStatus.ACTIVE,
    val triggerType: TriggerType,
    val triggerConfig: TriggerConfig,
    val actionConfig: ActionConfig,
    val createdAt: Instant = Instant.now(),
    val lastExecutedAt: Instant? = null,
    val lastExecutionStatus: ExecutionStatus? = null,
    /** Mensagem de diagnóstico explicando por que está inativa */
    val diagnosticMessage: String? = null
)
