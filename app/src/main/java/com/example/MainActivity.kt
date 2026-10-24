package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cloud.SyncState
import com.example.ui.NavalCadViewModel
import com.example.ui.screens.*
import com.example.ui.theme.*

enum class NavalNavDestination(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    CAD_VIEWPORT("3D CAD", Icons.Default.ViewInAr),
    FEA_STRESS("FEA Stress", Icons.Default.ShowChart),
    PYTHON_CONSOLE("Python REPL", Icons.Default.Terminal),
    TEAM_CLOUD("Team Sync", Icons.Default.CloudSync)
}

class MainActivity : ComponentActivity() {

    private val viewModel: NavalCadViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                NavalCadApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavalCadApp(viewModel: NavalCadViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentNav by remember { mutableStateOf(NavalNavDestination.CAD_VIEWPORT) }

    // Android Back Handler: return to CAD Viewport if in other tabs
    BackHandler(enabled = currentNav != NavalNavDestination.CAD_VIEWPORT) {
        currentNav = NavalNavDestination.CAD_VIEWPORT
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = NavalNavyDark,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MarineCyan.copy(alpha = 0.2f),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(MarineCyan)
                            ),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.PrecisionManufacturing,
                                    contentDescription = "NavalCAD Logo",
                                    tint = MarineCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = "NavalCAD",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = SteelLight
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (uiState.currentProject.syncState == SyncState.SYNCED) FeaGoodGreen else MarineGold)
                                )
                                Text(
                                    text = uiState.currentProject.name,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = SteelMuted
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Export Button
                    IconButton(
                        onClick = { viewModel.setExportDialogOpen(true) },
                        modifier = Modifier.testTag("top_bar_export_button")
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Export DWG/STEP",
                            tint = MarineCyan
                        )
                    }

                    // Cloud Sync Trigger
                    IconButton(
                        onClick = { viewModel.triggerCloudSync() },
                        modifier = Modifier.testTag("top_bar_sync_button")
                    ) {
                        Icon(
                            Icons.Default.CloudSync,
                            contentDescription = "Sync Cloud",
                            tint = if (uiState.currentProject.syncState == SyncState.SYNCED) FeaGoodGreen else MarineGold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NavalSurfaceDark,
                    titleContentColor = SteelLight,
                    actionIconContentColor = MarineCyan
                ),
                windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = NavalSurfaceDark,
                contentColor = SteelLight,
                tonalElevation = 8.dp,
                windowInsets = WindowInsets.navigationBars,
                modifier = Modifier.testTag("bottom_navigation_bar")
            ) {
                NavalNavDestination.values().forEach { destination ->
                    val selected = currentNav == destination
                    NavigationBarItem(
                        selected = selected,
                        onClick = { currentNav = destination },
                        icon = {
                            Icon(
                                destination.icon,
                                contentDescription = destination.title,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = {
                            Text(
                                text = destination.title,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NavalNavyDark,
                            selectedTextColor = MarineCyan,
                            indicatorColor = MarineCyan,
                            unselectedIconColor = SteelMuted,
                            unselectedTextColor = SteelMuted
                        ),
                        modifier = Modifier.testTag("nav_item_${destination.name}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentNav) {
                NavalNavDestination.CAD_VIEWPORT -> CadScreen(viewModel = viewModel, uiState = uiState)
                NavalNavDestination.FEA_STRESS -> FeaScreen(viewModel = viewModel, uiState = uiState)
                NavalNavDestination.PYTHON_CONSOLE -> PythonConsoleScreen(viewModel = viewModel, uiState = uiState)
                NavalNavDestination.TEAM_CLOUD -> TeamCollaborationScreen(viewModel = viewModel, uiState = uiState)
            }

            // Notification / Status Banner
            AnimatedVisibility(
                visible = uiState.statusBanner != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 10.dp, start = 14.dp, end = 14.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = NavalSurfaceElevated.copy(alpha = 0.95f),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(MarineCyan.copy(alpha = 0.8f))
                    ),
                    modifier = Modifier
                        .clickable { viewModel.dismissBanner() }
                        .testTag("status_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MarineCyan, modifier = Modifier.size(16.dp))
                        Text(
                            text = uiState.statusBanner ?: "",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.5.sp),
                            color = SteelLight,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = SteelMuted, modifier = Modifier.size(14.dp))
                    }
                }
            }

            // Export Dialog
            if (uiState.isExportDialogOpen) {
                ExportDialog(
                    viewModel = viewModel,
                    uiState = uiState,
                    onDismiss = { viewModel.setExportDialogOpen(false) }
                )
            }
        }
    }
}
