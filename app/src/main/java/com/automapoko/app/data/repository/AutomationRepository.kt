package com.automapoko.app.data.repository

import com.automapoko.app.data.local.dao.AutomationDao
import com.automapoko.app.data.local.dao.ExecutionLogDao
import com.automapoko.app.data.local.entity.toDomain
import com.automapoko.app.data.local.entity.toEntity
import com.automapoko.app.domain.model.Automation
import com.automapoko.app.domain.model.AutomationStatus
import com.automapoko.app.domain.model.ExecutionLog
import com.automapoko.app.domain.model.ExecutionStatus
import com.automapoko.app.domain.model.TriggerType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutomationRepository @Inject constructor(
    private val automationDao: AutomationDao,
    private val executionLogDao: ExecutionLogDao
) {

    // ======================== AUTOMAÇÕES ========================

    /** Flow reativo com todas as automações — alimenta a tela principal */
    fun observeAll(): Flow<List<Automation>> =
        automationDao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }

    /** Flow reativo com uma automação específica */
    fun observeById(id: String): Flow<Automation?> =
        automationDao.observeById(id).map { it?.toDomain() }

    suspend fun getAll(): List<Automation> =
        automationDao.getAll().map { it.toDomain() }

    suspend fun getActive(): List<Automation> =
        automationDao.getActive().map { it.toDomain() }

    suspend fun getActiveByTriggerType(type: TriggerType): List<Automation> =
        automationDao.getActiveByTriggerType(type.name).map { it.toDomain() }

    suspend fun getById(id: String): Automation? =
        automationDao.getById(id)?.toDomain()

    suspend fun save(automation: Automation) =
        automationDao.insert(automation.toEntity())

    suspend fun update(automation: Automation) =
        automationDao.update(automation.toEntity())

    suspend fun delete(automation: Automation) {
        automationDao.delete(automation.toEntity())
    }

    suspend fun updateStatus(id: String, status: AutomationStatus, message: String? = null) =
        automationDao.updateStatus(id, status.name, message)

    // ======================== HISTÓRICO ========================

    /** Flow reativo com logs de uma automação */
    fun observeLogsByAutomation(automationId: String): Flow<List<ExecutionLog>> =
        executionLogDao.observeByAutomation(automationId).map { entities ->
            entities.map { it.toDomain() }
        }

    suspend fun saveLog(log: ExecutionLog) {
        executionLogDao.insert(log.toEntity())
        // Atualiza dados da última execução na automação
        automationDao.updateLastExecution(
            id = log.automationId,
            executedAt = log.executedAt.toEpochMilli(),
            status = log.status.name
        )
    }

    /** Remove logs com mais de 7 dias — chamado pelo WorkManager de saúde */
    suspend fun cleanOldLogs() {
        val cutoff = Instant.now().minus(7, ChronoUnit.DAYS).toEpochMilli()
        executionLogDao.deleteOlderThan(cutoff)
    }
}
