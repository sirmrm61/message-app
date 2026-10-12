package com.sirmrm.messageapp.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
        UserFolderEntity::class,
        SenderRuleEntity::class,
        MoveLogEntity::class,
        NbTokenEntity::class,
        NbMetaEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class MessageAppDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun userFolderDao(): UserFolderDao
    abstract fun senderRuleDao(): SenderRuleDao
    abstract fun moveLogDao(): MoveLogDao
    abstract fun nbDao(): NbDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE conversations ADD COLUMN photoUri TEXT")
            }
        }
    }
}
