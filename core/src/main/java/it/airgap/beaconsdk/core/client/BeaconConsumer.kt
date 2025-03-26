package it.airgap.beaconsdk.core.client

import it.airgap.beaconsdk.core.exception.BeaconException
import it.airgap.beaconsdk.core.message.BeaconResponse
import it.airgap.beaconsdk.core.transport.data.PairingRequest
import it.airgap.beaconsdk.core.transport.data.PairingResponse

public interface BeaconConsumer {
    public val senderId: String
    /**
     * Sends the [response] in reply to a previously received request.
     *
     * @throws [BeaconException] if processing and sending the [response] failed.
     */
    @Throws(BeaconException::class)
    public suspend fun respond(response: BeaconResponse)

    /**
     * Responds to the pairing [request] and finalizes the process. Returns
     * the pairing response, which, depending on the transport used, may require additional handling.
     *
     * @throws [BeaconException] if the process failed.
     */
    @Throws(BeaconException::class)
    public suspend fun pair(request: PairingRequest): PairingResponse

    /**
     * Responds to the pairing [request] and finalizes the process. Returns
     * the pairing response, which, depending on the transport used, may require additional handling.
     *
     * @throws [BeaconException] if the process failed.
     */
    public suspend fun pair(request: String): PairingResponse
}