package com.sirmrm.messageapp.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.os.LocaleListCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sirmrm.messageapp.app.sms.SmsRoleHelper
import com.sirmrm.messageapp.app.ui.OnboardingScreen
import com.sirmrm.messageapp.core.ui.theme.MessageAppTheme
import com.sirmrm.messageapp.domain.model.AppLanguage
import com.sirmrm.messageapp.domain.repository.SettingsRepository
import com.sirmrm.messageapp.feature.chat.ChatScreen
import com.sirmrm.messageapp.feature.conversations.ConversationsScreen
import com.sirmrm.messageapp.feature.promotions.PromotionsScreen
import com.sirmrm.messageapp.feature.settings.SettingsScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var settingsRepository: SettingsRepository

    private val requestDefaultSms =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            recreate()
        }

    private var smsAlertReceiver: android.content.BroadcastReceiver? = null
    private val snackbarHostState = androidx.compose.material3.SnackbarHostState()

    override fun onResume() {
        super.onResume()
        com.sirmrm.messageapp.app.MessageApp.isInForeground = true
        // In-app alert for SMS arriving while the UI is showing.
        val r = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: android.content.Context, intent: android.content.Intent) {
                val sender = intent.getStringExtra(com.sirmrm.messageapp.app.ui.InAppAlerts.EXTRA_SENDER) ?: return
                val body = intent.getStringExtra(com.sirmrm.messageapp.app.ui.InAppAlerts.EXTRA_BODY) ?: ""
                com.sirmrm.messageapp.app.ui.InAppAlerts.playAlert(context, alertSoundIndex)
                scope.launch {
                    snackbarHostState.showSnackbar(
                        "${context.getString(R.string.sms_from, sender)}: ${body.take(100)}",
                        withDismissAction = true,
                        duration = androidx.compose.material3.SnackbarDuration.Long,
                    )
                }
            }
        }
        smsAlertReceiver = r
        val filter = android.content.IntentFilter(com.sirmrm.messageapp.app.ui.InAppAlerts.ACTION_INCOMING_SMS)
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            registerReceiver(r, filter, android.content.Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION") registerReceiver(r, filter)
        }
    }

    override fun onPause() {
        super.onPause()
        com.sirmrm.messageapp.app.MessageApp.isInForeground = false
        smsAlertReceiver?.let { runCatching { unregisterReceiver(it) } }
        smsAlertReceiver = null
    }

    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main)
    private var alertSoundIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Apply persisted per-app language before first composition.
        runBlocking {
            val s = settingsRepository.settings.first()
            val current = AppCompatDelegate.getApplicationLocales().toLanguageTags()
            if (current != s.language.tag) {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(s.language.tag))
            }
            alertSoundIndex = s.alertSoundIndex
            if (s.flagSecure) {
                window.setFlags(
                    WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE,
                )
            }
        }

        val startThreadId = intent.getLongExtra(EXTRA_THREAD_ID, -1L)
        val composeAddress = intent.getStringExtra(EXTRA_COMPOSE_ADDRESS)
        val composeBody = intent.getStringExtra(EXTRA_COMPOSE_BODY)

        setContent {
            val settings by settingsRepository.settings
                .collectAsState(initial = null)

            val s = settings ?: return@setContent

            MessageAppTheme(themeMode = s.theme, language = s.language) {
                androidx.compose.foundation.layout.Box {
                androidx.compose.material3.SnackbarHost(
                    snackbarHostState,
                    modifier = androidx.compose.ui.Modifier.align(androidx.compose.ui.Alignment.BottomCenter),
                )
                val nav = rememberNavController()
                val start = when {
                    !s.onboardingDone -> "onboarding"
                    startThreadId > 0 -> "chat/$startThreadId"
                    else -> "conversations"
                }

                NavHost(navController = nav, startDestination = start) {
                    composable("onboarding") {
                        OnboardingScreen(
                            onRequestDefault = {
                                requestDefaultSms.launch(SmsRoleHelper.changeDefaultIntent(this@MainActivity))
                            },
                            onFinished = {
                                nav.navigate("conversations") {
                                    popUpTo("onboarding") { inclusive = true }
                                }
                            },
                        )
                    }
                    composable("conversations") {
                        ConversationsScreen(
                            isDefaultSms = SmsRoleHelper.isDefaultSmsApp(this@MainActivity),
                            onEnableDefault = {
                                requestDefaultSms.launch(SmsRoleHelper.changeDefaultIntent(this@MainActivity))
                            },
                            onOpenChat = { threadId, _ -> nav.navigate("chat/$threadId") },
                            onOpenPromotions = { nav.navigate("promotions") },
                            onOpenSettings = { nav.navigate("settings") },
                            onNewMessage = { nav.navigate("compose") },
                        )
                    }
                    composable(
                        "chat/{threadId}",
                        arguments = listOf(navArgument("threadId") { type = NavType.LongType }),
                    ) { entry ->
                        ChatScreen(
                            threadId = entry.arguments?.getLong("threadId") ?: 0L,
                            onBack = { nav.popBackStack() },
                        )
                    }
                    composable("compose") {
                        ChatScreen(
                            threadId = -1L,
                            composeAddress = composeAddress ?: "",
                            composeBody = composeBody ?: "",
                            onBack = { nav.popBackStack() },
                        )
                    }
                    composable("promotions") {
                        PromotionsScreen(onBack = { nav.popBackStack() })
                    }
                    composable("settings") {
                        SettingsScreen(onBack = { nav.popBackStack() })
                    }
                }
                }
            }
        }
    }

    companion object {
        const val EXTRA_THREAD_ID = "extra_thread_id"
        const val EXTRA_COMPOSE_ADDRESS = "extra_compose_address"
        const val EXTRA_COMPOSE_BODY = "extra_compose_body"
    }
}
