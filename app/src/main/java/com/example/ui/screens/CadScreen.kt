package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.CadUiState
import com.example.ui.NavalCadViewModel
import com.example.ui.theme.*
import com.example.viewport.CadViewport3D

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CadScreen(
    viewModel: NavalCadViewModel,
    uiState: CadUiState,
    modifier: Modifier = Modifier
) {
    var showMaterialMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NavalNavyDark)
    ) {
        // Machinery Category Selector Carousel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(NavalSurfaceDark)
                .padding(vertical = 8.dp, horizontal = 12.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NavalMachineryType.values().forEach { type ->
                val isSelected = uiState.machineryType == type
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.setMachineryType(type) },
                    label = {
                        Text(
                            text = type.title,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 11.5.sp,
                                color = if (isSelected) NavalNavyDark else SteelLight
                            )
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MarineCyan,
                        containerColor = NavalSurfaceElevated
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = if (isSelected) MarineCyan else NavalBorder
                    ),
                    modifier = Modifier.testTag("chip_${type.name}")
                )
            }
        }

        // Subheader: Material Selector & Quick Engineering Stats
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(NavalSurfaceElevated)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Material dropdown trigger
            Box {
                Surface(
                    onClick = { showMaterialMenu = true },
                    shape = RoundedCornerShape(6.dp),
                    color = NavalSurfaceDark,
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(NavalBorder)),
                    modifier = Modifier.testTag("material_selector_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(uiState.activeMaterial.baseColor)
                        )
                        Text(
                            text = uiState.activeMaterial.displayName,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                            color = SteelLight
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select Material",
                            tint = MarineCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = showMaterialMenu,
                    onDismissRequest = { showMaterialMenu = false },
                    modifier = Modifier.background(NavalSurfaceDark)
                ) {
                    MarineMaterial.values().forEach { mat ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = mat.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = SteelLight
                                    )
                                    Text(
                                        text = "${mat.standardGrade} | Yield: ${mat.yieldStrengthMPa} MPa",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SteelMuted
                                    )
                                }
                            },
                            onClick = {
                                viewModel.setMaterial(mat)
                                showMaterialMenu = false
                            },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(mat.baseColor, RoundedCornerShape(2.dp))
                                )
                            }
                        )
                    }
                }
            }

            // Mass & Classification Rule info
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Mass: ${"%.1f".format(uiState.activeMesh.massKg)} kg",
                    style = MaterialTheme.typography.labelSmall,
                    color = MarineGold
                )
                Text(
                    text = "Peak: ${"%.1f".format(uiState.activeMesh.maxStressMPa)} MPa",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (uiState.compliance.isPassed) FeaGoodGreen else FeaCriticalRed
                )
            }
        }

        // Main 3D Viewport Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            CadViewport3D(
                mesh = uiState.activeMesh,
                material = uiState.activeMaterial,
                shadingMode = uiState.shadingMode,
                cutawayPlanePos = uiState.cutawayPlanePos,
                isProbeModeActive = uiState.isProbeModeActive,
                onProbeSelected = { pt, node ->
                    viewModel.selectProbePoint(pt, node)
                },
                modifier = Modifier.fillMaxSize()
            )

            // Shading Mode Floating Selector (Top Right)
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CadShadingMode.values().forEach { mode ->
                    val isModeSelected = uiState.shadingMode == mode
                    Surface(
                        onClick = { viewModel.setShadingMode(mode) },
                        shape = RoundedCornerShape(6.dp),
                        color = if (isModeSelected) MarineCyan else NavalSurfaceElevated.copy(alpha = 0.9f),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(if (isModeSelected) MarineCyan else NavalBorder)
                        ),
                        modifier = Modifier.testTag("shading_mode_${mode.name}")
                    ) {
                        Text(
                            text = mode.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                color = if (isModeSelected) NavalNavyDark else SteelLight
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            // Cross-Section Cutaway Plane Slider (Bottom Overlay when Cutaway active)
            if (uiState.shadingMode == CadShadingMode.CROSS_SECTION) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(0.92f)
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = NavalSurfaceElevated.copy(alpha = 0.95f),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(NavalBorder))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Cross-Section Cutaway Plane",
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 11.5.sp),
                                color = MarineCyanLight
                            )
                            Text(
                                text = "Z: ${"%.1f".format(uiState.cutawayPlanePos * 100)}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = SteelMuted
                            )
                        }
                        Slider(
                            value = uiState.cutawayPlanePos,
                            onValueChange = { viewModel.setCutawayPlanePos(it) },
                            valueRange = -1.0f..1.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = MarineCyan,
                                activeTrackColor = MarineCyan,
                                inactiveTrackColor = NavalBorder
                            ),
                            modifier = Modifier.testTag("cutaway_slider")
                        )
                    }
                }
            }

            // Quick Floating Action Controls (Bottom Right: Recalculate & Export)
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SmallFloatingActionButton(
                    onClick = { viewModel.runFeaAnalysis() },
                    containerColor = MarineCyan,
                    contentColor = NavalNavyDark,
                    modifier = Modifier.testTag("solve_fea_fab")
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = "Solve FEA", modifier = Modifier.size(18.dp))
                }
                SmallFloatingActionButton(
                    onClick = { viewModel.setExportDialogOpen(true) },
                    containerColor = MarineBronze,
                    contentColor = NavalNavyDark,
                    modifier = Modifier.testTag("open_export_fab")
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Export DWG/STEP", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
