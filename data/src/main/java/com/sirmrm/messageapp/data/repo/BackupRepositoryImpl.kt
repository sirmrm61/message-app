package com.sirmrm.messageapp.data.repo

import com.sirmrm.messageapp.data.backup.SmsBackupXml
import com.sirmrm.messageapp.data.db.MessageDao
import com.sirmrm.messageapp.data.db.MessageEntity
import com.sirmrm.messageapp.data.db.toDomain
import com.sirmrm.messageapp.domain.model.MessageType
import com.sirmrm.messageapp.domain.repository.BackupRepository
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepositoryImpl @Inject constructor(
    private val messageDao: MessageDao,
) : BackupRepository {

    override suspend fun exportXml(destination: OutputStream): Int {
        val messages = messageDao.exportAll().map { it.toDomain() }
        SmsBackupXml.write(messages, destination)
        return messages.size
    }

    override suspend fun importXml(source: InputStream): Int {
        val parsed = SmsBackupXml.parse(source)
        val entities = parsed.mapIndexed { i, p ->
            MessageEntity(
                // Negative ids = rows that only exist in our DB, not the provider.
                id = -(System.currentTimeMillis() + i),
                threadId = p.address.hashCode().toLong(),
                address = p.address,
                body = p.body,
                date = p.date,
                type = if (p.type == 1) MessageType.INBOX.name else MessageType.SENT.name,
                read = p.read,
                seen = true,
                subscriptionId = -1,
                folder = "INBOX",
                customFolderId = null,
                category = "UNKNOWN",
                promoScore = 0f,
                reasonCodes = "",
                deletedAt = null,
                txSubtype = null, txAmountRials = null, txBalanceRials = null,
                bankName = null, suspicious = false, maybeTransactional = false,
            )
        }
        messageDao.upsertAll(entities)
        return entities.size
    }
}
