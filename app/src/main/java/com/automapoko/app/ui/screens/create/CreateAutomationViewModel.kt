package com.automapoko.app.ui.screens.create

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.automapoko.app.data.repository.AutomationRepository
import com.automapoko.app.domain.model.*
import com.automapoko.app.domain.usecase.SaveAutomationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class CreateStep {
    NAME, TRIGGER_TYPE, TRIGGER_CONFIG, ACTION_TYPE, APP_SELECT, REVIEW
}

data class InstalledApp(
    val packageName: String,
    val appName: String
)

data class CreateUiState(
    val step: CreateStep = CreateStep.NAME,
    val name: String = "",
    val nameError: String? = null,
    val selectedTriggerType: TriggerType? = null,
    val triggerConfig: TriggerConfig? = null,
    val selectedApp: InstalledApp? = null,
    val installedApps: List<InstalledApp> = emptyList(),
    val isLoadingApps: Boolean = false,
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false,
    val isEditing: Boolean = false
)

@HiltViewModel
class CreateAutomationViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: AutomationRepository,
    private val saveAutomationUseCase: SaveAutomationUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateUiState())
    val uiState: StateFlow<CreateUiState> = _uiState.asStateFlow()

    // ID da automação sendo editada (null = nova)
    private var editingAutomationId: String? = null

    fun loadForEditing(automationId: String) {
        viewModelScope.launch {
            val automation = repository.getById(automationId) ?: return@launch
            editingAutomationId = automation.id

            val app = (automation.actionConfig as? ActionConfig.OpenAppConfig)?.let {
                InstalledApp(it.packageName, it.appName)
            }

            _uiState.update {
                it.copy(
                    name = automation.name,
                    selectedTriggerType = automation.triggerType,
                    triggerConfig = automation.triggerConfig,
                    selectedApp = app,
                    isEditing = true,
                    step = CreateStep.NAME
                )
            }
        }
    }

    // ======================== NAVEGAÇÃO ========================

    fun goToNextStep() {
        val state = _uiState.value
        val next = when (state.step) {
            CreateStep.NAME -> {
                if (state.name.isBlank()) {
                    _uiState.update { it.copy(nameError = "O nome da automação é obrigatório.") }
                    return
                }
                CreateStep.TRIGGER_TYPE
            }
            CreateStep.TRIGGER_TYPE -> CreateStep.TRIGGER_CONFIG
            CreateStep.TRIGGER_CONFIG -> CreateStep.ACTION_TYPE
            CreateStep.ACTION_TYPE -> {
                loadInstalledApps()
                CreateStep.APP_SELECT
            }
            CreateStep.APP_SELECT -> CreateStep.REVIEW
            CreateStep.REVIEW -> {
                save()
                return
            }
        }
        _uiState.update { it.copy(step = next) }
    }

    fun goToPreviousStep() {
        val prev = when (_uiState.value.step) {
            CreateStep.NAME -> return
            CreateStep.TRIGGER_TYPE -> CreateStep.NAME
            CreateStep.TRIGGER_CONFIG -> CreateStep.TRIGGER_TYPE
            CreateStep.ACTION_TYPE -> CreateStep.TRIGGER_CONFIG
            CreateStep.APP_SELECT -> CreateStep.ACTION_TYPE
            CreateStep.REVIEW -> CreateStep.APP_SELECT
        }
        _uiState.update { it.copy(step = prev) }
    }

    // ======================== ATUALIZAÇÃO DE ESTADO ========================

    fun setName(name: String) {
        _uiState.update { it.copy(name = name, nameError = null) }
    }

    fun setTriggerType(type: TriggerType) {
        _uiState.update { it.copy(selectedTriggerType = type, triggerConfig = null) }
    }

    fun setTriggerConfig(config: TriggerConfig) {
        _uiState.update { it.copy(triggerConfig = config) }
    }

    fun setSelectedApp(app: InstalledApp) {
        _uiState.update { it.copy(selectedApp = app) }
    }

    // ======================== APPS INSTALADOS ========================

    private fun loadInstalledApps() {
        if (_uiState.value.installedApps.isNotEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingApps = true) }
            val apps = withContext(Dispatchers.IO) {
                getInstalledApps()
            }
            _uiState.update { it.copy(installedApps = apps, isLoadingApps = false) }
        }
    }

    private fun getInstalledApps(): List<InstalledApp> {
        val pm = context.packageManager
        return pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 } // Apenas apps do usuário
            .mapNotNull { info ->
                val label = pm.getApplicationLabel(info).toString()
                val launchIntent = pm.getLaunchIntentForPackage(info.packageName)
                if (launchIntent != null) {
                    InstalledApp(packageName = info.packageName, appName = label)
                } else null
            }
            .sortedBy { it.appName.lowercase() }
    }

    // ======================== SALVAR ========================

    private fun save() {
        val state = _uiState.value
        val triggerConfig = state.triggerConfig ?: return
        val selectedApp = state.selectedApp ?: return
        val triggerType = state.selectedTriggerType ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            val automation = if (editingAutomationId != null) {
                // Edição: preserva ID, createdAt e status da automação original
                val existing = repository.getById(editingAutomationId!!)
                existing?.copy(
                    name = state.name,
                    triggerType = triggerType,
                    triggerConfig = triggerConfig,
                    actionConfig = ActionConfig.OpenAppConfig(
                        packageName = selectedApp.packageName,
                        appName = selectedApp.appName
                    )
                ) ?: return@launch
            } else {
                Automation(
                    name = state.name,
                    triggerType = triggerType,
                    triggerConfig = triggerConfig,
                    actionConfig = ActionConfig.OpenAppConfig(
                        packageName = selectedApp.packageName,
                        appName = selectedApp.appName
                    )
                )
            }

            saveAutomationUseCase(automation)
            _uiState.update { it.copy(isSaving = false, savedSuccessfully = true) }
        }
    }
}
