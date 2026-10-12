package com.sirmrm.messageapp.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sirmrm.messageapp.domain.model.AppLanguage
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
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize()) {

            item { SectionHeader(stringResource(R.string.section_appearance)) }
            item {
                SettingRow(stringResource(R.string.setting_language)) {
                    SingleChoiceSegmentedButtonRow {
                        AppLanguage.entries.forEachIndexed { i, lang ->
                            SegmentedButton(
                                selected = s.language == lang,
                                onClick = { viewModel.setLanguage(lang) },
                                shape = SegmentedButtonDefaults.itemShape(i, 2),
                            ) { Text(if (lang == AppLanguage.FA) "فارسی" else "English") }
                        }
                    }
                }
            }
            item {
                SettingRow(stringResource(R.string.setting_theme)) {
                    SingleChoiceSegmentedButtonRow {
                        ThemeMode.entries.forEachIndexed { i, t ->
                            SegmentedButton(
                                selected = s.theme == t,
                                onClick = { viewModel.setTheme(t) },
                                shape = SegmentedButtonDefaults.itemShape(i, 3),
                            ) {
                                Text(
                                    when (t) {
                                        ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
                                        ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                                        ThemeMode.DARK -> stringResource(R.string.theme_dark)
                                    }
                                )
                            }
                        }
                    }
                }
            }
            item {
                SettingRow(stringResource(R.string.setting_digits)) {
                    SingleChoiceSegmentedButtonRow {
                        SegmentedButton(
                            selected = s.digits == DigitStyle.PERSIAN,
                            onClick = { viewModel.setDigits(DigitStyle.PERSIAN) },
                            shape = SegmentedButtonDefaults.itemShape(0, 2),
                        ) { Text("۱۲۳") }
                        SegmentedButton(
                            selected = s.digits == DigitStyle.LATIN,
                            onClick = { viewModel.setDigits(DigitStyle.LATIN) },
                            shape = SegmentedButtonDefaults.itemShape(1, 2),
                        ) { Text("123") }
                    }
                }
            }
            item {
                SettingRow(stringResource(R.string.setting_calendar)) {
                    SingleChoiceSegmentedButtonRow {
                        SegmentedButton(
                            selected = s.calendar == CalendarMode.JALALI,
                            onClick = { viewModel.setCalendar(CalendarMode.JALALI) },
                            shape = SegmentedButtonDefaults.itemShape(0, 2),
                        ) { Text(stringResource(R.string.calendar_jalali)) }
                        SegmentedButton(
                            selected = s.calendar == CalendarMode.GREGORIAN,
                            onClick = { viewModel.setCalendar(CalendarMode.GREGORIAN) },
                            shape = SegmentedButtonDefaults.itemShape(1, 2),
                        ) { Text(stringResource(R.string.calendar_gregorian)) }
                    }
                }
            }
            item {
                ToggleRow(
                    title = stringResource(R.string.setting_swipe),
                    checked = s.swipeRightArchives,
                    onChange = viewModel::setSwipeRightArchives,
                )
            }

            item { SectionHeader(stringResource(R.string.section_classifier)) }
            item {
                ToggleRow(
                    title = stringResource(R.string.setting_auto_delete),
                    subtitle = stringResource(R.string.setting_auto_delete_sub),
                    checked = s.autoDeleteEnabled,
                    onChange = viewModel::setAutoDelete,
                )
            }
            item {
                if (s.autoDeleteEnabled) {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        Text(
                            stringResource(R.string.setting_auto_delete_days, s.autoDeleteDays),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Slider(
                            value = s.autoDeleteDays.toFloat(),
                            onValueChange = { viewModel.setAutoDeleteDays(it.toInt()) },
                            valueRange = 7f..90f,
                        )
                    }
                }
            }
            item { SenderPatternsEditor(viewModel, s.serviceSenderPatterns) }

            item { SectionHeader(stringResource(R.string.section_privacy)) }
            item {
                ToggleRow(
                    title = stringResource(R.string.setting_biometric),
                    checked = s.biometricLock,
                    onChange = viewModel::setBiometric,
                )
            }
            item {
                ToggleRow(
                    title = stringResource(R.string.setting_flag_secure),
                    checked = s.flagSecure,
                    onChange = viewModel::setFlagSecure,
                )
            }
            item { BlockedSendersEditor(viewModel, s.blockedSenders) }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
    )
}

@Composable
private fun SettingRow(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String? = null, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SenderPatternsEditor(viewModel: SettingsViewModel, patterns: List<String>) {
    var input by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }

    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            stringResource(R.string.setting_sender_patterns),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            stringResource(R.string.setting_sender_patterns_sub),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("^3000") },
                singleLine = true,
            )
            TextButton(
                onClick = {
                    if (input.isNotBlank()) {
                        viewModel.addSenderPattern(input.trim())
                        input = ""
                    }
                }
            ) { Text(stringResource(R.string.action_add)) }
        }
        Spacer(Modifier.height(8.dp))
        Row {
            for (p in patterns) {
                FilterChip(
                    selected = false,
                    onClick = { viewModel.removeSenderPattern(p) },
                    label = { Text(p) },
                    modifier = Modifier.padding(end = 4.dp),
                )
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("0912...") },
                singleLine = true,
            )
            TextButton(
                onClick = {
                    if (input.isNotBlank()) {
                        viewModel.blockSender(input.trim())
                        input = ""
                    }
                }
            ) { Text(stringResource(R.string.action_block)) }
        }
        for (b in blocked) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(b, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                IconButton(onClick = { viewModel.unblockSender(b) }) {
                    Icon(Icons.Default.Delete, null)
                }
            }
        }
    }
}
