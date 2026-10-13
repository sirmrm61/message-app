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
        CustomBankPatternEntity::class, KeyContactEntity::class,
    ],
    version = 5,
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
    abstract fun keyContactDao(): KeyContactDao

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

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sender_rules ADD COLUMN decision TEXT")
                db.execSQL("ALTER TABLE sender_rules ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS key_contacts " +
                        "(senderKey TEXT PRIMARY KEY NOT NULL, displayName TEXT NOT NULL, " +
                        "autoRead INTEGER NOT NULL DEFAULT 1, voiceId TEXT, addedAt INTEGER NOT NULL)"
                )
            }
        }
    }
}
