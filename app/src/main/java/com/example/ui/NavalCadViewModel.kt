package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cloud.*
import com.example.console.PythonEngine
import com.example.fea.FeaEngine
import com.example.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class CadUiState(
    val currentProject: NavalProject,
    val allProjects: List<NavalProject>,
    val activeMesh: Mesh3D,
    val machineryType: NavalMachineryType,
    val activeMaterial: MarineMaterial,
    val loadParams: FeaEngine.MarineLoadParams,
    val compliance: FeaEngine.ClassificationCompliance,
    val shadingMode: CadShadingMode = CadShadingMode.SHADED_SOLID,
    val cutawayPlanePos: Float = 0.0f,
    val isProbeModeActive: Boolean = false,
    val selectedProbePoint: Point3D? = null,
    val selectedProbeStress: StressNode? = null,
    // Console
    val consoleOutputs: List<PythonEngine.ConsoleOutput> = emptyList(),
    val commandInput: String = "",
    val activeScript: PythonEngine.EngineeringScript? = null,
    // Export Modal
    val isExportDialogOpen: Boolean = false,
    // Notification Banner
    val statusBanner: String? = null
)

class NavalCadViewModel : ViewModel() {

    private val defaultProjects = TeamSyncManager.getDefaultProjects()

    private val _uiState = MutableStateFlow(
        createInitialState(defaultProjects.first())
    )
    val uiState: StateFlow<CadUiState> = _uiState.asStateFlow()

    val pythonEngine: PythonEngine = PythonEngine(
        onModelGenerated = { mesh, type ->
            updateGeneratedMesh(mesh, type)
        },
        onMaterialChanged = { mat ->
            setMaterial(mat)
        },
        onLoadsUpdated = { loads ->
            updateLoads(loads)
        },
        onTriggerFea = {
            runFeaAnalysis()
        },
        onExportRequested = { format, name ->
            _uiState.update { it.copy(isExportDialogOpen = true) }
        }
    )

    init {
        // Run initial FEA on startup model
        runFeaAnalysis()
        // Initialize Python welcome message
        _uiState.update {
            it.copy(
                consoleOutputs = listOf(
                    PythonEngine.ConsoleOutput("NavalCAD Marine Engineering Kernel v2.4 (Python 3.11 Embed)", isSuccess = true),
                    PythonEngine.ConsoleOutput("Type cad.propeller(), cad.shaft(), sim.run_fea(), or load from Script Library."),
                    PythonEngine.ConsoleOutput("Governing Class Rule: ${_uiState.value.machineryType.classificationRule}")
                )
            )
        }
    }

    private fun createInitialState(project: NavalProject): CadUiState {
        val initialMesh = MachineryGenerators.generatePropeller(
            blades = 5,
            diameterMm = 2800f,
            pitchRatio = 1.15f,
            rakeDeg = 9.5f,
            skewDeg = 24.0f
        )
        val initialMaterial = MarineMaterial.NICKEL_ALUMINUM_BRONZE
        val initialLoads = FeaEngine.MarineLoadParams(
            shaftRpm = 145f,
            enginePowerKw = 9800f,
            thrustKn = 780f
        )
        val (solvedMesh, compliance) = FeaEngine.runAnalysis(
            mesh = initialMesh,
            type = project.machineryType,
            material = initialMaterial,
            loads = initialLoads
        )

        return CadUiState(
            currentProject = project,
            allProjects = defaultProjects,
            activeMesh = solvedMesh,
            machineryType = project.machineryType,
            activeMaterial = initialMaterial,
            loadParams = initialLoads,
            compliance = compliance,
            activeScript = null
        )
    }

