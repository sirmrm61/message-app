package com.sirmrm.messageapp.feature.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.res.stringResource
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
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {

            if (threadId < 0) {
                OutlinedTextField(
                    value = recipient,
                    onValueChange = { recipient = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    label = { Text(stringResource(R.string.chat_recipient)) },
                    singleLine = true,
                )
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.messages, key = { it.id }) { msg ->
                    MessageBubble(
                        message = msg,
                        calendar = state.settings?.calendar ?: CalendarMode.JALALI,
                        digits = state.settings?.digits ?: DigitStyle.PERSIAN,
                        onMarkPromo = { viewModel.markPromo(msg, true) },
                        onMarkNotPromo = { viewModel.markPromo(msg, false) },
                    )
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
) {
    val incoming = message.isIncoming
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = if (incoming) Alignment.Start else Alignment.End,
    ) {
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
                    JalaliDate.formatListDate(message.date, calendar, digits),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
