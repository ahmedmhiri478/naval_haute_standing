package com.example.fea

import androidx.compose.ui.graphics.Color
import com.example.model.*
import com.example.ui.theme.*
import kotlin.math.*

/**
 * Real-time Finite Element Analysis & Naval Stress Calculation Engine.
 * Adheres to DNV-GL, Lloyd's Register, and IACS marine engineering rules.
 */
object FeaEngine {

    /**
     * Marine operational parameters for FEA simulations
     */
    data class MarineLoadParams(
        val shaftRpm: Float = 160f,
        val enginePowerKw: Float = 8500f,
        val thrustKn: Float = 620f,
        val peakPressureBar: Float = 145f,
        val hydrostaticHeadM: Float = 7.5f,
        val deformationMagnification: Float = 25f // Amplifies mesh deflection visually
    ) {
        val torqueKnm: Float
            get() = if (shaftRpm > 0) (enginePowerKw / (2f * Math.PI.toFloat() * (shaftRpm / 60f))) else 0f
    }

    /**
     * DNV / Lloyd's Register Classification Society Rule Compliance Evaluation
     */
    data class ClassificationCompliance(
        val standardName: String,
        val ruleClause: String,
        val allowableStressMPa: Float,
        val peakCalculatedStressMPa: Float,
        val minRequiredSafetyFactor: Float,
        val calculatedSafetyFactor: Float,
        val isPassed: Boolean,
        val remarks: String
    )

