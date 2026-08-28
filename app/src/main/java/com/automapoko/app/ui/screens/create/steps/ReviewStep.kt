package com.automapoko.app.ui.screens.create.steps

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.automapoko.app.domain.model.*
import com.automapoko.app.ui.screens.create.InstalledApp

@Composable
fun ReviewStep(
    name: String,
    triggerType: TriggerType?,
    triggerConfig: TriggerConfig?,
    selectedApp: InstalledApp?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Cabeçalho
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = "Tudo certo!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Revise o resumo abaixo antes de salvar.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Card de resumo
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Nome
                ReviewRow(
                    icon = Icons.Filled.DriveFileRenameOutline,
                    label = "Nome",
                    value = name
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Gatilho
                val triggerIcon = when (triggerType) {
                    TriggerType.BLUETOOTH -> Icons.Filled.Bluetooth
                    TriggerType.WIFI -> Icons.Filled.Wifi
                    TriggerType.LOCATION -> Icons.Filled.LocationOn
                    null -> Icons.Filled.Bolt
                }
                val triggerLabel = when (triggerType) {
                    TriggerType.BLUETOOTH -> "Bluetooth"
                    TriggerType.WIFI -> "Wi-Fi"
                    TriggerType.LOCATION -> "Localização"
                    null -> "—"
                }
                val triggerDetail = buildTriggerDetail(triggerConfig)

                ReviewRow(
                    icon = triggerIcon,
                    label = "Quando",
                    value = "$triggerLabel — $triggerDetail"
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Ação
                ReviewRow(
                    icon = Icons.Filled.OpenInNew,
                    label = "Então",
                    value = "Abrir ${selectedApp?.appName ?: "—"}"
                )
            }
        }

        // Informativo
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    Icons.Filled.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "A automação ficará ativa imediatamente após ser salva. " +
                        "Você pode pausá-la ou editá-la a qualquer momento.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ReviewRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
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

private fun buildTriggerDetail(config: TriggerConfig?): String = when (config) {
    is TriggerConfig.BluetoothConfig -> {
        val event = if (config.event == TriggerEvent.CONNECTED) "conectar" else "desconectar"
        "ao $event em ${config.deviceName}"
    }
    is TriggerConfig.WifiConfig -> {
        val event = if (config.event == TriggerEvent.CONNECTED) "conectar" else "desconectar"
        "ao $event em ${config.ssid}"
    }
    is TriggerConfig.LocationConfig -> {
        val event = if (config.event == TriggerEvent.ENTER) "entrar em" else "sair de"
        "ao $event ${config.locationName}, raio ${config.radiusMeters.toInt()}m"
    }
    null -> "—"
}
