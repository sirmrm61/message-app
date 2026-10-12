package com.sirmrm.messageapp.data.di

import android.content.Context
import androidx.room.Room
import com.sirmrm.messageapp.data.db.MessageAppDatabase
import com.sirmrm.messageapp.data.prefs.SettingsRepositoryImpl
import com.sirmrm.messageapp.data.repo.BackupRepositoryImpl
import com.sirmrm.messageapp.data.repo.ConversationRepositoryImpl
import com.sirmrm.messageapp.data.repo.MessageRepositoryImpl
import com.sirmrm.messageapp.data.repo.SenderRuleRepositoryImpl
import com.sirmrm.messageapp.domain.repository.BackupRepository
import com.sirmrm.messageapp.domain.repository.ConversationRepository
import com.sirmrm.messageapp.domain.repository.MessageRepository
import com.sirmrm.messageapp.domain.repository.SenderRuleRepository
import com.sirmrm.messageapp.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindConversationRepository(impl: ConversationRepositoryImpl): ConversationRepository

    @Binds
    @Singleton
    abstract fun bindMessageRepository(impl: MessageRepositoryImpl): MessageRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindSenderRuleRepository(impl: SenderRuleRepositoryImpl): SenderRuleRepository

    @Binds
    @Singleton
    abstract fun bindBackupRepository(impl: BackupRepositoryImpl): BackupRepository

    companion object {
        @Provides
        @Singleton
        fun provideDatabase(@ApplicationContext context: Context): MessageAppDatabase =
            Room.databaseBuilder(context, MessageAppDatabase::class.java, "message-app.db")
                .addMigrations(MessageAppDatabase.MIGRATION_1_2, MessageAppDatabase.MIGRATION_2_3)
                .fallbackToDestructiveMigration()
                .build()

        @Provides fun conversationDao(db: MessageAppDatabase) = db.conversationDao()
        @Provides fun messageDao(db: MessageAppDatabase) = db.messageDao()
        @Provides fun userFolderDao(db: MessageAppDatabase) = db.userFolderDao()
        @Provides fun senderRuleDao(db: MessageAppDatabase) = db.senderRuleDao()
        @Provides fun moveLogDao(db: MessageAppDatabase) = db.moveLogDao()
        @Provides fun nbDao(db: MessageAppDatabase) = db.nbDao()
        @Provides fun customBankPatternDao(db: MessageAppDatabase) = db.customBankPatternDao()
    }
}
