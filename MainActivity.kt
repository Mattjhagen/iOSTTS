package com.metroreader.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.metroreader.app.data.repository.UserPreferencesRepository
import com.metroreader.app.parser.BookImportManager
import com.metroreader.app.ui.navigation.MetroReaderNavHost
import com.metroreader.app.ui.theme.MetroReaderTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var prefsRepository: UserPreferencesRepository

    @Inject
    lateinit var importManager: BookImportManager

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before super.onCreate
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        // Handle open-with / share intents
        handleIncomingIntent(intent)

        setContent {
            val isDark by prefsRepository.isDarkTheme.collectAsState(initial = isSystemInDarkTheme())

            MetroReaderTheme(darkTheme = isDark) {
                MetroReaderNavHost()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_VIEW -> {
                val uri = intent.data ?: return
                val mimeType = intent.type
                importBookFromUri(uri, mimeType)
            }
            Intent.ACTION_SEND -> {
                val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM) ?: return
                importBookFromUri(uri, intent.type)
            }
        }
    }

    private fun importBookFromUri(uri: Uri, mimeType: String?) {
        lifecycleScope.launch {
            importManager.importFromUri(uri, mimeType)
        }
    }
}
