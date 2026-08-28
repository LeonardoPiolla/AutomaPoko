package com.automapoko.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.automapoko.app.data.local.dao.AutomationDao
import com.automapoko.app.data.local.dao.ExecutionLogDao
import com.automapoko.app.data.local.entity.AutomationEntity
import com.automapoko.app.data.local.entity.ExecutionLogEntity

@Database(
    entities = [AutomationEntity::class, ExecutionLogEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AutomaPokoDatabase : RoomDatabase() {
    abstract fun automationDao(): AutomationDao
    abstract fun executionLogDao(): ExecutionLogDao
}
