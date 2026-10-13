package com.sirmrm.messageapp.feature.chat

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sirmrm.messageapp.classifier.SmsCategory
import com.sirmrm.messageapp.core.designsystem.AppSegmentedControl
import com.sirmrm.messageapp.core.designsystem.Hairline
import com.sirmrm.messageapp.core.designsystem.appColors
import com.sirmrm.messageapp.core.designsystem.pressable
import com.sirmrm.messageapp.core.util.JalaliDate
import com.sirmrm.messageapp.domain.model.CalendarMode
import com.sirmrm.messageapp.domain.model.DigitStyle
import com.sirmrm.messageapp.domain.model.MessageFolder
import com.sirmrm.messageapp.domain.model.SmsMessage

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
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
        containerColor = appColors.groupedBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = appColors.card),
                title = {
                    Text(
                        state.conversation?.displayName
                            ?: recipient.ifBlank { stringResource(R.string.chat_new_message) },
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            stringResource(R.string.action_back),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                actions = {
                    if (state.settings?.ttsEnabled == true) {
                        var menuOpen by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(48.dp)) {
                                Icon(
                                    Icons.AutoMirrored.Outlined.VolumeUp,
                                    contentDescription = stringResource(R.string.tts_read_menu),
                                    tint = MaterialTheme.colorScheme.primary,
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
                                                if (state.isKeyContact) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                                contentDescription = null,
                                                tint = if (state.isKeyContact) appColors.warning
                                                       else appColors.secondaryLabel,
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
                RecipientField(recipient, onChange = { recipient = it })
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
                    var prevIncoming: Boolean? = null
                    for (m in filtered) {
                        val k = JalaliDate.dayKey(m.date)
                        if (k != lastDay) { lastDay = k; add(k); prevIncoming = null }
                        add(m)
                        prevIncoming = m.isIncoming
                    }
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
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
                            modifier = Modifier.animateItem(),
                        )
                    } else {
                        DaySeparator(
                            JalaliDate.formatDayHeader(
                                dayKeyToMillis(item as Long), // day header entries are Long dayKeys
                                state.settings?.calendar ?: CalendarMode.JALALI,
                                state.settings?.digits ?: DigitStyle.PERSIAN,
                            ),
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
private fun DaySeparator(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = appColors.secondaryLabel,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
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
    modifier: Modifier = Modifier,
) {
    val incoming = message.isIncoming
    Column(
        modifier.fillMaxWidth(),
        horizontalAlignment = if (incoming) Alignment.Start else Alignment.End,
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 18.dp, topEnd = 18.dp,
                    bottomStart = if (incoming) 5.dp else 18.dp,
                    bottomEnd = if (incoming) 18.dp else 5.dp,
                ),
                // outgoing: soft blue; incoming: flat gray fill
                color = if (incoming) appColors.fill else MaterialTheme.colorScheme.primary,
                modifier = Modifier.widthIn(max = 290.dp),
            ) {
                Column(Modifier.padding(horizontal = 13.dp, vertical = 8.dp)) {
                    Text(
                        message.body,
                        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 23.sp),
                        color = if (incoming) MaterialTheme.colorScheme.onSurface else Color.White,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        JalaliDate.formatFull(message.date, calendar, digits),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (incoming) appColors.tertiaryLabel else Color.White.copy(alpha = 0.7f),
                    )
                }
            }
            if (ttsEnabled && incoming) {
                // Per-bubble read button — a second press stops playback.
                var speaking by remember { mutableStateOf(false) }
                Spacer(Modifier.width(4.dp))
                IconButton(
                    onClick = {
                        if (speaking) onStopSpeak() else onSpeak()
                        speaking = !speaking
                    },
                    modifier = Modifier.size(32.dp),
                ) {
                    if (speaking) MiniWaveform()
                    else Icon(
                        Icons.AutoMirrored.Outlined.VolumeUp,
                        contentDescription = stringResource(R.string.tts_read_bubble),
                        modifier = Modifier.size(17.dp),
                        tint = appColors.secondaryLabel,
                    )
                }
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

/** Three thin animated bars shown while the bubble is being read aloud. */
@Composable
private fun MiniWaveform() {
    val t = rememberInfiniteTransition(label = "wave")
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        for (i in 0..2) {
            val a by t.animateFloat(
                initialValue = 3f,
                targetValue = 12f,
                animationSpec = infiniteRepeatable(tween(350 + i * 120), RepeatMode.Reverse),
                label = "w$i",
            )
            Box(Modifier.width(2.5.dp).height(a.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.primary))
        }
    }
}

@Composable
private fun OtpCodeCard(body: String) {
    val code = OTP_CODE_REGEX.find(body)?.value ?: return
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    val iconTint by animateColorAsState(
        if (copied) appColors.deposit else MaterialTheme.colorScheme.primary,
        label = "otpTint",
    )
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = appColors.card,
        modifier = Modifier.padding(top = 4.dp),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    stringResource(R.string.otp_code_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = appColors.secondaryLabel,
                )
                Text(
                    code,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.width(12.dp))
            IconButton(onClick = {
                clipboard.setText(AnnotatedString(code))
                copied = true
            }) {
                Icon(
                    if (copied) Icons.Filled.Check else Icons.Outlined.ContentCopy,
                    contentDescription = stringResource(R.string.action_copy_code),
                    tint = iconTint,
                    modifier = Modifier.size(20.dp),
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
private fun RecipientField(value: String, onChange: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(appColors.fill)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontFamily = MaterialTheme.typography.bodyLarge.fontFamily,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            stringResource(R.string.chat_recipient),
                            style = MaterialTheme.typography.bodyMedium,
                            color = appColors.secondaryLabel,
                        )
                    }
                    inner()
                }
            },
        )
    }
}

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
        com.sirmrm.messageapp.core.designsystem.AppSearchBar(
            value = query,
            onValueChange = onQuery,
            hint = stringResource(R.string.search_in_thread),
            modifier = Modifier.weight(1f),
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
                Icon(Icons.Default.DateRange, stringResource(R.string.action_filter_date), tint = appColors.secondaryLabel)
            }
        }
    }
}

