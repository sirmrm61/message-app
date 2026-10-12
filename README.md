# پیامک هوشمند — Smart SMS

پیامک‌رسان کامل اندروید (Kotlin + Jetpack Compose) که با هوش مصنوعی **روی دستگاه**
پیامک‌های تبلیغاتی را تشخیص می‌دهد و به پوشه‌ی «تبلیغات» منتقل می‌کند — بدون هیچ
دسترسی اینترنتی و بدون خروج هیچ داده‌ای از گوشی.

An Android SMS replacement app (Kotlin + Jetpack Compose) whose **on-device AI**
detects promotional messages and files them into a Promotions folder — no
`INTERNET` permission, nothing ever leaves the phone.

## ویژگی‌ها / Features

- جایگزین کامل پیامک پیش‌فرض: دریافت، ارسال، چندبخشی، گزارش تحویل، دوسیم‌کارت
  (Full SMS replacement: receive/send, multipart, delivery reports, dual SIM)
- تشخیص تبلیغات دو لایه: قواعد قطعی + Naive Bayes قابل‌آموزش روی دستگاه
  (Two-layer classifier: deterministic rules + incremental on-device Naive Bayes)
- OTP/تراکنشی هرگز جابه‌جا یا حذف نمی‌شوند — خطای «OTP اشتباهاً تبلیغ» = صفر
  (OTP/transactional messages are protected — zero OTP-false-positive target)
- توضیح‌پذیری: دلیل هر تشخیص روی پیام نمایش داده می‌شود
  (Explainable: every verdict shows its reasons on the message)
- فارسی/انگلیسی، RTL کامل، فونت وزیرمتن، تقویم شمسی/میلادی، ارقام فارسی/لاتین
  (Persian/English, full RTL, Vazirmatn font, Jalali/Gregorian calendar)
- سطل بازیافت ۳۰ روزه + Undo برای هر انتقال؛ حذف خودکار فقط با فعال‌سازی کاربر
  (30-day recycle bin + Undo on every move; auto-delete is opt-in, off by default)
- پشتیبان‌گیری XML سازگار با SMS Backup & Restore
  (XML backup compatible with SMS Backup & Restore)

## معماری / Architecture

```
app/                   Application, manifest, receivers, services, notifications, workers
core/                  Design system (M3 + Vazirmatn), Persian digit/Jalali utils
domain/                Pure-Kotlin models + repository interfaces
data/                  Room, SMS provider, sender, backup, classifier persistence
classifier/            Pure-Kotlin engine: rules + NaiveBayes (+200-sample tests)
feature-conversations/ Inbox, folder tabs, swipe, multi-select, undo
feature-chat/          Thread view, composer, SIM picker, promo feedback
feature-promotions/    Smart cleanup center + sensitivity slider
feature-settings/      Language/theme/digits/auto-delete/sender prefixes/blocklist
```

Details: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)

## ساخت / Build

```bash
# Android Studio (Hedgehog+) or:
./gradlew assembleDebug

# classifier unit tests (pure JVM, ~215 samples, reports precision/recall):
./gradlew :classifier:test
```

Requirements: JDK 17, Android SDK 35, Gradle 8.9 (wrapper included).
Verified: `:app:assembleDebug` ✅ — `:classifier:test` ✅ precision 1.0 / recall 1.0, OTP error 0.

## حریم خصوصی / Privacy

No `INTERNET` permission is declared. All classification is on-device; the
Naive Bayes model persists only in the local Room database. Google Play's
SMS/Call Log policy requires this app to function as the default SMS
handler — see docs/ARCHITECTURE.md.
