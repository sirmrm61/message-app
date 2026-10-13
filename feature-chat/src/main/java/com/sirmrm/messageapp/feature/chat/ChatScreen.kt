package com.sirmrm.messageapp.feature.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sirmrm.messageapp.classifier.SmsCategory
import com.sirmrm.messageapp.core.util.JalaliDate
import com.sirmrm.messageapp.domain.model.CalendarMode
import com.sirmrm.messageapp.domain.model.DigitStyle
import com.sirmrm.messageapp.domain.model.MessageFolder
import com.sirmrm.messageapp.domain.model.SmsMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    threadId: Long,
    composeAddress: String = "",
    composeBody: String = "",
    onBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    LaunchedEffect(threadId) { viewModel.init(threadId) }
    val state by viewModel.state.collectAsState()
    var recipient by remember { mutableStateOf(composeAddress) }

    LaunchedEffect(composeBody) {
        if (composeBody.isNotBlank()) viewModel.setDraft(composeBody)
    }

    val listState = rememberLazyListState()
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.size - 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.conversation?.displayName
                            ?: recipient.ifBlank { stringResource(R.string.chat_new_message) }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                },
                actions = {
                    if (state.settings?.ttsEnabled == true) {
                        var menuOpen by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(48.dp)) {
                                Icon(
                                    Icons.Filled.PlayArrow,
                                    contentDescription = stringResource(R.string.tts_read_menu),
                                )
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.tts_read_unread)) },
                                    onClick = { menuOpen = false; viewModel.speakMessages(unreadOnly = true) },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.tts_read_all)) },
                                    onClick = { menuOpen = false; viewModel.speakMessages(unreadOnly = false) },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.tts_stop)) },
                                    onClick = { menuOpen = false; viewModel.stopSpeaking() },
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                if (state.isKeyContact) Icons.Filled.Star else Icons.Filled.Star,
                                                contentDescription = null,
                                                tint = if (state.isKeyContact) MaterialTheme.colorScheme.primary
                                                       else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp),
                                            )
                                            Spacer(Modifier.size(8.dp))
                                            Text(stringResource(R.string.tts_key_contact))
                                        }
                                    },
                                    onClick = { menuOpen = false; viewModel.toggleKeyContact() },
                                )
                            }
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            // «Is this sender transactional?» banner — only while no user rule exists.
            if (state.needsTxConfirm) {
                TxConfirmBar(
                    onConfirm = { viewModel.confirmTxSender(true) },
                    onReject = { viewModel.confirmTxSender(false) },
                )
            }
            LaunchedEffect(state.txRuleChange) {
                state.txRuleChange?.let {
                    // simple confirmation surface; undo handled via undoTxRuleChange
                }
            }

            if (threadId < 0) {
                OutlinedTextField(
                    value = recipient,
                    onValueChange = { recipient = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    label = { Text(stringResource(R.string.chat_recipient)) },
                    singleLine = true,
                )
            } else {
                ThreadFilterBar(
                    query = state.searchQuery,
                    filterDate = state.filterDate,
                    calendar = state.settings?.calendar ?: CalendarMode.JALALI,
                    digits = state.settings?.digits ?: DigitStyle.PERSIAN,
                    onQuery = viewModel::setSearchQuery,
                    onDate = viewModel::setFilterDate,
                )
            }

            val searchQuery = state.searchQuery
            val filterDate = state.filterDate
            val filtered = state.messages.filter { m ->
                (searchQuery.isBlank() ||
                    m.body.contains(searchQuery, ignoreCase = true)) &&
                    (filterDate == null || JalaliDate.sameDay(m.date, filterDate))
            }
            val grouped = remember(filtered, state.settings) {
                buildList<Any?> {
                    var lastDay = -1L
                    for (m in filtered) {
                        val k = JalaliDate.dayKey(m.date)
                        if (k != lastDay) { lastDay = k; add(k) }
                        add(m)
                    }
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(
                    grouped,
                    key = { if (it is SmsMessage) "m${it.id}" else "d${it}" },
                ) { item ->
                    if (item is SmsMessage) {
                        val msg = item
                        MessageBubble(
                            message = msg,
                            calendar = state.settings?.calendar ?: CalendarMode.JALALI,
                            digits = state.settings?.digits ?: DigitStyle.PERSIAN,
                            onMarkPromo = { viewModel.markPromo(msg, true) },
                            onMarkNotPromo = { viewModel.markPromo(msg, false) },
                            ttsEnabled = state.settings?.ttsEnabled == true,
                            onSpeak = { viewModel.speak(msg) },
                            onStopSpeak = viewModel::stopSpeaking,
                        )
                    } else {
                        Text(
                            JalaliDate.formatDayHeader(
                                dayKeyToMillis(item as Long), // day header entries are Long dayKeys
                                state.settings?.calendar ?: CalendarMode.JALALI,
                                state.settings?.digits ?: DigitStyle.PERSIAN,
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
            }

            state.sendError?.let { err ->
                Text(
                    err,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            Composer(
                draft = state.draft,
                sending = state.sending,
                selectedSim = state.selectedSim,
                onDraftChange = viewModel::setDraft,
                onSimChange = viewModel::selectSim,
                onSend = { viewModel.send(recipient.ifBlank { state.conversation?.address ?: "" }, state.draft) },
            )
        }
    }
}

@Composable
private fun MessageBubble(
    message: SmsMessage,
    calendar: CalendarMode,
    digits: DigitStyle,
    onMarkPromo: () -> Unit,
    onMarkNotPromo: () -> Unit,
    ttsEnabled: Boolean = false,
    onSpeak: () -> Unit = {},
    onStopSpeak: () -> Unit = {},
) {
    val incoming = message.isIncoming
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = if (incoming) Alignment.Start else Alignment.End,
    ) {
        if (ttsEnabled && incoming) {
            // Per-bubble read button — a second press stops playback.
            var speaking by remember { mutableStateOf(false) }
            IconButton(
                onClick = {
                    if (speaking) onStopSpeak() else onSpeak()
                    speaking = !speaking
                },
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = stringResource(R.string.tts_read_bubble),
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp, topEnd = 18.dp,
                bottomStart = if (incoming) 4.dp else 18.dp,
                bottomEnd = if (incoming) 18.dp else 4.dp,
            ),
            color = if (incoming) MaterialTheme.colorScheme.surfaceVariant
            else MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.widthIn(max = 300.dp),
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(message.body, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(4.dp))
                Text(
                    JalaliDate.formatFull(message.date, calendar, digits),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // OTP: highlight the extracted code with a copy action.
        if (incoming && message.category == SmsCategory.OTP) {
            OtpCodeCard(message.body)
        }

        // Explainability: why this message was classified as promo.
        if (message.category == SmsCategory.PROMO && message.reasonCodes.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            ReasonChips(message)
            Row {
                TextButton(onClick = onMarkNotPromo) {
                    Text(stringResource(R.string.action_not_promo))
                }
            }
        } else if (message.folder == MessageFolder.INBOX && incoming &&
            message.category == SmsCategory.UNKNOWN
        ) {
            TextButton(onClick = onMarkPromo) {
                Text(stringResource(R.string.action_is_promo))
            }
        }
    }
}

@Composable
private fun OtpCodeCard(body: String) {
    val code = OTP_CODE_REGEX.find(body)?.value ?: return
    val clipboard = LocalClipboardManager.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.padding(top = 6.dp),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    stringResource(R.string.otp_code_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    code,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Spacer(Modifier.width(12.dp))
            IconButton(onClick = { clipboard.setText(AnnotatedString(code)) }) {
                Icon(
                    Icons.Default.AddCircle,
                    contentDescription = stringResource(R.string.action_copy_code),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

private val OTP_CODE_REGEX = Regex("""\b\d{4,8}\b""")

private fun dayKeyToMillis(key: Long): Long =
    java.util.Calendar.getInstance().apply {
        clear()
        set(java.util.Calendar.YEAR, (key / 1000).toInt())
        set(java.util.Calendar.DAY_OF_YEAR, (key % 1000).toInt())
    }.timeInMillis

@Composable
private fun ThreadFilterBar(
    query: String,
    filterDate: Long?,
    calendar: CalendarMode,
    digits: DigitStyle,
    onQuery: (String) -> Unit,
    onDate: (Long?) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            modifier = Modifier.weight(1f),
            placeholder = { Text(stringResource(R.string.search_in_thread)) },
            singleLine = true,
        )
        if (filterDate != null) {
            TextButton(onClick = { onDate(null) }) {
                Text(JalaliDate.format(filterDate, calendar, digits))
            }
        } else {
            IconButton(onClick = {
                val c = java.util.Calendar.getInstance()
                android.app.DatePickerDialog(
                    context,
                    { _, y, m, d ->
                        val cal = java.util.Calendar.getInstance()
                        cal.set(y, m, d, 12, 0, 0)
                        onDate(cal.timeInMillis)
                    },
                    c.get(java.util.Calendar.YEAR),
                    c.get(java.util.Calendar.MONTH),
                    c.get(java.util.Calendar.DAY_OF_MONTH),
                ).show()
            }) {
                Icon(Icons.Default.Info, stringResource(R.string.action_filter_date))
            }
        }
    }
}

@Composable
private fun ReasonChips(message: SmsMessage) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (code in message.reasonCodes.take(3)) {
            AssistChip(
                onClick = {},
                label = {
                    Text(
                        when (code) {
                            "unsub" -> stringResource(R.string.reason_unsub)
                            "festival" -> stringResource(R.string.reason_festival)
                            "discount" -> stringResource(R.string.reason_discount)
                            "cta" -> stringResource(R.string.reason_cta)
                            "prize" -> stringResource(R.string.reason_prize)
                            "sale_words" -> stringResource(R.string.reason_sale)
                            "service_sender" -> stringResource(R.string.reason_service_sender)
                            "short_link" -> stringResource(R.string.reason_short_link)
                            "unknown_mobile" -> stringResource(R.string.reason_unknown_mobile)
                            "model" -> stringResource(R.string.reason_model)
                            "free_charge" -> stringResource(R.string.reason_free)
                            "visit" -> stringResource(R.string.reason_visit)
                            "percent" -> stringResource(R.string.reason_percent)
                            else -> code
                        },
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
            )
        }
    }
}

@Composable
private fun Composer(
    draft: String,
    sending: Boolean,
    selectedSim: Int,
    onDraftChange: (String) -> Unit,
    onSimChange: (Int) -> Unit,
    onSend: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(12.dp)) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = selectedSim <= 0,
                onClick = { onSimChange(-1) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            ) { Text(stringResource(R.string.sim_1)) }
            SegmentedButton(
                selected = selectedSim > 0,
                onClick = { onSimChange(1) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            ) { Text(stringResource(R.string.sim_2)) }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(R.string.chat_hint)) },
                maxLines = 4,
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onSend, enabled = draft.isNotBlank() && !sending) {
                Icon(Icons.AutoMirrored.Filled.Send, stringResource(R.string.action_send))
            }
        }
    }
}

@Composable
private fun TxConfirmBar(onConfirm: () -> Unit, onReject: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.tx_confirm_sender),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
            )
            IconButton(onClick = onConfirm, modifier = Modifier.size(48.dp)) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = stringResource(R.string.cd_confirm_tx),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            IconButton(onClick = onReject, modifier = Modifier.size(48.dp)) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(R.string.cd_reject_tx),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
