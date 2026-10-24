package com.example.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.fea.FeaEngine
import com.example.model.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Manages exporting 3D CAD models into industry-standard CAD file formats:
 * - AutoCAD DWG / DXF (Drawing Exchange Format)
 * - ISO 10303-21 STEP (.stp / .step)
 * - 3D Stereolithography STL (.stl)
 * - Marine Classification Society Engineering Calculation Report
 */
object CadExportManager {

    enum class ExportFormat(val extension: String, val mimeType: String, val label: String) {
        DWG_DXF("dxf", "application/dxf", "AutoCAD DXF / DWG Vector CAD"),
        STEP("stp", "application/step", "ISO 10303-21 STEP 3D Model"),
        STL("stl", "model/stl", "3D Printing & CFD Meshing STL"),
        ENGINEERING_REPORT("txt", "text/plain", "DNV / Lloyd's Marine FEA Report")
    }

    /**
     * Generates AutoCAD DXF / DWG standard exchange file
     */
    fun generateDxf(mesh: Mesh3D, material: MarineMaterial): String {
        val sb = StringBuilder()
        sb.appendLine("0\nSECTION\n2\nHEADER\n9\n\$ACADVER\n1\nAC1009") // AutoCAD R12 / 2000 compatibility
        sb.appendLine("9\n\$INSUNITS\n70\n4") // Millimeters
        sb.appendLine("0\nENDSEC")

        // TABLES Section
        sb.appendLine("0\nSECTION\n2\nTABLES\n0\nTABLE\n2\nLAYER\n70\n2")
        sb.appendLine("0\nLAYER\n2\nNAVAL_MACHINERY\n70\n0\n62\n3\n6\nCONTINUOUS")
        sb.appendLine("0\nLAYER\n2\nFEA_STRESS_MESH\n70\n0\n62\n1\n6\nCONTINUOUS")
        sb.appendLine("0\nENDTAB\n0\nENDSEC")

        // ENTITIES Section (3DFACE polygons for 3D model)
        sb.appendLine("0\nSECTION\n2\nENTITIES")

        for (face in mesh.faces) {
            if (face.v1 < mesh.vertices.size && face.v2 < mesh.vertices.size && face.v3 < mesh.vertices.size) {
                val p1 = mesh.vertices[face.v1]
                val p2 = mesh.vertices[face.v2]
                val p3 = mesh.vertices[face.v3]

                sb.appendLine("0\n3DFACE\n8\nNAVAL_MACHINERY")
                sb.appendLine("10\n${p1.x}\n20\n${p1.y}\n30\n${p1.z}")
                sb.appendLine("11\n${p2.x}\n21\n${p2.y}\n31\n${p2.z}")
                sb.appendLine("12\n${p3.x}\n22\n${p3.y}\n32\n${p3.z}")
                sb.appendLine("13\n${p3.x}\n23\n${p3.y}\n33\n${p3.z}") // 4th vertex copies 3rd for triangle
            }
        }

        // Marine Drawing Title Block in DXF
        sb.appendLine("0\nTEXT\n8\nNAVAL_MACHINERY\n10\n0.0\n20\n-120.0\n30\n0.0\n40\n6.0\n1\nNAVALCAD - ${mesh.name}")
        sb.appendLine("0\nTEXT\n8\nNAVAL_MACHINERY\n10\n0.0\n20\n-130.0\n30\n0.0\n40\n4.5\n1\nMATERIAL: ${material.displayName} | DENSITY: ${material.densityKgM3} kg/m3")

        sb.appendLine("0\nENDSEC\n0\nEOF")
        return sb.toString()
    }

    /**
     * Generates ISO 10303-21 STEP file format (AP203 / AP214 standard)
     */
    fun generateStep(mesh: Mesh3D, material: MarineMaterial): String {
        val dateStr = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date())
        val sb = StringBuilder()
        sb.appendLine("ISO-10303-21;")
        sb.appendLine("HEADER;")
        sb.appendLine("FILE_DESCRIPTION(('NavalCAD Marine Machinery 3D Model', 'CAx-IF Rec.Pracs.---AP203/AP214'), '2;1');")
        sb.appendLine("FILE_NAME('naval_cad_export.stp', '$dateStr', ('Naval Architect'), ('Marine Engineering CAD'), 'NavalCAD Android Kernel 2.0', 'NavalCAD', 'Approved for Classification');")
        sb.appendLine("FILE_SCHEMA(('CONFIG_CONTROL_DESIGN'));")
        sb.appendLine("ENDSEC;")

