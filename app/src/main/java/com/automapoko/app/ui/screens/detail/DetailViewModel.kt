package com.automapoko.app.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.automapoko.app.data.repository.AutomationRepository
import com.automapoko.app.domain.model.Automation
import com.automapoko.app.domain.model.AutomationStatus
import com.automapoko.app.domain.model.ExecutionLog
import com.automapoko.app.domain.usecase.DeleteAutomationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val automation: Automation? = null,
    val logs: List<ExecutionLog> = emptyList(),
    val isLoading: Boolean = true,
    val showDeleteConfirmation: Boolean = false,
    val deleted: Boolean = false
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: AutomationRepository,
    private val deleteAutomationUseCase: DeleteAutomationUseCase
) : ViewModel() {

    private val _automationId = MutableStateFlow<String?>(null)
    private val _showDeleteConfirmation = MutableStateFlow(false)
    private val _deleted = MutableStateFlow(false)

    val uiState: StateFlow<DetailUiState> = combine(
        _automationId.filterNotNull().flatMapLatest { id ->
            repository.observeById(id)
        },
        _automationId.filterNotNull().flatMapLatest { id ->
            repository.observeLogsByAutomation(id)
        },
        _showDeleteConfirmation,
        _deleted
    ) { automation, logs, showDelete, deleted ->
        DetailUiState(
            automation = automation,
            logs = logs,
            isLoading = false,
            showDeleteConfirmation = showDelete,
            deleted = deleted
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DetailUiState()
    )

    fun loadAutomation(automationId: String) {
        _automationId.value = automationId
    }

    fun toggleStatus() {
        viewModelScope.launch {
            val automation = uiState.value.automation ?: return@launch
            val newStatus = when (automation.status) {
                AutomationStatus.ACTIVE -> AutomationStatus.PAUSED
                AutomationStatus.PAUSED -> AutomationStatus.ACTIVE
                AutomationStatus.INACTIVE -> AutomationStatus.INACTIVE
            }
            repository.updateStatus(automation.id, newStatus)
        }
    }

    fun requestDelete() { _showDeleteConfirmation.value = true }
    fun cancelDelete() { _showDeleteConfirmation.value = false }

    fun confirmDelete() {
        viewModelScope.launch {
            val automation = uiState.value.automation ?: return@launch
            _showDeleteConfirmation.value = false
            deleteAutomationUseCase(automation)
            _deleted.value = true
        }
    }
}
