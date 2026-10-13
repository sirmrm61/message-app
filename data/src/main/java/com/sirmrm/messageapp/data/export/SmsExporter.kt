package com.sirmrm.messageapp.data.export

import android.content.Context
import android.net.Uri
import com.sirmrm.messageapp.data.db.MessageDao
import com.sirmrm.messageapp.data.db.MessageEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Exports every stored SMS to a user-picked file (SAF) for offline analysis
 * (e.g. feeding to an AI model). Includes this app's own classification
 * fields alongside the raw Telephony fields.
 */
@Singleton
class SmsExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val messageDao: MessageDao,
) {

    enum class Format(val extension: String, val mime: String) {
        JSON("json", "application/json"),
        CSV("csv", "text/csv"),
    }

    /** Writes all messages to [uri]. Returns the number of rows written. */
    suspend fun export(uri: Uri, format: Format): Int = withContext(Dispatchers.IO) {
        val messages = messageDao.exportAll().sortedBy { it.date }
        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.bufferedWriter(Charsets.UTF_8).use { w ->
                when (format) {
                    Format.JSON -> writeJson(messages, w)
                    Format.CSV -> writeCsv(messages, w)
                }
            }
        } ?: error("cannot open output stream: $uri")
        messages.size
    }

    private fun writeJson(messages: List<MessageEntity>, w: java.io.Writer) {
        w.write("{\n  \"messages\": [\n")
        messages.forEachIndexed { i, m ->
            w.write("    {")
            w.write("\"id\": ${m.id}")
            w.write(", \"thread_id\": ${m.threadId}")
            w.write(", \"address\": ${jsonString(m.address)}")
            w.write(", \"body\": ${jsonString(m.body)}")
            w.write(", \"date\": ${m.date}")
            w.write(", \"date_iso\": ${jsonString(iso(m.date))}")
            w.write(", \"type\": ${jsonString(m.type)}")
            w.write(", \"read\": ${m.read}")
            w.write(", \"folder\": ${jsonString(m.folder)}")
            w.write(", \"category\": ${jsonString(m.category)}")
            m.txSubtype?.let { w.write(", \"tx_subtype\": ${jsonString(it)}") }
            m.txAmountRials?.let { w.write(", \"tx_amount_rials\": $it") }
            m.txBalanceRials?.let { w.write(", \"tx_balance_rials\": $it") }
            m.bankName?.let { w.write(", \"bank_name\": ${jsonString(it)}") }
            if (m.suspicious) w.write(", \"suspicious\": true")
            w.write("}")
            w.write(if (i == messages.lastIndex) "\n" else ",\n")
        }
        w.write("  ]\n}\n")
    }

    private fun writeCsv(messages: List<MessageEntity>, w: java.io.Writer) {
        w.write("id,thread_id,address,body,date,date_iso,type,read,folder,category,tx_subtype,tx_amount_rials,tx_balance_rials,bank_name,suspicious\n")
        for (m in messages) {
            w.write(
                listOf(
                    m.id, m.threadId,
                    csvCell(m.address), csvCell(m.body),
                    m.date, csvCell(iso(m.date)),
                    csvCell(m.type), if (m.read) 1 else 0,
                    csvCell(m.folder), csvCell(m.category),
                    csvCell(m.txSubtype ?: ""), m.txAmountRials ?: "",
                    m.txBalanceRials ?: "", csvCell(m.bankName ?: ""),
                    if (m.suspicious) 1 else 0,
                ).joinToString(","),
            )
            w.write("\n")
        }
    }

    private fun iso(epochMs: Long): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date(epochMs))

    private fun jsonString(s: String): String = buildString(s.length + 2) {
        append('"')
        for (ch in s) {
            when (ch) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (ch < ' ') append("\\u%04x".format(ch.code)) else append(ch)
            }
        }
        append('"')
    }

    private fun csvCell(s: String): String =
        if (s.any { it == ',' || it == '"' || it == '\n' || it == '\r' })
            "\"${s.replace("\"", "\"\"")}\""
        else s
}
