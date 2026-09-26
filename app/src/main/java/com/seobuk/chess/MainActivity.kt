package com.seobuk.chess

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.seobuk.chess.ui.ChessApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // SystemBarStyle.auto: bar icons follow light/dark mode
        setContent { ChessApp() }
    }
}
