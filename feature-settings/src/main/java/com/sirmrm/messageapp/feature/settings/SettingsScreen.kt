package com.sirmrm.messageapp.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sirmrm.messageapp.core.designsystem.AppChevron
import com.sirmrm.messageapp.core.designsystem.AppListGroup
import com.sirmrm.messageapp.core.designsystem.AppListRow
import com.sirmrm.messageapp.core.designsystem.AppSectionHeader
import com.sirmrm.messageapp.core.designsystem.AppSegmentedControl
import com.sirmrm.messageapp.core.designsystem.AppSwitch
import com.sirmrm.messageapp.core.designsystem.Hairline
import com.sirmrm.messageapp.core.designsystem.appColors
import com.sirmrm.messageapp.core.designsystem.pressable
import com.sirmrm.messageapp.domain.model.AppLanguage
import com.sirmrm.messageapp.domain.model.AppSettings
import com.sirmrm.messageapp.domain.model.CalendarMode
import com.sirmrm.messageapp.domain.model.DigitStyle
import com.sirmrm.messageapp.domain.model.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsState()
    val s = settings ?: return

    Scaffold(
        containerColor = appColors.groupedBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = appColors.card),
                title = { Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.primary)
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize()) {

            item { AppSectionHeader(stringResource(R.string.section_appearance)) }
            item {
                AppListGroup {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Text(stringResource(R.string.setting_language), style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        AppSegmentedControl(
                            items = AppLanguage.entries,
                            selected = s.language,
                            onSelect = viewModel::setLanguage,
                            label = { if (it == AppLanguage.FA) "فارسی" else "English" },
                        )
                    }
                    Hairline()
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Text(stringResource(R.string.setting_theme), style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        AppSegmentedControl(
                            items = ThemeMode.entries,
                            selected = s.theme,
                            onSelect = viewModel::setTheme,
                            label = {
                                when (it) {
                                    ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
                                    ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                                    ThemeMode.DARK -> stringResource(R.string.theme_dark)
                                }
                            },
                        )
                    }
                    Hairline()
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Text(stringResource(R.string.setting_digits), style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        AppSegmentedControl(
                            items = DigitStyle.entries,
                            selected = s.digits,
                            onSelect = viewModel::setDigits,
                            label = { if (it == DigitStyle.PERSIAN) "۱۲۳" else "123" },
                        )
                    }
                    Hairline()
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Text(stringResource(R.string.setting_calendar), style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        AppSegmentedControl(
                            items = CalendarMode.entries,
                            selected = s.calendar,
                            onSelect = viewModel::setCalendar,
                            label = {
                                if (it == CalendarMode.JALALI) stringResource(R.string.calendar_jalali)
                                else stringResource(R.string.calendar_gregorian)
                            },
                        )
                    }
                    Hairline()
                    AppListRow(
                        title = stringResource(R.string.setting_swipe),
                        trailing = {
                            AppSwitch(s.swipeRightArchives, viewModel::setSwipeRightArchives)
                        },
                    )
                }
            }

            item { AppSectionHeader(stringResource(R.string.section_classifier)) }
            item {
                AppListGroup {
                    AppListRow(
                        title = stringResource(R.string.setting_auto_delete),
                        subtitle = stringResource(R.string.setting_auto_delete_sub),
                        trailing = {
                            AppSwitch(s.autoDeleteEnabled, viewModel::setAutoDelete)
                        },
                    )
                    if (s.autoDeleteEnabled) {
                        Hairline()
                        Column(Modifier.fillMaxWidth().padding(14.dp)) {
                            Text(
                                stringResource(R.string.setting_auto_delete_days, s.autoDeleteDays),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Slider(
                                value = s.autoDeleteDays.toFloat(),
                                onValueChange = { viewModel.setAutoDeleteDays(it.toInt()) },
                                valueRange = 7f..90f,
                                colors = SliderDefaults.colors(
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = appColors.fill,
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                ),
                            )
                        }
                    }
                }
            }
            item { SenderPatternsEditor(viewModel, s.serviceSenderPatterns) }
            item {
                val bankPatterns = viewModel.bankPatterns.collectAsState().value
                BankPatternsEditor(viewModel, bankPatterns)
            }

            item { AppSectionHeader(stringResource(R.string.section_privacy)) }
            item {
                AppListGroup {
                    AppListRow(
                        title = stringResource(R.string.setting_biometric),
                        trailing = { AppSwitch(s.biometricLock, viewModel::setBiometric) },
                    )
                    Hairline()
                    AppListRow(
                        title = stringResource(R.string.setting_flag_secure),
                        trailing = { AppSwitch(s.flagSecure, viewModel::setFlagSecure) },
                    )
                    Hairline()
                    AppListRow(
                        title = stringResource(R.string.setting_hide_otp),
                        trailing = { AppSwitch(s.hideOtpContent, viewModel::setHideOtpContent) },
                    )
                }
            }
            item { BlockedSendersEditor(viewModel, s.blockedSenders) }

            item { TtsSection(viewModel, s) }
            item {
                val keys by viewModel.keyContacts.collectAsState()
                KeyContactsEditor(viewModel, keys, s.ttsEnabled)
            }
            item {
                val rules by viewModel.senderRules.collectAsState()
                SenderRulesSection(viewModel, rules)
            }

            item { LicensesSection() }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

/** Pill input row used by the three editors below. */
@Composable
private fun InlineAddField(
    value: String,
    onValue: (String) -> Unit,
    hint: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier
                .weight(1f)
                .height(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(appColors.fill)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValue,
                singleLine = true,
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontFamily = MaterialTheme.typography.bodyMedium.fontFamily,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    androidx.compose.foundation.layout.Box {
                        if (value.isEmpty()) {
                            Text(hint, style = MaterialTheme.typography.bodySmall, color = appColors.secondaryLabel)
                        }
                        inner()
                    }
                },
            )
        }
        TextButton(onClick = onAction) { Text(actionLabel) }
    }
}

