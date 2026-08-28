package com.automapoko.app.ui.screens.create.steps

import androidx.compose.runtime.Composable
import com.automapoko.app.domain.model.TriggerConfig
import com.automapoko.app.domain.model.TriggerType

@Composable
fun TriggerConfigStep(
    triggerType: TriggerType?,
    currentConfig: TriggerConfig?,
    onConfigChanged: (TriggerConfig) -> Unit
) {
    when (triggerType) {
        TriggerType.BLUETOOTH -> BluetoothConfigStep(
            currentConfig = currentConfig as? TriggerConfig.BluetoothConfig,
            onConfigChanged = onConfigChanged
        )
        TriggerType.WIFI -> WifiConfigStep(
            currentConfig = currentConfig as? TriggerConfig.WifiConfig,
            onConfigChanged = onConfigChanged
        )
        TriggerType.LOCATION -> LocationConfigStep(
            currentConfig = currentConfig as? TriggerConfig.LocationConfig,
            onConfigChanged = onConfigChanged
        )
        null -> Unit
    }
}
