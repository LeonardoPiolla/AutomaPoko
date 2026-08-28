package com.automapoko.app.ui.screens.create.steps

import android.content.Context
import android.net.wifi.WifiManager
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.automapoko.app.domain.model.TriggerConfig
import com.automapoko.app.domain.model.TriggerEvent
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun WifiConfigStep(
    currentConfig: TriggerConfig.WifiConfig?,
    onConfigChanged: (TriggerConfig) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedSsid by remember { mutableStateOf(currentConfig?.ssid) }
    var selectedEvent by remember { mutableStateOf(currentConfig?.event ?: TriggerEvent.CONNECTED) }
    var scannedNetworks by remember { mutableStateOf<List<String>>(emptyList()) }
    var isScanning by remember { mutableStateOf(false) }

    val locationPermissions = rememberMultiplePermissionsState(
        permissions = listOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_WIFI_STATE,
            android.Manifest.permission.CHANGE_WIFI_STATE
        )
    )

    // Atualiza config quando seleção muda
    LaunchedEffect(selectedSsid, selectedEvent) {
        val ssid = selectedSsid ?: return@LaunchedEffect
        onConfigChanged(TriggerConfig.WifiConfig(ssid = ssid, event = selectedEvent))
    }

    // Inicia scan automaticamente quando permissões concedidas
    LaunchedEffect(locationPermissions.allPermissionsGranted) {
        if (locationPermissions.allPermissionsGranted) {
            isScanning = true
            scope.launch {
                scannedNetworks = scanWifiNetworks(context)
                isScanning = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Cabeçalho
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                imageVector = Icons.Filled.Wifi,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = "Configurar Wi-Fi",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (!locationPermissions.allPermissionsGranted) {
            PermissionRequestCard(
                message = "Para identificar redes Wi-Fi, o AutomaPoko precisa de permissão de localização. Isso é exigido pelo Android para leitura de SSIDs.",
                onRequest = { locationPermissions.launchMultiplePermissionRequest() }
            )
        } else {
            // Lista de redes
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Redes disponíveis",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    // Botão de reescanear
                    IconButton(
                        onClick = {
                            scope.launch {
                                isScanning = true
                                scannedNetworks = scanWifiNetworks(context)
                                isScanning = false
                            }
                        },
                        enabled = !isScanning
                    ) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = "Buscar novamente",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                when {
                    isScanning -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    text = "Buscando redes…",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    scannedNetworks.isEmpty() -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.WifiOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Nenhuma rede encontrada. Certifique-se de que o Wi-Fi está ativo e tente novamente.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    else -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.heightIn(max = 260.dp)
                        ) {
                            items(scannedNetworks) { ssid ->
                                WifiNetworkItem(
                                    ssid = ssid,
                                    selected = selectedSsid == ssid,
                                    onClick = { selectedSsid = ssid }
                                )
                            }
                        }
                    }
                }
            }

            // Evento
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Disparar quando",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EventChip(
                        label = "Conectar",
                        selected = selectedEvent == TriggerEvent.CONNECTED,
                        onClick = { selectedEvent = TriggerEvent.CONNECTED },
                        modifier = Modifier.weight(1f)
                    )
                    EventChip(
                        label = "Desconectar",
                        selected = selectedEvent == TriggerEvent.DISCONNECTED,
                        onClick = { selectedEvent = TriggerEvent.DISCONNECTED },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun WifiNetworkItem(
    ssid: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Filled.Wifi,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = ssid,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (selected) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private suspend fun scanWifiNetworks(context: Context): List<String> =
    withContext(Dispatchers.IO) {
        try {
            val wifiManager =
                context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

            @Suppress("DEPRECATION")
            val results = wifiManager.scanResults ?: emptyList()

            results
                .mapNotNull { result ->
                    val ssid = result.SSID.removeSurrounding("\"")
                    if (ssid.isBlank()) null else ssid
                }
                .distinct()
                .sorted()
        } catch (e: Exception) {
            emptyList()
        }
    }
