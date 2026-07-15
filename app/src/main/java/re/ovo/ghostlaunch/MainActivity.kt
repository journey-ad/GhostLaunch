package re.ovo.ghostlaunch

import android.content.Intent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import android.os.Bundle
import re.ovo.ghostlaunch.R
import re.ovo.ghostlaunch.data.AppRepository
import re.ovo.ghostlaunch.ui.calculator.CalculatorScreen
import re.ovo.ghostlaunch.ui.calculator.CalculatorViewModel
import re.ovo.ghostlaunch.ui.manager.AppPickerScreen
import re.ovo.ghostlaunch.ui.manager.ManagerScreen
import re.ovo.ghostlaunch.ui.manager.ManagerViewModel
import re.ovo.ghostlaunch.ui.settings.SettingsScreen
import re.ovo.ghostlaunch.ui.setup.SetupScreen
import re.ovo.ghostlaunch.ui.theme.GhostLaunchTheme

enum class Screen { CALCULATOR, SETUP, MANAGER, PICKER, SETTINGS }

class MainActivity : ComponentActivity() {

    private var screenState: androidx.compose.runtime.MutableState<Screen>? = null
    private var repository: AppRepository? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repo = AppRepository(this)
        repository = repo

        setContent {
            GhostLaunchTheme {
                val context = LocalContext.current
                val screen = remember {
                    mutableStateOf(if (repo.isFirstRun()) Screen.SETUP else Screen.CALCULATOR)
                }
                screenState = screen

                when (screen.value) {
                    Screen.SETUP -> SetupScreen { pwd ->
                        repo.setDefaultPassword(pwd)
                        screen.value = Screen.CALCULATOR
                    }

                    Screen.CALCULATOR -> {
                        val vm: CalculatorViewModel = viewModel(key = "calc") {
                            CalculatorViewModel(context.applicationContext)
                        }
                        val shizukuRequestAuth = stringResource(R.string.shizuku_request_auth)
                        val toastLaunchingFmt = stringResource(R.string.toast_launching)
                        CalculatorScreen(
                            viewModel = vm,
                            onOpenManager = { screen.value = Screen.MANAGER },
                            onRequestPermission = {
                                Toast.makeText(context, shizukuRequestAuth, Toast.LENGTH_SHORT).show()
                            },
                            onToast = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() },
                            onLaunched = { app ->
                                Toast.makeText(context, toastLaunchingFmt.format(app.label), Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    Screen.MANAGER -> {
                        val vm: ManagerViewModel = viewModel(key = "mgr") {
                            ManagerViewModel(context.applicationContext)
                        }
                        ManagerScreen(
                            viewModel = vm,
                            onPickApp = { screen.value = Screen.PICKER },
                            onSettings = { screen.value = Screen.SETTINGS },
                            onBack = { screen.value = Screen.CALCULATOR }
                        )
                    }

                    Screen.PICKER -> {
                        val vm: ManagerViewModel = viewModel(key = "mgr") {
                            ManagerViewModel(context.applicationContext)
                        }
                        AppPickerScreen(
                            viewModel = vm,
                            onBack = { screen.value = Screen.MANAGER }
                        )
                    }

                    Screen.SETTINGS -> {
                        val vm: ManagerViewModel = viewModel(key = "mgr") {
                            ManagerViewModel(context.applicationContext)
                        }
                        SettingsScreen(
                            viewModel = vm,
                            onBack = { screen.value = Screen.MANAGER }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN) {
            val repo = repository
            val state = screenState
            if (repo != null && state != null && !repo.isFirstRun() && state.value != Screen.SETUP) {
                state.value = Screen.CALCULATOR
            }
        }
    }
}
