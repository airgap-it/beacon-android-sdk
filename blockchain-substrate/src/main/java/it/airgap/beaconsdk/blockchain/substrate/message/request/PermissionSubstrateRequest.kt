package it.airgap.beaconsdk.blockchain.substrate.message.request

import it.airgap.beaconsdk.blockchain.substrate.Substrate
import it.airgap.beaconsdk.blockchain.substrate.data.SubstrateAppMetadata
import it.airgap.beaconsdk.blockchain.substrate.data.SubstrateNetwork
import it.airgap.beaconsdk.blockchain.substrate.data.SubstratePermission
import it.airgap.beaconsdk.blockchain.substrate.extensions.ownAppMetadata
import it.airgap.beaconsdk.blockchain.substrate.internal.SubstrateBeaconConfiguration
import it.airgap.beaconsdk.core.client.BeaconClient
import it.airgap.beaconsdk.core.client.BeaconProducer
import it.airgap.beaconsdk.core.data.Connection
import it.airgap.beaconsdk.core.message.PermissionBeaconRequest

public data class PermissionSubstrateRequest internal constructor(
    override val id: String,
    override val version: String,
    override val blockchainIdentifier: String,
    override val senderId: String,
    override val origin: Connection.Id,
    override val destination: Connection.Id?,
    override val appMetadata: SubstrateAppMetadata,
    public val scopes: List<SubstratePermission.Scope>,
    public val networks: List<SubstrateNetwork>,
) : PermissionBeaconRequest() {
    public companion object {}
}

public suspend fun <T> PermissionSubstrateRequest(
    networks: List<SubstrateNetwork>,
    scopes: List<SubstratePermission.Scope> = listOf(SubstratePermission.Scope.SignPayloadJson, SubstratePermission.Scope.SignPayloadRaw, SubstratePermission.Scope.Transfer),
    producer: T,
    transportType: Connection.Type = Connection.Type.P2P,
) : PermissionSubstrateRequest where T : BeaconClient<*>, T : BeaconProducer {
    val requestMetadata = producer.prepareRequest(transportType)

    return PermissionSubstrateRequest(
        id = requestMetadata.id,
        version = SubstrateBeaconConfiguration.MESSAGE_VERSION,
        blockchainIdentifier = Substrate.IDENTIFIER,
        senderId = requestMetadata.senderId,
        appMetadata = producer.ownAppMetadata(),
        origin = requestMetadata.origin,
        destination = requestMetadata.destination,
        networks = networks,
        scopes = scopes,
    )
}