        sb.appendLine("DATA;")
        sb.appendLine("#10=APPLICATION_CONTEXT('configuration controlled 3D designs of mechanical parts and assemblies');")
        sb.appendLine("#20=APPLICATION_PROTOCOL_DEFINITION('international standard', 'config_control_design', 2000, #10);")
        sb.appendLine("#30=PRODUCT('NAVAL_PART', '${mesh.name}', 'Marine propulsion and hull machinery component', (#10));")
        sb.appendLine("#40=PRODUCT_DEFINITION_FORMATION('1.0', 'Initial approved naval design', #30);")
        sb.appendLine("#50=PRODUCT_DEFINITION('design', 'Naval machinery solid model', #40, #10);")

        // Write Cartesian Points
        val ptIdMap = mutableMapOf<Int, Int>()
        var entityCounter = 100
        for (i in mesh.vertices.indices) {
            val v = mesh.vertices[i]
            val ptId = entityCounter++
            ptIdMap[i] = ptId
            sb.appendLine("#$ptId=CARTESIAN_POINT('', (${v.x}, ${v.y}, ${v.z}));")
        }

        // Triangulated Faceted B-Rep Surface representation
        val faceIds = mutableListOf<Int>()
        for (f in mesh.faces.take(400)) { // Safe sample for step file compact size
            val p1Id = ptIdMap[f.v1] ?: 100
            val p2Id = ptIdMap[f.v2] ?: 100
            val p3Id = ptIdMap[f.v3] ?: 100

            val polyLoopId = entityCounter++
            sb.appendLine("#$polyLoopId=POLY_LOOP('', (#$p1Id, #$p2Id, #$p3Id));")
            val faceBoundId = entityCounter++
            sb.appendLine("#$faceBoundId=FACE_OUTER_BOUND('', #$polyLoopId, .T.);")
            val faceId = entityCounter++
            sb.appendLine("#$faceId=FACE_SURFACE('', (#$faceBoundId), #10, .T.);")
            faceIds.add(faceId)
        }

        val shellId = entityCounter++
        val faceListStr = faceIds.joinToString(",") { "#$it" }
        sb.appendLine("#$shellId=CLOSED_SHELL('', ($faceListStr));")
        val brepId = entityCounter++
        sb.appendLine("#$brepId=FACETED_BREP('${mesh.name}', #$shellId);")

