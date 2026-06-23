package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AppDatabase
import com.example.data.CalculatorRepository
import com.example.ui.CalculatorViewModel
import com.example.ui.CalculatorViewModelFactory
import com.example.ui.ChemDoseApp
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Let's instantiate Room Database and the Repository
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = CalculatorRepository(database.slideDao())
        
        // Instantiate the CalculatorViewModel with the repository
        val viewModel: CalculatorViewModel by viewModels {
            CalculatorViewModelFactory(repository, application)
        }
        
        enableEdgeToEdge()
        setContent {
            val followSystemTheme by viewModel.followSystemTheme.collectAsStateWithLifecycle()
            val manualDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
            val systemInDark = isSystemInDarkTheme()
            val isThemeDark = if (followSystemTheme) systemInDark else manualDarkMode

            MyApplicationTheme(darkTheme = isThemeDark) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Main app container
                    ChemDoseApp(viewModel)
                }
            }
        }
    }
}
