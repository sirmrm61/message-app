package com.sirmrm.messageapp.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sirmrm.messageapp.app.R

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.onboarding_step_counter, state.step + 1, 3),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(24.dp))

        when (state.step) {
            0 -> {
                Text(
                    stringResource(R.string.onboarding_welcome_title),
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(R.string.onboarding_welcome_body),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
            }
            1 -> {
                Text(
                    stringResource(R.string.onboarding_permissions_title),
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(R.string.onboarding_permissions_body),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
            }
            2 -> {
                Text(
                    stringResource(R.string.onboarding_import_title),
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                if (state.importing) {
                    LinearProgressIndicator(
                        progress = { state.importProgress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.onboarding_import_progress, state.imported, state.total),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    Text(
                        stringResource(R.string.onboarding_import_done, state.imported),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.Center) {
            when (state.step) {
                0 -> Button(onClick = { onRequestDefault(); viewModel.next() }) {
                    Text(stringResource(R.string.onboarding_make_default))
                }
                1 -> Button(onClick = {
                    viewModel.requestPermissions()
                    viewModel.next()
                }) {
                    Text(stringResource(R.string.onboarding_grant_permissions))
                }
                2 -> {
                    if (state.importDone) {
                        Button(onClick = { viewModel.finish(); onFinished() }) {
                            Text(stringResource(R.string.onboarding_start))
                        }
                    } else {
                        Button(
                            onClick = { viewModel.startImport() },
                            enabled = !state.importing,
                        ) {
                            Text(stringResource(R.string.onboarding_import_action))
                        }
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = { viewModel.finish(); onFinished() }) {
                            Text(stringResource(R.string.action_skip))
                        }
                    }
                }
            }
        }
    }
}
