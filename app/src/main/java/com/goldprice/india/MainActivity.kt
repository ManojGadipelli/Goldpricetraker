package com.goldprice.india

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.goldprice.india.ui.GoldPriceScreen
import com.goldprice.india.ui.theme.GoldPriceIndiaTheme

class MainActivity : ComponentActivity() {

    private val viewModel: GoldViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GoldPriceIndiaTheme {
                GoldPriceScreen(viewModel = viewModel)
            }
        }
    }
}
