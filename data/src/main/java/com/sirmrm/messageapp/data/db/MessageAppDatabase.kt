package com.sirmrm.messageapp.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

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
    version = 1,
    exportSchema = true,
)
abstract class MessageAppDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun userFolderDao(): UserFolderDao
    abstract fun senderRuleDao(): SenderRuleDao
    abstract fun moveLogDao(): MoveLogDao
    abstract fun nbDao(): NbDao
}
