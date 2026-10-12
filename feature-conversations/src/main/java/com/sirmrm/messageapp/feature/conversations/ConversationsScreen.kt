package com.sirmrm.messageapp.feature.conversations

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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

            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
            )

            FolderTabs(
                selected = state.folder,
                onSelect = viewModel::selectFolder,
            )

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
        MessageFolder.TRANSACTIONAL to R.string.folder_transactional,
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
                Avatar(conversation.displayName, selected)
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
private fun Avatar(name: String, selected: Boolean) {
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
        if (selected) {
            Icon(Icons.Default.Done, null, tint = MaterialTheme.colorScheme.onPrimary)
        } else {
            Text(
                name.firstOrNull()?.toString() ?: "#",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}
