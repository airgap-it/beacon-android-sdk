package it.airgap.beaconsdk.blockchain.tezos.message.request

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.blockchain.tezos.Tezos
import it.airgap.beaconsdk.blockchain.tezos.client.ownAppMetadata
import it.airgap.beaconsdk.blockchain.tezos.data.TezosAppMetadata
import it.airgap.beaconsdk.blockchain.tezos.data.TezosNetwork
import it.airgap.beaconsdk.blockchain.tezos.data.TezosPermission
import it.airgap.beaconsdk.blockchain.tezos.message.response.PermissionTezosResponse
import it.airgap.beaconsdk.core.client.BeaconClient
import it.airgap.beaconsdk.core.client.BeaconProducer
import it.airgap.beaconsdk.core.data.Connection
import it.airgap.beaconsdk.core.message.PermissionBeaconRequest

/**
 * Message requesting the granting of the specified [permissions][scopes] to the [sender dApp][appMetadata].
 *
 * Expects [PermissionTezosResponse] as a response.
 *
 * @property [id] The value that identifies this request.
 * @property [version] The message version.
 * @property [blockchainIdentifier] The unique name of the blockchain that specifies this request.
 * @property [senderId] The value that identifies the sender of this request.
 * @property [appMetadata] The metadata describing the dApp asking for permissions.
 * @property [origin] The origination data of this request.
 * @property [network] The network to which the permissions apply.
 * @property [scopes] The list of permissions asked to be granted.
 */
public data class PermissionTezosRequest internal constructor(
    override val id: String,
    override val version: String,
    override val blockchainIdentifier: String,
    override val senderId: String,
    override val appMetadata: TezosAppMetadata,
    override val origin: Connection.Id,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) override val destination: Connection.Id?,
    public val network: TezosNetwork,
    public val scopes: List<TezosPermission.Scope>,
) : PermissionBeaconRequest() {
    public companion object {}
}

public suspend fun <T> PermissionTezosRequest(
    network: TezosNetwork = TezosNetwork.Mainnet(),
    scopes: List<TezosPermission.Scope> = listOf(TezosPermission.Scope.OperationRequest, TezosPermission.Scope.Sign),
    producer: T,
    transportType: Connection.Type = Connection.Type.P2P,
) : PermissionTezosRequest where T : BeaconClient<*>, T : BeaconProducer {
    val requestMetadata = producer.prepareRequest(transportType)

    return PermissionTezosRequest(
        id = requestMetadata.id,
        version = requestMetadata.version,
        blockchainIdentifier = Tezos.IDENTIFIER,
        senderId = requestMetadata.senderId,
        appMetadata = producer.ownAppMetadata(),
        origin = requestMetadata.origin,
        destination = requestMetadata.destination,
        network = network,
        scopes = scopes,
    )
}