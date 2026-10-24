package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.*
import kotlin.math.sqrt

/**
 * 3D Coordinate in millimeters (engineering standard)
 */
data class Point3D(
    val x: Float,
    val y: Float,
    val z: Float
) {
    operator fun plus(other: Point3D) = Point3D(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Point3D) = Point3D(x - other.x, y - other.y, z - other.z)
    operator fun times(factor: Float) = Point3D(x * factor, y * factor, z * factor)

    fun length(): Float = sqrt(x * x + y * y + z * z)

    fun normalized(): Point3D {
        val len = length()
        return if (len > 0.0001f) Point3D(x / len, y / len, z / len) else Point3D(0f, 1f, 0f)
    }

    fun cross(o: Point3D) = Point3D(
        y * o.z - z * o.y,
        z * o.x - x * o.z,
        x * o.y - y * o.x
    )

    fun dot(o: Point3D): Float = x * o.x + y * o.y + z * o.z
}

/**
 * Triangle face referencing vertex indices with normal vector
 */
data class Face3D(
    val v1: Int,
    val v2: Int,
    val v3: Int,
    var normal: Point3D = Point3D(0f, 0f, 1f)
)

/**
 * Finite Element Stress data for a vertex
 */
data class StressNode(
    val vonMisesMPa: Float,
    val principalStress1: Float,
    val principalStress2: Float,
    val displacementMicrons: Float,
    val safetyFactor: Float
)

/**
 * Complete 3D CAD Mesh representation
 */
data class Mesh3D(
    val name: String,
    val vertices: List<Point3D>,
    val faces: List<Face3D>,
    val stressNodes: List<StressNode> = emptyList(),
    val maxStressMPa: Float = 0f,
    val minSafetyFactor: Float = 1.0f,
    val volumeM3: Float = 0f,
    val massKg: Float = 0f,
    val boundingBoxMax: Point3D = Point3D(100f, 100f, 100f),
    val boundingBoxMin: Point3D = Point3D(-100f, -100f, -100f)
)

/**
 * Naval engineering materials with physical and classification rule properties
 */
enum class MarineMaterial(
    val displayName: String,
    val standardGrade: String,
    val densityKgM3: Float,
    val yieldStrengthMPa: Float,
    val tensileStrengthMPa: Float,
    val youngsModulusGPa: Float,
    val poissonRatio: Float,
    val baseColor: Color
) {
    NICKEL_ALUMINUM_BRONZE(
        displayName = "Ni-Al Bronze (Cu3)",
        standardGrade = "EN 1982 CuAl10Fe5Ni5",
        densityKgM3 = 7600f,
        yieldStrengthMPa = 250f,
        tensileStrengthMPa = 650f,
        youngsModulusGPa = 115f,
        poissonRatio = 0.33f,
        baseColor = Color(0xFFD4AF37)
    ),
    FORGED_SHAFT_STEEL(
        displayName = "Forged Alloy Steel",
        standardGrade = "DNV NV 34CrNiMo6",
        densityKgM3 = 7850f,
        yieldStrengthMPa = 600f,
        tensileStrengthMPa = 880f,
        youngsModulusGPa = 210f,
        poissonRatio = 0.28f,
        baseColor = Color(0xFF90A4AE)
    ),
    MARINE_STAINLESS_316L(
        displayName = "Stainless Steel 316L",
        standardGrade = "ASTM A276 Type 316L",
        densityKgM3 = 8000f,
        yieldStrengthMPa = 290f,
        tensileStrengthMPa = 580f,
        youngsModulusGPa = 193f,
        poissonRatio = 0.30f,
        baseColor = Color(0xFFCFD8DC)
    ),
    NODULAR_CAST_IRON(
        displayName = "Nodular Iron (Cylinder)",
        standardGrade = "EN-GJS-400-18-LT",
        densityKgM3 = 7200f,
        yieldStrengthMPa = 320f,
        tensileStrengthMPa = 520f,
        youngsModulusGPa = 170f,
        poissonRatio = 0.27f,
        baseColor = Color(0xFF546E7A)
    ),
    HULL_STRUCTURAL_STEEL(
        displayName = "High-Tensile Hull Steel",
        standardGrade = "DNV Grade NV EH36",
        densityKgM3 = 7850f,
        yieldStrengthMPa = 355f,
        tensileStrengthMPa = 510f,
        youngsModulusGPa = 206f,
        poissonRatio = 0.30f,
        baseColor = Color(0xFF455A64)
    )
}

/**
 * Supported Naval Machinery CAD Models
 */
enum class NavalMachineryType(
    val title: String,
    val description: String,
    val classificationRule: String
) {
    CONTROLLABLE_PITCH_PROPELLER(
        title = "Marine Propeller (CPP)",
        description = "High-skew marine propulsion propeller with hub and variable pitch blades",
        classificationRule = "DNV-RU-SHIP Pt.4 Ch.5 Sec.2 (Propellers & Ice Class)"
    ),
    PROPULSION_SHAFTING(
        title = "Propulsion Shaft Assembly",
        description = "Propeller shaft, stern tube white-metal bearing, coupling flange & hollow bore",
        classificationRule = "IACS UR M68 / Lloyd's Register Pt.5 Ch.6 (Shafting Systems)"
    ),
    DIESEL_CYLINDER_HEAD(
        title = "Marine Diesel Cylinder Head",
        description = "Two/Four-stroke cylinder head with exhaust valve seat, injector bore & cooling jackets",
        classificationRule = "CIMAC Working Group on Heavy Fuel Diesel Engine Components"
    ),
    REDUCTION_GEARBOX(
        title = "Main Propulsion Reduction Gear",
        description = "Double-helical herringbone bull gear & high-speed pinion shaft",
        classificationRule = "ISO 6336 / DNV-RU-SHIP Pt.4 Ch.5 Sec.3 (Gearing)"
    ),
    BALLAST_CENTRIFUGAL_PUMP(
        title = "Ballast & Bilge Pump Impeller",
        description = "High-capacity marine volute pump with curved backward vanes",
        classificationRule = "IMO Ballast Water Management Convention & API 610 Marine"
    ),
    WATERTIGHT_BULKHEAD(
        title = "Watertight Bulkhead & Frame",
        description = "Transverse watertight subdivision bulkhead with stiffened web frames",
        classificationRule = "SOLAS Ch.II-1 & DNV-RU-SHIP Pt.3 Ch.6 (Hull Structures)"
    )
}

/**
 * Active CAD Shading Mode
 */
enum class CadShadingMode(val label: String) {
    SHADED_SOLID("Solid Shaded"),
    FEA_STRESS("FEA Stress Map"),
    WIREFRAME("Wireframe"),
    CROSS_SECTION("Cutaway Section")
}

/**
 * Camera View Angle Presets
 */
enum class ViewPreset(val label: String) {
    ISOMETRIC("ISO"),
    TOP("TOP (XY)"),
    FRONT("FRONT (XZ)"),
    SIDE("SIDE (YZ)")
}
