package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.ExpenseViewModel
import com.example.ui.SmartKharchaApp
import com.example.ui.theme.SmartKharchaTheme

class MainActivity : ComponentActivity() {

    private val expenseViewModel: ExpenseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartKharchaTheme {
                SmartKharchaApp(viewModel = expenseViewModel)
            }
        }
    }
}
