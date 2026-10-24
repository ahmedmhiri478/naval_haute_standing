package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.cloud.*
import com.example.ui.CadUiState
import com.example.ui.NavalCadViewModel
import com.example.ui.theme.*

@Composable
fun TeamCollaborationScreen(
    viewModel: NavalCadViewModel,
    uiState: CadUiState,
    modifier: Modifier = Modifier
) {
    val project = uiState.currentProject
    var showProjectMenu by remember { mutableStateOf(false) }
    var newCommentText by remember { mutableStateOf("") }
    var newCommentLocation by remember { mutableStateOf("Blade Root Fillet") }
    var newCommentStatus by remember { mutableStateOf(ReviewStatus.APPROVED) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(NavalNavyDark)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Active Cloud Project & Sync Status Card
        item {
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = NavalSurfaceElevated),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NavalBorder)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("team_project_card")
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Active Marine Project",
                                style = MaterialTheme.typography.labelSmall,
                                color = SteelMuted
                            )
                            Text(
                                text = project.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MarineCyanLight
                            )
                            Text(
                                text = "${project.vesselType} | ${project.classificationSociety}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.5.sp),
                                color = SteelLight
                            )
                        }

                        // Project Switcher Button
                        Box {
                            OutlinedButton(
                                onClick = { showProjectMenu = true },
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MarineCyan),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(NavalBorder)
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("switch_project_btn")
                            ) {
                                Text("Switch", style = MaterialTheme.typography.labelSmall)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                            }

                            DropdownMenu(
                                expanded = showProjectMenu,
                                onDismissRequest = { showProjectMenu = false },
                                modifier = Modifier.background(NavalSurfaceDark)
                            ) {
                                uiState.allProjects.forEach { proj ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(proj.name, style = MaterialTheme.typography.bodyMedium, color = SteelLight)
                                                Text(proj.vesselType, style = MaterialTheme.typography.labelSmall, color = SteelMuted)
                                            }
                                        },
                                        onClick = {
                                            viewModel.selectProject(proj)
                                            showProjectMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Divider(color = NavalBorder.copy(alpha = 0.5f), thickness = 0.8.dp)

                    // Cloud Sync Action Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (project.syncState == SyncState.SYNCED) FeaGoodGreen else MarineGold)
                            )
                            Text(
                                text = "${project.syncState.label} (${project.lastSyncTime})",
                                style = MaterialTheme.typography.labelSmall,
                                color = SteelMuted
                            )
                        }

                        Button(
                            onClick = { viewModel.triggerCloudSync() },
                            colors = ButtonDefaults.buttonColors(containerColor = MarineCyan, contentColor = NavalNavyDark),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("cloud_sync_now_btn")
                        ) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Sync Now", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        }

        // Active Team Engineers Roster
        item {
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = NavalSurfaceDark),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NavalBorder)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Engineering Collaboration Team",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp),
                        color = SteelLight
                    )

                    project.activeMembers.forEach { member ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(contentAlignment = Alignment.BottomEnd) {
                                Surface(
                                    shape = CircleShape,
                                    color = MarineCyan.copy(alpha = 0.15f),
                                    border = ButtonDefaults.outlinedButtonBorder.copy(
                                        brush = androidx.compose.ui.graphics.SolidColor(MarineCyan.copy(alpha = 0.5f))
                                    ),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = member.avatarInitials,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MarineCyanLight
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (member.isOnline) FeaGoodGreen else SteelMuted)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(member.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = SteelLight)
                                Text("${member.role} • ${member.organization}", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = SteelMuted)
                            }
                        }
                    }
                }
            }
        }

        // Add Review Comment Box
        item {
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = NavalSurfaceDark),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NavalBorder)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Post Engineering Review & 3D Annotation",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp),
                        color = MarineCyanLight
                    )

                    OutlinedTextField(
                        value = newCommentText,
                        onValueChange = { newCommentText = it },
                        placeholder = { Text("Add engineering observation, rule feedback, or design note...", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.5.sp), color = SteelMuted) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("comment_input_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MarineCyan,
                            unfocusedBorderColor = NavalBorder,
                            focusedContainerColor = NavalSurfaceElevated,
                            unfocusedContainerColor = NavalSurfaceElevated
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newCommentLocation,
                            onValueChange = { newCommentLocation = it },
                            label = { Text("Part Location", style = MaterialTheme.typography.labelSmall) },
                            singleLine = true,
                            modifier = Modifier.width(180.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MarineCyan,
                                unfocusedBorderColor = NavalBorder
                            )
                        )

                        Button(
                            onClick = {
                                viewModel.addReviewComment(newCommentText, newCommentLocation, newCommentStatus)
                                newCommentText = ""
                            },
                            enabled = newCommentText.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = MarineCyan, contentColor = NavalNavyDark),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.testTag("submit_comment_btn")
                        ) {
                            Text("Post Comment", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        // Engineering Review Comments List
        item {
            Text(
                text = "Review Comments & Anchored Annotations (${project.comments.size})",
                style = MaterialTheme.typography.titleMedium,
                color = SteelLight
            )
        }

        items(project.comments) { c ->
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = NavalSurfaceElevated),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NavalBorder)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${c.author.name} (${c.author.role})",
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                            color = MarineCyanLight
                        )
                        Surface(
                            color = if (c.status == ReviewStatus.APPROVED) FeaGoodGreen.copy(alpha = 0.15f) else MarineGold.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = c.status.label,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = if (c.status == ReviewStatus.APPROVED) FeaGoodGreen else MarineGold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = "Anchor: ${c.partLocation} • ${c.timestamp}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = SteelMuted
                    )

                    Text(
                        text = c.text,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                        color = SteelLight
                    )
                }
            }
        }

        // Project Revision History Timeline
        item {
            Text(
                text = "Engineering Revision History",
                style = MaterialTheme.typography.titleMedium,
                color = SteelLight
            )
        }

        items(project.revisions) { rev ->
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = NavalSurfaceDark),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NavalBorder)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MarineCyan.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = rev.revisionTag,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MarineCyan,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = rev.commitMessage,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                            color = SteelLight
                        )
                        Text(
                            text = "${rev.authorName} • ${rev.timestamp} • commit ${rev.hash}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = SteelMuted
                        )
                    }
                }
            }
        }
    }
}
