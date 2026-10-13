package com.sirmrm.messageapp.feature.promotions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sirmrm.messageapp.core.designsystem.AppListGroup
import com.sirmrm.messageapp.core.designsystem.AppPrimaryButton
import com.sirmrm.messageapp.core.designsystem.AppSectionHeader
import com.sirmrm.messageapp.core.designsystem.Hairline
import com.sirmrm.messageapp.core.designsystem.appColors
import com.sirmrm.messageapp.core.designsystem.pressable

/**
 * Smart cleanup center: review promo suggestions, confirm them in bulk,
 * and tune classifier sensitivity.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromotionsScreen(
    onBack: () -> Unit,
    viewModel: PromotionsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        containerColor = appColors.groupedBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = appColors.card),
                title = { Text(stringResource(R.string.cleanup_title), style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.primary)
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize()) {

            // Summary card
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                        .background(appColors.card)
                        .padding(16.dp),
                ) {
                    Text(
                        "${state.promoConversations.size}",
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        stringResource(R.string.promos_title),
                        style = MaterialTheme.typography.bodyMedium,
                        color = appColors.secondaryLabel,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        stringResource(R.string.sensitivity_title),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Slider(
                        value = state.settings?.sensitivity ?: 0.5f,
                        onValueChange = viewModel::setSensitivity,
                        steps = 4,
                        colors = SliderDefaults.colors(
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = appColors.fill,
                            thumbColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                    Row(Modifier.fillMaxWidth()) {
                        Text(
                            stringResource(R.string.sensitivity_low),
                            style = MaterialTheme.typography.labelSmall,
                            color = appColors.secondaryLabel,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            stringResource(R.string.sensitivity_high),
                            style = MaterialTheme.typography.labelSmall,
                            color = appColors.secondaryLabel,
                        )
                    }
                }
            }

            if (state.suggestions.isNotEmpty()) {
                item {
                    AppSectionHeader(stringResource(R.string.suggestions_title))
                }
                item {
                    AppListGroup {
                        state.suggestions.forEachIndexed { i, conv ->
                            if (i > 0) Hairline(indent = 44.dp)
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .pressable { viewModel.toggleChecked(conv.id) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                // iOS-style selection circle
                                val checked = conv.id in state.checked
                                Box(
                                    Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .then(
                                            if (checked) Modifier.background(MaterialTheme.colorScheme.primary)
                                            else Modifier.border(1.5.dp, appColors.separator, CircleShape)
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (checked) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(13.dp))
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(conv.displayName, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        conv.snippet,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = appColors.secondaryLabel,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    AppPrimaryButton(
                        text = stringResource(R.string.action_confirm_bulk, state.checked.size),
                        onClick = viewModel::confirmSelected,
                        enabled = state.checked.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                    )
                }
            }

            item {
                AppSectionHeader(stringResource(R.string.promos_title))
            }
            item {
                AppListGroup {
                    state.promoConversations.forEachIndexed { i, conv ->
                        if (i > 0) Hairline()
                        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
                            Text(conv.displayName, style = MaterialTheme.typography.titleSmall)
                            Text(
                                conv.snippet,
                                style = MaterialTheme.typography.bodySmall,
                                color = appColors.secondaryLabel,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}