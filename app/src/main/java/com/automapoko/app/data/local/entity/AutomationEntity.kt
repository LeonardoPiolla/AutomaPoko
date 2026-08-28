package com.automapoko.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidade Room que persiste uma automação no banco de dados.
 *
 * TriggerConfig e ActionConfig são serializadas como JSON (via TypeConverter)
 * para permitir extensibilidade futura sem migrações complexas de schema.
 */
@Entity(tableName = "automations")
data class AutomationEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val status: String,             // AutomationStatus.name
    val triggerType: String,        // TriggerType.name
    val triggerConfigJson: String,  // JSON serializado de TriggerConfig
    val actionConfigJson: String,   // JSON serializado de ActionConfig
    val createdAt: Long,            // Instant.toEpochMilli()
    val lastExecutedAt: Long?,
    val lastExecutionStatus: String?,
    val diagnosticMessage: String?
)
