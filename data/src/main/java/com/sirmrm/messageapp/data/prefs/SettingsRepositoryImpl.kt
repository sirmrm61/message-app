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
        val HIDE_OTP = booleanPreferencesKey("hide_otp_content")
        val ALERT_SOUND = intPreferencesKey("alert_sound_index")
        val TTS_ENABLED = booleanPreferencesKey("tts_enabled")
        val TTS_VOICE = stringPreferencesKey("tts_voice_id")
        val TTS_SPEED = floatPreferencesKey("tts_speed")
        val TTS_VOLUME = floatPreferencesKey("tts_volume")
        val TTS_HP_ONLY = booleanPreferencesKey("tts_hp_only")
        val TTS_SCREEN_ONLY = booleanPreferencesKey("tts_screen_only")
        val TTS_AUTO_OTP = booleanPreferencesKey("tts_auto_otp")
        val TTS_AUTO_TX = booleanPreferencesKey("tts_auto_tx")
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
            hideOtpContent = p[Keys.HIDE_OTP] ?: true,
            alertSoundIndex = p[Keys.ALERT_SOUND] ?: 0,
            ttsEnabled = p[Keys.TTS_ENABLED] ?: false,
            ttsVoiceId = p[Keys.TTS_VOICE] ?: "voice1",
            ttsSpeed = p[Keys.TTS_SPEED] ?: 1.0f,
            ttsVolume = p[Keys.TTS_VOLUME] ?: 1.0f,
            ttsAutoReadHeadphoneOnly = p[Keys.TTS_HP_ONLY] ?: false,
            ttsAutoReadScreenOnOnly = p[Keys.TTS_SCREEN_ONLY] ?: false,
            ttsAutoReadOtp = p[Keys.TTS_AUTO_OTP] ?: false,
            ttsAutoReadTransactional = p[Keys.TTS_AUTO_TX] ?: false,
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
            p[Keys.HIDE_OTP] = s.hideOtpContent
            p[Keys.ALERT_SOUND] = s.alertSoundIndex
            p[Keys.TTS_ENABLED] = s.ttsEnabled
            p[Keys.TTS_VOICE] = s.ttsVoiceId
            p[Keys.TTS_SPEED] = s.ttsSpeed
            p[Keys.TTS_VOLUME] = s.ttsVolume
            p[Keys.TTS_HP_ONLY] = s.ttsAutoReadHeadphoneOnly
            p[Keys.TTS_SCREEN_ONLY] = s.ttsAutoReadScreenOnOnly
            p[Keys.TTS_AUTO_OTP] = s.ttsAutoReadOtp
            p[Keys.TTS_AUTO_TX] = s.ttsAutoReadTransactional
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
