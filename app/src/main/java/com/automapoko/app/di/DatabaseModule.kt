package com.automapoko.app.di

import android.content.Context
import androidx.room.Room
import com.automapoko.app.data.local.AutomaPokoDatabase
import com.automapoko.app.data.local.dao.AutomationDao
import com.automapoko.app.data.local.dao.ExecutionLogDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AutomaPokoDatabase =
        Room.databaseBuilder(
            context,
            AutomaPokoDatabase::class.java,
            "automapoko.db"
        )
            .fallbackToDestructiveMigration() // MVP: aceita recriar o banco em migrações
            .build()

    @Provides
    fun provideAutomationDao(db: AutomaPokoDatabase): AutomationDao =
        db.automationDao()

    @Provides
    fun provideExecutionLogDao(db: AutomaPokoDatabase): ExecutionLogDao =
        db.executionLogDao()
}
