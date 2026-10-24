package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.CadUiState
import com.example.ui.NavalCadViewModel
import com.example.ui.theme.*

@Composable
fun FeaScreen(
    viewModel: NavalCadViewModel,
    uiState: CadUiState,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val compliance = uiState.compliance
    val loads = uiState.loadParams

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NavalNavyDark)
            .verticalScroll(scrollState)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Classification Society Compliance Card
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = NavalSurfaceElevated),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(
                    if (compliance.isPassed) FeaGoodGreen.copy(alpha = 0.6f) else FeaCriticalRed.copy(alpha = 0.6f)
                )
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("fea_compliance_card")
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (compliance.isPassed) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (compliance.isPassed) FeaGoodGreen else FeaCriticalRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (compliance.isPassed) "RULE COMPLIANT" else "EXCEEDS ALLOWABLE LIMIT",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (compliance.isPassed) FeaGoodGreen else FeaCriticalRed
                        )
                    }
                    Surface(
                        color = (if (compliance.isPassed) FeaGoodGreen else FeaCriticalRed).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "SF ${"%.2f".format(compliance.calculatedSafetyFactor)}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (compliance.isPassed) FeaGoodGreen else FeaCriticalRed,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = compliance.standardName,
                    style = MaterialTheme.typography.bodyMedium.copy(color = MarineCyanLight, fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = compliance.ruleClause,
                    style = MaterialTheme.typography.labelSmall,
                    color = SteelMuted
                )
                Text(
                    text = compliance.remarks,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.5.sp),
                    color = SteelLight
                )

                Divider(color = NavalBorder.copy(alpha = 0.5f), thickness = 0.8.dp)

                // Key Numerical Comparisons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Peak Von Mises", style = MaterialTheme.typography.labelSmall, color = SteelMuted)
                        Text("${"%.1f".format(compliance.peakCalculatedStressMPa)} MPa", style = MaterialTheme.typography.titleMedium, color = SteelLight)
                    }
                    Column {
                        Text("Rule Limit", style = MaterialTheme.typography.labelSmall, color = SteelMuted)
                        Text("${"%.1f".format(compliance.allowableStressMPa)} MPa", style = MaterialTheme.typography.titleMedium, color = SteelMuted)
                    }
                    Column {
                        Text("Min Required SF", style = MaterialTheme.typography.labelSmall, color = SteelMuted)
                        Text("${"%.2f".format(compliance.minRequiredSafetyFactor)}", style = MaterialTheme.typography.titleMedium, color = SteelLight)
                    }
                }
            }
        }

        // Von Mises Colormap Spectrum Legend
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = NavalSurfaceDark),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NavalBorder)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Von Mises Stress Colormap (MPa)",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 12.sp),
                    color = MarineCyanLight
                )

                // Color gradient bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                listOf(
                                    FeaSafeBlue,
                                    FeaLowCyan,
                                    FeaGoodGreen,
                                    FeaMedYellow,
                                    FeaHighOrange,
                                    FeaCriticalRed
                                )
                            ),
                            shape = RoundedCornerShape(4.dp)
                        )
                )

                // Scale ticks
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("0.0", style = MaterialTheme.typography.labelSmall, color = SteelMuted)
                    Text("${"%.0f".format(uiState.activeMesh.maxStressMPa * 0.25f)}", style = MaterialTheme.typography.labelSmall, color = SteelMuted)
                    Text("${"%.0f".format(uiState.activeMesh.maxStressMPa * 0.5f)}", style = MaterialTheme.typography.labelSmall, color = SteelMuted)
                    Text("${"%.0f".format(uiState.activeMesh.maxStressMPa * 0.75f)}", style = MaterialTheme.typography.labelSmall, color = SteelMuted)
                    Text("${"%.0f".format(uiState.activeMesh.maxStressMPa)} MPa", style = MaterialTheme.typography.labelSmall, color = FeaCriticalRed)
                }
            }
        }

        // Real-Time Marine Loading Conditions
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = NavalSurfaceDark),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NavalBorder)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Boundary Loads & Environmental Conditions",
                    style = MaterialTheme.typography.titleMedium,
                    color = SteelLight
                )

                // Shaft RPM Slider
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Shaft Speed (RPM)", style = MaterialTheme.typography.bodyMedium, color = SteelLight)
                        Text("${"%.0f".format(loads.shaftRpm)} RPM", style = MaterialTheme.typography.labelSmall, color = MarineCyan)
                    }
                    Slider(
                        value = loads.shaftRpm,
                        onValueChange = { viewModel.updateLoads(loads.copy(shaftRpm = it)) },
                        valueRange = 60f..300f,
                        colors = SliderDefaults.colors(thumbColor = MarineCyan, activeTrackColor = MarineCyan)
                    )
                }

                // Delivered Power Slider
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Engine Power (kW)", style = MaterialTheme.typography.bodyMedium, color = SteelLight)
                        Text("${"%.0f".format(loads.enginePowerKw)} kW (${"%.1f".format(loads.torqueKnm)} kNm)", style = MaterialTheme.typography.labelSmall, color = MarineGold)
                    }
                    Slider(
                        value = loads.enginePowerKw,
                        onValueChange = { viewModel.updateLoads(loads.copy(enginePowerKw = it)) },
                        valueRange = 2000f..20000f,
                        colors = SliderDefaults.colors(thumbColor = MarineGold, activeTrackColor = MarineGold)
                    )
                }

                // Propeller Hydrodynamic Thrust
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Hydrodynamic Thrust (kN)", style = MaterialTheme.typography.bodyMedium, color = SteelLight)
                        Text("${"%.0f".format(loads.thrustKn)} kN", style = MaterialTheme.typography.labelSmall, color = MarineCyan)
                    }
                    Slider(
                        value = loads.thrustKn,
                        onValueChange = { viewModel.updateLoads(loads.copy(thrustKn = it)) },
                        valueRange = 100f..1500f,
                        colors = SliderDefaults.colors(thumbColor = MarineCyan, activeTrackColor = MarineCyan)
                    )
                }

                // Peak Cylinder Pressure
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Peak Firing Pressure (bar)", style = MaterialTheme.typography.bodyMedium, color = SteelLight)
                        Text("${"%.0f".format(loads.peakPressureBar)} bar (${loads.peakPressureBar * 0.1f} MPa)", style = MaterialTheme.typography.labelSmall, color = FeaMedYellow)
                    }
                    Slider(
                        value = loads.peakPressureBar,
                        onValueChange = { viewModel.updateLoads(loads.copy(peakPressureBar = it)) },
                        valueRange = 80f..220f,
                        colors = SliderDefaults.colors(thumbColor = FeaMedYellow, activeTrackColor = FeaMedYellow)
                    )
                }

                // Hydrostatic Immersion Head
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Seawater Hydrostatic Head", style = MaterialTheme.typography.bodyMedium, color = SteelLight)
                        Text("${"%.1f".format(loads.hydrostaticHeadM)} m Head", style = MaterialTheme.typography.labelSmall, color = MarineCyanLight)
                    }
                    Slider(
                        value = loads.hydrostaticHeadM,
                        onValueChange = { viewModel.updateLoads(loads.copy(hydrostaticHeadM = it)) },
                        valueRange = 2f..25f,
                        colors = SliderDefaults.colors(thumbColor = MarineCyanLight, activeTrackColor = MarineCyanLight)
                    )
                }

                // Elastic Deflection Magnification
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Elastic Deflection Magnification", style = MaterialTheme.typography.bodyMedium, color = SteelLight)
                        Text("${"%.0f".format(loads.deformationMagnification)}x", style = MaterialTheme.typography.labelSmall, color = MarineBronze)
                    }
                    Slider(
                        value = loads.deformationMagnification,
                        onValueChange = { viewModel.updateLoads(loads.copy(deformationMagnification = it)) },
                        valueRange = 1f..100f,
                        colors = SliderDefaults.colors(thumbColor = MarineBronze, activeTrackColor = MarineBronze)
                    )
                }

                Button(
                    onClick = { viewModel.runFeaAnalysis() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("run_fea_simulation_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = MarineCyan, contentColor = NavalNavyDark),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Re-Run Finite Element Solver", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}
