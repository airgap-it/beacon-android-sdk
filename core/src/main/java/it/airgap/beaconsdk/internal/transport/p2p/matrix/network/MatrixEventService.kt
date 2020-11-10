package it.airgap.beaconsdk.internal.transport.p2p.matrix.network

import it.airgap.beaconsdk.internal.network.HttpClient
import it.airgap.beaconsdk.internal.network.data.ApplicationJson
import it.airgap.beaconsdk.internal.network.data.BearerHeader
import it.airgap.beaconsdk.internal.network.data.HttpParameter
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.api.event.MatrixEventResponse
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.api.sync.MatrixSyncResponse
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.api.sync.MatrixSyncStateEvent
import it.airgap.beaconsdk.internal.utils.InternalResult
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*

internal class MatrixEventService(private val httpClient: HttpClient) {
    private var ongoingSync: Flow<InternalResult<MatrixSyncResponse>>? = null

    suspend fun sync(
        accessToken: String,
        since: String? = null,
        timeout: Long? = null,
    ): InternalResult<MatrixSyncResponse> {
        val parameters = mutableListOf<HttpParameter>().apply {
            since?.let { add("since" to it) }
            timeout?.let { add("timeout" to it.toString()) }
        }.toList()

        return getOngoingOrRun(this::ongoingSync::get, this::ongoingSync::set) {
            httpClient.get(
                "/sync",
                headers = listOf(BearerHeader(accessToken), ApplicationJson()),
                parameters = parameters,
                timeoutMillis = timeout?.times(1.5)?.toLong()
            )
        }.single()
    }

    suspend fun sendTextMessage(
        accessToken: String,
        roomId: String,
        txnId: String,
        message: String,
    ): InternalResult<MatrixEventResponse> =
        sendEvent(
            accessToken,
            roomId,
            MatrixSyncStateEvent.TYPE_MESSAGE,
            txnId,
            MatrixSyncStateEvent.Message.Content(
                MatrixSyncStateEvent.Message.TYPE_TEXT,
                message,
            )
        )

    private suspend inline fun <reified T : Any> sendEvent(
        accessToken: String,
        roomId: String,
        eventType: String,
        txnId: String,
        content: T,
    ): InternalResult<MatrixEventResponse> =
        httpClient.put<T, MatrixEventResponse>(
            "/rooms/$roomId/send/$eventType/$txnId",
            body = content,
            headers = listOf(BearerHeader(accessToken), ApplicationJson())
        ).single()

    private inline fun <T : Any> getOngoingOrRun(
        getter: () -> Flow<InternalResult<T>>?,
        crossinline setter: (Flow<InternalResult<T>>?) -> Unit,
        action: () -> Flow<InternalResult<T>>,
    ): Flow<InternalResult<T>> =
        getter() ?: run {
            val scope = CoroutineScope(CoroutineName("sync") + Dispatchers.Default)
            action()
                .shareIn(scope, SharingStarted.Eagerly, replay = 1)
                .take(1)
                .onCompletion {
                    setter(null)
                    scope.cancel()
                }
                .also { setter(it) }
        }
}