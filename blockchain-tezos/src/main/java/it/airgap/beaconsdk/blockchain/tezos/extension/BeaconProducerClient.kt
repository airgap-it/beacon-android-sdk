package it.airgap.beaconsdk.blockchain.tezos.extension

import it.airgap.beaconsdk.blockchain.tezos.data.TezosAppMetadata
import it.airgap.beaconsdk.blockchain.tezos.data.TezosNetwork
import it.airgap.beaconsdk.blockchain.tezos.data.TezosPermission
import it.airgap.beaconsdk.blockchain.tezos.data.operation.TezosOperation
import it.airgap.beaconsdk.blockchain.tezos.message.request.BroadcastTezosRequest
import it.airgap.beaconsdk.blockchain.tezos.message.request.OperationTezosRequest
import it.airgap.beaconsdk.blockchain.tezos.message.request.PermissionTezosRequest
import it.airgap.beaconsdk.blockchain.tezos.message.request.SignPayloadTezosRequest
import it.airgap.beaconsdk.core.client.BeaconClient
import it.airgap.beaconsdk.core.client.BeaconProducer
import it.airgap.beaconsdk.core.data.Connection
import it.airgap.beaconsdk.core.data.SigningType

// -- AppMetadata --

public fun <T> T.ownAppMetadata(): TezosAppMetadata where T : BeaconProducer, T : BeaconClient<*> =
    TezosAppMetadata(
        senderId = senderId,
        name = app.name,
        icon = app.icon,
    )

// -- request --

public suspend fun <T> T.requestTezosPermission(
    network: TezosNetwork = TezosNetwork.Mainnet(),
    scopes: List<TezosPermission.Scope> = listOf(TezosPermission.Scope.OperationRequest, TezosPermission.Scope.Sign),
    transportType: Connection.Type = Connection.Type.P2P,
) where T : BeaconProducer, T : BeaconClient<*> {
    val permissionRequest = PermissionTezosRequest(network, scopes, this, transportType)
    request(permissionRequest)
}

public suspend fun <T> T.requestTezosOperation(
    operationDetails: List<TezosOperation> = emptyList(),
    network: TezosNetwork? = null,
    transportType: Connection.Type = Connection.Type.P2P,
) where T : BeaconProducer, T : BeaconClient<*> {
    val permissionRequest = OperationTezosRequest(operationDetails, network, this, transportType)
    request(permissionRequest)
}

public suspend fun <T> T.requestTezosSignPayload(
    signingType: SigningType,
    payload: String,
    transportType: Connection.Type = Connection.Type.P2P,
) where T : BeaconProducer, T : BeaconClient<*> {
    val permissionRequest = SignPayloadTezosRequest(signingType, payload, this, transportType)
    request(permissionRequest)
}

public suspend fun <T> T.requestTezosBroadcast(
    signedTransaction: String,
    network: TezosNetwork? = null,
    transportType: Connection.Type = Connection.Type.P2P,
) where T : BeaconProducer, T : BeaconClient<*> {
    val permissionRequest = BroadcastTezosRequest(signedTransaction, network, this, transportType)
    request(permissionRequest)
}