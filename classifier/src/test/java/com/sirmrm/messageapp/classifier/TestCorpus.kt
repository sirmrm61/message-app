package com.sirmrm.messageapp.classifier

/**
 * Synthetic but realistic-looking corpus of ≥200 Persian + English samples.
 * Each sample carries the expected category so precision / recall can be
 * measured, plus the hard invariant: no OTP may ever be auto-moved.
 */
object TestCorpus {

    data class Sample(
        val body: String,
        val sender: String,
        val expected: SmsCategory,
        val isContact: Boolean = false,
        val hasConversation: Boolean = false,
        /** Expected to be flagged suspicious (phishing-imitating). */
        val expectSuspicious: Boolean = false,
        /** Sender is a learned (auto-detected) bank sender. */
        val learnedBankSender: Boolean = false,
    )

    fun samples(): List<Sample> {
        val out = ArrayList<Sample>(256)

        // ---------- OTP (protected) ----------
        val otpBodies = listOf(
            "رمز پویا: 458213\nبانک ملت",
            "کد تایید شما: 99120",
            "کد تأیید ورود به سامانه: 334455",
            "رمز یکبارمصرف شما 778899 می‌باشد",
            "کد فعال سازی: 1234",
            "Your verification code is 482910",
            "Your OTP is 556677. Do not share it.",
            "Code: 9021",
            "کد: 88912",
            "رمز پویا ۶۵۴۳۲۱",
            "Your one-time code is 112233",
            "کد ورود به اپلیکیشن: 4455",
            "Confirmation code: 7878",
            "رمز تأیید تراکنش 99887",
            "رمز دوم پویا: 221133",
        )
        val otpSenders = listOf("BankMelat", "983000", "10002170", "982000", "+989123456789")
        for ((i, b) in otpBodies.withIndex()) {
            out += Sample(b, otpSenders[i % otpSenders.size], SmsCategory.OTP)
        }
        // OTP variants x3 with different senders → 45 OTP samples
        for ((i, b) in otpBodies.withIndex()) {
            out += Sample(b, "3000" + (100 + i), SmsCategory.OTP)
            out += Sample(b, "98" + (1000 + i), SmsCategory.OTP)
        }

        // ---------- TRANSACTIONAL (protected) ----------
        // Bank transaction SMS only — postal / billing / appointment
        // messages are NOT transactional and live in the inbox flow.
        val trxBodies = listOf(
            "برداشت: 1,250,000 ریال\nکارت 6037**1234\nمانده: 8,900,000 ریال",
            "واریز وجه 4500000 ریال به حساب شما انجام شد",
            "تراکنش کارت به کارت موفق. کد پیگیری: 55443322",
            "تراکنش خرید: 350,000 ریال - فروشگاه رفاه",
            "بانک ملت: واریز مبلغ 25,000,000 ریال به حساب 6104** موجودی: 80,500,000 ریال",
            "حساب: 5022-1234-5678 واریز 1000000 ریال",
            "شماره کارت 6037-9975-1234-5678 برداشت 250000 تومان",
            "شبا: IR062960000000100324200001 واریز شد",
            "حواله پایا به مبلغ 3,000,000 ریال صادر شد",
            "حواله ساتنا به مبلغ ۲٬۰۰۰٬۰۰۰ ریال ثبت شد",
            "چک صیادی شما وصول شد. مبلغ 5,000,000 ریال",
            "مانده حساب شما: 12,400,000 ریال",
            "Transaction of 500000 IRR approved. Balance: 2400000",
            "Debit: 750000 IRR. Account balance: 1000000",
            "خرید موفق: 89000 ریال - شتاب",
            "مانده حساب: 4,500,000 ریال",
            "بانک ملی: واریز +12,300,000 ریال 1404/07/12 14:22 مانده: 56,000,000 ریال",
            "بانک صادرات: برداشت -800,000 ریال کارت 6273** مانده 9,100,000 ریال",
            "بانک تجارت: انتقال وجه 2,500,000 ریال موفق. شماره پیگیری 77881122",
            "بانک سپه: خرید اینترنتی 950,000 ریال موفق. مانده 3,200,000",
            "بانک مسکن: قسط وام شما 4,000,000 ریال برداشت شد. مانده 12,000,000",
            "بانک کشاورزی: واریز تسهیلات 30,000,000 ریال به حساب شما",
            "بانک پارسیان: برداشت خودپرداز 500,000 تومان مانده 2,300,000 تومان",
            "پاسارگاد: انتقال کارت به کارت 7,700,000 ریال. سریال 445566",
            "بانک سامان: واریز سود سهام 1,150,000 ریال",
            "اقتصاد نوین: ورود به اینترنت بانک شما ثبت شد. اگر شما نبودید تماس بگیرید",
            "پست بانک: چک شماره 4412 برگشت شد. مبلغ 6,000,000 ریال",
            "بانک رفاه: کارمزد انتقال 12,000 ریال برداشت شد",
            "بانک آینده: مانده حساب صبحگاهی 8,450,000 ریال",
            "بانک شهر: برداشت چک مبالغ بالا 90,000,000 ریال. مانده 5,000,000",
            "بانک سینا: تغییر رمز اینترنت بانک شما انجام شد",
            "بانک دی: حواله اینترنتی 3,300,000 ریال صادر شد",
            "ایران زمین: سررسید قسط تسهیلات شما نزدیک است. مانده قسط 2,100,000",
            "بانک انصار: واریز حقوق 28,000,000 ریال",
            "قوامین: مسدودی حساب شما برداشته شد. موجودی 1,500,000 ریال",
            "مهر ایران: واریز جایزه قرعه کشی 5,500,000 ریال به حساب شما",
            "بانک گردشگری: خرید 670,000 ریال پذیرنده 88771",
            "توسعه تعاون: کارمزد 9,000 ریال. مانده 4,400,000",
            "بانک حکمت: انتقال 1,900,000 ریال. کد رهگیری 992211",
            "رسالت: واریز 3,750,000 ریال. مانده 9,990,000 ریال",
            "خاورمیانه: برداشت POS 240,000 ریال فروشگاه 3341",
            "بانک سرمایه: واریز سود 820,000 ریال به سپرده شما",
            "توسعه: گردش حساب: +4,400,000 ریال مانده 7,700,000",
            "کارآفرین: برداشت 1,100,000 ریال. شماره مرجع 665544",
            "ملل: واریز 2,200,000 ریال. مانده حساب 6,600,000",
            "پایا: انتقال 13,000,000 ریال به شما. شبا IR123456",
            "ساتنا: واریز 9,800,000 ریال. پیگیری 10101010",
            "خرید کارت شتابی 145,000 ریال. مانده کارت 2,900,000 ریال",
            "واریز مبلغ ۵٬۰۰۰٬۰۰۰ ریال به حساب 5022**. مانده ۲۱٬۰۰۰٬۰۰۰ ریال",
            "برداشت: ۱۲٬۵۰۰٬۰۰۰ ریال کارت ۶۱۰۴**. مانده: ۷٬۴۰۰٬۰۰۰",
            "خرید موفق 320 هزار تومان - کارت 6037**. مانده 1,900,000 تومان",
            "سود تسهیلات 640,000 ریال برداشت شد. مانده 11,000,000",
            "رمز پویا بانک ملت: 334455. به کسی ندهید",
            "رمز یکبار مصرف خرید: 778899. فروشگاه دیجی کالا. به کسی ندهید",
            "کد تایید اینترنت بانک: 556677. آن را افشا نکنید",
            "فعال سازی همراه بانک: کد 112233",
            "چک صیادی به شماره سریال 909090 وصول شد",
            "قبض 55660 با مبلغ 88,000 ریال پرداخت شد. مانده 1,000,000",
            "شارژ سیم کارت 20,000 تومان خریداری شد. مانده حساب 500,000 تومان",
            "پل: انتقال 6,600,000 ریال از کارت 5022** به 6037** موفق بود",
            "ورود به اینترنت بانک در 1404/07/15 09:41 ثبت شد",
        )
        // Non-bank informational texts: postal, billing, appointments.
        // These must NOT be transactional anymore.
        val nonBankBodies = listOf(
            "کد رهگیری مرسوله شما: 123456789012345678901234",
            "مرسوله پستی شما تحویل داده شد",
            "سفارش شما ارسال شد. کد پیگیری: 99876",
            "قبض برق شما صادر شد. مبلغ: 120,000 تومان",
            "پرداخت قبض 55000 تومان موفق بود",
            "Your payment of $19.99 was received",
            "Your order #4471 has been shipped",
            "بدهی قبض اینترنت شما 180,000 تومان است",
            "نوبت شما در درمانگاه سلامت روز سه‌شنبه ساعت 10",
            "ویزیت دکتر احمدی فردا ساعت ۱۸ تایید شد",
            "صورت حساب دوره‌ای شما آماده شد",
            "آزمایش شما آماده تحویل است",
        )
        val nonBankSenders = listOf("PostIran", "10003355", "Digikala", "20009988")
        for ((i, b) in nonBankBodies.withIndex()) {
            out += Sample(b, nonBankSenders[i % nonBankSenders.size], SmsCategory.PERSONAL)
        }
        val bankSenders = listOf(
            "30003305", // ملت
            "30002957", // آینده
            "30006905", // ایران زمین
            "30006767", // سامان
            "BankMelli", "BankSaderat", "BankTejarat", "BankSepah",
            "BankMaskan", "BankKeshavarzi", "BankParsian", "Pasargad",
            "PostBank", "BankRefah", "BankShahr", "BankSina",
        )
        // ≥150 bank samples across ≥15 banks.
        for ((i, b) in trxBodies.withIndex()) {
            out += Sample(b, bankSenders[i % bankSenders.size], SmsCategory.TRANSACTIONAL)
            out += Sample(b, bankSenders[(i + 7) % bankSenders.size], SmsCategory.TRANSACTIONAL)
        }
        // Bank bodies arriving on learned (auto-detected) senders.
        for ((i, b) in trxBodies.withIndex()) {
            out += Sample(b, "learned-" + (i % 5), SmsCategory.TRANSACTIONAL, learnedBankSender = true)
        }

        // ---------- PROMO ----------
        val promoTemplates = listOf(
            "جشنواره فروش ویژه! تا ۵۰٪ تخفیف روی همه کالاها. لغو ۱۱",
            "فروش ویژه آخر هفته! خرید کنید: https://bit.ly/abc لغو۱۱",
            "کد تخفیف ۲۰٪: OFF20 همین حالا خرید کنید",
            "فرصت محدود! ثبت نام کنید و جایزه بگیرید. لغو ۱۱",
            "قرعه‌کشی بزرگ! همین حالا شرکت کنید. lilnk.ir/xyz",
            "شارژ رایگان بگیر! فقط امروز. لغو ۱۱",
            "پیشنهاد ویژه: ارسال رایگان برای خرید بالای 500 هزار تومان",
            "تخفیف شگفت انگیز تا ۷۰ درصد! opizo.com/sale",
            "برای عضویت در باشگاه مشتریان عدد 1 را ارسال کنید. لغو۱۱",
            "30% تخفیف خرید آنلاین، همین حالا اقدام کنید",
            "Special offer! Buy now and get 50% off. Unsub: 11",
            "Limited time discount code: SAVE25. Register now!",
            "بازدید کنید: www.shop.example فروش ویژه",
            "دانلود کنید و هدیه رایگان دریافت کنید. لغو ۱۱",
            "جشنواره تابستانه - تخفیف + ارسال رایگان. b2n.ir/ab",
            "فروش اقساطی بدون بهره، همین حالا اقدام کنید. لغو۱۱",
            "کد تخفیف اختصاصی شما: VIP30 - فرصت محدود",
            "برنده شوید! در قرعه‌کشی بزرگ شرکت کنید. لغو ۱۱",
            "خرید ویژه موبایل با ۴۰٪ تخفیف. کلیک کنید",
            "Win a prize! Click here now: bit.ly/win22",
        )
        val promoSenders = listOf("3000123", "2000456", "5000789", "981011", "1000123", "+989123456789")
        // Bank-issued promos from real bank senders must stay promo unless
        // they carry amount+balance — the negative weights handle this.
        val bankPromoBodies = listOf(
            "بانک ملت: وام ویژه 500 میلیون تومان! همین حالا ثبت نام کنید. لغو ۱۱",
            "قرعه کشی بزرگ بانک ملی! جایزه 1 میلیارد. شرکت کنید. لغو۱۱",
            "بانک سامان: سود ویژه سپرده ۳۰٪! افتتاح حساب کنید. لغو ۱۱",
            "اعتبارسنجی رایگان بانک پاسارگاد - همین حالا امتحان کنید. لغو۱۱",
        )
        for ((i, b) in bankPromoBodies.withIndex()) {
            out += Sample(b, bankSenders[i % bankSenders.size], SmsCategory.PROMO)
        }
        var k = 0
        while (out.count { it.expected == SmsCategory.PROMO } < 75) {
            val t = promoTemplates[k % promoTemplates.size]
            out += Sample(t, promoSenders[k % promoSenders.size], SmsCategory.PROMO)
            k++
        }

        // ---------- PHISHING (bank-imitating, suspicious) ----------
        val phishBodies = listOf(
            "بانک ملت: حساب شما مسدود شد. برای رفع ان فورا کلیک کنید: bit.ly/bank-fix",
            "بانک ملی: اطلاعات شما نیاز به بروزرسانی دارد: cutt.ly/verify22",
            "بانک پاسارگاد: جایزه شما آماده برداشت است: tiny.cc/win88",
            "بانک سامان: کارت شما غیرفعال شد. فعالسازی: b2n.ir/bank",
            "حساب بانکی شما به دلیل تخلف مسدود شد. احراز هویت: short.link/x9",
            "بانک آینده: رمز دوم شما منقضی شد. تمدید: is.gd/renew44",
            "بانک رفاه: بازپرداخت وجه شما در انتظار است: rb.gy/refund",
            "هشدار: ورود مشکوک به اینترنت بانک شما. لغو: bit.ly/sec11",
            "بانک دی: شما برنده قرعه کشی شدید! دریافت جایزه: tiny.cc/prize7",
            "بانک سپه: وام بدون ضامن 2 میلیارد! دریافت: liink.ir/loan",
            "Bank Mellat: your account is locked. Verify: bit.ly/mlt-sec",
            "بانک پارسیان: برای جلوگیری از مسدودی حساب وارد شوید: tiny.cc/pars",
            "کارت به کارت ناموفق! بازگشت وجه: cutt.ly/back99",
            "بانک مسکن: برنده جایزه نقدی شدید. دریافت: bit.ly/maskan1",
            "بانک انصار: حساب شما هک شد! فورا تأیید کنید: tiny.cc/ansar8",
            "رمز شما: 445566. برای تایید وارد شوید: bit.ly/otp-verify",
            "بانک صادرات: تراکنش مشکوک ثبت شد. لغو: short.cm/saderat",
            "بانک کشاورزی: تسهیلات فوری بدون کارمزد! کلیک: is.gd/kesh77",
            "بانک پست: واریز جایزه شما در انتظار است: bit.ly/post-win",
            "بانک شهر: اطلاعات کارت شما نیاز به تایید دارد: tiny.cc/shahr3",
        )
        val phishSenders = listOf("BankMelli", "30003305", "BankMelat", "bank-melli", "10003305")
        for ((i, b) in phishBodies.withIndex()) {
            out += Sample(b, phishSenders[i % phishSenders.size],
                SmsCategory.PERSONAL, expectSuspicious = true)
            out += Sample(b, "98" + (2000 + i),
                SmsCategory.PERSONAL, expectSuspicious = true)
        }

        // ---------- PERSONAL / KEEP ----------
        val personalBodies = listOf(
            "سلام، چطوری؟ فردا قراره بیای خونه",
            "جلسه ساعت ۹ یادت نره",
            "مامان گفت زنگ بزنی",
            "عکس‌ها رو فرستادم، ببین",
            "Are you coming to the party tonight?",
            "Thanks for the help yesterday",
            "میری فوتبال؟",
            "فایل رو برات ایمیل کردم",
            "Happy birthday! Have a great day",
            "غذا رو سفارش دادی؟",
            "واریزش کردم، چک کن حسابت",
            "پولت رو فردا واریز میکنم",
            "واریز انجام شد؟ لطفا بگو",
            "برداشت از کارت اشتباه شد",
            "پول خونه رو واریز کردی؟",
            "قراره پول واریز بشه هفته دیگه",
            "برداشت 50 هزاری دادم دستت",
            "واریز اول هر ماه یادت نره",
            "پول 20,000 تومان شد درست؟",
            "سلام واریز رو چک کردم نبود",
            "دوشنبه واریز میشه نگران نباش",
            "حسابت شمارشو بده واریز کنم",
            "برداشت نقدی ممنوعه راست میگه؟",
            "واریز حقوق کی میاد؟",
            "مانده کارتم رو دیدی؟",
            "واریز دانشجو رسید گفتی؟",
            "امروز واریز دادم رسیدشو فرستادم",
            "سلام. واریز اجاره رو یادت نره",
            "پول وام رو واریز کردم بالاخره",
            "واریز شد ممنون! 500,000",
            "برداشتت چقدر بود؟",
            "واریز به حساب جدید انجام شد?",
            "سلام چرا هنوز واریز نکردی",
            "واریز و برداشت هر روز چک کن",
            "حواله رو واریز کردم داداش",
        )
        for ((i, b) in personalBodies.withIndex()) {
            out += Sample(b, "+98912" + (1000000 + i), SmsCategory.PERSONAL, isContact = i % 2 == 0, hasConversation = i % 2 == 1)
        }

        return out
    }
}
