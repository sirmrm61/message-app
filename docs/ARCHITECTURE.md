# Architecture — پیامک هوشمند / Smart SMS

## Decisions (تصمیم‌ها)

| # | Decision | Rationale |
|---|----------|-----------|
| 1 | `:classifier` and `:domain` are pure Kotlin/JVM modules | Classifier tests run on any JVM with zero Android deps — the OTP-zero-error invariant is CI-verifiable |
| 2 | Two-layer classifier: deterministic rules **before** learned model | Rules give hard guarantees (OTP/transactional protected, whitelist). NB only nudges borderline scores; it can never move a protected message |
| 3 | Room is the app's source of truth; Telephony provider is mirrored | Folders, scores and reasons have no provider columns. Writes to provider happen only while the app holds the SMS role |
| 4 | Every destructive op is reversible | Deletes go to TRASH for 30 days; every move writes a `move_log` row for single-level Undo |
| 5 | No `INTERNET` permission | Privacy requirement; also means no crash reporting/analytics can exist |
| 6 | Vazirmatn variable font in `core/res/font` | One OFL file serves all weights and both scripts |
| 7 | Per-app language via `AppCompatDelegate` + `autoStoreLocales` | Works below API 33 too; `locales_config.xml` declares fa/en |

## Classifier (موتور تشخیص)

`SmsClassifier.classify(SmsInput)` → `ClassificationResult(score 0..1, category, decision, reasons)`

Order of evaluation (`RuleEngine`):
1. User sender rules (`ALWAYS_PROMO` / `NEVER_PROMO`)
2. Whitelist: saved contact OR prior two-way conversation → `PERSONAL`, protected
3. OTP patterns (fa: رمز پویا/یکبارمصرف/کد تأیید…, en: otp/code…) → `OTP`, protected
4. Transactional patterns (bank/postal/billing/appointment, fa+en) → `TRANSACTIONAL`, protected
5. Promo signals with weights: «لغو N» unsubscribe (0.45), جشنواره/فروش ویژه (0.45), تخفیف/CTA/جایزه (0.25), weak signals (0.12), service sender prefixes (0.30, **editable**), short links (0.15), unknown MSISDN + marketing (0.10)

`NaiveBayesLearner` — multinomial NB over word unigrams + char 3/4-grams; incremental `learn()` on user feedback; persisted via `nb_tokens`/`nb_meta` tables. Weighted into the final score only after ≥10 labelled samples.

Decision: `score ≥ autoMove (0.72)` → AUTO_MOVE to Promotions · `≥ suggest (0.35)` → inbox badge "شاید تبلیغ" · protected → never touched. Sensitivity slider shifts both thresholds.

Normalization (`TextNormalizer`): Arabic ی/ك→فارسی, fa+ar digits→ASCII, ZWNJ/ZWJ/tatweel removed, alef/heh variants folded — «ثبت‌نام» == «ثبت نام» == «ثبتنام».

Tests: `classifier/src/test` — 215-sample corpus; asserts zero OTP/transactional false-positives and reports precision/recall (currently 1.0/1.0).

## Default-SMS-role requirements (نقش پیش‌فرض)

- `SmsDeliverReceiver` — `SMS_DELIVER` (persists → classifies → notifies)
- `MmsWapPushReceiver` — `WAP_PUSH_DELIVER` (ack-only stub)
- `RespondViaMessageService` — `RESPOND_VIA_MESSAGE`
- `SendToActivity` — `ACTION_SENDTO`/`VIEW`/`SEND` with sms/smsto/mms/mmsto
- Role request: `RoleManager.ROLE_SMS` (API 29+), `ACTION_CHANGE_DEFAULT` below

## Known limitations (محدودیت‌ها)

- **MMS**: WAP push is acknowledged but MMS download/rendering is not implemented
- **Search** uses `LIKE` (not FTS) — fine for tens of thousands, revisit for 100k+
- **Rcs/chat features**: none (SMS/MMS only)
- Sent/delivered status updates are received but not yet surfaced in the UI
- Google Play SMS policy: this app is ineligible for Play distribution of SMS features unless it is genuinely the default handler; declared usage must match Play Console declarations

## Data flow

```
SMS_DELIVER → SmsDeliverReceiver
  → SmsProvider.insertReceived (system provider, required)
  → ClassifierManager.classify (rules + NB + contacts whitelist)
  → Room upsert (message + conversation, folder = PROMOTIONS if AUTO_MOVE)
  → NotificationHelper (Messaging-style notif + Reply + Copy-code for OTP)

User marks promo/not-promo → ClassifierManager.learn + sender_rules upsert
CleanupWorker (daily) → purge expired trash; if enabled, trash promos > N days
```

## Bank SMS scoring (v3)

`BankTxParser` is a JSON-driven weighted scorer. Patterns live in
`data/src/main/assets/patterns/banking_fa.json` (versioned): 30 Iranian
banks, structure regexes (signed/unsigned amount, balance, masked card,
Jalali datetime, ref/serial, OTP, security-event), vocabulary groups,
phishing signals and weights. Adding a bank = editing the JSON only.
User-defined senders are stored in Room (`custom_bank_patterns`) and the
classifier auto-learns senders with ≥3 structured transactional messages.

Verdict fields persisted per message (DB v3): `txSubtype`,
`txAmountRials`, `txBalanceRials`, `bankName`, `suspicious`,
`maybeTransactional`. Order: sender rules → contacts → phishing → OTP →
transactional → promo → personal. Corpus: 390+ samples; tx recall 1.000,
promo precision 1.000, OTP-as-promo error 0.
