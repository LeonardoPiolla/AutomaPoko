package com.automapoko.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidade Room para registro de execuções.
 * ForeignKey garante que logs órfãos sejam removidos quando a automação é excluída.
 */
@Entity(
    tableName = "execution_logs",
    foreignKeys = [
        ForeignKey(
            entity = AutomationEntity::class,
            parentColumns = ["id"],
            childColumns = ["automationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("automationId"), Index("executedAt")]
)
data class ExecutionLogEntity(
    @PrimaryKey
    val id: String,
    val automationId: String,
    val automationName: String,
    val executedAt: Long,       // Instant.toEpochMilli()
    val status: String,         // ExecutionStatus.name
    val failureReason: String?
)
