package com.automapoko.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.automapoko.app.data.repository.AutomationRepository
import com.automapoko.app.domain.model.Automation
import com.automapoko.app.domain.model.AutomationStatus
import com.automapoko.app.domain.model.TriggerType
import com.automapoko.app.domain.usecase.DeleteAutomationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TriggerFilter { ALL, BLUETOOTH, WIFI, LOCATION }

data class HomeUiState(
    val automations: List<Automation> = emptyList(),
    val activeFilter: TriggerFilter = TriggerFilter.ALL,
    val isLoading: Boolean = true,
    val automationToDelete: Automation? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: AutomationRepository,
    private val deleteAutomationUseCase: DeleteAutomationUseCase
) : ViewModel() {

    private val _filter = MutableStateFlow(TriggerFilter.ALL)
    private val _automationToDelete = MutableStateFlow<Automation?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        repository.observeAll(),
        _filter,
        _automationToDelete
    ) { automations, filter, toDelete ->
        val filtered = when (filter) {
            TriggerFilter.ALL -> automations
            TriggerFilter.BLUETOOTH -> automations.filter { it.triggerType == TriggerType.BLUETOOTH }
            TriggerFilter.WIFI -> automations.filter { it.triggerType == TriggerType.WIFI }
            TriggerFilter.LOCATION -> automations.filter { it.triggerType == TriggerType.LOCATION }
        }
        HomeUiState(
            automations = filtered,
            activeFilter = filter,
            isLoading = false,
            automationToDelete = toDelete
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

    fun setFilter(filter: TriggerFilter) {
        _filter.value = filter
    }

    fun toggleStatus(automation: Automation) {
        viewModelScope.launch {
            val newStatus = when (automation.status) {
                AutomationStatus.ACTIVE -> AutomationStatus.PAUSED
                AutomationStatus.PAUSED -> AutomationStatus.ACTIVE
                AutomationStatus.INACTIVE -> AutomationStatus.INACTIVE // Não alterna — diagnóstico necessário
            }
            repository.updateStatus(automation.id, newStatus)
        }
    }

    fun requestDelete(automation: Automation) {
        _automationToDelete.value = automation
    }

    fun cancelDelete() {
        _automationToDelete.value = null
    }

    fun confirmDelete() {
        viewModelScope.launch {
            val automation = _automationToDelete.value ?: return@launch
            _automationToDelete.value = null
            deleteAutomationUseCase(automation)
        }
    }
}
