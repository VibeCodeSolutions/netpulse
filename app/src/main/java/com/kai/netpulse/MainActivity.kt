package com.kai.netpulse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.kai.netpulse.ui.NetPulseScreen
import com.kai.netpulse.ui.theme.NetPulseTheme

class MainActivity : ComponentActivity() {

    private val viewModel: NetworkViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NetPulseTheme {
                NetPulseScreen(viewModel = viewModel)
            }
        }
    }
}
