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
        val trxBodies = listOf(
            "برداشت: 1,250,000 ریال\nکارت 6037**1234\nمانده: 8,900,000 ریال",
            "واریز وجه 4500000 ریال به حساب شما انجام شد",
            "تراکنش کارت به کارت موفق. کد پیگیری: 55443322",
            "تراکنش خرید: 350,000 ریال - فروشگاه رفاه",
            "کد رهگیری مرسوله شما: 123456789012345678901234",
            "مرسوله پستی شما تحویل داده شد",
            "سفارش شما ارسال شد. کد پیگیری: 99876",
            "قبض برق شما صادر شد. مبلغ: 120,000 تومان",
            "پرداخت قبض 55000 تومان موفق بود",
            "نوبت شما در درمانگاه سلامت روز سه‌شنبه ساعت 10",
            "ویزیت دکتر احمدی فردا ساعت ۱۸ تایید شد",
            "حواله ساتنا به مبلغ ۲٬۰۰۰٬۰۰۰ ریال ثبت شد",
            "Your payment of $19.99 was received",
            "Your order #4471 has been shipped",
            "Transaction of 500000 IRR approved. Balance: 2400000",
            "صورت حساب دوره‌ای شما آماده شد",
            "بدهی قبض اینترنت شما 180,000 تومان است",
            "خرید 89000 ریال - شتاب",
            "مانده حساب: 4,500,000 ریال",
            "آزمایش شما آماده تحویل است",
        )
        val trxSenders = listOf("BankMelli", "982000", "PostIran", "10003355", "Digikala")
        for ((i, b) in trxBodies.withIndex()) {
            out += Sample(b, trxSenders[i % trxSenders.size], SmsCategory.TRANSACTIONAL)
        }
        for ((i, b) in trxBodies.withIndex()) {
            out += Sample(b, "2000" + i, SmsCategory.TRANSACTIONAL)
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
        var k = 0
        while (out.size < 60 + 15 + 20 + 20 + 90) { // reach ~205
            val t = promoTemplates[k % promoTemplates.size]
            out += Sample(t, promoSenders[k % promoSenders.size], SmsCategory.PROMO)
            k++
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
        )
        for ((i, b) in personalBodies.withIndex()) {
            out += Sample(b, "+98912" + (1000000 + i), SmsCategory.PERSONAL, isContact = i % 2 == 0, hasConversation = i % 2 == 1)
        }

        return out
    }
}
