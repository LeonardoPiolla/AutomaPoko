package com.automapoko.app.domain.usecase

import com.automapoko.app.data.repository.AutomationRepository
import com.automapoko.app.domain.model.Automation
import com.automapoko.app.domain.model.TriggerType
import javax.inject.Inject

class DeleteAutomationUseCase @Inject constructor(
    private val repository: AutomationRepository,
    private val geofenceManager: GeofenceManager
) {
    suspend operator fun invoke(automation: Automation) {
        // Cancela geofence antes de excluir (evita geofences órfãos)
        if (automation.triggerType == TriggerType.LOCATION) {
            geofenceManager.removeGeofence(automation.id)
        }
        repository.delete(automation)
    }
}