/** Removable pattern pill. */
@Composable
private fun PatternPill(text: String, onRemove: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(end = 6.dp, top = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(appColors.fill)
            .pressable(onClick = onRemove)
            .padding(horizontal = 9.dp, vertical = 5.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.width(4.dp))
        Icon(
            Icons.Default.Check,
            contentDescription = null,
            tint = appColors.secondaryLabel,
            modifier = Modifier.size(12.dp),
        )
    }
}

@Composable
private fun SenderPatternsEditor(viewModel: SettingsViewModel, patterns: List<String>) {
    var input by remember { mutableStateOf("") }
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            stringResource(R.string.setting_sender_patterns),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            stringResource(R.string.setting_sender_patterns_sub),
            style = MaterialTheme.typography.bodySmall,
            color = appColors.secondaryLabel,
        )
        Spacer(Modifier.height(8.dp))
        AppListGroup {
            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                InlineAddField(
                    value = input,
                    onValue = { input = it },
                    hint = "^3000",
                    actionLabel = stringResource(R.string.action_add),
                    onAction = {
                        if (input.isNotBlank()) {
                            viewModel.addSenderPattern(input.trim())
                            input = ""
                        }
                    },
                )
                Row {
                    for (p in patterns) {
                        PatternPill(p) { viewModel.removeSenderPattern(p) }
                    }
                }
            }
        }
    }
}

