package it.airgap.beaconsdk.blockchain.substrate.extensions

import it.airgap.beaconsdk.blockchain.substrate.data.SubstrateAppMetadata
import it.airgap.beaconsdk.blockchain.substrate.data.SubstrateNetwork
import it.airgap.beaconsdk.blockchain.substrate.data.SubstratePermission
import it.airgap.beaconsdk.blockchain.substrate.data.SubstrateSignerPayload
import it.airgap.beaconsdk.blockchain.substrate.message.request.*
import it.airgap.beaconsdk.core.client.BeaconClient
import it.airgap.beaconsdk.core.client.BeaconProducer
import it.airgap.beaconsdk.core.data.Connection

// -- AppMetadata --

public fun <T> T.ownAppMetadata(): SubstrateAppMetadata where T : BeaconProducer, T : BeaconClient<*> =
    SubstrateAppMetadata(
        senderId = senderId,
        name = app.name,
        icon = app.icon,
    )

// -- request --

public suspend fun <T> T.requestSubstratePermission(
    networks: List<SubstrateNetwork>,
    scopes: List<SubstratePermission.Scope> = listOf(SubstratePermission.Scope.SignPayloadJson, SubstratePermission.Scope.SignPayloadRaw, SubstratePermission.Scope.Transfer),
    transportType: Connection.Type = Connection.Type.P2P,
) where T : BeaconProducer, T : BeaconClient<*> {
    val permissionRequest = PermissionSubstrateRequest(networks, scopes, this, transportType)
    request(permissionRequest)
}

public suspend fun <T> T.requestSubstrateTransferSubmit(
    amount: String,
    recipient: String,
    network: SubstrateNetwork? = null,
    transportType: Connection.Type = Connection.Type.P2P,
) where T : BeaconProducer, T : BeaconClient<*> {
    val permissionRequest = TransferSubmitSubstrateRequest(amount, recipient, network, this, transportType)
    request(permissionRequest)
}

public suspend fun <T> T.requestSubstrateTransferSubmitAndReturn(
    amount: String,
    recipient: String,
    network: SubstrateNetwork? = null,
    transportType: Connection.Type = Connection.Type.P2P,
) where T : BeaconProducer, T : BeaconClient<*> {
    val permissionRequest = TransferSubmitAndReturnSubstrateRequest(amount, recipient, network, this, transportType)
    request(permissionRequest)
}

public suspend fun <T> T.requestSubstrateTransferReturn(
    amount: String,
    recipient: String,
    network: SubstrateNetwork? = null,
    transportType: Connection.Type = Connection.Type.P2P,
) where T : BeaconProducer, T : BeaconClient<*> {
    val permissionRequest = TransferReturnSubstrateRequest(amount, recipient, network, this, transportType)
    request(permissionRequest)
}

public suspend fun <T> T.requestSubstrateSignPayloadSubmit(
    address: String,
    payload: SubstrateSignerPayload,
    transportType: Connection.Type = Connection.Type.P2P,
) where T : BeaconProducer, T : BeaconClient<*> {
    val permissionRequest = SignPayloadSubmitSubstrateRequest(address, payload, this, transportType)
    request(permissionRequest)
}

public suspend fun <T> T.requestSubstrateSignPayloadSubmitAndReturn(
    address: String,
    payload: SubstrateSignerPayload,
    transportType: Connection.Type = Connection.Type.P2P,
) where T : BeaconProducer, T : BeaconClient<*> {
    val permissionRequest = SignPayloadSubmitAndReturnSubstrateRequest(address, payload, this, transportType)
    request(permissionRequest)
}

public suspend fun <T> T.requestSubstrateSignPayloadReturn(
    address: String,
    payload: SubstrateSignerPayload,
    transportType: Connection.Type = Connection.Type.P2P,
) where T : BeaconProducer, T : BeaconClient<*> {
    val permissionRequest = SignPayloadReturnSubstrateRequest(address, payload, this, transportType)
    request(permissionRequest)
}
