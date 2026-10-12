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
        CustomBankPatternEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class MessageAppDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun userFolderDao(): UserFolderDao
    abstract fun senderRuleDao(): SenderRuleDao
    abstract fun moveLogDao(): MoveLogDao
    abstract fun nbDao(): NbDao
    abstract fun customBankPatternDao(): CustomBankPatternDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE conversations ADD COLUMN photoUri TEXT")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE messages ADD COLUMN txSubtype TEXT")
                db.execSQL("ALTER TABLE messages ADD COLUMN txAmountRials INTEGER")
                db.execSQL("ALTER TABLE messages ADD COLUMN txBalanceRials INTEGER")
                db.execSQL("ALTER TABLE messages ADD COLUMN bankName TEXT")
                db.execSQL("ALTER TABLE messages ADD COLUMN suspicious INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE messages ADD COLUMN maybeTransactional INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS custom_bank_patterns " +
                        "(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "pattern TEXT NOT NULL, createdAt INTEGER NOT NULL)"
                )
            }
        }
    }
}
