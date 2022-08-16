package it.airgap.beaconsdk.blockchain.tezos.extension

import it.airgap.beaconsdk.blockchain.tezos.data.TezosAccount
import it.airgap.beaconsdk.blockchain.tezos.data.TezosPermission
import it.airgap.beaconsdk.blockchain.tezos.message.request.BroadcastTezosRequest
import it.airgap.beaconsdk.blockchain.tezos.message.request.OperationTezosRequest
import it.airgap.beaconsdk.blockchain.tezos.message.request.PermissionTezosRequest
import it.airgap.beaconsdk.blockchain.tezos.message.request.SignPayloadTezosRequest
import it.airgap.beaconsdk.blockchain.tezos.message.response.BroadcastTezosResponse
import it.airgap.beaconsdk.blockchain.tezos.message.response.OperationTezosResponse
import it.airgap.beaconsdk.blockchain.tezos.message.response.PermissionTezosResponse
import it.airgap.beaconsdk.blockchain.tezos.message.response.SignPayloadTezosResponse
import it.airgap.beaconsdk.core.client.BeaconClient
import it.airgap.beaconsdk.core.client.BeaconConsumer
import it.airgap.beaconsdk.core.data.SigningType

// -- response --

public suspend fun <T> T.respondToTezosPermission(
    request: PermissionTezosRequest,
    account: TezosAccount,
    scopes: List<TezosPermission.Scope> = request.scopes
) where T : BeaconConsumer, T : BeaconClient<*> {
    val response = PermissionTezosResponse.from(request, account, scopes)
    respond(response)
}

public suspend fun <T> T.respondToTezosOperation(
    request: OperationTezosRequest,
    transactionHash: String,
) where T : BeaconConsumer, T : BeaconClient<*> {
    val response = OperationTezosResponse.from(request, transactionHash)
    respond(response)
}

public suspend fun <T> T.respondToTezosSignPayload(
    request: SignPayloadTezosRequest,
    signingType: SigningType,
    signature: String,
) where T : BeaconConsumer, T : BeaconClient<*> {
    val response = SignPayloadTezosResponse.from(request, signingType, signature)
    respond(response)
}

public suspend fun <T> T.respondToTezosBroadcast(
    request: BroadcastTezosRequest,
    transactionHash: String,
) where T : BeaconConsumer, T : BeaconClient<*> {
    val response = BroadcastTezosResponse.from(request, transactionHash)
    respond(response)
}