package com.sirmrm.messageapp.feature.conversations

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.border
import androidx.hilt.navigation.compose.hiltViewModel
import com.sirmrm.messageapp.classifier.BankTxParser
import com.sirmrm.messageapp.core.designsystem.AppBadge
import com.sirmrm.messageapp.core.designsystem.AppAvatar
import com.sirmrm.messageapp.core.designsystem.AppEmptyState
import com.sirmrm.messageapp.core.designsystem.AppSearchBar
import com.sirmrm.messageapp.core.designsystem.Hairline
import com.sirmrm.messageapp.core.designsystem.UnreadDot
import com.sirmrm.messageapp.core.designsystem.appColors
import com.sirmrm.messageapp.core.designsystem.pressable
import com.sirmrm.messageapp.core.util.JalaliDate
import com.sirmrm.messageapp.core.util.PersianDigits.toPersianDigits
import com.sirmrm.messageapp.domain.model.CalendarMode
import com.sirmrm.messageapp.domain.model.Conversation
import com.sirmrm.messageapp.domain.model.DigitStyle
import com.sirmrm.messageapp.domain.model.MessageFolder

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = appColors.groupedBackground,
        topBar = {
            if (state.selectionMode) {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = appColors.card),
                    title = { Text("${state.selection.size}", style = MaterialTheme.typography.titleMedium) },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, stringResource(R.string.cd_confirm_tx))
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.selectAll() }) {
                            Icon(Icons.Default.CheckCircle, stringResource(R.string.action_select_all), tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { viewModel.markSelectedRead() }) {
                            Icon(Icons.Default.Done, stringResource(R.string.action_mark_read), tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { viewModel.archiveSelected() }) {
                            Icon(Icons.Outlined.Archive, stringResource(R.string.action_archive), tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { viewModel.trashSelected() }) {
                            Icon(Icons.Outlined.Delete, stringResource(R.string.action_delete), tint = MaterialTheme.colorScheme.error)
                        }
                    },
                )
            } else {
                LargeTopAppBar(
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = appColors.groupedBackground,
                        scrolledContainerColor = appColors.groupedBackground,
                    ),
                    title = { Text(stringResource(R.string.app_title)) },
                    actions = {
                        IconButton(onClick = onOpenPromotions) {
                            Icon(Icons.Outlined.CleaningServices, stringResource(R.string.action_cleanup), tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Outlined.Settings, stringResource(R.string.action_settings), tint = MaterialTheme.colorScheme.primary)
                        }
                        if (isDefaultSms) {
                            IconButton(onClick = onNewMessage) {
                                Icon(Icons.Outlined.Create, stringResource(R.string.action_new_message), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    },
                )
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
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppSearchBar(
                    value = state.query,
                    onValueChange = viewModel::setQuery,
                    hint = stringResource(R.string.search_hint),
                    modifier = Modifier.weight(1f),
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
                if (state.conversations.isEmpty()) {
                    AppEmptyState(
                        icon = Icons.AutoMirrored.Outlined.VolumeUp,
                        title = stringResource(R.string.app_title),
                        description = "",
                    )
                }
                LazyColumn(Modifier.fillMaxSize()) {
                    items(state.conversations, key = { it.id }) { conv ->
                        ConversationRow(
                            conversation = conv,
                            selected = conv.id in state.selection,
                            selectionMode = state.selectionMode,
                            calendar = state.settings?.calendar ?: CalendarMode.JALALI,
                            digits = state.settings?.digits ?: DigitStyle.PERSIAN,
                            swipeRightArchives = state.settings?.swipeRightArchives ?: true,
                            modifier = Modifier.animateItem(),
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

@OptIn(ExperimentalFoundationApi::class)
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
        // Month summary card + subtype chips
        TxSummaryHeader(state, digits, onMonthlyToggle, onSelectAll, onSubtypeFilter)

        Box(Modifier.weight(1f)) {
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
        val grouped = remember(tx) { tx.groupBy { it.message.address } }
        LazyColumn(Modifier.fillMaxSize()) {
            // «Maybe transactional» suggestions — confirm to move them in.
            if (state.maybeTransactions.isNotEmpty()) {
                item(key = "maybe_header") {
                    Text(
                        stringResource(R.string.tx_maybe_header),
                        style = MaterialTheme.typography.labelMedium,
                        color = appColors.warning,
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
                            color = appColors.secondaryLabel,
                        )
                        TextButton(onClick = { onFeedback(item, true) }) {
                            Text(stringResource(R.string.feedback_is_tx))
                        }
                    }
                    Hairline()
                }
            }
            grouped.forEach { (sender, items_) ->
                val senderKey =
                    com.sirmrm.messageapp.classifier.SenderKey.of(sender).ifBlank { sender.trim() }
                val needsConfirm = senderKey !in state.txSenderDecisions
                stickyHeader {
                    Surface(color = appColors.groupedBackground) {
                        Column {
                            Text(
                                sender,
                                style = MaterialTheme.typography.labelMedium,
                                color = appColors.secondaryLabel,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                            )
                            if (needsConfirm) {
                                // «Is this sender transactional?» thin confirm card.
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 2.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(appColors.card)
                                        .padding(horizontal = 8.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        stringResource(R.string.tx_confirm_sender),
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.weight(1f).padding(start = 8.dp),
                                        color = appColors.secondaryLabel,
                                    )
                                    IconButton(
                                        onClick = { onConfirmSender(sender, true) },
                                        modifier = Modifier.size(48.dp),
                                    ) {
                                        Icon(
                                            Icons.Filled.Check,
                                            contentDescription = stringResource(R.string.cd_confirm_tx),
                                            tint = appColors.deposit,
                                        )
                                    }
                                    IconButton(
                                        onClick = { onConfirmSender(sender, false) },
                                        modifier = Modifier.size(48.dp),
                                    ) {
                                        Icon(
                                            Icons.Filled.Close,
                                            contentDescription = stringResource(R.string.cd_reject_tx),
                                            tint = appColors.withdrawal,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                items(items_, key = { it.id }) { item ->
                    TxCard(
                        item = item,
                        selected = item.id in state.txSelection,
                        calendar = calendar,
                        digits = digits,
                        otp = if (item.subtype == BankTxParser.TxSubtype.OTP)
                            BankTxParser.extractOtp(item.message.body) else null,
                        onToggle = { onToggle(item.id) },
                        onCopyOtp = { otp ->
                            clipboard.setText(androidx.compose.ui.text.AnnotatedString(otp))
                        },
                        onFeedback = { isTx -> onFeedback(item, isTx) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
        }
    }
}

/** Month summary + subtype filter strip for the transactions tab. */
@Composable
private fun TxSummaryHeader(
    state: ConversationsUiState,
    digits: DigitStyle,
    onMonthlyToggle: () -> Unit,
    onSelectAll: () -> Unit,
    onSubtypeFilter: (BankTxParser.TxSubtype?) -> Unit,
) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(appColors.card)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.tx_counts, state.depositCount, state.withdrawalCount),
                    style = MaterialTheme.typography.labelMedium,
                    color = appColors.secondaryLabel,
                )
                if (state.showMonthlySums) {
                    val (dep, wdr) = state.monthlySums(System.currentTimeMillis())
                    Text(
                        formatAmount(dep, digits),
                        style = MaterialTheme.typography.titleMedium,
                        color = appColors.deposit,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        formatAmount(wdr, digits),
                        style = MaterialTheme.typography.titleMedium,
                        color = appColors.withdrawal,
                        fontWeight = FontWeight.Bold,
                    )
                    MiniTxBar(dep, wdr)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                TextButton(onClick = onMonthlyToggle) { Text(stringResource(R.string.tx_monthly)) }
                TextButton(onClick = onSelectAll) { Text(stringResource(R.string.action_select_all)) }
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
                color = appColors.secondaryLabel,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        // Subtype filters: quiet text tabs.
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 4.dp)) {
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
                val sel = state.txSubtypeFilter == sub
                Text(
                    stringResource(label),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (sel) MaterialTheme.colorScheme.primary else appColors.secondaryLabel,
                    modifier = Modifier
                        .pressable { onSubtypeFilter(if (sel) null else sub) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
    }
}

/** Tiny two-bar deposit/withdrawal chart drawn with Canvas — offline, no lib. */
@Composable
private fun MiniTxBar(dep: Long, wdr: Long) {
    val depC = appColors.deposit
    val wdrC = appColors.withdrawal
    val max = maxOf(dep, wdr).coerceAtLeast(1)
    Canvas(Modifier.padding(top = 8.dp).fillMaxWidth().height(8.dp)) {
        val h = size.height / 2 - 1.dp.toPx()
        val w1 = size.width * dep / max
        val w2 = size.width * wdr / max
        drawRoundRect(depC, Offset(0f, 0f), Size(w1, h), CornerRadius(h / 2))
        drawRoundRect(wdrC, Offset(0f, h + 2.dp.toPx()), Size(w2, h), CornerRadius(h / 2))
    }
}

/** Minimal transaction card: big green/red amount, bank, balance, Jalali date. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TxCard(
    item: BankTxItem,
    selected: Boolean,
    calendar: CalendarMode,
    digits: DigitStyle,
    otp: String?,
    onToggle: () -> Unit,
    onCopyOtp: (String) -> Unit,
    onFeedback: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else appColors.card)
            .combinedClickable(onClick = onToggle, onLongClick = onToggle)
            .padding(14.dp),
    ) {
        if (item.suspicious) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Warning, null, tint = appColors.warning, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    stringResource(R.string.tx_suspicious_warning),
                    style = MaterialTheme.typography.labelSmall,
                    color = appColors.warning,
                )
            }
            Spacer(Modifier.height(4.dp))
        }
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(
                    item.bankName ?: item.message.address,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    item.message.body,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = appColors.secondaryLabel,
                )
                item.balanceRials?.let {
                    Text(
                        stringResource(R.string.tx_balance_label, formatAmount(it, digits)),
                        style = MaterialTheme.typography.labelSmall,
                        color = appColors.secondaryLabel,
                    )
                }
                Text(
                    JalaliDate.formatDayHeader(item.message.date, calendar, digits),
                    style = MaterialTheme.typography.labelSmall,
                    color = appColors.tertiaryLabel,
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.End) {
                item.amountRials?.let {
                    Text(
                        formatAmount(it, digits),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (item.direction == BankTxParser.Direction.DEPOSIT)
                            appColors.deposit else appColors.withdrawal,
                    )
                }
                Text(
                    when (item.direction) {
                        BankTxParser.Direction.DEPOSIT -> stringResource(R.string.tx_deposit)
                        BankTxParser.Direction.WITHDRAWAL -> stringResource(R.string.tx_withdrawal)
                        else -> "—"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = appColors.secondaryLabel,
                )
            }
        }
        // OTP copy + feedback actions
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (otp != null) {
                TextButton(onClick = { onCopyOtp(otp) }) { Text(stringResource(R.string.copy_otp)) }
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { onFeedback(true) }) {
                Text(stringResource(R.string.feedback_is_tx), style = MaterialTheme.typography.labelSmall)
            }
            TextButton(onClick = { onFeedback(false) }) {
                Text(
                    stringResource(R.string.feedback_not_tx),
                    style = MaterialTheme.typography.labelSmall,
                    color = appColors.secondaryLabel,
                )
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
    var showJalaliPicker by remember { mutableStateOf(false) }
    if (filterDate != null) {
        TextButton(onClick = onClear) {
            Text(JalaliDate.format(filterDate, calendar, digits))
            Icon(Icons.Default.Close, null, Modifier.size(14.dp))
        }
    } else {
        IconButton(onClick = {
            if (calendar == CalendarMode.JALALI) {
                showJalaliPicker = true
            } else {
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
            }
        }) {
            Icon(Icons.Default.DateRange, stringResource(R.string.action_filter_date), tint = appColors.secondaryLabel)
        }
    }
    if (showJalaliPicker) {
        JalaliDatePickerDialog(
            digits = digits,
            onDismiss = { showJalaliPicker = false },
            onPick = { showJalaliPicker = false; onPick(it) },
        )
    }
}

/** Jalali wheel picker: year / month-name / day columns. */
@Composable
private fun JalaliDatePickerDialog(
    digits: DigitStyle,
    onDismiss: () -> Unit,
    onPick: (Long) -> Unit,
) {
    val today = JalaliDate.gregorianToJalali(System.currentTimeMillis())
    var year by remember { mutableStateOf(today.year) }
    var month by remember { mutableStateOf(today.month) }
    var day by remember { mutableStateOf(today.day) }
    val monthDays = JalaliDate.jalaliMonthDays(year, month)
    if (day > monthDays) day = monthDays

    fun num(n: Int) = if (digits == DigitStyle.PERSIAN) n.toString().toPersianDigits() else n.toString()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.filter_pick_date)) },
        text = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                PickerColumn(
                    items = ((today.year - 10)..(today.year + 1)).toList(),
                    selected = year,
                    label = { num(it) },
                    onSelect = { year = it },
                )
                PickerColumn(
                    items = (1..12).toList(),
                    selected = month,
                    label = { JalaliDate.jalaliMonthName(it) },
                    onSelect = { month = it },
                )
                PickerColumn(
                    items = (1..monthDays).toList(),
                    selected = day.coerceIn(1, monthDays),
                    label = { num(it) },
                    onSelect = { day = it },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onPick(JalaliDate.jalaliToGregorianMillis(year, month, day.coerceIn(1, monthDays)))
            }) { Text(stringResource(R.string.action_select)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun <T> PickerColumn(
    items: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    LazyColumn(
        Modifier
            .height(160.dp)
            .width(88.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        items(items.size) { i ->
            val it = items[i]
            val sel = it == selected
            Text(
                label(it),
                style = if (sel) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                color = if (sel) MaterialTheme.colorScheme.primary else appColors.secondaryLabel,
                modifier = Modifier
                    .fillMaxWidth()
                    .pressable { onSelect(it) }
                    .padding(vertical = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun NotDefaultBanner(onEnableDefault: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(appColors.warning.copy(alpha = 0.12f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Warning, null, tint = appColors.warning)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.banner_not_default_title),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                stringResource(R.string.banner_not_default_body),
                style = MaterialTheme.typography.bodySmall,
                color = appColors.secondaryLabel,
            )
        }
        TextButton(onClick = onEnableDefault) {
            Text(stringResource(R.string.action_enable))
        }
    }
}

/**
 * Quiet tab strip: text labels with a thin moving underline — iOS-like instead
 * of colored chips.
 */
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
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        folders.forEach { (folder, label) ->
            val sel = folder == selected
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.pressable { onSelect(folder) },
            ) {
                Text(
                    stringResource(label),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                    color = if (sel) MaterialTheme.colorScheme.onSurface else appColors.secondaryLabel,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
                Box(
                    Modifier
                        .width(if (sel) 20.dp else 0.dp)
                        .height(2.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(if (sel) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent),
                )
            }
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
    modifier: Modifier = Modifier,
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
        modifier = modifier,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val archiveSwipe = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> swipeRightArchives
                else -> !swipeRightArchives
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(if (archiveSwipe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                    .padding(horizontal = 24.dp),
                contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd)
                    Alignment.CenterStart else Alignment.CenterEnd,
            ) {
                Icon(
                    if (archiveSwipe) Icons.Outlined.Archive else Icons.Outlined.Delete,
                    null,
                    tint = androidx.compose.ui.graphics.Color.White,
                )
            }
        },
    ) {
        Surface(color = appColors.card) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (selectionMode) {
                    Box(
                        Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .then(
                                if (selected) Modifier.background(MaterialTheme.colorScheme.primary)
                                else Modifier.border(1.5.dp, appColors.separator, CircleShape)
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) Icon(Icons.Filled.Check, null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(14.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                }
                AppAvatar(
                    conversation.displayName,
                    photo = conversation.photoUri?.let { uri ->
                        { ContactPhoto(uri, conversation.displayName) }
                    },
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (conversation.unreadCount > 0) {
                            UnreadDot()
                            Spacer(Modifier.width(6.dp))
                        }
                        if (conversation.pinned) {
                            Icon(
                                Icons.Outlined.Star, null,
                                Modifier.size(13.dp),
                                tint = appColors.warning,
                            )
                            Spacer(Modifier.width(3.dp))
                        }
                        Text(
                            conversation.displayName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (conversation.unreadCount > 0) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            JalaliDate.formatListDate(conversation.lastMessageDate, calendar, digits),
                            style = MaterialTheme.typography.labelSmall,
                            color = appColors.tertiaryLabel,
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            conversation.draft?.let { "draft: $it" } ?: conversation.snippet,
                            style = MaterialTheme.typography.bodySmall,
                            color = appColors.secondaryLabel,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (conversation.maybePromo) {
                            AppBadge(stringResource(R.string.badge_maybe_promo), tint = appColors.warning)
                            Spacer(Modifier.width(4.dp))
                        }
                        if (conversation.unreadCount > 0) {
                            AppBadge(
                                if (digits == DigitStyle.PERSIAN)
                                    conversation.unreadCount.toString().toPersianDigits()
                                else conversation.unreadCount.toString(),
                            )
                        }
                    }
                }
            }
            Hairline(indent = 74.dp)
        }
    }
}

@Composable
private fun ContactPhoto(photoUri: String, name: String) {
    val photo = rememberContactPhoto(photoUri)
    if (photo != null) {
        Image(
            bitmap = photo.asImageBitmap(),
            contentDescription = name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().clip(CircleShape),
        )
    } else {
        Text(
            name.firstOrNull()?.toString() ?: "#",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
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
