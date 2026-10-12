package com.sirmrm.messageapp.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sirmrm.messageapp.domain.model.AppLanguage
import com.sirmrm.messageapp.domain.model.AppSettings
import com.sirmrm.messageapp.domain.model.CalendarMode
import com.sirmrm.messageapp.domain.model.DigitStyle
import com.sirmrm.messageapp.domain.model.ThemeMode
import com.sirmrm.messageapp.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : SettingsRepository {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val THEME = stringPreferencesKey("theme")
        val DIGITS = stringPreferencesKey("digits")
        val CALENDAR = stringPreferencesKey("calendar")
        val SENSITIVITY = floatPreferencesKey("sensitivity")
        val AUTO_DELETE = booleanPreferencesKey("auto_delete")
        val AUTO_DELETE_DAYS = intPreferencesKey("auto_delete_days")
        val TRASH_DAYS = intPreferencesKey("trash_days")
        val SENDER_PATTERNS = stringSetPreferencesKey("sender_patterns")
        val BLOCKED = stringSetPreferencesKey("blocked_senders")
        val ONBOARDING = booleanPreferencesKey("onboarding_done")
        val BIOMETRIC = booleanPreferencesKey("biometric_lock")
        val FLAG_SECURE = booleanPreferencesKey("flag_secure")
        val SWIPE_RIGHT_ARCHIVES = booleanPreferencesKey("swipe_right_archives")
        val PROMOS_CLEANED = intPreferencesKey("promos_cleaned_week")
    }

    override val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            language = p[Keys.LANGUAGE]?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() }
                ?: AppLanguage.FA,
            theme = p[Keys.THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            digits = p[Keys.DIGITS]?.let { runCatching { DigitStyle.valueOf(it) }.getOrNull() }
                ?: DigitStyle.PERSIAN,
            calendar = p[Keys.CALENDAR]?.let { runCatching { CalendarMode.valueOf(it) }.getOrNull() }
                ?: CalendarMode.JALALI,
            sensitivity = p[Keys.SENSITIVITY] ?: 0.5f,
            autoDeleteEnabled = p[Keys.AUTO_DELETE] ?: false,
            autoDeleteDays = p[Keys.AUTO_DELETE_DAYS] ?: 30,
            trashRetentionDays = p[Keys.TRASH_DAYS] ?: 30,
            serviceSenderPatterns = p[Keys.SENDER_PATTERNS]?.toList() ?: emptyList(),
            blockedSenders = p[Keys.BLOCKED] ?: emptySet(),
            onboardingDone = p[Keys.ONBOARDING] ?: false,
            biometricLock = p[Keys.BIOMETRIC] ?: false,
            flagSecure = p[Keys.FLAG_SECURE] ?: false,
            swipeRightArchives = p[Keys.SWIPE_RIGHT_ARCHIVES] ?: true,
            promosCleanedThisWeek = p[Keys.PROMOS_CLEANED] ?: 0,
        )
    }

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.dataStore.edit { p ->
            val current = snapshot(p)
            val s = transform(current)
            p[Keys.LANGUAGE] = s.language.name
            p[Keys.THEME] = s.theme.name
            p[Keys.DIGITS] = s.digits.name
            p[Keys.CALENDAR] = s.calendar.name
            p[Keys.SENSITIVITY] = s.sensitivity
            p[Keys.AUTO_DELETE] = s.autoDeleteEnabled
            p[Keys.AUTO_DELETE_DAYS] = s.autoDeleteDays
            p[Keys.TRASH_DAYS] = s.trashRetentionDays
            p[Keys.SENDER_PATTERNS] = s.serviceSenderPatterns.toSet()
            p[Keys.BLOCKED] = s.blockedSenders
            p[Keys.ONBOARDING] = s.onboardingDone
            p[Keys.BIOMETRIC] = s.biometricLock
            p[Keys.FLAG_SECURE] = s.flagSecure
            p[Keys.SWIPE_RIGHT_ARCHIVES] = s.swipeRightArchives
            p[Keys.PROMOS_CLEANED] = s.promosCleanedThisWeek
        }
    }

    private fun snapshot(p: androidx.datastore.preferences.core.Preferences) = AppSettings(
        language = p[Keys.LANGUAGE]?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() } ?: AppLanguage.FA,
        theme = p[Keys.THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
        digits = p[Keys.DIGITS]?.let { runCatching { DigitStyle.valueOf(it) }.getOrNull() } ?: DigitStyle.PERSIAN,
        calendar = p[Keys.CALENDAR]?.let { runCatching { CalendarMode.valueOf(it) }.getOrNull() } ?: CalendarMode.JALALI,
        sensitivity = p[Keys.SENSITIVITY] ?: 0.5f,
        autoDeleteEnabled = p[Keys.AUTO_DELETE] ?: false,
        autoDeleteDays = p[Keys.AUTO_DELETE_DAYS] ?: 30,
        trashRetentionDays = p[Keys.TRASH_DAYS] ?: 30,
        serviceSenderPatterns = p[Keys.SENDER_PATTERNS]?.toList() ?: emptyList(),
        blockedSenders = p[Keys.BLOCKED] ?: emptySet(),
        onboardingDone = p[Keys.ONBOARDING] ?: false,
        biometricLock = p[Keys.BIOMETRIC] ?: false,
        flagSecure = p[Keys.FLAG_SECURE] ?: false,
        swipeRightArchives = p[Keys.SWIPE_RIGHT_ARCHIVES] ?: true,
        promosCleanedThisWeek = p[Keys.PROMOS_CLEANED] ?: 0,
    )
}