    fun selectProject(project: NavalProject) {
        viewModelScope.launch {
            _uiState.update { it.copy(statusBanner = "Switching to ${project.name}...") }
            delay(250)
            val newMesh = when (project.machineryType) {
                NavalMachineryType.CONTROLLABLE_PITCH_PROPELLER -> MachineryGenerators.generatePropeller()
                NavalMachineryType.PROPULSION_SHAFTING -> MachineryGenerators.generatePropulsionShafting()
                NavalMachineryType.DIESEL_CYLINDER_HEAD -> MachineryGenerators.generateDieselCylinderHead()
                NavalMachineryType.REDUCTION_GEARBOX -> MachineryGenerators.generateReductionGearbox()
                NavalMachineryType.BALLAST_CENTRIFUGAL_PUMP -> MachineryGenerators.generateCentrifugalPump()
                NavalMachineryType.WATERTIGHT_BULKHEAD -> MachineryGenerators.generateWatertightBulkhead()
            }
            val mat = when (project.machineryType) {
                NavalMachineryType.CONTROLLABLE_PITCH_PROPELLER -> MarineMaterial.NICKEL_ALUMINUM_BRONZE
                NavalMachineryType.PROPULSION_SHAFTING, NavalMachineryType.REDUCTION_GEARBOX -> MarineMaterial.FORGED_SHAFT_STEEL
                NavalMachineryType.DIESEL_CYLINDER_HEAD -> MarineMaterial.NODULAR_CAST_IRON
                NavalMachineryType.WATERTIGHT_BULKHEAD -> MarineMaterial.HULL_STRUCTURAL_STEEL
                NavalMachineryType.BALLAST_CENTRIFUGAL_PUMP -> MarineMaterial.MARINE_STAINLESS_316L
            }
            val (solvedMesh, comp) = FeaEngine.runAnalysis(
                mesh = newMesh,
                type = project.machineryType,
                material = mat,
                loads = _uiState.value.loadParams
            )
            _uiState.update {
                it.copy(
                    currentProject = project,
                    machineryType = project.machineryType,
                    activeMesh = solvedMesh,
                    activeMaterial = mat,
                    compliance = comp,
                    statusBanner = null
                )
            }
        }
    }

    fun setMachineryType(type: NavalMachineryType) {
        val mesh = when (type) {
            NavalMachineryType.CONTROLLABLE_PITCH_PROPELLER -> MachineryGenerators.generatePropeller()
            NavalMachineryType.PROPULSION_SHAFTING -> MachineryGenerators.generatePropulsionShafting()
            NavalMachineryType.DIESEL_CYLINDER_HEAD -> MachineryGenerators.generateDieselCylinderHead()
            NavalMachineryType.REDUCTION_GEARBOX -> MachineryGenerators.generateReductionGearbox()
            NavalMachineryType.BALLAST_CENTRIFUGAL_PUMP -> MachineryGenerators.generateCentrifugalPump()
            NavalMachineryType.WATERTIGHT_BULKHEAD -> MachineryGenerators.generateWatertightBulkhead()
        }
        updateGeneratedMesh(mesh, type)
    }

    fun updateGeneratedMesh(mesh: Mesh3D, type: NavalMachineryType) {
        val (solved, comp) = FeaEngine.runAnalysis(
            mesh = mesh,
            type = type,
            material = _uiState.value.activeMaterial,
            loads = _uiState.value.loadParams
        )
        _uiState.update {
            it.copy(
                activeMesh = solved,
                machineryType = type,
                compliance = comp
            )
        }
    }

    fun setMaterial(material: MarineMaterial) {
        val (solved, comp) = FeaEngine.runAnalysis(
            mesh = _uiState.value.activeMesh,
            type = _uiState.value.machineryType,
            material = material,
            loads = _uiState.value.loadParams
        )
        _uiState.update {
            it.copy(
                activeMaterial = material,
                activeMesh = solved,
                compliance = comp
            )
        }
    }

    fun updateLoads(loads: FeaEngine.MarineLoadParams) {
        val (solved, comp) = FeaEngine.runAnalysis(
            mesh = _uiState.value.activeMesh,
            type = _uiState.value.machineryType,
            material = _uiState.value.activeMaterial,
            loads = loads
        )
        _uiState.update {
            it.copy(
                loadParams = loads,
                activeMesh = solved,
                compliance = comp
            )
        }
    }

