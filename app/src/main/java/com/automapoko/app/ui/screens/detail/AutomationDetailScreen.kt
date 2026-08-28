package com.automapoko.app.ui.screens.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.automapoko.app.domain.model.*
import com.automapoko.app.ui.components.*
import com.automapoko.app.util.PermissionChecker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomationDetailScreen(
    automationId: String,
    onNavigateBack: () -> Unit,
    onEdit: (String) -> Unit,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissionChecker = remember { PermissionChecker(context) }

    LaunchedEffect(automationId) {
        viewModel.loadAutomation(automationId)
    }

    // Navega de volta quando excluída
    LaunchedEffect(uiState.deleted) {
        if (uiState.deleted) onNavigateBack()
    }

    // Dialog de confirmação de exclusão
    if (uiState.showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            title = { Text("Excluir automação?") },
            text = {
                Text(
                    "A automação \"${uiState.automation?.name}\" e todo o seu histórico " +
                    "serão excluídos permanentemente."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::confirmDelete,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Excluir") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelDelete) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.automation?.name ?: "",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        uiState.automation?.let { onEdit(it.id) }
                    }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Editar")
                    }
                    IconButton(onClick = viewModel::requestDelete) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Excluir",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (uiState.isLoading || uiState.automation == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            return@Scaffold
        }

        val automation = uiState.automation!!
        val config = automation.triggerConfig
        val appName = (automation.actionConfig as? ActionConfig.OpenAppConfig)?.appName ?: ""

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card de status e toggle
            item {
                StatusCard(
                    automation = automation,
                    onToggle = viewModel::toggleStatus
                )
            }

            // Diagnóstico (se inativa)
            if (automation.status == AutomationStatus.INACTIVE
                && automation.diagnosticMessage != null
            ) {
                item {
                    DiagnosticBanner(
                        message = automation.diagnosticMessage,
                        onTap = {
                            // Abre configurações relevantes
                            if (automation.diagnosticMessage.contains("overlay", ignoreCase = true)
                                || automation.diagnosticMessage.contains("sobre outros", ignoreCase = true)
                            ) {
                                permissionChecker.openOverlaySettings(context as android.app.Activity)
                            } else {
                                permissionChecker.openAppSettings(context as android.app.Activity)
                            }
                        }
                    )
                }
            }

            // Card de detalhes da automação
            item {
                DetailCard(
                    config = config,
                    appName = appName,
                    triggerType = automation.triggerType
                )
            }

            // Histórico
            item {
                Text(
                    text = "Histórico (últimos 7 dias)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (uiState.logs.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier.padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Nenhuma execução nos últimos 7 dias",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            uiState.logs.forEachIndexed { index, log ->
                                ExecutionLogItem(log = log)
                                if (index < uiState.logs.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusCard(
    automation: Automation,
    onToggle: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Status",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                StatusChip(automation.status)
            }

            if (automation.status != AutomationStatus.INACTIVE) {
                Switch(
                    checked = automation.status == AutomationStatus.ACTIVE,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

@Composable
private fun DetailCard(
    config: TriggerConfig,
    appName: String,
    triggerType: TriggerType
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Configuração",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Gatilho
            DetailRow(
                icon = when (triggerType) {
                    TriggerType.BLUETOOTH -> Icons.Filled.Bluetooth
                    TriggerType.WIFI -> Icons.Filled.Wifi
                    TriggerType.LOCATION -> Icons.Filled.LocationOn
                },
                label = "Gatilho",
                value = buildDetailValue(config)
            )

            // Ação
            DetailRow(
                icon = Icons.Filled.OpenInNew,
                label = "Ação",
                value = "Abrir $appName"
            )
        }
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun buildDetailValue(config: TriggerConfig): String = when (config) {
    is TriggerConfig.BluetoothConfig -> {
        val event = if (config.event == TriggerEvent.CONNECTED) "conectar" else "desconectar"
        "Bluetooth ao $event em \"${config.deviceName}\""
    }
    is TriggerConfig.WifiConfig -> {
        val event = if (config.event == TriggerEvent.CONNECTED) "conectar" else "desconectar"
        "Wi-Fi ao $event em \"${config.ssid}\""
    }
    is TriggerConfig.LocationConfig -> {
        val event = if (config.event == TriggerEvent.ENTER) "entrar em" else "sair de"
        "Ao $event \"${config.locationName}\" (raio: ${config.radiusMeters.toInt()}m)"
    }
}
