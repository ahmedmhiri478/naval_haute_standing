package com.example.console

import com.example.fea.FeaEngine
import com.example.model.*
import kotlin.math.*

/**
 * Embedded Naval Python CAD & Engineering Console Engine.
 * Supports parametric CAD commands, physics simulation setups, and interactive script execution.
 */
class PythonEngine(
    private val onModelGenerated: (Mesh3D, NavalMachineryType) -> Unit,
    private val onMaterialChanged: (MarineMaterial) -> Unit,
    private val onLoadsUpdated: (FeaEngine.MarineLoadParams) -> Unit,
    private val onTriggerFea: () -> Unit,
    private val onExportRequested: (String, String) -> Unit
) {

    data class ConsoleOutput(
        val text: String,
        val isError: Boolean = false,
        val isCommand: Boolean = false,
        val isSuccess: Boolean = false
    )

    data class EngineeringScript(
        val id: String,
        val name: String,
        val description: String,
        val code: String
    )

    val environmentVariables = mutableMapOf<String, Double>(
        "PI" to Math.PI,
        "g" to 9.80665,
        "RHO_SEAWATER" to 1025.0
    )

    val preloadedScripts = listOf(
        EngineeringScript(
            id = "script_propeller",
            name = "DNV Ice-Class CPP Propeller Optimization",
            description = "Parametric 5-blade high-skew propeller generation with hydrodynamic thrust & blade root FEA",
            code = """
# NavalCAD - DNV Ice Class 1A Super Propeller Analysis
# Vessel: Arctic Ice-Class Chemical Tanker (12,000 DWT)
blades = 5
prop_diameter_mm = 2800.0
pitch_ratio = 1.15
rake_angle_deg = 9.5
skew_angle_deg = 24.0

print(f"Generating CPP Propeller: Dia={prop_diameter_mm}mm, {blades} Blades")
cad.propeller(blades=5, diameter=2800, pitch=1.15, rake=9.5, skew=24.0)

# Set Nickel-Aluminum Bronze Marine Material (Cu3)
material.set("bronze")

# Operating condition: Maximum continuous rating (MCR)
rpm = 145.0
power_kw = 9800.0
thrust_kn = 780.0

sim.set_rpm(rpm)
sim.set_power_kw(power_kw)
sim.set_thrust_kn(thrust_kn)

print("Running Real-time Von Mises FEA Stress Analysis...")
sim.run_fea()

print("Calculation complete. Safety factor evaluated against DNV-RU-SHIP Pt.4.")
""".trimIndent()
        ),
        EngineeringScript(
            id = "script_shafting",
            name = "Propulsion Shaft Alignment & Torsional Stress",
            description = "Hollow forged alloy steel propeller shaft with stern tube white-metal bearing reaction",
            code = """
# NavalCAD - Main Propulsion Shafting & Bearing Sizing
# Standard: IACS Unified Requirement UR M68
length_mm = 6200.0
outer_dia_mm = 420.0
inner_bore_mm = 160.0

cad.shaft(length=length_mm, dia=outer_dia_mm, bore=inner_bore_mm)
material.set("steel")

# Torsional load from marine slow-speed diesel
sim.set_rpm(120.0)
sim.set_power_kw(11500.0)
sim.run_fea()

print("Shaft assembly mesh updated with keyway stress concentration factors.")
""".trimIndent()
        ),
        EngineeringScript(
            id = "script_cylinder_head",
            name = "Two-Stroke Cylinder Head Combustion FEA",
            description = "Heavy fuel marine diesel cylinder head with 165 bar peak combustion pressure",
            code = """
# NavalCAD - Marine Two-Stroke Engine Cylinder Head
# Standard: CIMAC Working Group Recommendation
bore_mm = 500.0
height_mm = 620.0
p_max_bar = 165.0

cad.cylinder_head(bore=bore_mm, height=height_mm)
material.set("iron")

sim.set_pressure_bar(p_max_bar)
sim.run_fea()

print("Firedeck thermal-mechanical stress evaluated at exhaust valve bridge.")
""".trimIndent()
        ),
        EngineeringScript(
            id = "script_reduction_gear",
            name = "Double-Helical Propulsion Gearbox",
            description = "Herringbone gear set for twin-engine geared propulsion system",
            code = """
# NavalCAD - Marine Reduction Gearbox ISO 6336 Rating
ratio = 4.2
teeth = 28
module_mm = 18.0

cad.gearbox(ratio=ratio, teeth=teeth, module=module_mm)
material.set("steel")

sim.set_power_kw(7500.0)
sim.set_rpm(180.0)
sim.run_fea()

print("Tooth flank contact stress and root bending stress verified.")
""".trimIndent()
        ),
        EngineeringScript(
            id = "script_bulkhead",
            name = "Watertight Subdivision Bulkhead Immersion",
            description = "SOLAS damaged stability test with deep sea hydrostatic head pressure",
            code = """
# NavalCAD - Ship Subdivision Watertight Bulkhead
# Standard: SOLAS II-1 & DNV Hull Structures
width_m = 9.5
height_m = 7.0
plate_thickness_mm = 16.0
head_m = 8.5

cad.bulkhead(width=width_m, height=height_m, thickness=plate_thickness_mm)
material.set("hull_steel")

sim.set_head_m(head_m)
sim.run_fea()

print(f"Hydrostatic head {head_m}m evaluated. Stiffener web stress analyzed.")
""".trimIndent()
        )
    )

    /**
     * Executes a line or multi-line python code block
     */
    fun execute(code: String): List<ConsoleOutput> {
        val outputs = mutableListOf<ConsoleOutput>()
        val lines = code.split("\n")

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#")) continue

            try {
                when {
                    line.startsWith("print(") -> {
                        val content = line.removePrefix("print(").removeSuffix(")")
                        val formatted = evaluateString(content)
                        outputs.add(ConsoleOutput(formatted))
                    }
                    line.startsWith("cad.propeller(") -> {
                        val args = parseKwargs(line.removePrefix("cad.propeller(").removeSuffix(")"))
                        val blades = args["blades"]?.toInt() ?: 4
                        val dia = args["diameter"]?.toFloat() ?: 2400f
                        val pitch = args["pitch"]?.toFloat() ?: 1.05f
                        val rake = args["rake"]?.toFloat() ?: 8f
                        val skew = args["skew"]?.toFloat() ?: 22f

                        val mesh = MachineryGenerators.generatePropeller(blades, dia, pitch, rake, skew)
                        onModelGenerated(mesh, NavalMachineryType.CONTROLLABLE_PITCH_PROPELLER)
                        outputs.add(ConsoleOutput(">>> Created Propeller (Dia ${dia}mm, $blades blades, P/D $pitch)", isSuccess = true))
                    }
                    line.startsWith("cad.shaft(") -> {
                        val args = parseKwargs(line.removePrefix("cad.shaft(").removeSuffix(")"))
                        val length = args["length"]?.toFloat() ?: 5500f
                        val dia = args["dia"]?.toFloat() ?: 380f
                        val bore = args["bore"]?.toFloat() ?: 140f

                        val mesh = MachineryGenerators.generatePropulsionShafting(length, dia, bore)
                        onModelGenerated(mesh, NavalMachineryType.PROPULSION_SHAFTING)
                        outputs.add(ConsoleOutput(">>> Created Shaft Assembly (L ${length}mm, OD ${dia}mm, Bore ${bore}mm)", isSuccess = true))
                    }
                    line.startsWith("cad.cylinder_head(") -> {
                        val args = parseKwargs(line.removePrefix("cad.cylinder_head(").removeSuffix(")"))
                        val bore = args["bore"]?.toFloat() ?: 480f
                        val height = args["height"]?.toFloat() ?: 550f

                        val mesh = MachineryGenerators.generateDieselCylinderHead(bore, height)
                        onModelGenerated(mesh, NavalMachineryType.DIESEL_CYLINDER_HEAD)
                        outputs.add(ConsoleOutput(">>> Created Marine Diesel Cylinder Head (Bore ${bore}mm)", isSuccess = true))
                    }
                    line.startsWith("cad.gearbox(") -> {
                        val args = parseKwargs(line.removePrefix("cad.gearbox(").removeSuffix(")"))
                        val ratio = args["ratio"]?.toFloat() ?: 3.8f
                        val teeth = args["teeth"]?.toInt() ?: 24
                        val module = args["module"]?.toFloat() ?: 16f

                        val mesh = MachineryGenerators.generateReductionGearbox(ratio, module, teeth)
                        onModelGenerated(mesh, NavalMachineryType.REDUCTION_GEARBOX)
                        outputs.add(ConsoleOutput(">>> Created Reduction Gearbox (Ratio $ratio:1, Module $module mm)", isSuccess = true))
                    }
                    line.startsWith("cad.pump(") -> {
                        val args = parseKwargs(line.removePrefix("cad.pump(").removeSuffix(")"))
                        val dia = args["dia"]?.toFloat() ?: 450f
                        val vanes = args["vanes"]?.toInt() ?: 6

                        val mesh = MachineryGenerators.generateCentrifugalPump(dia, vanes)
                        onModelGenerated(mesh, NavalMachineryType.BALLAST_CENTRIFUGAL_PUMP)
                        outputs.add(ConsoleOutput(">>> Created Ballast Pump Impeller (Dia ${dia}mm, $vanes vanes)", isSuccess = true))
                    }
                    line.startsWith("cad.bulkhead(") -> {
                        val args = parseKwargs(line.removePrefix("cad.bulkhead(").removeSuffix(")"))
                        val width = args["width"]?.toFloat() ?: 8.0f
                        val height = args["height"]?.toFloat() ?: 6.0f
                        val thickness = args["thickness"]?.toFloat() ?: 14f

                        val mesh = MachineryGenerators.generateWatertightBulkhead(width, height, thickness)
                        onModelGenerated(mesh, NavalMachineryType.WATERTIGHT_BULKHEAD)
                        outputs.add(ConsoleOutput(">>> Created Bulkhead (${width}m x ${height}m, ${thickness}mm)", isSuccess = true))
                    }
                    line.startsWith("material.set(") -> {
                        val matName = line.removePrefix("material.set(").removeSuffix(")").replace("\"", "").replace("'", "").trim().lowercase()
                        val mat = when {
                            matName.contains("bronze") -> MarineMaterial.NICKEL_ALUMINUM_BRONZE
                            matName.contains("shaft") || matName.contains("steel") -> MarineMaterial.FORGED_SHAFT_STEEL
                            matName.contains("stainless") || matName.contains("316") -> MarineMaterial.MARINE_STAINLESS_316L
                            matName.contains("iron") || matName.contains("cast") -> MarineMaterial.NODULAR_CAST_IRON
                            else -> MarineMaterial.HULL_STRUCTURAL_STEEL
                        }
                        onMaterialChanged(mat)
                        outputs.add(ConsoleOutput(">>> Active Material: ${mat.displayName} (Yield: ${mat.yieldStrengthMPa} MPa)", isSuccess = true))
                    }
                    line.startsWith("sim.set_rpm(") -> {
                        val value = evaluateExpression(line.removePrefix("sim.set_rpm(").removeSuffix(")")).toFloat()
                        onLoadsUpdated(FeaEngine.MarineLoadParams(shaftRpm = value))
                        outputs.add(ConsoleOutput(">>> Shaft RPM set to $value RPM"))
                    }
                    line.startsWith("sim.set_power_kw(") -> {
                        val value = evaluateExpression(line.removePrefix("sim.set_power_kw(").removeSuffix(")")).toFloat()
                        onLoadsUpdated(FeaEngine.MarineLoadParams(enginePowerKw = value))
                        outputs.add(ConsoleOutput(">>> Engine Power set to $value kW"))
                    }
                    line.startsWith("sim.set_thrust_kn(") -> {
                        val value = evaluateExpression(line.removePrefix("sim.set_thrust_kn(").removeSuffix(")")).toFloat()
                        onLoadsUpdated(FeaEngine.MarineLoadParams(thrustKn = value))
                        outputs.add(ConsoleOutput(">>> Propeller Thrust set to $value kN"))
                    }
                    line.startsWith("sim.set_pressure_bar(") -> {
                        val value = evaluateExpression(line.removePrefix("sim.set_pressure_bar(").removeSuffix(")")).toFloat()
                        onLoadsUpdated(FeaEngine.MarineLoadParams(peakPressureBar = value))
                        outputs.add(ConsoleOutput(">>> Cylinder Peak Pressure set to $value bar"))
                    }
                    line.startsWith("sim.set_head_m(") -> {
                        val value = evaluateExpression(line.removePrefix("sim.set_head_m(").removeSuffix(")")).toFloat()
                        onLoadsUpdated(FeaEngine.MarineLoadParams(hydrostaticHeadM = value))
                        outputs.add(ConsoleOutput(">>> Hydrostatic Head set to $value m"))
                    }
                    line.startsWith("sim.run_fea()") -> {
                        onTriggerFea()
                        outputs.add(ConsoleOutput(">>> Real-Time FEA Solved: Von Mises contours & compliance updated.", isSuccess = true))
                    }
                    line.startsWith("export.dwg(") || line.startsWith("export.dxf(") -> {
                        val filename = line.substringAfter("(").substringBefore(")").replace("\"", "").replace("'", "").trim()
                        onExportRequested("DXF", filename)
                        outputs.add(ConsoleOutput(">>> Exported AutoCAD DWG/DXF: $filename", isSuccess = true))
                    }
                    line.startsWith("export.step(") || line.startsWith("export.stp(") -> {
                        val filename = line.substringAfter("(").substringBefore(")").replace("\"", "").replace("'", "").trim()
                        onExportRequested("STEP", filename)
                        outputs.add(ConsoleOutput(">>> Exported ISO 10303-21 STEP: $filename", isSuccess = true))
                    }
                    line.startsWith("export.stl(") -> {
                        val filename = line.substringAfter("(").substringBefore(")").replace("\"", "").replace("'", "").trim()
                        onExportRequested("STL", filename)
                        outputs.add(ConsoleOutput(">>> Exported 3D Stereolithography STL: $filename", isSuccess = true))
                    }
                    line.contains("=") && !line.contains("==") -> {
                        val parts = line.split("=", limit = 2)
                        val varName = parts[0].trim()
                        val expr = parts[1].trim()
                        val res = evaluateExpression(expr)
                        environmentVariables[varName] = res
                        outputs.add(ConsoleOutput("$varName = $res"))
                    }
                    else -> {
                        // Math calculation or expression
                        val result = evaluateExpression(line)
                        outputs.add(ConsoleOutput("=> $result"))
                    }
                }
            } catch (e: Exception) {
                outputs.add(ConsoleOutput("SyntaxError: ${e.message ?: "Invalid command"}", isError = true))
            }
        }

        return outputs
    }

    private fun parseKwargs(str: String): Map<String, Double> {
        val map = mutableMapOf<String, Double>()
        val pairs = str.split(",")
        for (p in pairs) {
            val kv = p.split("=")
            if (kv.size == 2) {
                val key = kv[0].trim()
                val value = evaluateExpression(kv[1].trim())
                map[key] = value
            }
        }
        return map
    }

    private fun evaluateString(s: String): String {
        var clean = s.trim()
        if (clean.startsWith("f\"") || clean.startsWith("f'")) {
            clean = clean.drop(2).dropLast(1)
            // Replace {var} with variable values
            for ((k, v) in environmentVariables) {
                clean = clean.replace("{$k}", if (v % 1.0 == 0.0) v.toLong().toString() else "%.2f".format(v))
            }
            return clean
        }
        return clean.removePrefix("\"").removeSuffix("\"").removePrefix("'").removeSuffix("'")
    }

    private fun evaluateExpression(expr: String): Double {
        val clean = expr.trim()
        // Check if variable
        if (environmentVariables.containsKey(clean)) {
            return environmentVariables[clean]!!
        }
        // Basic arithmetic parsing (addition, subtraction, multiplication, division)
        return try {
            when {
                "+" in clean -> {
                    val p = clean.split("+")
                    p.sumOf { evaluateExpression(it) }
                }
                "-" in clean && !clean.startsWith("-") -> {
                    val p = clean.split("-")
                    val first = evaluateExpression(p[0])
                    p.drop(1).fold(first) { acc, s -> acc - evaluateExpression(s) }
                }
                "*" in clean -> {
                    val p = clean.split("*")
                    p.fold(1.0) { acc, s -> acc * evaluateExpression(s) }
                }
                "/" in clean -> {
                    val p = clean.split("/")
                    val first = evaluateExpression(p[0])
                    p.drop(1).fold(first) { acc, s -> acc / evaluateExpression(s) }
                }
                else -> clean.toDouble()
            }
        } catch (_: Exception) {
            0.0
        }
    }
}
