package it.airgap.beaconsdk.core.client

import it.airgap.beaconsdk.core.exception.BeaconException
import it.airgap.beaconsdk.core.message.BeaconResponse
import it.airgap.beaconsdk.core.transport.data.PairingRequest
import it.airgap.beaconsdk.core.transport.data.PairingResponse

public interface BeaconConsumer {
    /**
     * Sends the [response] in reply to a previously received request.
     *
     * @throws [BeaconException] if processing and sending the [response] failed.
     */
    @Throws(IllegalArgumentException::class, IllegalStateException::class, BeaconException::class)
    public suspend fun respond(response: BeaconResponse)

    public suspend fun pair(request: PairingRequest): PairingResponse
    public suspend fun pair(request: String): PairingResponse
}