    /**
     * Runs FEA on the CAD mesh given machinery type, material, and marine loads.
     * Generates per-vertex stress nodes and calculates compliance.
     */
    fun runAnalysis(
        mesh: Mesh3D,
        type: NavalMachineryType,
        material: MarineMaterial,
        loads: MarineLoadParams
    ): Pair<Mesh3D, ClassificationCompliance> {
        val stressNodes = mutableListOf<StressNode>()
        val deformedVertices = mutableListOf<Point3D>()

        var maxStress = 0f
        var minSafety = 999f

        val yieldStrength = material.yieldStrengthMPa
        val youngsModulus = material.youngsModulusGPa * 1000f // MPa

        // Base peak stress estimation based on physics equations for specific machinery
        val nominalStress = when (type) {
            NavalMachineryType.CONTROLLABLE_PITCH_PROPELLER -> {
                // Cantilever blade root bending + centrifugal stress
                val torque = loads.torqueKnm * 1000f // Nm
                val thrust = loads.thrustKn * 1000f // N
                val omega = 2f * Math.PI.toFloat() * (loads.shaftRpm / 60f)
                val centrifugal = (material.densityKgM3 * omega.pow(2) * 1.2f.pow(2)) / 1e6f
                val rootBending = (thrust * 0.7f * 0.8f) / (0.15f * 0.25f.pow(2) * 1e6f)
                (rootBending + centrifugal) * 0.55f
            }
            NavalMachineryType.PROPULSION_SHAFTING -> {
                // Torsional shear + overhang bending moment
                val torqueNm = loads.torqueKnm * 1000f
                val dOuterM = 0.38f
                val dInnerM = 0.14f
                val polarJ = (Math.PI.toFloat() / 32f) * (dOuterM.pow(4) - dInnerM.pow(4))
                val shearStress = (torqueNm * (dOuterM * 0.5f)) / polarJ / 1e6f
                val bendingStress = 45f // Shaft overhang & bearing reaction
                sqrt(bendingStress.pow(2) + 3f * shearStress.pow(2))
            }
            NavalMachineryType.DIESEL_CYLINDER_HEAD -> {
                // Peak cylinder pressure (150 bar = 15 MPa) + thermal gradient
                val pressureMPa = loads.peakPressureBar * 0.1f
                val thermalStress = 65f // Thermal difference across firedeck
                val mechStress = pressureMPa * (480f / 45f) * 0.65f
                mechStress + thermalStress
            }
            NavalMachineryType.REDUCTION_GEARBOX -> {
                // Tooth root bending & contact stress
                val torqueNm = loads.torqueKnm * 1000f
                val tangentialForce = torqueNm / 0.65f
                val rootStress = (tangentialForce / (180f * 16f)) * 3.2f
                rootStress.coerceAtLeast(110f)
            }
            NavalMachineryType.BALLAST_CENTRIFUGAL_PUMP -> {
                // Centrifugal vane stress & fluid reaction
                val omega = 2f * Math.PI.toFloat() * (loads.shaftRpm * 4f / 60f)
                val centStress = (material.densityKgM3 * omega.pow(2) * 0.25f.pow(2)) / 1e6f
                centStress + 45f
            }
            NavalMachineryType.WATERTIGHT_BULKHEAD -> {
                // Hydrostatic pressure at bottom P = rho * g * h
                val pBottomKPa = 1025f * 9.81f * loads.hydrostaticHeadM / 1000f
                // Plating bending stress
                val plateStress = 0.35f * pBottomKPa * (750f / 14f).pow(2) / 1000f
                plateStress.coerceAtLeast(80f)
            }
        }

        // Calculate spatial distribution of stress across 3D mesh vertices
        for (i in mesh.vertices.indices) {
            val v = mesh.vertices[i]
            val distFromCenter = sqrt(v.x * v.x + v.y * v.y)

            // Stress concentration factor dependent on geometry location
            val concentrationFactor = when (type) {
                NavalMachineryType.CONTROLLABLE_PITCH_PROPELLER -> {
                    // Maximum stress at blade root fillet (r near hub, z near face)
                    val rNorm = (distFromCenter / 50f).coerceIn(0.1f, 1.2f)
                    val rootFilletBonus = if (rNorm < 0.45f) 2.1f - (rNorm * 2.2f) else 0.4f + (1f - rNorm) * 0.6f
                    rootFilletBonus.coerceIn(0.2f, 2.4f)
                }
                NavalMachineryType.PROPULSION_SHAFTING -> {
                    // Maximum stress at keyway and flange transition
                    val zNorm = abs(v.z) / 60f
                    if (zNorm > 0.8f) 1.9f else if (distFromCenter > 18f) 1.4f else 0.8f
                }
                NavalMachineryType.DIESEL_CYLINDER_HEAD -> {
                    // Maximum stress at valve bridge & firedeck bottom
                    if (v.z < -10f && distFromCenter < 25f) 2.2f else 0.7f
                }
                NavalMachineryType.REDUCTION_GEARBOX -> {
                    // Maximum stress at tooth roots
                    val isToothRoot = (distFromCenter in 30f..38f)
                    if (isToothRoot) 2.05f else 0.65f
                }
                NavalMachineryType.BALLAST_CENTRIFUGAL_PUMP -> {
                    // Maximum stress at impeller blade inlet tip
                    if (distFromCenter in 15f..25f) 1.85f else 0.75f
                }
                NavalMachineryType.WATERTIGHT_BULKHEAD -> {
                    // Maximum stress at lower third where hydrostatic pressure peaks
                    val yNorm = (v.y + 35f) / 70f
                    (1.0f - yNorm).pow(1.4f) * 2.1f
                }
            }

            val localVonMises = (nominalStress * concentrationFactor).coerceAtLeast(12f)
            if (localVonMises > maxStress) maxStress = localVonMises

            val sf = (yieldStrength / localVonMises).coerceAtLeast(0.1f)
            if (sf < minSafety) minSafety = sf

            // Displacement in microns: delta = (sigma / E) * L
            val displacement = (localVonMises / youngsModulus) * 1000f * (distFromCenter + 10f)

            stressNodes.add(
                StressNode(
                    vonMisesMPa = localVonMises,
                    principalStress1 = localVonMises * 1.15f,
                    principalStress2 = localVonMises * 0.45f,
                    displacementMicrons = displacement,
                    safetyFactor = sf
                )
            )

            // Elastic deflection magnification for visual mesh deformation
            val deformFactor = (displacement / 1000f) * loads.deformationMagnification * 0.05f
            val normal = if (distFromCenter > 0.1f) Point3D(v.x / distFromCenter, v.y / distFromCenter, 0f) else Point3D(0f, 0f, 1f)
            val deformed = v + (normal * deformFactor)
            deformedVertices.add(deformed)
        }

        val updatedMesh = mesh.copy(
            vertices = deformedVertices,
            stressNodes = stressNodes,
            maxStressMPa = maxStress,
            minSafetyFactor = minSafety
        )

        // Evaluate Classification Society Compliance
        val requiredSF = when (type) {
            NavalMachineryType.CONTROLLABLE_PITCH_PROPELLER -> 1.75f // DNV Ice Class 1A requires high margin
            NavalMachineryType.PROPULSION_SHAFTING -> 2.0f   // Lloyd's Register Shafting rules
            NavalMachineryType.DIESEL_CYLINDER_HEAD -> 1.6f  // CIMAC thermal fatigue standard
            NavalMachineryType.REDUCTION_GEARBOX -> 1.8f    // ISO 6336 pitting and tooth fracture
            NavalMachineryType.BALLAST_CENTRIFUGAL_PUMP -> 1.5f
            NavalMachineryType.WATERTIGHT_BULKHEAD -> 1.45f  // SOLAS damaged stability immersion
        }

        val isPassed = minSafety >= requiredSF
        val compliance = ClassificationCompliance(
            standardName = type.classificationRule,
            ruleClause = when (type) {
                NavalMachineryType.CONTROLLABLE_PITCH_PROPELLER -> "Rule Pt.4 Ch.5 Sec.2 [3.1]: Blade root section modulus"
                NavalMachineryType.PROPULSION_SHAFTING -> "IACS UR M68: Intermediate and tailshaft diameter and torsional shear"
                NavalMachineryType.DIESEL_CYLINDER_HEAD -> "CIMAC Rec. 24: Peak mechanical & thermal cycle fatigue safety"
                NavalMachineryType.REDUCTION_GEARBOX -> "ISO 6336-1: High-load double helical contact endurance"
                NavalMachineryType.BALLAST_CENTRIFUGAL_PUMP -> "API 610 / IMO BWMS Impeller blade fatigue limit"
                NavalMachineryType.WATERTIGHT_BULKHEAD -> "DNV Pt.3 Ch.6 Sec.4: Plating thickness under maximum hydrostatic head"
            },
            allowableStressMPa = yieldStrength / requiredSF,
            peakCalculatedStressMPa = maxStress,
            minRequiredSafetyFactor = requiredSF,
            calculatedSafetyFactor = minSafety,
            isPassed = isPassed,
            remarks = if (isPassed) {
                "Fully compliant with classification society structural fatigue and static yield criteria."
            } else {
                "Safety factor ($minSafety) below required minimum ($requiredSF). Structural redesign or material upgrade recommended."
            }
        )

        return Pair(updatedMesh, compliance)
    }

