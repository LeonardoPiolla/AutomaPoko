package com.automapoko.app.ui.screens.create.steps

import android.annotation.SuppressLint
import android.location.Geocoder
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.automapoko.app.domain.model.TriggerConfig
import com.automapoko.app.domain.model.TriggerEvent
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocationConfigStep(
    currentConfig: TriggerConfig.LocationConfig?,
    onConfigChanged: (TriggerConfig) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    // Estado do local selecionado
    var selectedLatLng by remember {
        mutableStateOf(
            currentConfig?.let { LatLng(it.latitude, it.longitude) }
        )
    }
    var locationName by remember { mutableStateOf(currentConfig?.locationName ?: "") }
    var radiusMeters by remember { mutableStateOf(currentConfig?.radiusMeters ?: 200f) }
    var selectedEvent by remember { mutableStateOf(currentConfig?.event ?: TriggerEvent.ENTER) }
    var searchQuery by remember { mutableStateOf("") }
    var isGeocodingLoading by remember { mutableStateOf(false) }
    var isLocating by remember { mutableStateOf(false) }

    // Câmera do mapa
    val defaultLatLng = LatLng(-23.5505, -46.6333) // São Paulo como fallback
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            selectedLatLng ?: defaultLatLng, 14f
        )
    }

    // Permissões de localização
    val locationPermissions = rememberMultiplePermissionsState(
        permissions = listOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    val backgroundLocationPermission = rememberPermissionState(
        permission = android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
    )

    // Atualiza config sempre que estado mudar
    LaunchedEffect(selectedLatLng, locationName, radiusMeters, selectedEvent) {
        val latlng = selectedLatLng ?: return@LaunchedEffect
        if (locationName.isBlank()) return@LaunchedEffect
        onConfigChanged(
            TriggerConfig.LocationConfig(
                latitude = latlng.latitude,
                longitude = latlng.longitude,
                radiusMeters = radiusMeters,
                locationName = locationName,
                event = selectedEvent
            )
        )
    }

    // Aviso sobre localização em background
    var showBackgroundLocationDialog by remember { mutableStateOf(false) }
    LaunchedEffect(locationPermissions.allPermissionsGranted) {
        if (locationPermissions.allPermissionsGranted
            && !backgroundLocationPermission.status.isGranted
        ) {
            showBackgroundLocationDialog = true
        }
    }

    if (showBackgroundLocationDialog) {
        AlertDialog(
            onDismissRequest = { showBackgroundLocationDialog = false },
            icon = {
                Icon(Icons.Filled.LocationOn, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary)
            },
            title = { Text("Localização em segundo plano") },
            text = {
                Text(
                    "Para que a automação funcione com o app fechado ou a tela bloqueada, " +
                    "é necessário conceder acesso à localização em segundo plano.\n\n" +
                    "Nas configurações que vão abrir, selecione \"Permitir sempre\"."
                )
            },
            confirmButton = {
                Button(onClick = {
                    showBackgroundLocationDialog = false
                    backgroundLocationPermission.launchPermissionRequest()
                }) {
                    Text("Conceder permissão")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackgroundLocationDialog = false }) {
                    Text("Agora não")
                }
            }
        )
    }

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
                imageVector = Icons.Filled.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = "Configurar localização",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (!locationPermissions.allPermissionsGranted) {
            PermissionRequestCard(
                message = "Para usar automações por localização, o AutomaPoko precisa de acesso à sua localização.",
                onRequest = { locationPermissions.launchMultiplePermissionRequest() }
            )
        } else {
            // Busca por endereço
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Buscar endereço ou local") },
                placeholder = { Text("Ex: Av. Paulista, 1000") },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (isGeocodingLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else if (searchQuery.isNotBlank()) {
                        IconButton(onClick = {
                            keyboardController?.hide()
                            scope.launch {
                                isGeocodingLoading = true
                                val result = geocodeAddress(context, searchQuery)
                                if (result != null) {
                                    selectedLatLng = result.first
                                    locationName = result.second
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngZoom(result.first, 15f)
                                    )
                                }
                                isGeocodingLoading = false
                            }
                        }) {
                            Icon(Icons.Filled.Search, contentDescription = "Buscar")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        keyboardController?.hide()
                        scope.launch {
                            isGeocodingLoading = true
                            val result = geocodeAddress(context, searchQuery)
                            if (result != null) {
                                selectedLatLng = result.first
                                locationName = result.second
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newLatLngZoom(result.first, 15f)
                                )
                            }
                            isGeocodingLoading = false
                        }
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary
                )
            )

            // Botão: usar localização atual
            OutlinedButton(
                onClick = {
                    scope.launch {
                        isLocating = true
                        val location = getCurrentLocation(context)
                        if (location != null) {
                            val latlng = LatLng(location.first, location.second)
                            selectedLatLng = latlng
                            locationName = reverseGeocode(context, latlng) ?: "Local atual"
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(latlng, 15f)
                            )
                        }
                        isLocating = false
                    }
                },
                enabled = !isLocating,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLocating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Obtendo localização…")
                } else {
                    Icon(Icons.Filled.MyLocation, contentDescription = null,
                        modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Usar localização atual")
                }
            }

            // Mapa — o usuário também pode tocar para selecionar o local
            Text(
                text = if (selectedLatLng != null)
                    "Local: $locationName"
                else
                    "Toque no mapa para selecionar o local",
                style = MaterialTheme.typography.bodySmall,
                color = if (selectedLatLng != null) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (selectedLatLng != null) FontWeight.SemiBold else FontWeight.Normal
            )

            GoogleMap(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                cameraPositionState = cameraPositionState,
                onMapClick = { latlng ->
                    selectedLatLng = latlng
                    scope.launch {
                        locationName = reverseGeocode(context, latlng) ?: "Local selecionado"
                    }
                },
                properties = MapProperties(isMyLocationEnabled = true),
                uiSettings = MapUiSettings(
                    myLocationButtonEnabled = false,
                    zoomControlsEnabled = false
                )
            ) {
                selectedLatLng?.let { latlng ->
                    Marker(
                        state = MarkerState(position = latlng),
                        title = locationName
                    )
                    Circle(
                        center = latlng,
                        radius = radiusMeters.toDouble(),
                        strokeColor = MaterialTheme.colorScheme.primary,
                        fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        strokeWidth = 2f
                    )
                }
            }

            // Raio
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Raio de ativação",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${radiusMeters.toInt()} metros",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Slider(
                    value = radiusMeters,
                    onValueChange = { radiusMeters = it },
                    valueRange = 100f..2000f,
                    steps = 18, // Passos de ~100m
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("100 m", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("2000 m", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        label = "Entrar no local",
                        selected = selectedEvent == TriggerEvent.ENTER,
                        onClick = { selectedEvent = TriggerEvent.ENTER },
                        modifier = Modifier.weight(1f)
                    )
                    EventChip(
                        label = "Sair do local",
                        selected = selectedEvent == TriggerEvent.EXIT,
                        onClick = { selectedEvent = TriggerEvent.EXIT },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
private suspend fun getCurrentLocation(context: android.content.Context): Pair<Double, Double>? =
    withContext(Dispatchers.IO) {
        try {
            val client = LocationServices.getFusedLocationProviderClient(context)
            val location = client.lastLocation.await()
            location?.let { Pair(it.latitude, it.longitude) }
        } catch (e: Exception) {
            null
        }
    }

private suspend fun geocodeAddress(
    context: android.content.Context,
    address: String
): Pair<LatLng, String>? = withContext(Dispatchers.IO) {
    try {
        val geocoder = Geocoder(context, Locale("pt", "BR"))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            var result: Pair<LatLng, String>? = null
            geocoder.getFromLocationName(address, 1) { addresses ->
                val addr = addresses.firstOrNull()
                if (addr != null) {
                    result = Pair(
                        LatLng(addr.latitude, addr.longitude),
                        addr.getAddressLine(0) ?: address
                    )
                }
            }
            // Aguarda resultado assíncrono
            kotlinx.coroutines.delay(1500)
            result
        } else {
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocationName(address, 1)
            val addr = addresses?.firstOrNull()
            addr?.let {
                Pair(
                    LatLng(it.latitude, it.longitude),
                    it.getAddressLine(0) ?: address
                )
            }
        }
    } catch (e: Exception) {
        null
    }
}

private suspend fun reverseGeocode(
    context: android.content.Context,
    latLng: LatLng
): String? = withContext(Dispatchers.IO) {
    try {
        val geocoder = Geocoder(context, Locale("pt", "BR"))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            var result: String? = null
            geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1) { addresses ->
                result = addresses.firstOrNull()?.getAddressLine(0)
            }
            kotlinx.coroutines.delay(1500)
            result
        } else {
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            addresses?.firstOrNull()?.getAddressLine(0)
        }
    } catch (e: Exception) {
        null
    }
}
