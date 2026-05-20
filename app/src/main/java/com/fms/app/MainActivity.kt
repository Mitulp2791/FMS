package com.fms.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fms.app.data.UserSession
import com.fms.app.ui.theme.FMSTheme
import com.fms.app.ui.theme.LoginScreen
import com.fms.app.ui.theme.LoginViewModel
import com.fms.app.ui.theme.MainAppScreen
import com.fms.app.ui.theme.MasterAdminScreen

enum class AppState {
    LOGGED_OUT,
    LOGGED_IN
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FMSTheme {
                // Initialize top-level routing state
                var currentAppState by remember { mutableStateOf(AppState.LOGGED_OUT) }

                // Instantiate the ViewModel
                val loginViewModel: LoginViewModel = viewModel()

                when (currentAppState) {
                    AppState.LOGGED_OUT -> {
                        // Render the 3-field login gate
                        LoginScreen(
                            onLoginClick = { companyCode, email, password ->
                                loginViewModel.performCompanySecureLogin(companyCode, email, password) { role ->
                                    currentAppState = AppState.LOGGED_IN
                                }
                            }
                        )

                        if (loginViewModel.errorMessage != null) {
                            Toast.makeText(this@MainActivity, loginViewModel.errorMessage, Toast.LENGTH_SHORT).show()
                            loginViewModel.clearError()
                        }
                    }
                    AppState.LOGGED_IN -> {
                        // THIS fixes the warning! The screen is now officially used in the app.
                        if (UserSession.role == "MasterAdmin") {
                            MasterAdminScreen()
                        } else {
                            MainAppScreen()
                        }
                    }
                }
            }
        }
    }
}