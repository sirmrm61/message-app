package com.sirmrm.messageapp.feature.conversations

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sirmrm.messageapp.classifier.BankTxParser
import com.sirmrm.messageapp.core.util.JalaliDate
import com.sirmrm.messageapp.core.util.PersianDigits.toPersianDigits
import com.sirmrm.messageapp.domain.model.CalendarMode
import com.sirmrm.messageapp.domain.model.Conversation
import com.sirmrm.messageapp.domain.model.DigitStyle
import com.sirmrm.messageapp.domain.model.MessageFolder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsScreen(
    isDefaultSms: Boolean,
    onEnableDefault: () -> Unit,
    onOpenChat: (threadId: Long, address: String) -> Unit,
    onOpenPromotions: () -> Unit,
    onOpenSettings: () -> Unit,
    onNewMessage: () -> Unit,
    viewModel: ConversationsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Show the last fatal crash trace (if any) so it can be reported.
    var crashLog by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        crashLog = withContext(Dispatchers.IO) {
            runCatching {
                java.io.File(context.filesDir, "crash.log")
                    .takeIf { it.exists() }?.readText()
            }.getOrNull()
        }
    }
    crashLog?.let { log ->
        AlertDialog(
            onDismissRequest = {
                runCatching { java.io.File(context.filesDir, "crash.log").delete() }
                crashLog = null
            },
            confirmButton = {
                TextButton(onClick = {
                    runCatching { java.io.File(context.filesDir, "crash.log").delete() }
                    crashLog = null
                }) { Text("OK") }
            },
            title = { Text(stringResource(R.string.crash_title)) },
            text = {
                SelectionContainer {
                    Text(log, style = MaterialTheme.typography.bodySmall)
                }
            },
        )
    }
    val scope = rememberCoroutineScope()
    val undoLabel = stringResource(R.string.action_undo)
    val archivedMsg = stringResource(R.string.msg_archived)
    val trashedMsg = stringResource(R.string.msg_trashed)

    fun showUndo(message: String) {
        scope.launch {
            val res = snackbar.showSnackbar(message, undoLabel)
            if (res == SnackbarResult.ActionPerformed) viewModel.undo()
        }
    }

    Scaffold(
        topBar = {
            if (state.selectionMode) {
                TopAppBar(
                    title = { Text("${state.selection.size}") },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, null)
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.selectAll() }) {
                            Icon(Icons.Default.CheckCircle, stringResource(R.string.action_select_all))
                        }
                        IconButton(onClick = { viewModel.markSelectedRead() }) {
                            Icon(Icons.Default.Done, stringResource(R.string.action_mark_read))
                        }
                        IconButton(onClick = { viewModel.archiveSelected() }) {
                            Icon(Icons.Default.Email, stringResource(R.string.action_archive))
                        }
                        IconButton(onClick = { viewModel.trashSelected() }) {
                            Icon(Icons.Default.Delete, stringResource(R.string.action_delete))
                        }
                    },
                )
            } else {
                TopAppBar(
                    title = { Text(stringResource(R.string.app_title)) },
                    actions = {
                        IconButton(onClick = onOpenPromotions) {
                            Icon(Icons.Default.CleaningServices, stringResource(R.string.action_cleanup))
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Default.Settings, stringResource(R.string.action_settings))
                        }
                    },
                )
            }
        },
        floatingActionButton = {
            if (isDefaultSms && !state.selectionMode) {
                FloatingActionButton(onClick = onNewMessage) {
                    Icon(Icons.Default.Create, stringResource(R.string.action_new_message))
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {

            if (!isDefaultSms) {
                NotDefaultBanner(onEnableDefault)
            }

            // Promo counter chip
            state.settings?.let { s ->
                if (s.promosCleanedThisWeek > 0) {
                    Text(
                        text = stringResource(R.string.promos_cleaned_counter)
                            .format(s.promosCleanedThisWeek),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::setQuery,
                    modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                    placeholder = { Text(stringResource(R.string.search_hint)) },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true,
                )
                DateFilterButton(
                    filterDate = state.filterDate,
                    calendar = state.settings?.calendar ?: CalendarMode.JALALI,
                    digits = state.settings?.digits ?: DigitStyle.PERSIAN,
                    onPick = viewModel::setFilterDate,
                    onClear = { viewModel.setFilterDate(null) },
                )
            }

            FolderTabs(
                selected = state.folder,
                onSelect = viewModel::selectFolder,
            )

            if (state.folder == MessageFolder.TRANSACTIONAL) {
                TransactionsView(
                    state = state,
                    calendar = state.settings?.calendar ?: CalendarMode.JALALI,
                    digits = state.settings?.digits ?: DigitStyle.PERSIAN,
                    onToggle = viewModel::toggleTxSelection,
                    onSelectAll = viewModel::selectAllTx,
                    onSubtypeFilter = viewModel::setTxSubtypeFilter,
                    onMonthlyToggle = viewModel::toggleMonthlySums,
                    onFeedback = { item, isTx -> viewModel.feedbackTransactional(item, isTx) },
                    onConfirmSender = { addr, isTx -> viewModel.confirmTxSender(addr, isTx) },
                    onUndoRule = viewModel::undoTxRuleChange,
                )
            } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(state.conversations, key = { it.id }) { conv ->
                    ConversationRow(
                        conversation = conv,
                        selected = conv.id in state.selection,
                        selectionMode = state.selectionMode,
                        calendar = state.settings?.calendar ?: CalendarMode.JALALI,
                        digits = state.settings?.digits ?: DigitStyle.PERSIAN,
                        swipeRightArchives = state.settings?.swipeRightArchives ?: true,
                        onClick = {
                            if (state.selectionMode) viewModel.toggleSelection(conv.id)
                            else onOpenChat(conv.id, conv.address)
                        },
                        onLongClick = { viewModel.toggleSelection(conv.id) },
                        onSwipeArchive = {
                            viewModel.archive(conv.id)
                            showUndo(archivedMsg)
                        },
                        onSwipeTrash = {
                            viewModel.trash(conv.id)
                            showUndo(trashedMsg)
                        },
                    )
                }
            }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun TransactionsView(
    state: ConversationsUiState,
    calendar: CalendarMode,
    digits: DigitStyle,
    onToggle: (Long) -> Unit,
    onSelectAll: () -> Unit,
    onSubtypeFilter: (BankTxParser.TxSubtype?) -> Unit,
    onMonthlyToggle: () -> Unit,
    onFeedback: (BankTxItem, Boolean) -> Unit,
    onConfirmSender: (String, Boolean) -> Unit,
    onUndoRule: () -> Unit = {},
) {
    val clipboard = LocalClipboardManager.current
    val snackbar = remember { SnackbarHostState() }
    val undoLabel = stringResource(R.string.undo)
    val savedMsg = stringResource(R.string.tx_rule_saved)
    LaunchedEffect(state.lastTxRuleChange) {
        if (state.lastTxRuleChange != null) {
            val r = snackbar.showSnackbar(savedMsg, actionLabel = undoLabel, withDismissAction = true)
            if (r == SnackbarResult.ActionPerformed) onUndoRule()
        }
    }
    val tx = state.transactions
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.tx_counts, state.depositCount, state.withdrawalCount),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f).padding(vertical = 4.dp),
            )
            TextButton(onClick = onMonthlyToggle) {
                Text(stringResource(R.string.tx_monthly))
            }
            TextButton(onClick = onSelectAll) {
                Text(stringResource(R.string.action_select_all))
            }
        }
        if (state.showMonthlySums) {
            val (dep, wdr) = state.monthlySums(System.currentTimeMillis())
            Text(
                stringResource(
                    R.string.tx_monthly_sums,
                    formatAmount(dep, digits),
                    formatAmount(wdr, digits),
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        // Subtype filter chips: واریز / برداشت / خرید-پرداخت / OTP / چک / وام / امنیتی
        Row(Modifier.padding(horizontal = 12.dp)) {
            val chips = listOf(
                null to R.string.tx_filter_all,
                BankTxParser.TxSubtype.DEPOSIT to R.string.tx_deposit,
                BankTxParser.TxSubtype.WITHDRAWAL to R.string.tx_withdrawal,
                BankTxParser.TxSubtype.PURCHASE_PAYMENT to R.string.tx_purchase,
                BankTxParser.TxSubtype.OTP to R.string.tx_otp,
                BankTxParser.TxSubtype.CHEQUE to R.string.tx_cheque,
                BankTxParser.TxSubtype.LOAN to R.string.tx_loan,
                BankTxParser.TxSubtype.SECURITY to R.string.tx_security,
            )
            for ((sub, label) in chips) {
                FilterChip(
                    selected = state.txSubtypeFilter == sub,
                    onClick = { onSubtypeFilter(if (state.txSubtypeFilter == sub) null else sub) },
                    label = { Text(stringResource(label)) },
                    modifier = Modifier.padding(end = 6.dp),
                )
            }
        }
        if (state.txSelectionMode) {
            Text(
                stringResource(
                    R.string.tx_selected_sums,
                    formatAmount(state.selectedSum(BankTxParser.Direction.DEPOSIT), digits),
                    formatAmount(state.selectedSum(BankTxParser.Direction.WITHDRAWAL), digits),
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        Box(Modifier.weight(1f)) {
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
        val grouped = remember(tx) { tx.groupBy { it.message.address } }
        LazyColumn(Modifier.fillMaxSize()) {
            // «Maybe transactional» suggestions — confirm to move them in.
            if (state.maybeTransactions.isNotEmpty()) {
                item(key = "maybe_header") {
                    Text(
                        stringResource(R.string.tx_maybe_header),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                }
                items(state.maybeTransactions, key = { "m" + it.id }) { item ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            item.message.body,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TextButton(onClick = { onFeedback(item, true) }) {
                            Text(stringResource(R.string.feedback_is_tx))
                        }
                    }
                }
            }
            grouped.forEach { (sender, items_) ->
                val senderKey =
                    com.sirmrm.messageapp.classifier.SenderKey.of(sender).ifBlank { sender.trim() }
                val needsConfirm = senderKey !in state.txSenderDecisions
                stickyHeader {
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
                        Column {
                            Text(
                                sender,
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                            )
                            if (needsConfirm) {
                                // «Is this sender transactional?» compact ✓/✗ bar.
                                Row(
                                    Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        stringResource(R.string.tx_confirm_sender),
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.weight(1f).padding(start = 8.dp),
                                    )
                                    IconButton(
                                        onClick = { onConfirmSender(sender, true) },
                                        modifier = Modifier.size(48.dp),
                                    ) {
                                        Icon(
                                            Icons.Filled.Check,
                                            contentDescription = stringResource(R.string.cd_confirm_tx),
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                    IconButton(
                                        onClick = { onConfirmSender(sender, false) },
                                        modifier = Modifier.size(48.dp),
                                    ) {
                                        Icon(
                                            Icons.Filled.Close,
                                            contentDescription = stringResource(R.string.cd_reject_tx),
                                            tint = MaterialTheme.colorScheme.error,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                items(items_, key = { it.id }) { item ->
                val selected = item.id in state.txSelection
                Surface(
                    color = if (selected) MaterialTheme.colorScheme.secondaryContainer
                    else MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { onToggle(item.id) },
                            onLongClick = { onToggle(item.id) },
                        ),
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            if (item.suspicious) {
                                Text(
                                    stringResource(R.string.tx_suspicious_warning),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                            Text(item.bankName ?: item.message.address, style = MaterialTheme.typography.titleSmall)
                            Text(
                                item.message.body,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            item.balanceRials?.let {
                                Text(
                                    stringResource(R.string.tx_balance_label, formatAmount(it, digits)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                JalaliDate.formatDayHeader(item.message.date, calendar, digits),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            // OTP copy button + feedback actions
                            val otp = if (item.subtype == BankTxParser.TxSubtype.OTP)
                                BankTxParser.extractOtp(item.message.body) else null
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (otp != null) {
                                    TextButton(onClick = {
                                        clipboard.setText(androidx.compose.ui.text.AnnotatedString(otp))
                                    }) { Text(stringResource(R.string.copy_otp)) }
                                }
                                TextButton(onClick = { onFeedback(item, true) }) {
                                    Text(stringResource(R.string.feedback_is_tx), style = MaterialTheme.typography.labelSmall)
                                }
                                TextButton(onClick = { onFeedback(item, false) }) {
                                    Text(stringResource(R.string.feedback_not_tx), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                when (item.direction) {
                                    BankTxParser.Direction.DEPOSIT -> stringResource(R.string.tx_deposit)
                                    BankTxParser.Direction.WITHDRAWAL -> stringResource(R.string.tx_withdrawal)
                                    else -> "—"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = if (item.direction == BankTxParser.Direction.DEPOSIT)
                                    MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            )
                            item.amountRials?.let {
                                Text(
                                    formatAmount(it, digits),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (item.direction == BankTxParser.Direction.DEPOSIT)
                                        MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
                }
            }
        }
        }
    }
}

private fun formatAmount(rials: Long, digits: DigitStyle): String {
    val s = "%,d".format(rials)
    return if (digits == DigitStyle.PERSIAN) s.toPersianDigits() else s
}

@Composable
private fun DateFilterButton(
    filterDate: Long?,
    calendar: CalendarMode,
    digits: DigitStyle,
    onPick: (Long) -> Unit,
    onClear: () -> Unit,
) {
    val context = LocalContext.current
    if (filterDate != null) {
        TextButton(onClick = onClear) {
            Text(JalaliDate.format(filterDate, calendar, digits))
            Icon(Icons.Default.Close, null, Modifier.size(14.dp))
        }
    } else {
        IconButton(onClick = {
            val c = java.util.Calendar.getInstance()
            android.app.DatePickerDialog(
                context,
                { _, y, m, d ->
                    val cal = java.util.Calendar.getInstance()
                    cal.set(y, m, d, 12, 0, 0)
                    onPick(cal.timeInMillis)
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

@Composable
private fun NotDefaultBanner(onEnableDefault: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth().padding(16.dp),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.banner_not_default_title),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    stringResource(R.string.banner_not_default_body),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            TextButton(onClick = onEnableDefault) {
                Text(stringResource(R.string.action_enable))
            }
        }
    }
}

@Composable
private fun FolderTabs(selected: MessageFolder, onSelect: (MessageFolder) -> Unit) {
    val folders = listOf(
        MessageFolder.INBOX to R.string.folder_all,
        MessageFolder.PERSONAL to R.string.folder_personal,
        MessageFolder.OTP to R.string.folder_otp,
        MessageFolder.TRANSACTIONAL to R.string.folder_transactional,
        MessageFolder.DISCOUNT to R.string.folder_discount,
        MessageFolder.PROMOTIONS to R.string.folder_promotions,
        MessageFolder.ARCHIVE to R.string.folder_archive,
        MessageFolder.TRASH to R.string.folder_trash,
    )
    val idx = folders.indexOfFirst { it.first == selected }.coerceAtLeast(0)
    ScrollableTabRow(selectedTabIndex = idx, edgePadding = 8.dp) {
        folders.forEach { (folder, label) ->
            Tab(
                selected = folder == selected,
                onClick = { onSelect(folder) },
                text = { Text(stringResource(label)) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ConversationRow(
    conversation: Conversation,
    selected: Boolean,
    selectionMode: Boolean,
    calendar: CalendarMode,
    digits: DigitStyle,
    swipeRightArchives: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onSwipeArchive: () -> Unit,
    onSwipeTrash: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd ->
                    if (swipeRightArchives) onSwipeArchive() else onSwipeTrash()
                SwipeToDismissBoxValue.EndToStart ->
                    if (swipeRightArchives) onSwipeTrash() else onSwipeArchive()
                SwipeToDismissBoxValue.Settled -> return@rememberSwipeToDismissBoxState false
            }
            false // keep the row; the folder change removes it from the list
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val color = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.errorContainer
            }
            Box(
                Modifier.fillMaxSize().background(color).padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Icon(Icons.Default.Email, null)
            }
        },
    ) {
        Surface(
            color = if (selected) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surface,
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Avatar(conversation.displayName, selected, conversation.photoUri)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (conversation.pinned) {
                            Icon(
                                Icons.Default.Star, null,
                                Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(
                            conversation.displayName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (conversation.unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            JalaliDate.formatListDate(conversation.lastMessageDate, calendar, digits),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            conversation.draft?.let { "draft: $it" } ?: conversation.snippet,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (conversation.maybePromo) {
                            Badge { Text(stringResource(R.string.badge_maybe_promo)) }
                            Spacer(Modifier.width(4.dp))
                        }
                        if (conversation.unreadCount > 0) {
                            Badge {
                                Text(
                                    if (digits == DigitStyle.PERSIAN)
                                        conversation.unreadCount.toString().toPersianDigits()
                                    else conversation.unreadCount.toString()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Avatar(name: String, selected: Boolean, photoUri: String?) {
    val photo = rememberContactPhoto(photoUri)
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.primaryContainer,
            ),
        contentAlignment = Alignment.Center,
    ) {
        when {
            selected -> Icon(Icons.Default.Done, null, tint = MaterialTheme.colorScheme.onPrimary)
            photo != null -> Image(
                bitmap = photo.asImageBitmap(),
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            else -> Text(
                name.firstOrNull()?.toString() ?: "#",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun rememberContactPhoto(photoUri: String?): Bitmap? {
    val context = LocalContext.current
    return produceState<Bitmap?>(null, photoUri) {
        if (photoUri != null) {
            value = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(Uri.parse(photoUri))?.use {
                        BitmapFactory.decodeStream(it)
                    }
                }.getOrNull()
            }
        }
    }.value
}
