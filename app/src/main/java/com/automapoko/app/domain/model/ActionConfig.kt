package com.automapoko.app.domain.model

/**
 * Configuração de ação a executar quando a automação disparar.
 * Sealed class permite adicionar novas ações futuramente sem quebrar código existente.
 */
sealed class ActionConfig {

    /**
     * Ação: abrir um aplicativo instalado.
     * @param packageName Identificador único do app (ex: "com.spotify.music")
     * @param appName Nome amigável para exibição (ex: "Spotify")
     */
    data class OpenAppConfig(
        val packageName: String,
        val appName: String
    ) : ActionConfig()
}