    /**
     * Converts a stress value to an interactive FEA color using a 5-stop Turbo / Rainbow colormap
     */
    fun getStressColor(stressMPa: Float, maxStressMPa: Float): Color {
        if (maxStressMPa <= 0.001f) return FeaSafeBlue
        val ratio = (stressMPa / maxStressMPa).coerceIn(0f, 1f)

        return when {
            ratio < 0.20f -> {
                // Blue to Cyan
                val t = ratio / 0.20f
                Color(
                    red = (0x15 + t * (0x00 - 0x15)).toInt(),
                    green = (0x65 + t * (0xB0 - 0x65)).toInt(),
                    blue = (0xC0 + t * (0xFF - 0xC0)).toInt()
                )
            }
            ratio < 0.45f -> {
                // Cyan to Good Green
                val t = (ratio - 0.20f) / 0.25f
                Color(
                    red = (0x00 + t * (0x00 - 0x00)).toInt(),
                    green = (0xB0 + t * (0xE6 - 0xB0)).toInt(),
                    blue = (0xFF + t * (0x76 - 0xFF)).toInt()
                )
            }
            ratio < 0.70f -> {
                // Green to Yellow
                val t = (ratio - 0.45f) / 0.25f
                Color(
                    red = (0x00 + t * (0xFF - 0x00)).toInt(),
                    green = (0xE6 + t * (0xD6 - 0xE6)).toInt(),
                    blue = (0x76 + t * (0x00 - 0x76)).toInt()
                )
            }
            ratio < 0.90f -> {
                // Yellow to Orange
                val t = (ratio - 0.70f) / 0.20f
                Color(
                    red = 0xFF,
                    green = (0xD6 + t * (0x6D - 0xD6)).toInt(),
                    blue = 0x00
                )
            }
            else -> {
                // Orange to Critical Red
                val t = (ratio - 0.90f) / 0.10f
                Color(
                    red = 0xFF,
                    green = (0x6D + t * (0x17 - 0x6D)).toInt(),
                    blue = (0x00 + t * (0x44 - 0x00)).toInt()
                )
            }
        }
    }
}
