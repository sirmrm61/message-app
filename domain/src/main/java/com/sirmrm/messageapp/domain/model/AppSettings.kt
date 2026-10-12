package com.sirmrm.messageapp.domain.model

/** User-facing theme option. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** App language — fa is the default. */
enum class AppLanguage(val tag: String) { FA("fa"), EN("en") }

/** Digit style for dates/numbers in the UI. */
enum class DigitStyle { PERSIAN, LATIN }

/** Calendar used to display dates. */
enum class CalendarMode { JALALI, GREGORIAN }

data class AppSettings(
    val language: AppLanguage = AppLanguage.FA,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val digits: DigitStyle = DigitStyle.PERSIAN,
    val calendar: CalendarMode = CalendarMode.JALALI,
    /** 0..1 multiplier applied to classifier thresholds (sensitivity slider). */
    val sensitivity: Float = 0.5f,
    /** Auto-delete old promos — OFF by default, requires explicit opt-in. */
    val autoDeleteEnabled: Boolean = false,
    /** Promos older than this many days are moved to trash by the worker. */
    val autoDeleteDays: Int = 30,
    /** Days an item stays in trash before permanent deletion. */
    val trashRetentionDays: Int = 30,
    /** Editable service-sender prefix patterns (regex fragments). */
    val serviceSenderPatterns: List<String> = emptyList(),
    val blockedSenders: Set<String> = emptySet(),
    val onboardingDone: Boolean = false,
    val biometricLock: Boolean = false,
    val flagSecure: Boolean = false,
    /** Swipe actions: true = swipe-right archives / swipe-left deletes. */
    val swipeRightArchives: Boolean = true,
    /** Weekly cleaned-promo counter shown on the home screen. */
    val promosCleanedThisWeek: Int = 0,
)
