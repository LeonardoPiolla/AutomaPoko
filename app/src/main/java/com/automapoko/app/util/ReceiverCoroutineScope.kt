package com.automapoko.app.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Scope de coroutine para BroadcastReceivers.
 *
 * BroadcastReceivers têm vida muito curta — o sistema pode encerrar o processo
 * logo após onReceive() retornar. Por isso usamos goAsync() nos receivers
 * junto com este scope para garantir que a coroutine complete antes do encerramento.
 *
 * Singleton: compartilhado entre todos os receivers para evitar leaks.
 */
object ReceiverCoroutineScope {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}
