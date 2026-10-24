package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.CadUiState
import com.example.ui.NavalCadViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun PythonConsoleScreen(
    viewModel: NavalCadViewModel,
    uiState: CadUiState,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Interactive Terminal, 1: Script Library

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NavalNavyDark)
    ) {
        // Mode Tabs: Terminal vs Script Library
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = NavalSurfaceDark,
            contentColor = MarineCyan,
            divider = {}
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Python REPL Terminal", style = MaterialTheme.typography.titleMedium.copy(fontSize = 12.sp)) },
                icon = { Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.testTag("tab_repl_terminal")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Engineering Scripts (${viewModel.pythonEngine.preloadedScripts.size})", style = MaterialTheme.typography.titleMedium.copy(fontSize = 12.sp)) },
                icon = { Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.testTag("tab_script_library")
            )
        }

        if (selectedTab == 0) {
            // Interactive REPL Terminal View
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // Quick Snippet Insertion Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NavalSurfaceElevated)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Snippets:", style = MaterialTheme.typography.labelSmall, color = SteelMuted)

                    val quickSnippets = listOf(
                        "cad.propeller(blades=5, diameter=2600)",
                        "cad.shaft(length=5000, dia=400)",
                        "cad.cylinder_head(bore=500)",
                        "sim.run_fea()",
                        "export.dwg('prop.dwg')",
                        "export.step('assembly.stp')",
                        "material.set('bronze')"
                    )

                    quickSnippets.forEach { snippet ->
                        Surface(
                            onClick = {
                                viewModel.onCommandInputChange(snippet)
                            },
                            shape = RoundedCornerShape(4.dp),
                            color = NavalSurfaceDark,
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(NavalBorder)
                            )
                        ) {
                            Text(
                                text = snippet,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontFamily = FontFamily.Monospace),
                                color = MarineCyanLight,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.clearConsole() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Clear", tint = SteelMuted, modifier = Modifier.size(14.dp))
                    }
                }

                // Terminal Output History
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(NavalViewportBg)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(uiState.consoleOutputs) { out ->
                        val textColor = when {
                            out.isError -> FeaCriticalRed
                            out.isSuccess -> FeaGoodGreen
                            out.isCommand -> MarineGold
                            else -> SteelLight
                        }
                        Text(
                            text = out.text,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 16.sp
                            ),
                            color = textColor
                        )
                    }
                }

                // Command Input Line
                Surface(
                    color = NavalSurfaceElevated,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = ">>> ",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MarineCyan,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        )

                        OutlinedTextField(
                            value = uiState.commandInput,
                            onValueChange = { viewModel.onCommandInputChange(it) },
                            placeholder = {
                                Text(
                                    "Enter Python command or expression...",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = SteelMuted
                                )
                            },
                            textStyle = MaterialTheme.typography.labelSmall.copy(
                                color = SteelLight,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = {
                                viewModel.submitConsoleCommand()
                                coroutineScope.launch {
                                    if (uiState.consoleOutputs.isNotEmpty()) {
                                        listState.animateScrollToItem(uiState.consoleOutputs.size - 1)
                                    }
                                }
                            }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MarineCyan,
                                unfocusedBorderColor = NavalBorder,
                                focusedContainerColor = NavalSurfaceDark,
                                unfocusedContainerColor = NavalSurfaceDark
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("console_command_input")
                        )

                        Spacer(Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                viewModel.submitConsoleCommand()
                                coroutineScope.launch {
                                    if (uiState.consoleOutputs.isNotEmpty()) {
                                        listState.animateScrollToItem(uiState.consoleOutputs.size - 1)
                                    }
                                }
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MarineCyan,
                                contentColor = NavalNavyDark
                            ),
                            modifier = Modifier.testTag("console_run_btn")
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Run", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        } else {
            // Preloaded Naval Engineering Scripts Library
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(viewModel.pythonEngine.preloadedScripts) { script ->
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = NavalSurfaceDark),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NavalBorder)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("script_card_${script.id}")
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = script.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MarineCyanLight
                                )
                                Button(
                                    onClick = {
                                        viewModel.loadAndExecuteScript(script)
                                        selectedTab = 0 // Switch to REPL to see output
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MarineCyan, contentColor = NavalNavyDark),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("run_script_${script.id}")
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Load & Run", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp))
                                }
                            }

                            Text(
                                text = script.description,
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                color = SteelMuted
                            )

                            // Code preview box
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = NavalNavyDark,
                                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(NavalBorder.copy(alpha = 0.5f))),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = script.code.lines().take(6).joinToString("\n") + "\n...",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        lineHeight = 14.sp
                                    ),
                                    color = SteelLight.copy(alpha = 0.8f),
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
