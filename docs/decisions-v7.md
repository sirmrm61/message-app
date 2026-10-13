# تصمیم‌های فنی — نسخه‌ی ۷ (قواعد فرستنده، اعلان‌ها، TTS)

## ۱. تأیید/رد فرستنده تراکنشی
- `sender_rules.decision` ∈ {ALWAYS_TX, NEVER_TX} + `createdAt` (DB v4 → migration 3→4).
- کلید فرستنده: `SenderKey.of()` — ارقام فارسی/عربی→لاتین، +98 E.164 (0098، 98×12، 0 ابتدایی، 9xx×10، فاصله/خط‌تیره)، حروف→lowercase alnum، سرشماره‌های کوتاه بدون تغییر.
- اولویت قاعده‌ی کاربر بالاتر از مدل و لیست بانک‌هاست (در RuleEngine قبل از ارزیابی‌ها). NEVER_TX فقط تب تراکنشی را سرکوب می‌کند — تبلیغات جدا ارزیابی می‌شود.
- هر انتخاب به nb_tokens تغذیه می‌شود (آموزش افزایشی).

## ۲. هشدار پیام جدید
- ترتیب در SMS_DELIVER: persist → classify → rules → notify.
- Foreground: broadcast ACTION_INCOMING_SMS → Snackbar+صدا (بدون SYSTEM_ALERT_WINDOW). Background: heads-up روی کانال مربوطه.
- کانال‌ها: msgs_personal / msgs_tx / msgs_otp (IMPORTANCE_HIGH) و msgs_promo (IMPORTANCE_LOW، بی‌صدا).
- صدای هشدار: ۳ صدای داخلی (notification/alarm/ringtone) قابل انتخاب در تنظیمات. DND/ringerMode در playAlert چک می‌شود — دور زده نمی‌شود.

## ۳. اعلان‌ها
- MessagingStyle + گروه به‌ازای هر فرستنده + اعلان خلاصه (GROUP_SUMMARY_BASE=900000).
- اکشن‌ها: خوانده‌شد (READ+SEEN در Telephony و Room + cancel + badge)، پاسخ سریع (RemoteInput، PendingIntent MUTABLE)، کپی رمز (فقط OTP)، خواندن (فقط وقتی ttsEnabled).
- OTP با hideOtpContent=true → VISIBILITY_PRIVATE. tap → مکالمه.

## ۴. TTS فارسی آفلاین (sherpa-onnx)
- ماژول `:tts` جدا — feature‌ها بدون دسترسی به app از آن استفاده می‌کنند.
- مدل‌ها: ۳ صدای Piper VITS فارسی int8 (~۱۸MB هرکدام) در `assets/tts/voice{1,2,3}` + voices.json با SHA-256 + espeak-ng-data (فقط fa/en dict برای کم کردن حجم).
- Lazy-load: مدل فقط هنگام نیاز لود و بعد از idle آزاد می‌شود. پیش‌پردازش: ارقام→حروف، مبالغ، لینک→«یک پیوند»، ی/ک+ZWNJ، نقطه‌ی پایانی+سکوت، تکه‌کردن جمله‌های بلند.
- پخش AudioTrack روی thread جدا + audio focus + توقف با تماس.
- سرویس mediaPlayback FGS با catch برای ForegroundServiceStartNotAllowedException → fallback به اعلان با اکشن «خواندن». صف ≤۳ پیام.
- مخاطبین کلیدی: key_contacts + AutoReadDecider — OTP/تراکنشی هرگز خودکار خوانده نمی‌شود مگر opt-in جداگانه؛ گزینه‌های «فقط هدفون» و «فقط صفحه‌روشن».

## ۵. خواندن خودکار به تفکیک نسخه‌ی اندروید
| نسخه | رفتار | جایگزین |
|---|---|---|
| ۱۱ (API 30) | شروع FGS از گیرنده‌ی پس‌زمینه معمولاً مجاز | — |
| ۱۲/۱۳ (API 31-33) | محدودیت FGS — catch استثنا | اعلان با اکشن «خواندن» |
| ۱۴ (API 34) | نیازمند FOREGROUND_SERVICE_MEDIA_PLAYBACK + type در مانیفست (اعلام‌شده) | همان fallback |
| ۱۵ (API 35) | مثل ۱۴؛ boot-time FGS ممنوع | همان fallback |
| تست‌شده روی | emulator API 35 | — |

## ۶. محدودیت‌ها
- صداها فارسی معیارند؛ **لهجه‌ی منطقه‌ای واقعی پشتیبانی نمی‌شود** (در UI هم ادعا نشده).
- مدل gyro تا تأیید مجوز همراه برنامه نیست.
- تولید گفتار روی گوشی میان‌رده ~۱-۲ ثانیه برای پیام کوتاه (int8، ۲ thread).
- INTERNET permission وجود ندارد — همه‌چیز آفلاین.
- POST_NOTIFICATIONS ۱۳+: درخواست در آنبوردینگ + در صورت رد، جایگزین هشدار درون‌برنامه‌ای.
