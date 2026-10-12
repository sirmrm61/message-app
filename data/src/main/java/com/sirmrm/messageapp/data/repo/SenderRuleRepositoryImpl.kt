package com.sirmrm.messageapp.data.repo

import com.sirmrm.messageapp.data.db.SenderRuleDao
import com.sirmrm.messageapp.data.db.SenderRuleEntity
import com.sirmrm.messageapp.domain.repository.SenderRuleRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class SenderRuleRepositoryImpl @Inject constructor(
    private val senderRuleDao: SenderRuleDao,
) : SenderRuleRepository {

    private fun key(address: String) =
        com.sirmrm.messageapp.classifier.TextNormalizer.normalizeSender(address).ifBlank { address.trim() }

    override suspend fun setRule(address: String, alwaysPromo: Boolean?) {
        val k = key(address)
        if (alwaysPromo == null) {
            senderRuleDao.delete(k)
        } else {
            val existing = senderRuleDao.get(k)
            senderRuleDao.upsert(
                SenderRuleEntity(
                    address = k,
                    alwaysPromo = alwaysPromo,
                    blocked = existing?.blocked ?: false,
                )
            )
        }
    }

    override suspend fun ruleFor(address: String): Boolean? =
        senderRuleDao.get(key(address))?.alwaysPromo

    override fun observeBlocked(): Flow<Set<String>> =
        senderRuleDao.observeBlocked().map { it.toSet() }

    override suspend fun setBlocked(address: String, blocked: Boolean) {
        val k = key(address)
        val existing = senderRuleDao.get(k)
        senderRuleDao.upsert(
            SenderRuleEntity(
                address = k,
                alwaysPromo = existing?.alwaysPromo ?: false,
                blocked = blocked,
            )
        )
    }
}
