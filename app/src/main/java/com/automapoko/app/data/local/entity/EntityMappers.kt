package com.automapoko.app.data.local.entity

import com.automapoko.app.data.local.converter.ActionConfigConverter
import com.automapoko.app.data.local.converter.TriggerConfigConverter
import com.automapoko.app.domain.model.Automation
import com.automapoko.app.domain.model.AutomationStatus
import com.automapoko.app.domain.model.ExecutionLog
import com.automapoko.app.domain.model.ExecutionStatus
import com.automapoko.app.domain.model.TriggerType
import java.time.Instant

fun AutomationEntity.toDomain(): Automation = Automation(
    id = id,
    name = name,
    status = AutomationStatus.valueOf(status),
    triggerType = TriggerType.valueOf(triggerType),
    triggerConfig = TriggerConfigConverter.fromJson(triggerConfigJson),
    actionConfig = ActionConfigConverter.fromJson(actionConfigJson),
    createdAt = Instant.ofEpochMilli(createdAt),
    lastExecutedAt = lastExecutedAt?.let { Instant.ofEpochMilli(it) },
    lastExecutionStatus = lastExecutionStatus?.let { ExecutionStatus.valueOf(it) },
    diagnosticMessage = diagnosticMessage
)

fun Automation.toEntity(): AutomationEntity = AutomationEntity(
    id = id,
    name = name,
    status = status.name,
    triggerType = triggerType.name,
    triggerConfigJson = TriggerConfigConverter.toJson(triggerConfig),
    actionConfigJson = ActionConfigConverter.toJson(actionConfig),
    createdAt = createdAt.toEpochMilli(),
    lastExecutedAt = lastExecutedAt?.toEpochMilli(),
    lastExecutionStatus = lastExecutionStatus?.name,
    diagnosticMessage = diagnosticMessage
)

fun ExecutionLogEntity.toDomain(): ExecutionLog = ExecutionLog(
    id = id,
    automationId = automationId,
    automationName = automationName,
    executedAt = Instant.ofEpochMilli(executedAt),
    status = ExecutionStatus.valueOf(status),
    failureReason = failureReason
)

fun ExecutionLog.toEntity(): ExecutionLogEntity = ExecutionLogEntity(
    id = id,
    automationId = automationId,
    automationName = automationName,
    executedAt = executedAt.toEpochMilli(),
    status = status.name,
    failureReason = failureReason
)