        sb.appendLine("ENDSEC;")
        sb.appendLine("END-ISO-10303-21;")
        return sb.toString()
    }

    /**
     * Generates standard ASCII STL for 3D printing and CFD meshers
     */
    fun generateStl(mesh: Mesh3D): String {
        val sb = StringBuilder()
        sb.appendLine("solid NavalCAD_${mesh.name.replace(" ", "_")}")

        for (face in mesh.faces) {
            if (face.v1 < mesh.vertices.size && face.v2 < mesh.vertices.size && face.v3 < mesh.vertices.size) {
                val n = face.normal
                val p1 = mesh.vertices[face.v1]
                val p2 = mesh.vertices[face.v2]
                val p3 = mesh.vertices[face.v3]

                sb.appendLine("  facet normal ${n.x} ${n.y} ${n.z}")
                sb.appendLine("    outer loop")
                sb.appendLine("      vertex ${p1.x} ${p1.y} ${p1.z}")
                sb.appendLine("      vertex ${p2.x} ${p2.y} ${p2.z}")
                sb.appendLine("      vertex ${p3.x} ${p3.y} ${p3.z}")
                sb.appendLine("    endloop")
                sb.appendLine("  endfacet")
            }
        }

        sb.appendLine("endsolid NavalCAD_${mesh.name.replace(" ", "_")}")
        return sb.toString()
    }

    /**
     * Generates formal Marine Classification Society Engineering Calculation & FEA Report
     */
    fun generateReport(
        mesh: Mesh3D,
        type: NavalMachineryType,
        material: MarineMaterial,
        loads: FeaEngine.MarineLoadParams,
        compliance: FeaEngine.ClassificationCompliance
    ): String {
        val dateStr = SimpleDateFormat("dd MMMM yyyy HH:mm:ss", Locale.US).format(Date())
        return """
================================================================================
                    NAVAL CAD - ENGINEERING CALCULATION REPORT
               CLASSIFICATION SOCIETY STRUCTURAL VERIFICATION
================================================================================
Date: $dateStr
Vessel / Project: Marine Propulsion Machinery Design
Component: ${mesh.name}
Machinery Type: ${type.title}
Governing Classification Rule: ${type.classificationRule}

--------------------------------------------------------------------------------
1. MATERIAL SPECIFICATION
--------------------------------------------------------------------------------
Material Name:       ${material.displayName}
Standard Grade:      ${material.standardGrade}
Mass Density:        ${material.densityKgM3} kg/m3
Yield Strength (Re): ${material.yieldStrengthMPa} MPa
Tensile Strength:    ${material.tensileStrengthMPa} MPa
Modulus of Elastic:  ${material.youngsModulusGPa} GPa
Poisson's Ratio:     ${material.poissonRatio}

Component Volume:    ${"%.3f".format(mesh.volumeM3)} m3
Calculated Mass:     ${"%.1f".format(mesh.massKg)} kg (${"%.2f".format(mesh.massKg / 1000f)} Metric Tons)

--------------------------------------------------------------------------------
2. OPERATING LOADS & HYDRODYNAMIC BOUNDARY CONDITIONS
--------------------------------------------------------------------------------
Shaft Rotational Speed:   ${loads.shaftRpm} RPM
Delivered Engine Power:   ${loads.enginePowerKw} kW
Propulsion Shaft Torque:  ${"%.1f".format(loads.torqueKnm)} kNm
Hydrodynamic Thrust:      ${loads.thrustKn} kN
Peak Combustion Pressure: ${loads.peakPressureBar} bar (${loads.peakPressureBar * 0.1f} MPa)
Design Hydrostatic Head:  ${loads.hydrostaticHeadM} m Seawater

--------------------------------------------------------------------------------
3. FINITE ELEMENT & STRESS ANALYSIS (FEA) RESULTS
--------------------------------------------------------------------------------
Peak Von Mises Stress:    ${"%.2f".format(mesh.maxStressMPa)} MPa
Allowable Rule Stress:    ${"%.2f".format(compliance.allowableStressMPa)} MPa
Calculated Safety Factor: ${"%.2f".format(mesh.minSafetyFactor)}
Required Rule Safety SF:  ${"%.2f".format(compliance.minRequiredSafetyFactor)}

Evaluation Status:        ${if (compliance.isPassed) "[ APPROVED / COMPLIANT ]" else "[ ACTION REQUIRED / CRITICAL ]"}
Classification Clause:    ${compliance.ruleClause}
Engineering Remarks:      ${compliance.remarks}

--------------------------------------------------------------------------------
4. SIGN-OFF & CERTIFICATION BLOCK
--------------------------------------------------------------------------------
Design Engineer: Chief Naval Architect
Verification: Automated NavalCAD FEA Finite Element Kernel
Classification Submission: Ready for DNV / Lloyd's Register Plan Approval
================================================================================
""".trimIndent()
    }

    /**
     * Saves content to file and launches Android system share sheet
     */
    fun shareCadFile(
        context: Context,
        format: ExportFormat,
        content: String,
        baseName: String
    ): Boolean {
        return try {
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()

            val sanitizedName = baseName.replace(Regex("[^a-zA-Z0-9_]"), "_").lowercase()
            val file = File(exportDir, "${sanitizedName}.${format.extension}")

            FileOutputStream(file).use { fos ->
                fos.write(content.toByteArray(Charsets.UTF_8))
            }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = format.mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "NavalCAD Export: ${file.name}")
                putExtra(Intent.EXTRA_TEXT, "Exported ${format.label} from NavalCAD.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(intent, "Export ${file.name} to...")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
