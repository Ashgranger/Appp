package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.BrowserRootScreen
import com.example.ui.theme.SwiftWebTheme
import com.example.viewmodel.ActiveOverlay
import com.example.viewmodel.BrowserViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: BrowserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIncomingIntent(intent)

        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val isIncognito = state.activeTab?.isIncognito == true ||
                (state.activeOverlay == ActiveOverlay.TAB_SWITCHER && state.tabSwitcherIncognitoSegment)

            SwiftWebTheme(
                themeMode = state.preferences.themeMode,
                useDynamicColor = state.preferences.useDynamicColor,
                accentPreset = state.preferences.accentPreset,
                isIncognito = isIncognito
            ) {
                BrowserRootScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    override fun onStop() {
        super.onStop()
        if (isFinishing) {
            viewModel.handleClearOnExitIfNeeded()
        }
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            val incomingUrl = intent.dataString
            if (!incomingUrl.isNullOrBlank()) {
                viewModel.navigateActiveTab(incomingUrl)
            }
        }
    }
}
