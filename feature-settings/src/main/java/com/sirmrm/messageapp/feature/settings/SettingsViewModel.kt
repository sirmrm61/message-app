package com.sirmrm.messageapp.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sirmrm.messageapp.domain.model.AppLanguage
import com.sirmrm.messageapp.domain.model.AppSettings
import com.sirmrm.messageapp.domain.model.CalendarMode
import com.sirmrm.messageapp.domain.model.DigitStyle
import com.sirmrm.messageapp.domain.model.ThemeMode
import com.sirmrm.messageapp.data.db.CustomBankPatternDao
import com.sirmrm.messageapp.data.db.CustomBankPatternEntity
import com.sirmrm.messageapp.domain.repository.SenderRuleRepository
import com.sirmrm.messageapp.domain.repository.SettingsRepository
import com.sirmrm.messageapp.domain.repository.SenderRuleRow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val customBankPatternDao: CustomBankPatternDao,
    private val senderRuleRepository: SenderRuleRepository,
    private val keyContactDao: com.sirmrm.messageapp.data.db.KeyContactDao,
) : ViewModel() {

    /** User-defined bank sender patterns (added in Settings, used at classify time). */
    val bankPatterns: StateFlow<List<CustomBankPatternEntity>> =
        customBankPatternDao.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addBankPattern(pattern: String) = viewModelScope.launch {
        customBankPatternDao.insert(CustomBankPatternEntity(pattern = pattern, createdAt = System.currentTimeMillis()))
    }
    fun removeBankPattern(id: Long) = viewModelScope.launch {
        customBankPatternDao.delete(id)
    }

    /** Confirmed/rejected transactional + promo rules for the Settings page. */
    val senderRules: StateFlow<List<SenderRuleRow>> =
        senderRuleRepository.observeRules()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setTxRule(senderKey: String, decision: String?) = viewModelScope.launch {
        senderRuleRepository.setTxRule(senderKey, decision)
    }
    fun removeRule(senderKey: String) = viewModelScope.launch {
        senderRuleRepository.setTxRule(senderKey, null)
    }

    val settings: StateFlow<AppSettings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setLanguage(language: AppLanguage) = update { it.copy(language = language) }
    fun setTheme(theme: ThemeMode) = update { it.copy(theme = theme) }
    fun setDigits(digits: DigitStyle) = update { it.copy(digits = digits) }
    fun setCalendar(calendar: CalendarMode) = update { it.copy(calendar = calendar) }
    fun setAutoDelete(enabled: Boolean) = update { it.copy(autoDeleteEnabled = enabled) }
    fun setAutoDeleteDays(days: Int) = update { it.copy(autoDeleteDays = days) }
    fun setBiometric(enabled: Boolean) = update { it.copy(biometricLock = enabled) }
    fun setFlagSecure(enabled: Boolean) = update { it.copy(flagSecure = enabled) }
    fun setSwipeRightArchives(v: Boolean) = update { it.copy(swipeRightArchives = v) }
    fun setHideOtpContent(v: Boolean) = update { it.copy(hideOtpContent = v) }
    fun setAlertSound(i: Int) = update { it.copy(alertSoundIndex = i) }
    // --- read-aloud ---
    fun setTtsEnabled(v: Boolean) = update { it.copy(ttsEnabled = v) }
    fun setTtsVoice(id: String) = update { it.copy(ttsVoiceId = id) }
    fun setTtsSpeed(v: Float) = update { it.copy(ttsSpeed = v) }
    fun setTtsVolume(v: Float) = update { it.copy(ttsVolume = v) }
    fun setTtsHpOnly(v: Boolean) = update { it.copy(ttsAutoReadHeadphoneOnly = v) }
    fun setTtsScreenOnly(v: Boolean) = update { it.copy(ttsAutoReadScreenOnOnly = v) }
    fun setTtsAutoOtp(v: Boolean) = update { it.copy(ttsAutoReadOtp = v) }
    fun setTtsAutoTx(v: Boolean) = update { it.copy(ttsAutoReadTransactional = v) }

    /** Key contacts — their new messages may auto-read. */
    val keyContacts: StateFlow<List<com.sirmrm.messageapp.data.db.KeyContactEntity>> =
        keyContactDao.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addKeyContact(senderKey: String, displayName: String) = viewModelScope.launch {
        val key = com.sirmrm.messageapp.classifier.SenderKey.of(senderKey)
            .ifBlank { senderKey.trim() }
        keyContactDao.upsert(
            com.sirmrm.messageapp.data.db.KeyContactEntity(
                senderKey = key, displayName = displayName,
                autoRead = true, voiceId = null,
                addedAt = System.currentTimeMillis(),
            )
        )
    }
    fun removeKeyContact(key: String) = viewModelScope.launch { keyContactDao.delete(key) }
    fun setKeyAutoRead(key: String, autoRead: Boolean) = viewModelScope.launch {
        keyContactDao.get(key)?.copy(autoRead = autoRead)?.let { keyContactDao.upsert(it) }
    }
    fun setKeyVoice(key: String, voiceId: String?) = viewModelScope.launch {
        keyContactDao.get(key)?.copy(voiceId = voiceId)?.let { keyContactDao.upsert(it) }
    }

    fun addSenderPattern(pattern: String) = update {
        it.copy(serviceSenderPatterns = (it.serviceSenderPatterns + pattern).distinct())
    }
    fun removeSenderPattern(pattern: String) = update {
        it.copy(serviceSenderPatterns = it.serviceSenderPatterns - pattern)
    }
    fun blockSender(address: String) = update {
        it.copy(blockedSenders = it.blockedSenders + address)
    }
    fun unblockSender(address: String) = update {
        it.copy(blockedSenders = it.blockedSenders - address)
    }

    private fun update(transform: (AppSettings) -> AppSettings) =
        viewModelScope.launch { settingsRepository.update(transform) }
}
