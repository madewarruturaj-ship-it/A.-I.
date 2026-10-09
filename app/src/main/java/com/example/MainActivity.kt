package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.screens.AssistantScreen
import com.example.ui.screens.CorrectionsScreen
import com.example.ui.screens.NotebookScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TaskLibraryScreen
import com.example.ui.theme.DoBlue
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TeachGreen
import com.example.ui.viewmodel.TeachingViewModel
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

    private val viewModel: TeachingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainAppScaffold(viewModel = viewModel)
            }
        }
    }
}

enum class NavigationTab(val title: String) {
    ASSISTANT("Assistant"),
    NOTEBOOK("Notebook"),
    TASKS("Tasks"),
    MEMORY("Memory"),
    SETTINGS("Settings")
}

@Composable
fun MainAppScaffold(viewModel: TeachingViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Audio Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.onMicrophoneClick()
        } else {
            Toast.makeText(
                context,
                "Microphone permission is required for voice commands. You can type using text as well.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Intercept back button if on secondary screen
    BackHandler(enabled = selectedTab != 0) {
        selectedTab = 0
    }

    // Observe Snackbar notifications from ViewModel
    LaunchedEffect(Unit) {
        viewModel.snackBarEvent.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.navigationBars,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                // Tab 0: Assistant
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(
                            if (selectedTab == 0) Icons.Filled.Chat else Icons.Outlined.Chat,
                            contentDescription = "Assistant Tab"
                        )
                    },
                    label = { Text("Assistant", fontSize = 11.sp, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(indicatorColor = TeachGreen.copy(alpha = 0.2f)),
                    modifier = Modifier.testTag("nav_assistant")
                )

                // Tab 1: Notebook
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(
                            if (selectedTab == 1) Icons.Filled.MenuBook else Icons.Outlined.MenuBook,
                            contentDescription = "Notebook Tab"
                        )
                    },
                    label = { Text("Notebook", fontSize = 11.sp, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(indicatorColor = TeachGreen.copy(alpha = 0.2f)),
                    modifier = Modifier.testTag("nav_notebook")
                )

                // Tab 2: Task Library
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        Icon(
                            if (selectedTab == 2) Icons.Filled.Bolt else Icons.Outlined.Bolt,
                            contentDescription = "Tasks Tab"
                        )
                    },
                    label = { Text("Tasks", fontSize = 11.sp, fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(indicatorColor = DoBlue.copy(alpha = 0.2f)),
                    modifier = Modifier.testTag("nav_tasks")
                )

                // Tab 3: Corrections & Memory
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = {
                        Icon(
                            if (selectedTab == 3) Icons.Filled.Security else Icons.Outlined.Security,
                            contentDescription = "Memory Tab"
                        )
                    },
                    label = { Text("Memory", fontSize = 11.sp, fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    modifier = Modifier.testTag("nav_memory")
                )

                // Tab 4: Settings & Info
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = {
                        Icon(
                            if (selectedTab == 4) Icons.Filled.Settings else Icons.Outlined.Settings,
                            contentDescription = "Settings Tab"
                        )
                    },
                    label = { Text("Settings", fontSize = 11.sp, fontWeight = if (selectedTab == 4) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    modifier = Modifier.testTag("nav_settings")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> AssistantScreen(viewModel = viewModel)
                1 -> NotebookScreen(viewModel = viewModel)
                2 -> TaskLibraryScreen(viewModel = viewModel)
                3 -> CorrectionsScreen(viewModel = viewModel)
                4 -> SettingsScreen(viewModel = viewModel)
            }
        }
    }
}
