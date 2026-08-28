package com.automapoko.app.domain.model

enum class AutomationStatus {
    ACTIVE,     // Ativa e monitorando
    INACTIVE,   // Inativa por diagnóstico (permissão negada, app não encontrado, etc.)
    PAUSED      // Pausada manualmente pelo usuário
}