@Composable
private fun BlockedSendersEditor(viewModel: SettingsViewModel, blocked: Set<String>) {
    var input by remember { mutableStateOf("") }
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(stringResource(R.string.setting_blocked), style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))
        AppListGroup {
            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                InlineAddField(
                    value = input,
                    onValue = { input = it },
                    hint = "0912...",
                    actionLabel = stringResource(R.string.action_block),
                    onAction = {
                        if (input.isNotBlank()) {
                            viewModel.blockSender(input.trim())
                            input = ""
                        }
                    },
                )
                for (b in blocked) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(b, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        IconButton(onClick = { viewModel.unblockSender(b) }) {
                            Icon(Icons.Outlined.Delete, null, tint = appColors.withdrawal)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BankPatternsEditor(
    viewModel: SettingsViewModel,
    patterns: List<com.sirmrm.messageapp.data.db.CustomBankPatternEntity>,
) {
    var input by remember { mutableStateOf("") }
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            stringResource(R.string.setting_bank_patterns),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            stringResource(R.string.setting_bank_patterns_sub),
            style = MaterialTheme.typography.bodySmall,
            color = appColors.secondaryLabel,
        )
        Spacer(Modifier.height(8.dp))
        AppListGroup {
            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                InlineAddField(
                    value = input,
                    onValue = { input = it },
                    hint = "30003305",
                    actionLabel = stringResource(R.string.action_add),
                    onAction = {
                        if (input.isNotBlank()) {
                            viewModel.addBankPattern(input.trim())
                            input = ""
                        }
                    },
                )
                Row {
                    for (p in patterns) {
                        PatternPill(p.pattern) { viewModel.removeBankPattern(p.id) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SenderRulesSection(
    viewModel: SettingsViewModel,
    rules: List<com.sirmrm.messageapp.domain.repository.SenderRuleRow>,
) {
    val txRules = rules.filter { it.decision == "ALWAYS_TX" || it.decision == "NEVER_TX" }
    if (txRules.isEmpty()) return
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(stringResource(R.string.setting_tx_senders), style = MaterialTheme.typography.bodyLarge)
        Text(
            stringResource(R.string.setting_tx_senders_sub),
            style = MaterialTheme.typography.bodySmall,
            color = appColors.secondaryLabel,
        )
        Spacer(Modifier.height(8.dp))
        AppListGroup {
            txRules.forEachIndexed { i, r ->
                if (i > 0) Hairline()
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        r.senderKey,
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        if (r.decision == "ALWAYS_TX") "✓" else "✗",
                        color = if (r.decision == "ALWAYS_TX") appColors.deposit
                                else appColors.withdrawal,
                    )
                    TextButton(onClick = {
                        viewModel.setTxRule(
                            r.senderKey,
                            if (r.decision == "ALWAYS_TX") "NEVER_TX" else "ALWAYS_TX",
                        )
                    }) { Text(stringResource(R.string.action_toggle)) }
                    IconButton(onClick = { viewModel.removeRule(r.senderKey) }) {
                        Icon(Icons.Outlined.Delete, null, tint = appColors.withdrawal)
                    }
                }
            }
        }
    }
}

@Composable
private fun TtsSection(viewModel: SettingsViewModel, s: AppSettings) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(stringResource(R.string.setting_tts), style = MaterialTheme.typography.bodyLarge)
        Text(
            stringResource(R.string.setting_tts_sub),
            style = MaterialTheme.typography.bodySmall,
            color = appColors.secondaryLabel,
        )
        Spacer(Modifier.height(8.dp))
        AppListGroup {
            AppListRow(
                title = stringResource(R.string.setting_tts_master),
                trailing = { AppSwitch(s.ttsEnabled, viewModel::setTtsEnabled) },
            )
            if (s.ttsEnabled) {
                Hairline()
                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                    Text(stringResource(R.string.setting_tts_voice), style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    AppSegmentedControl(
                        items = listOf("voice1", "voice2", "voice3"),
                        selected = s.ttsVoiceId,
                        onSelect = viewModel::setTtsVoice,
                        label = {
                            when (it) {
                                "voice1" -> "صدای ۱"
                                "voice2" -> "صدای ۲"
                                else -> "صدای ۳"
                            }
                        },
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        stringResource(R.string.setting_tts_speed) + " — ${"%.1f".format(s.ttsSpeed)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = appColors.secondaryLabel,
                    )
                    Slider(
                        value = s.ttsSpeed,
                        onValueChange = viewModel::setTtsSpeed,
                        valueRange = 0.5f..2f,
                        colors = SliderDefaults.colors(
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = appColors.fill,
                            thumbColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                    // preview: default sample or the user's own text
                    var previewText by remember { mutableStateOf("") }
                    InlineAddField(
                        value = previewText,
                        onValue = { previewText = it },
                        hint = stringResource(R.string.setting_tts_preview_hint),
                        actionLabel = stringResource(R.string.setting_tts_preview),
                        onAction = { viewModel.previewVoice(previewText) },
                    )
                }
                Hairline()
                AppListRow(
                    title = stringResource(R.string.setting_tts_hp_only),
                    trailing = { AppSwitch(s.ttsAutoReadHeadphoneOnly, viewModel::setTtsHpOnly) },
                )
                Hairline()
                AppListRow(
                    title = stringResource(R.string.setting_tts_screen_only),
                    trailing = { AppSwitch(s.ttsAutoReadScreenOnOnly, viewModel::setTtsScreenOnly) },
                )
                Hairline()
                AppListRow(
                    title = stringResource(R.string.setting_tts_auto_otp),
                    trailing = { AppSwitch(s.ttsAutoReadOtp, viewModel::setTtsAutoOtp) },
                )
                Hairline()
                AppListRow(
                    title = stringResource(R.string.setting_tts_auto_tx),
                    trailing = { AppSwitch(s.ttsAutoReadTransactional, viewModel::setTtsAutoTx) },
                )
            }
        }
    }
}

@Composable
private fun KeyContactsEditor(
    viewModel: SettingsViewModel,
    contacts: List<com.sirmrm.messageapp.data.db.KeyContactEntity>,
    ttsEnabled: Boolean,
) {
    var input by remember { mutableStateOf("") }
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(stringResource(R.string.setting_key_contacts), style = MaterialTheme.typography.bodyLarge)
        Text(
            stringResource(R.string.setting_key_contacts_sub),
            style = MaterialTheme.typography.bodySmall,
            color = appColors.secondaryLabel,
        )
        if (ttsEnabled) {
            Spacer(Modifier.height(8.dp))
            AppListGroup {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    InlineAddField(
                        value = input,
                        onValue = { input = it },
                        hint = "0912...",
                        actionLabel = stringResource(R.string.action_add),
                        onAction = {
                            if (input.isNotBlank()) {
                                viewModel.addKeyContact(input.trim(), input.trim())
                                input = ""
                            }
                        },
                    )
                }
                contacts.forEach { kc ->
                    Hairline()
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(kc.displayName, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        AppSwitch(
                            checked = kc.autoRead,
                            onCheckedChange = { viewModel.setKeyAutoRead(kc.senderKey, it) },
                        )
                        IconButton(onClick = { viewModel.removeKeyContact(kc.senderKey) }) {
                            Icon(Icons.Outlined.Delete, null, tint = appColors.withdrawal)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LicensesSection() {
    AppSectionHeader(stringResource(R.string.licenses_title))
    AppListGroup {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.licenses_engine), style = MaterialTheme.typography.bodyMedium)
            Text("sherpa-onnx 1.13.8 — Apache-2.0 — github.com/k2-fsa/sherpa-onnx", style = MaterialTheme.typography.bodySmall, color = appColors.secondaryLabel)
            Text(stringResource(R.string.licenses_voices), style = MaterialTheme.typography.bodyMedium)
            Text("fa_IR-amir-medium / fa_IR-ganji-medium / fa_IR-ganji_adabi-medium — MIT — huggingface.co/rhasspy/piper-voices", style = MaterialTheme.typography.bodySmall, color = appColors.secondaryLabel)
            Text("espeak-ng data — GPL-3.0 — github.com/espeak-ng/espeak-ng", style = MaterialTheme.typography.bodySmall, color = appColors.secondaryLabel)
            Text(stringResource(R.string.licenses_disclaimer), style = MaterialTheme.typography.bodySmall, color = appColors.secondaryLabel)
        }
    }
}
