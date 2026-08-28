package com.automapoko.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.automapoko.app.data.local.entity.ExecutionLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExecutionLogDao {

    /** Observa logs de uma automação, mais recentes primeiro */
    @Query("""
        SELECT * FROM execution_logs 
        WHERE automationId = :automationId 
        ORDER BY executedAt DESC
    """)
    fun observeByAutomation(automationId: String): Flow<List<ExecutionLogEntity>>

    /** Insere um log de execução */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: ExecutionLogEntity)

    /** Remove logs com mais de 7 dias para manter o banco limpo */
    @Query("""
        DELETE FROM execution_logs 
        WHERE executedAt < :cutoffMillis
    """)
    suspend fun deleteOlderThan(cutoffMillis: Long)

    /** Remove todos os logs de uma automação (usado antes de excluir a automação) */
    @Query("DELETE FROM execution_logs WHERE automationId = :automationId")
    suspend fun deleteByAutomation(automationId: String)
}
