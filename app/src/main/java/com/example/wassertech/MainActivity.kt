package com.example.wassertech

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wassertech.ui.AppSection
import com.example.wassertech.ui.ClientsScreen
import com.example.wassertech.ui.ClientsViewModel
import com.example.wassertech.ui.EmptyScreen
import com.example.wassertech.ui.theme.WassertechTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var darkTheme by rememberSaveable { mutableStateOf(false) }
            var section by rememberSaveable { mutableStateOf(AppSection.Clients) }

            WassertechTheme(darkTheme = darkTheme) {
                when (section) {
                    AppSection.Clients -> {
                        val vm: ClientsViewModel = viewModel()
                        ClientsScreen(
                            viewModel = vm,
                            isDarkTheme = darkTheme,
                            onToggleTheme = { darkTheme = !darkTheme },
                            onNavigateTo = { section = it } // <- из меню
                        )
                    }
                    AppSection.Empty -> {
                        EmptyScreen(
                            isDarkTheme = darkTheme,
                            onToggleTheme = { darkTheme = !darkTheme },
                            onNavigateTo = { section = it }
                        )
                    }
                }
            }
        }
    }
}