    fun runFeaAnalysis() {
        val (solved, comp) = FeaEngine.runAnalysis(
            mesh = _uiState.value.activeMesh,
            type = _uiState.value.machineryType,
            material = _uiState.value.activeMaterial,
            loads = _uiState.value.loadParams
        )
        _uiState.update {
            it.copy(
                activeMesh = solved,
                compliance = comp,
                statusBanner = "FEA solved: Peak Stress ${"%.1f".format(solved.maxStressMPa)} MPa | SF ${"%.2f".format(comp.calculatedSafetyFactor)}"
            )
        }
    }

    fun setShadingMode(mode: CadShadingMode) {
        _uiState.update { it.copy(shadingMode = mode) }
    }

    fun setCutawayPlanePos(pos: Float) {
        _uiState.update { it.copy(cutawayPlanePos = pos) }
    }

    fun toggleProbeMode() {
        _uiState.update { it.copy(isProbeModeActive = !it.isProbeModeActive) }
    }

    fun selectProbePoint(pt: Point3D, node: StressNode?) {
        _uiState.update {
            it.copy(
                selectedProbePoint = pt,
                selectedProbeStress = node
            )
        }
    }

    // Python Console Handlers
    fun onCommandInputChange(text: String) {
        _uiState.update { it.copy(commandInput = text) }
    }

    fun submitConsoleCommand() {
        val cmd = _uiState.value.commandInput.trim()
        if (cmd.isEmpty()) return

        val newOutputs = _uiState.value.consoleOutputs.toMutableList()
        newOutputs.add(PythonEngine.ConsoleOutput(">>> $cmd", isCommand = true))

        val results = pythonEngine.execute(cmd)
        newOutputs.addAll(results)

        _uiState.update {
            it.copy(
                commandInput = "",
                consoleOutputs = newOutputs
            )
        }
    }

    fun loadAndExecuteScript(script: PythonEngine.EngineeringScript) {
        val newOutputs = _uiState.value.consoleOutputs.toMutableList()
        newOutputs.add(PythonEngine.ConsoleOutput(">>> # Executing '${script.name}'", isCommand = true))

        val results = pythonEngine.execute(script.code)
        newOutputs.addAll(results)

        _uiState.update {
            it.copy(
                activeScript = script,
                consoleOutputs = newOutputs,
                statusBanner = "Loaded script: ${script.name}"
            )
        }
    }

    fun clearConsole() {
        _uiState.update {
            it.copy(
                consoleOutputs = listOf(
                    PythonEngine.ConsoleOutput("Console output buffer cleared.")
                )
            )
        }
    }

    // Team Collaboration & Cloud Sync
    fun triggerCloudSync() {
        viewModelScope.launch {
            _uiState.update {
                val updatedProj = it.currentProject.copy(syncState = SyncState.SYNCING)
                it.copy(
                    currentProject = updatedProj,
                    statusBanner = "Syncing CAD models and FEA meshes with team cloud..."
                )
            }
            delay(1200)
            val now = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
            _uiState.update {
                val updatedProj = it.currentProject.copy(
                    syncState = SyncState.SYNCED,
                    lastSyncTime = "Today $now"
                )
                it.copy(
                    currentProject = updatedProj,
                    statusBanner = "Cloud sync successful! All team members in sync."
                )
            }
        }
    }

    fun addReviewComment(text: String, location: String, status: ReviewStatus) {
        if (text.isBlank()) return
        val now = SimpleDateFormat("HH:mm", Locale.US).format(Date())
        val newComment = ReviewComment(
            id = UUID.randomUUID().toString(),
            author = TeamSyncManager.currentEngineer,
            timestamp = "Just now ($now)",
            text = text,
            status = status,
            partLocation = location
        )
        _uiState.update {
            val updatedComments = listOf(newComment) + it.currentProject.comments
            val updatedProj = it.currentProject.copy(comments = updatedComments)
            it.copy(
                currentProject = updatedProj,
                statusBanner = "Review comment posted to team project"
            )
        }
    }

    fun setExportDialogOpen(open: Boolean) {
        _uiState.update { it.copy(isExportDialogOpen = open) }
    }

    fun dismissBanner() {
        _uiState.update { it.copy(statusBanner = null) }
    }
}
