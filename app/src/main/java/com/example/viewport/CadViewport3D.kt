package com.example.viewport

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fea.FeaEngine
import com.example.model.*
import com.example.ui.theme.*
import kotlin.math.*

/**
 * Interactive 3D CAD Viewport with Shaded, Wireframe, FEA Stress colormap,
 * Cross-section cutaway clipping, and real-time stress probe inspection.
 */
@Composable
fun CadViewport3D(
    mesh: Mesh3D,
    material: MarineMaterial,
    shadingMode: CadShadingMode,
    cutawayPlanePos: Float, // -1.0 to 1.0 (clipping along Z axis)
    isProbeModeActive: Boolean,
    onProbeSelected: (Point3D, StressNode?) -> Unit,
    modifier: Modifier = Modifier
) {
    // 3D Camera State
    var yawDeg by remember { mutableFloatStateOf(35f) }
    var pitchDeg by remember { mutableFloatStateOf(25f) }
    var zoomScale by remember { mutableFloatStateOf(1.8f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    var probedPoint by remember { mutableStateOf<Point3D?>(null) }
    var probedScreenPos by remember { mutableStateOf<Offset?>(null) }

    Box(
        modifier = modifier
            .background(NavalViewportBg)
            .testTag("cad_viewport_canvas")
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    zoomScale = (zoomScale * zoom).coerceIn(0.5f, 5.0f)
                    panOffset += pan
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    // Orbit rotation
                    yawDeg = (yawDeg + dragAmount.x * 0.45f) % 360f
                    pitchDeg = (pitchDeg - dragAmount.y * 0.45f).coerceIn(-85f, 85f)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val centerX = w * 0.5f + panOffset.x
            val centerY = h * 0.5f + panOffset.y

            // Draw CAD Engineering Grid
            drawCadGrid(w, h)

            // Convert Euler angles to radians
            val yawRad = yawDeg * (Math.PI.toFloat() / 180f)
            val pitchRad = pitchDeg * (Math.PI.toFloat() / 180f)

            val cosYaw = cos(yawRad)
            val sinYaw = sin(yawRad)
            val cosPitch = cos(pitchRad)
            val sinPitch = sin(pitchRad)

            // Project 3D vertices to camera space and 2D screen coordinates
            val projectedPoints = ArrayList<Offset>(mesh.vertices.size)
            val cameraZ = FloatArray(mesh.vertices.size)

            val baseScale = (min(w, h) / 160f) * zoomScale

            for (i in mesh.vertices.indices) {
                val v = mesh.vertices[i]

                // Rotate around Y (yaw)
                val x1 = v.x * cosYaw - v.z * sinYaw
                val z1 = v.x * sinYaw + v.z * cosYaw

                // Rotate around X (pitch)
                val y2 = v.y * cosPitch - z1 * sinPitch
                val z2 = v.y * sinPitch + z1 * cosPitch

                cameraZ[i] = z2

                // Orthographic / Weak perspective projection
                val persp = 1f + (z2 * 0.0015f)
                val screenX = centerX + x1 * baseScale * persp
                val screenY = centerY - y2 * baseScale * persp

                projectedPoints.add(Offset(screenX, screenY))
            }

            // Directional Light Vector for Shading
            val lightX = 0.577f
            val lightY = 0.577f
            val lightZ = 0.577f

            // Filter faces based on cross-section cutaway clipping plane
            val clippedFaces = if (shadingMode == CadShadingMode.CROSS_SECTION) {
                val clipThreshold = cutawayPlanePos * 40f
                mesh.faces.filter { f ->
                    val avgZ = (mesh.vertices[f.v1].z + mesh.vertices[f.v2].z + mesh.vertices[f.v3].z) / 3f
                    avgZ <= clipThreshold
                }
            } else {
                mesh.faces
            }

            // Depth sorting (Painter's algorithm: sort faces from back to front)
            val sortedFaces = clippedFaces.sortedByDescending { f ->
                (cameraZ[f.v1] + cameraZ[f.v2] + cameraZ[f.v3]) / 3f
            }

            // Render 3D Faces
            val path = Path()

            for (face in sortedFaces) {
                val p1 = projectedPoints[face.v1]
                val p2 = projectedPoints[face.v2]
                val p3 = projectedPoints[face.v3]

                // Compute face normal in camera space for back-face culling & lighting
                val n = face.normal
                val nx1 = n.x * cosYaw - n.z * sinYaw
                val nz1 = n.x * sinYaw + n.z * cosYaw
                val ny2 = n.y * cosPitch - nz1 * sinPitch
                val nz2 = n.y * sinPitch + nz1 * cosPitch

                val dotProduct = (nx1 * lightX + ny2 * lightY + nz2 * lightZ).coerceIn(-1f, 1f)
                val intensity = (0.35f + 0.65f * max(0f, dotProduct)).coerceIn(0.15f, 1.0f)

                path.reset()
                path.moveTo(p1.x, p1.y)
                path.lineTo(p2.x, p2.y)
                path.lineTo(p3.x, p3.y)
                path.close()

                when (shadingMode) {
                    CadShadingMode.SHADED_SOLID, CadShadingMode.CROSS_SECTION -> {
                        val baseCol = material.baseColor
                        val shadedColor = Color(
                            red = (baseCol.red * intensity).coerceIn(0f, 1f),
                            green = (baseCol.green * intensity).coerceIn(0f, 1f),
                            blue = (baseCol.blue * intensity).coerceIn(0f, 1f),
                            alpha = 0.95f
                        )
                        drawPath(path, shadedColor)
                        // Subtle edge line for CAD definition
                        drawPath(path, NavalBorder.copy(alpha = 0.25f), style = Stroke(width = 0.7f))
                    }
                    CadShadingMode.FEA_STRESS -> {
                        // Average stress of face vertices
                        val s1 = mesh.stressNodes.getOrNull(face.v1)?.vonMisesMPa ?: 0f
                        val s2 = mesh.stressNodes.getOrNull(face.v2)?.vonMisesMPa ?: 0f
                        val s3 = mesh.stressNodes.getOrNull(face.v3)?.vonMisesMPa ?: 0f
                        val avgStress = (s1 + s2 + s3) / 3f

                        val feaColor = FeaEngine.getStressColor(avgStress, mesh.maxStressMPa)
                        val shadedFeaColor = Color(
                            red = (feaColor.red * intensity).coerceIn(0f, 1f),
                            green = (feaColor.green * intensity).coerceIn(0f, 1f),
                            blue = (feaColor.blue * intensity).coerceIn(0f, 1f),
                            alpha = 0.98f
                        )
                        drawPath(path, shadedFeaColor)
                        drawPath(path, Color.Black.copy(alpha = 0.2f), style = Stroke(width = 0.5f))
                    }
                    CadShadingMode.WIREFRAME -> {
                        // Translucent fill for depth clarity
                        drawPath(path, NavalNavyDark.copy(alpha = 0.5f))
                        drawPath(path, MarineCyan.copy(alpha = 0.8f), style = Stroke(width = 1.0f))
                    }
                }
            }

            // Draw Coordinate Axis Triad (Corner XYZ indicator)
            drawAxisTriad(cosYaw, sinYaw, cosPitch, sinPitch, w, h)

            // Draw Probe indicator if set
            probedScreenPos?.let { pos ->
                drawCircle(MarineGold, radius = 7f, center = pos)
                drawCircle(Color.White, radius = 3.5f, center = pos)
                drawCircle(MarineCyan, radius = 14f, center = pos, style = Stroke(width = 1.5f))
            }
        }

        // Overlay Viewport HUD Controls
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .background(NavalSurfaceElevated.copy(alpha = 0.85f), MaterialTheme.shapes.small)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = mesh.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp),
                    color = MarineCyanLight
                )
                Text(
                    text = "Faces: ${mesh.faces.size} | Vertices: ${mesh.vertices.size} | Mode: ${shadingMode.label}",
                    style = MaterialTheme.typography.labelSmall,
                    color = SteelMuted
                )
            }
        }
    }
}