@Composable
private fun ReasonChips(message: SmsMessage) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (code in message.reasonCodes.take(3)) {
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
                color = appColors.secondaryLabel,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(appColors.fill)
                    .padding(horizontal = 7.dp, vertical = 2.dp),
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
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
        AppSegmentedControl(
            items = listOf(-1, 1),
            selected = if (selectedSim > 0) 1 else -1,
            onSelect = onSimChange,
            label = { if (it <= 0) stringResource(R.string.sim_1) else stringResource(R.string.sim_2) },
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            // Capsule input
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(appColors.fill)
                    .padding(horizontal = 14.dp, vertical = 9.dp),
            ) {
                BasicTextField(
                    value = draft,
                    onValueChange = onDraftChange,
                    maxLines = 4,
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontFamily = MaterialTheme.typography.bodyLarge.fontFamily,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        Box {
                            if (draft.isEmpty()) {
                                Text(
                                    stringResource(R.string.chat_hint),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = appColors.secondaryLabel,
                                )
                            }
                            inner()
                        }
                    },
                )
            }
            Spacer(Modifier.width(8.dp))
            // Circular send button — activates once there's text.
            val active = draft.isNotBlank() && !sending
            val sendTint by animateColorAsState(
                if (active) MaterialTheme.colorScheme.primary else appColors.fill,
                label = "sendTint",
            )
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(sendTint)
                    .pressable(enabled = active, onClick = onSend),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    stringResource(R.string.action_send),
                    tint = if (active) Color.White else appColors.tertiaryLabel,
                    modifier = Modifier.size(17.dp),
                )
            }
        }
    }
}

@Composable
private fun TxConfirmBar(onConfirm: () -> Unit, onReject: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(appColors.card)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.tx_confirm_sender),
            style = MaterialTheme.typography.bodySmall,
            color = appColors.secondaryLabel,
            modifier = Modifier.weight(1f).padding(start = 8.dp),
        )
        IconButton(onClick = onConfirm, modifier = Modifier.size(48.dp)) {
            Icon(
                Icons.Filled.Check,
                contentDescription = stringResource(R.string.cd_confirm_tx),
                tint = appColors.deposit,
            )
        }
        IconButton(onClick = onReject, modifier = Modifier.size(48.dp)) {
            Icon(
                Icons.Filled.Close,
                contentDescription = stringResource(R.string.cd_reject_tx),
                tint = appColors.withdrawal,
            )
        }
    }
}
