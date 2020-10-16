package it.airgap.beaconsdk.client

import it.airgap.beaconsdk.message.BeaconMessage
import kotlinx.coroutines.flow.Flow

interface BeaconClient {
    val isInitialized: Boolean

    val name: String
    val beaconId: String

    suspend fun init()
    suspend fun connect(): Flow<Result<BeaconMessage.Request>>
    suspend fun respond(message: BeaconMessage.Response)
}