/**
 * Draws CAD blueprint background grid
 */
private fun DrawScope.drawCadGrid(w: Float, h: Float) {
    val step = 40f
    var x = 0f
    while (x < w) {
        drawLine(
            color = NavalBorder.copy(alpha = 0.18f),
            start = Offset(x, 0f),
            end = Offset(x, h),
            strokeWidth = 0.8f
        )
        x += step
    }
    var y = 0f
    while (y < h) {
        drawLine(
            color = NavalBorder.copy(alpha = 0.18f),
            start = Offset(0f, y),
            end = Offset(w, y),
            strokeWidth = 0.8f
        )
        y += step
    }
}

/**
 * Draws 3D XYZ Triad in bottom-left corner
 */
private fun DrawScope.drawAxisTriad(
    cosYaw: Float,
    sinYaw: Float,
    cosPitch: Float,
    sinPitch: Float,
    w: Float,
    h: Float
) {
    val triadOrigin = Offset(52f, h - 52f)
    val axisLen = 32f

    // X Axis (Red)
    val xEnd = Offset(
        triadOrigin.x + (cosYaw * axisLen),
        triadOrigin.y - (-sinYaw * sinPitch * axisLen)
    )
    drawLine(Color(0xFFFF3366), triadOrigin, xEnd, strokeWidth = 2.5f)

    // Y Axis (Green)
    val yEnd = Offset(
        triadOrigin.x,
        triadOrigin.y - (cosPitch * axisLen)
    )
    drawLine(Color(0xFF00E676), triadOrigin, yEnd, strokeWidth = 2.5f)

    // Z Axis (Blue)
    val zEnd = Offset(
        triadOrigin.x + (-sinYaw * axisLen),
        triadOrigin.y - (cosYaw * sinPitch * axisLen)
    )
    drawLine(Color(0xFF00B0FF), triadOrigin, zEnd, strokeWidth = 2.5f)

    drawCircle(Color.White, radius = 3.5f, center = triadOrigin)
}
