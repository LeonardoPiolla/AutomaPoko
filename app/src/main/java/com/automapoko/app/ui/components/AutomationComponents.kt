package com.automapoko.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.automapoko.app.domain.model.*

// ======================== CHIP DE STATUS ========================

@Composable
fun StatusChip(status: AutomationStatus, modifier: Modifier = Modifier) {
    val (label, color) = when (status) {
        AutomationStatus.ACTIVE -> Pair(
            "Ativa",
            MaterialTheme.colorScheme.primary
        )
        AutomationStatus.INACTIVE -> Pair(
            "Inativa",
            MaterialTheme.colorScheme.error
        )
        AutomationStatus.PAUSED -> Pair(
            "Pausada",
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ======================== ÍCONE DE GATILHO ========================

@Composable
fun TriggerIcon(type: TriggerType, modifier: Modifier = Modifier) {
    val icon = when (type) {
        TriggerType.BLUETOOTH -> Icons.Filled.Bluetooth
        TriggerType.WIFI -> Icons.Filled.Wifi
        TriggerType.LOCATION -> Icons.Filled.LocationOn
    }
    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = modifier,
        tint = MaterialTheme.colorScheme.primary
    )
}

// ======================== DESCRIÇÃO DO GATILHO ========================

@Composable
fun TriggerDescription(config: TriggerConfig, event: TriggerEvent) {
    val text = when (config) {
        is TriggerConfig.BluetoothConfig -> {
            val eventLabel = if (event == TriggerEvent.CONNECTED) "conectar" else "desconectar"
            "Bluetooth ao $eventLabel: ${config.deviceName}"
        }
        is TriggerConfig.WifiConfig -> {
            val eventLabel = if (event == TriggerEvent.CONNECTED) "conectar" else "desconectar"
            "Wi-Fi ao $eventLabel: ${config.ssid}"
        }
        is TriggerConfig.LocationConfig -> {
            val eventLabel = if (event == TriggerEvent.ENTER) "Entrar em" else "Sair de"
            "$eventLabel: ${config.locationName}"
        }
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

// ======================== BANNER DE DIAGNÓSTICO ========================

@Composable
fun DiagnosticBanner(message: String, onTap: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        onClick = onTap
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ======================== ITEM DE HISTÓRICO ========================

@Composable
fun ExecutionLogItem(log: ExecutionLog) {
    val isSuccess = log.status == ExecutionStatus.SUCCESS
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = if (isSuccess) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
            contentDescription = null,
            tint = if (isSuccess) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(20.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isSuccess) "Executada com sucesso" else "Falha na execução",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!isSuccess && log.failureReason != null) {
                Text(
                    text = log.failureReason,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = formatInstant(log.executedAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ======================== HELPERS ========================

private fun formatInstant(instant: java.time.Instant): String {
    val formatter = java.time.format.DateTimeFormatter
        .ofPattern("dd/MM/yyyy HH:mm")
        .withZone(java.time.ZoneId.systemDefault())
    return formatter.format(instant)
}
