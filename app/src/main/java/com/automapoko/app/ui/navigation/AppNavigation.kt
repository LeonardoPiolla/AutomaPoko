package com.automapoko.app.ui.navigation

/**
 * Rotas de navegação do AutomaPoko.
 * Usamos sealed class para ter type-safety nas rotas.
 */
sealed class Screen(val route: String) {
    /** Onboarding — exibido apenas na primeira abertura */
    data object Onboarding : Screen("onboarding")

    /** Tela principal — lista de automações */
    data object Home : Screen("home")

    /** Criar nova automação */
    data object CreateAutomation : Screen("create_automation")

    /** Editar automação existente */
    data class EditAutomation(val automationId: String = "{automationId}") :
        Screen("edit_automation/{automationId}") {
        fun createRoute(id: String) = "edit_automation/$id"
    }

    /** Detalhe de uma automação */
    data class AutomationDetail(val automationId: String = "{automationId}") :
        Screen("automation_detail/{automationId}") {
        fun createRoute(id: String) = "automation_detail/$id"
    }
}
