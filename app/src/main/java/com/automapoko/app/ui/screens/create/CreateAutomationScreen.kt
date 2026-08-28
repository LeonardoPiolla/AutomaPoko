package com.automapoko.app.ui.screens.create

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.automapoko.app.domain.model.*
import com.automapoko.app.ui.screens.create.steps.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAutomationScreen(
    editingAutomationId: String? = null,
    onNavigateBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: CreateAutomationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Carrega automação para edição se necessário
    LaunchedEffect(editingAutomationId) {
        if (editingAutomationId != null) {
            viewModel.loadForEditing(editingAutomationId)
        }
    }

    // Navega de volta quando salvo com sucesso
    LaunchedEffect(uiState.savedSuccessfully) {
        if (uiState.savedSuccessfully) onSaved()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.isEditing) "Editar automação" else "Nova automação",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Indicador de progresso
            StepProgressIndicator(
                currentStep = uiState.step,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Conteúdo do passo atual
            AnimatedContent(
                targetState = uiState.step,
                transitionSpec = {
                    slideInHorizontally { it } + fadeIn() togetherWith
                    slideOutHorizontally { -it } + fadeOut()
                },
                modifier = Modifier.weight(1f),
                label = "step_transition"
            ) { step ->
                when (step) {
                    CreateStep.NAME -> NameStep(
                        name = uiState.name,
                        nameError = uiState.nameError,
                        onNameChange = viewModel::setName
                    )
                    CreateStep.TRIGGER_TYPE -> TriggerTypeStep(
                        selectedType = uiState.selectedTriggerType,
                        onTypeSelected = viewModel::setTriggerType
                    )
                    CreateStep.TRIGGER_CONFIG -> TriggerConfigStep(
                        triggerType = uiState.selectedTriggerType,
                        currentConfig = uiState.triggerConfig,
                        onConfigChanged = viewModel::setTriggerConfig
                    )
                    CreateStep.ACTION_TYPE -> ActionTypeStep()
                    CreateStep.APP_SELECT -> AppSelectStep(
                        apps = uiState.installedApps,
                        isLoading = uiState.isLoadingApps,
                        selectedApp = uiState.selectedApp,
                        onAppSelected = viewModel::setSelectedApp
                    )
                    CreateStep.REVIEW -> ReviewStep(
                        name = uiState.name,
                        triggerConfig = uiState.triggerConfig,
                        triggerType = uiState.selectedTriggerType,
                        selectedApp = uiState.selectedApp
                    )
                }
            }

            // Botões de navegação
            NavigationButtons(
                step = uiState.step,
                canProceed = canProceed(uiState),
                isSaving = uiState.isSaving,
                onBack = viewModel::goToPreviousStep,
                onNext = viewModel::goToNextStep
            )
        }
    }
}

@Composable
private fun StepProgressIndicator(
    currentStep: CreateStep,
    modifier: Modifier = Modifier
) {
    val steps = CreateStep.entries
    val currentIndex = steps.indexOf(currentStep)
    val total = steps.size

    Column(modifier = modifier) {
        LinearProgressIndicator(
            progress = { (currentIndex + 1).toFloat() / total.toFloat() },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Passo ${currentIndex + 1} de $total",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun NavigationButtons(
    step: CreateStep,
    canProceed: Boolean,
    isSaving: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (step != CreateStep.NAME) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.ArrowBack, contentDescription = null,
                    modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Voltar")
            }
        }

        Button(
            onClick = onNext,
            enabled = canProceed && !isSaving,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                val label = when (step) {
                    CreateStep.REVIEW -> "Salvar automação"
                    else -> "Continuar"
                }
                Text(label, fontWeight = FontWeight.SemiBold)
                if (step != CreateStep.REVIEW) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Filled.ArrowForward, contentDescription = null,
                        modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

private fun canProceed(state: CreateUiState): Boolean = when (state.step) {
    CreateStep.NAME -> state.name.isNotBlank()
    CreateStep.TRIGGER_TYPE -> state.selectedTriggerType != null
    CreateStep.TRIGGER_CONFIG -> state.triggerConfig != null
    CreateStep.ACTION_TYPE -> true
    CreateStep.APP_SELECT -> state.selectedApp != null
    CreateStep.REVIEW -> true
}
