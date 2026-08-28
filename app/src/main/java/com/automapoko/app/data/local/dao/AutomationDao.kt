package com.automapoko.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.automapoko.app.data.local.entity.AutomationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AutomationDao {

    /** Observa todas as automações em ordem de criação (mais antigas primeiro) */
    @Query("SELECT * FROM automations ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<AutomationEntity>>

    /** Retorna todas as automações (suspending, para uso em receivers/workers) */
    @Query("SELECT * FROM automations ORDER BY createdAt ASC")
    suspend fun getAll(): List<AutomationEntity>

    /** Retorna apenas automações ativas (status = ACTIVE) */
    @Query("SELECT * FROM automations WHERE status = 'ACTIVE' ORDER BY createdAt ASC")
    suspend fun getActive(): List<AutomationEntity>

    /** Retorna automação por ID */
    @Query("SELECT * FROM automations WHERE id = :id")
    suspend fun getById(id: String): AutomationEntity?

    /** Observa automação por ID */
    @Query("SELECT * FROM automations WHERE id = :id")
    fun observeById(id: String): Flow<AutomationEntity?>

    /** Insere ou substitui uma automação */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(automation: AutomationEntity)

    /** Atualiza automação existente */
    @Update
    suspend fun update(automation: AutomationEntity)

    /** Exclui automação (logs são excluídos em cascata via ForeignKey) */
    @Delete
    suspend fun delete(automation: AutomationEntity)

    /** Atualiza status e diagnóstico de uma automação */
    @Query("""
        UPDATE automations 
        SET status = :status, diagnosticMessage = :message 
        WHERE id = :id
    """)
    suspend fun updateStatus(id: String, status: String, message: String?)

    /** Atualiza dados da última execução */
    @Query("""
        UPDATE automations 
        SET lastExecutedAt = :executedAt, lastExecutionStatus = :status
        WHERE id = :id
    """)
    suspend fun updateLastExecution(id: String, executedAt: Long, status: String)

    /** Retorna automações ativas de um tipo específico de gatilho */
    @Query("""
        SELECT * FROM automations 
        WHERE status = 'ACTIVE' AND triggerType = :triggerType 
        ORDER BY createdAt ASC
    """)
    suspend fun getActiveByTriggerType(triggerType: String): List<AutomationEntity>
}
