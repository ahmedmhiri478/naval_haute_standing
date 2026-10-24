package com.example

import com.example.console.PythonEngine
import com.example.export.CadExportManager
import com.example.fea.FeaEngine
import com.example.model.*
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testMachineryGeneration() {
        val propeller = MachineryGenerators.generatePropeller(blades = 4, diameterMm = 2400f)
        assertTrue("Propeller should have vertices", propeller.vertices.isNotEmpty())
        assertTrue("Propeller should have faces", propeller.faces.isNotEmpty())

        val shaft = MachineryGenerators.generatePropulsionShafting(lengthMm = 5000f, outerDiaMm = 360f)
        assertTrue("Shaft should have vertices", shaft.vertices.isNotEmpty())
        assertTrue("Shaft should have faces", shaft.faces.isNotEmpty())

        val cylinderHead = MachineryGenerators.generateDieselCylinderHead(boreMm = 480f)
        assertTrue("Cylinder head should have vertices", cylinderHead.vertices.isNotEmpty())
    }

    @Test
    fun testFeaAnalysisCompliance() {
        val propeller = MachineryGenerators.generatePropeller()
        val loads = FeaEngine.MarineLoadParams(shaftRpm = 140f, enginePowerKw = 8000f, thrustKn = 650f)
        val (solved, compliance) = FeaEngine.runAnalysis(
            mesh = propeller,
            type = NavalMachineryType.CONTROLLABLE_PITCH_PROPELLER,
            material = MarineMaterial.NICKEL_ALUMINUM_BRONZE,
            loads = loads
        )

        assertTrue("Solved mesh should calculate stress nodes", solved.stressNodes.isNotEmpty())
        assertTrue("Max stress should be positive", solved.maxStressMPa > 0f)
        assertTrue("Safety factor should be positive", compliance.calculatedSafetyFactor > 0f)
    }

    @Test
    fun testPythonEngineScriptExecution() {
        var generatedMesh: Mesh3D? = null
        val engine = PythonEngine(
            onModelGenerated = { mesh, _ -> generatedMesh = mesh },
            onMaterialChanged = {},
            onLoadsUpdated = {},
            onTriggerFea = {},
            onExportRequested = { _, _ -> }
        )

        val outputs = engine.execute("cad.propeller(blades=5, diameter=2800)")
        assertTrue("Outputs should reflect propeller creation", outputs.any { it.isSuccess })
        assertNotNull("Generated mesh should not be null", generatedMesh)
    }

    @Test
    fun testCadExportGeneration() {
        val shaft = MachineryGenerators.generatePropulsionShafting()
        val dxf = CadExportManager.generateDxf(shaft, MarineMaterial.FORGED_SHAFT_STEEL)
        assertTrue("DXF should contain HEADER section", dxf.contains("HEADER"))
        assertTrue("DXF should contain 3DFACE entities", dxf.contains("3DFACE"))

        val step = CadExportManager.generateStep(shaft, MarineMaterial.FORGED_SHAFT_STEEL)
        assertTrue("STEP should contain ISO-10303-21 header", step.contains("ISO-10303-21"))
        assertTrue("STEP should contain CONFIG_CONTROL_DESIGN schema", step.contains("CONFIG_CONTROL_DESIGN"))

        val stl = CadExportManager.generateStl(shaft)
        assertTrue("STL should contain solid definition", stl.startsWith("solid NavalCAD"))
    }
}
