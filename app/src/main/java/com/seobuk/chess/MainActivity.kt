package com.seobuk.chess

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.seobuk.chess.ui.AppViewModel
import com.seobuk.chess.ui.ChessApp
import com.seobuk.chess.ui.update.Updater

class MainActivity : ComponentActivity() {
    private val app: AppViewModel by viewModels() // the same instance ChessApp's viewModel() gets

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // SystemBarStyle.auto: bar icons follow light/dark mode
        if (savedInstanceState == null) handle(intent)
        setContent { ChessApp() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    /** The PackageInstaller result (see [Updater]) or, in debug builds, the "updateUrl" test hook. */
    private fun handle(intent: Intent) {
        if (intent.action == Updater.ACTION_INSTALL) {
            app.updater.onInstallStatus(intent)?.let(::startActivity)
        } else if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            intent.getStringExtra("updateUrl")?.let { app.updater.debugUrl = it }
        }
    }
}
