package com.automapoko.app.domain.usecase

import com.automapoko.app.data.repository.AutomationRepository
import com.automapoko.app.domain.model.Automation
import com.automapoko.app.domain.model.TriggerType
import javax.inject.Inject

class SaveAutomationUseCase @Inject constructor(
    private val repository: AutomationRepository,
    private val geofenceManager: GeofenceManager
) {
    suspend operator fun invoke(automation: Automation) {
        repository.save(automation)
        // Se for automação de localização, registra o geofence imediatamente
        if (automation.triggerType == TriggerType.LOCATION) {
            geofenceManager.registerGeofence(automation)
        }
    }
}
