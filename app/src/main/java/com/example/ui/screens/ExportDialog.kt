package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.export.CadExportManager
import com.example.export.CadExportManager.ExportFormat
import com.example.ui.CadUiState
import com.example.ui.NavalCadViewModel
import com.example.ui.theme.*

@Composable
fun ExportDialog(
    viewModel: NavalCadViewModel,
    uiState: CadUiState,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedFormat by remember { mutableStateOf(ExportFormat.DWG_DXF) }
    var fileName by remember { mutableStateOf("naval_assembly") }

    val previewText = remember(selectedFormat, uiState.activeMesh, uiState.activeMaterial, uiState.loadParams) {
        when (selectedFormat) {
            ExportFormat.DWG_DXF -> CadExportManager.generateDxf(uiState.activeMesh, uiState.activeMaterial)
            ExportFormat.STEP -> CadExportManager.generateStep(uiState.activeMesh, uiState.activeMaterial)
            ExportFormat.STL -> CadExportManager.generateStl(uiState.activeMesh)
            ExportFormat.ENGINEERING_REPORT -> CadExportManager.generateReport(
                mesh = uiState.activeMesh,
                type = uiState.machineryType,
                material = uiState.activeMaterial,
                loads = uiState.loadParams,
                compliance = uiState.compliance
            )
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = NavalSurfaceElevated,
            border = ButtonDefaults.outlinedButtonBorder.copy(
                brush = androidx.compose.ui.graphics.SolidColor(NavalBorder)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .testTag("export_dialog_surface")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = MarineCyan)
                        Text(
                            text = "Export CAD / FEA Model",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = SteelLight
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = SteelMuted)
                    }
                }

                Text(
                    text = "Select industry-standard CAD exchange format or verification report:",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.5.sp),
                    color = SteelMuted
                )

                // Format Cards Selection
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExportFormat.values().forEach { fmt ->
                        val isSelected = selectedFormat == fmt
                        Surface(
                            onClick = { selectedFormat = fmt },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) NavalSurfaceDark else NavalSurfaceElevated,
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(
                                    if (isSelected) MarineCyan else NavalBorder
                                )
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("format_card_${fmt.name}")
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedFormat = fmt },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MarineCyan,
                                        unselectedColor = SteelMuted
                                    )
                                )
                                Column {
                                    Text(
                                        text = fmt.label,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MarineCyanLight else SteelLight
                                        )
                                    )
                                    Text(
                                        text = ".${fmt.extension} file extension",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SteelMuted
                                    )
                                }
                            }
                        }
                    }
                }

                // File name input
                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text("Export File Name", style = MaterialTheme.typography.labelSmall) },
                    suffix = { Text(".${selectedFormat.extension}", style = MaterialTheme.typography.labelSmall, color = MarineCyan) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MarineCyan,
                        unfocusedBorderColor = NavalBorder
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("export_filename_input")
                )

                // Preview box
                Text("File Header Preview:", style = MaterialTheme.typography.labelSmall, color = SteelMuted)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = NavalNavyDark,
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(NavalBorder.copy(alpha = 0.5f))
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Box(modifier = Modifier.padding(8.dp).verticalScroll(rememberScrollState())) {
                        Text(
                            text = previewText.lines().take(25).joinToString("\n") + "\n...",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.5.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 13.sp
                            ),
                            color = MarineCyanLight
                        )
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SteelLight),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(NavalBorder)
                        )
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val success = CadExportManager.shareCadFile(
                                context = context,
                                format = selectedFormat,
                                content = previewText,
                                baseName = fileName.ifBlank { "navalcad_export" }
                            )
                            if (success) {
                                onDismiss()
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MarineCyan,
                            contentColor = NavalNavyDark
                        ),
                        modifier = Modifier.weight(1.5f).testTag("share_cad_file_btn")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Export & Share", style = MaterialTheme.typography.titleMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}
