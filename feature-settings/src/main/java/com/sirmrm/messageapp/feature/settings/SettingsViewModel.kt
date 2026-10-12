package com.sirmrm.messageapp.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sirmrm.messageapp.domain.model.AppLanguage
import com.sirmrm.messageapp.domain.model.AppSettings
import com.sirmrm.messageapp.domain.model.CalendarMode
import com.sirmrm.messageapp.domain.model.DigitStyle
import com.sirmrm.messageapp.domain.model.ThemeMode
import com.sirmrm.messageapp.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

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
