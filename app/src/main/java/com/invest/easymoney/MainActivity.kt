package com.invest.easymoney

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.invest.easymoney.ui.navigation.AppNavGraph
import com.invest.easymoney.ui.portfolio.connection.PortfolioDeepLinkHandler
import com.invest.easymoney.ui.theme.EasyMoneyTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var portfolioDeepLinks: PortfolioDeepLinkHandler

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Permission result handled silently */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()
        // Only a fresh launch may carry a callback; a recreation (e.g. rotation) must not replay it.
        if (savedInstanceState == null) handlePortfolioCallback(intent)
        setContent {
            EasyMoneyTheme {
                AppNavGraph()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handlePortfolioCallback(intent)
    }

    /** Forwards the SnapTrade portal redirect once; the handler validates scheme, host and path. */
    private fun handlePortfolioCallback(intent: Intent?) {
        val uri = intent?.data ?: return
        portfolioDeepLinks.onUri(uri)
        intent.data = null // not replayed on recreation; the URI is never logged
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
