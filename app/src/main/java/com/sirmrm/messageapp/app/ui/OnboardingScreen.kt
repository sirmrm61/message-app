package com.sirmrm.messageapp.app.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sirmrm.messageapp.app.R
import com.sirmrm.messageapp.core.designsystem.AppPrimaryButton
import com.sirmrm.messageapp.core.designsystem.appColors

/**
 * 3-step onboarding:
 *  1) Why the app must become the default SMS handler
 *  2) Runtime permissions
 *  3) Importing existing messages with a progress bar
 */
@Composable
fun OnboardingScreen(
    onRequestDefault: () -> Unit,
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { /* result handled by the system; steps continue regardless */ }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(appColors.groupedBackground)
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Page icon + dots
        val icon = when (state.step) {
            0 -> Icons.Outlined.MailOutline
            1 -> Icons.Outlined.Notifications
            else -> Icons.Outlined.Sync
        }
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(28.dp))

        when (state.step) {
            0 -> {
                Text(
                    stringResource(R.string.onboarding_welcome_title),
                    style = MaterialTheme.typography.displayMedium,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    stringResource(R.string.onboarding_welcome_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = appColors.secondaryLabel,
                    textAlign = TextAlign.Center,
                )
            }
            1 -> {
                Text(
                    stringResource(R.string.onboarding_permissions_title),
                    style = MaterialTheme.typography.displayMedium,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    stringResource(R.string.onboarding_permissions_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = appColors.secondaryLabel,
                    textAlign = TextAlign.Center,
                )
            }
            2 -> {
                Text(
                    stringResource(R.string.onboarding_import_title),
                    style = MaterialTheme.typography.displayMedium,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                if (state.importing) {
                    LinearProgressIndicator(
                        progress = { state.importProgress },
                        modifier = Modifier.fillMaxWidth().clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = appColors.fill,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.onboarding_import_progress, state.imported, state.total),
                        style = MaterialTheme.typography.bodyMedium,
                        color = appColors.secondaryLabel,
                    )
                } else {
                    Text(
                        stringResource(R.string.onboarding_import_done, state.imported),
                        style = MaterialTheme.typography.bodyLarge,
                        color = appColors.secondaryLabel,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        Spacer(Modifier.height(36.dp))
        when (state.step) {
            0 -> AppPrimaryButton(
                text = stringResource(R.string.onboarding_make_default),
                onClick = { onRequestDefault(); viewModel.next() },
                modifier = Modifier.fillMaxWidth(),
            )
            1 -> AppPrimaryButton(
                text = stringResource(R.string.onboarding_grant_permissions),
                onClick = {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.READ_CONTACTS,
                            Manifest.permission.POST_NOTIFICATIONS,
                        )
                    )
                    viewModel.next()
                },
                modifier = Modifier.fillMaxWidth(),
            )
            2 -> {
                if (state.importDone) {
                    AppPrimaryButton(
                        text = stringResource(R.string.onboarding_start),
                        onClick = { viewModel.finish(); onFinished() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    AppPrimaryButton(
                        text = stringResource(R.string.onboarding_import_action),
                        onClick = { viewModel.startImport() },
                        enabled = !state.importing,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { viewModel.finish(); onFinished() }) {
                        Text(stringResource(R.string.action_skip), color = appColors.secondaryLabel)
                    }
                }
            }
        }

        Spacer(Modifier.height(28.dp))
        // Page dots
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            repeat(3) { i ->
                Box(
                    Modifier
                        .size(if (i == state.step) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (i == state.step) MaterialTheme.colorScheme.primary
                            else appColors.separator,
                        ),
                )
            }
        }
    }
}
