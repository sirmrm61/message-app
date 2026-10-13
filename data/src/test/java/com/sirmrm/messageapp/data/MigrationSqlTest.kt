package com.sirmrm.messageapp.data

import androidx.sqlite.db.SupportSQLiteDatabase
import com.sirmrm.messageapp.data.db.MessageAppDatabase
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

class MigrationSqlTest {

    private fun record(migration: androidx.room.migration.Migration): List<String> {
        val sql = mutableListOf<String>()
        val db = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java),
        ) { _, method, args ->
            if (method.name == "execSQL" && args != null && args.isNotEmpty()) {
                sql += args[0] as String
            }
            when (method.returnType) {
                java.lang.Boolean.TYPE -> false
                java.lang.Integer.TYPE -> 0
                java.lang.Long.TYPE -> 0L
                else -> null
            }
        } as SupportSQLiteDatabase
        migration.migrate(db)
        return sql
    }

    @Test
    fun migration34_adds_decision_and_createdAt() {
        val sql = record(MessageAppDatabase.MIGRATION_3_4).joinToString("\n")
        assertTrue(sql.contains("decision"))
        assertTrue(sql.contains("createdAt"))
        assertTrue(sql.contains("sender_rules"))
    }

    @Test
    fun migration45_creates_key_contacts() {
        val sql = record(MessageAppDatabase.MIGRATION_4_5).joinToString("\n")
        assertTrue(sql.contains("key_contacts"))
        assertTrue(sql.contains("senderKey TEXT PRIMARY KEY"))
        assertTrue(sql.contains("autoRead"))
        assertTrue(sql.contains("voiceId"))
    }
}
