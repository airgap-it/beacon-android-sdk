package it.airgap.beaconsdk.blockchain.substrate.message.request

import it.airgap.beaconsdk.blockchain.substrate.Substrate
import it.airgap.beaconsdk.blockchain.substrate.data.SubstrateNetwork
import it.airgap.beaconsdk.blockchain.substrate.data.SubstratePermission
import it.airgap.beaconsdk.blockchain.substrate.data.SubstrateSignerPayload
import it.airgap.beaconsdk.blockchain.substrate.extensions.ownAppMetadata
import it.airgap.beaconsdk.blockchain.substrate.internal.SubstrateBeaconConfiguration
import it.airgap.beaconsdk.blockchain.substrate.internal.utils.getNetworkFor
import it.airgap.beaconsdk.core.client.BeaconClient
import it.airgap.beaconsdk.core.client.BeaconProducer
import it.airgap.beaconsdk.core.data.AppMetadata
import it.airgap.beaconsdk.core.data.Connection
import it.airgap.beaconsdk.core.internal.utils.failWithActiveAccountNotSet
import it.airgap.beaconsdk.core.message.BlockchainBeaconRequest

public sealed class BlockchainSubstrateRequest : BlockchainBeaconRequest() {
    public abstract val scope: SubstratePermission.Scope

    public companion object {}
}

public sealed class TransferSubstrateRequest : BlockchainSubstrateRequest() {
    override val scope: SubstratePermission.Scope = SubstratePermission.Scope.Transfer

    public abstract val sourceAddress: String
    public abstract val amount: String
    public abstract val recipient: String
    public abstract val network: SubstrateNetwork

    public data class Submit internal constructor(
        override val id: String,
        override val version: String,
        override val blockchainIdentifier: String,
        override val senderId: String,
        override val appMetadata: AppMetadata?,
        override val origin: Connection.Id,
        override val destination: Connection.Id?,
        override val accountId: String,
        override val sourceAddress: String,
        override val amount: String,
        override val recipient: String,
        override val network: SubstrateNetwork,
    ) : TransferSubstrateRequest()

    public data class SubmitAndReturn internal constructor(
        override val id: String,
        override val version: String,
        override val blockchainIdentifier: String,
        override val senderId: String,
        override val appMetadata: AppMetadata?,
        override val origin: Connection.Id,
        override val destination: Connection.Id?,
        override val accountId: String,
        override val sourceAddress: String,
        override val amount: String,
        override val recipient: String,
        override val network: SubstrateNetwork,
    ) : TransferSubstrateRequest()

    public data class Return internal constructor(
        override val id: String,
        override val version: String,
        override val blockchainIdentifier: String,
        override val senderId: String,
        override val appMetadata: AppMetadata?,
        override val origin: Connection.Id,
        override val destination: Connection.Id?,
        override val accountId: String,
        override val sourceAddress: String,
        override val amount: String,
        override val recipient: String,
        override val network: SubstrateNetwork,
    ) : TransferSubstrateRequest()

    public companion object {}
}

@Suppress("FunctionName")
public suspend fun <T> TransferSubmitSubstrateRequest(
    amount: String,
    recipient: String,
    network: SubstrateNetwork? = null,
    producer: T,
    transportType: Connection.Type = Connection.Type.P2P,
) : TransferSubstrateRequest.Submit where T : BeaconClient<*>, T : BeaconProducer {
    val requestMetadata = producer.prepareRequest(transportType)
    val account = requestMetadata.account ?: failWithActiveAccountNotSet()
    val network = network ?: producer.getNetworkFor(account.accountId)

    return TransferSubstrateRequest.Submit(
        id = requestMetadata.id,
        version = SubstrateBeaconConfiguration.MESSAGE_VERSION,
        blockchainIdentifier = Substrate.IDENTIFIER,
        senderId = requestMetadata.senderId,
        appMetadata = producer.ownAppMetadata(),
        origin = requestMetadata.origin,
        destination = requestMetadata.destination,
        accountId = account.accountId,
        sourceAddress = account.address,
        amount = amount,
        recipient = recipient,
        network = network,
    )
}

@Suppress("FunctionName")
public suspend fun <T> TransferSubmitAndReturnSubstrateRequest(
    amount: String,
    recipient: String,
    network: SubstrateNetwork? = null,
    producer: T,
    transportType: Connection.Type = Connection.Type.P2P,
) : TransferSubstrateRequest.SubmitAndReturn where T : BeaconClient<*>, T : BeaconProducer {
    val requestMetadata = producer.prepareRequest(transportType)
    val account = requestMetadata.account ?: failWithActiveAccountNotSet()
    val network = network ?: producer.getNetworkFor(account.accountId)

    return TransferSubstrateRequest.SubmitAndReturn(
        id = requestMetadata.id,
        version = SubstrateBeaconConfiguration.MESSAGE_VERSION,
        blockchainIdentifier = Substrate.IDENTIFIER,
        senderId = requestMetadata.senderId,
        appMetadata = producer.ownAppMetadata(),
        origin = requestMetadata.origin,
        destination = requestMetadata.destination,
        accountId = account.accountId,
        sourceAddress = account.address,
        amount = amount,
        recipient = recipient,
        network = network,
    )
}

@Suppress("FunctionName")
public suspend fun <T> TransferReturnSubstrateRequest(
    amount: String,
    recipient: String,
    network: SubstrateNetwork? = null,
    producer: T,
    transportType: Connection.Type = Connection.Type.P2P,
) : TransferSubstrateRequest.Return where T : BeaconClient<*>, T : BeaconProducer {
    val requestMetadata = producer.prepareRequest(transportType)
    val account = requestMetadata.account ?: failWithActiveAccountNotSet()
    val network = network ?: producer.getNetworkFor(account.accountId)

    return TransferSubstrateRequest.Return(
        id = requestMetadata.id,
        version = SubstrateBeaconConfiguration.MESSAGE_VERSION,
        blockchainIdentifier = Substrate.IDENTIFIER,
        senderId = requestMetadata.senderId,
        appMetadata = producer.ownAppMetadata(),
        origin = requestMetadata.origin,
        destination = requestMetadata.destination,
        accountId = account.accountId,
        sourceAddress = account.address,
        amount = amount,
        recipient = recipient,
        network = network,
    )
}

public sealed class SignPayloadSubstrateRequest : BlockchainSubstrateRequest() {
    override val scope: SubstratePermission.Scope
        get() = when (payload) {
            is SubstrateSignerPayload.Json -> SubstratePermission.Scope.SignPayloadJson
            is SubstrateSignerPayload.Raw -> SubstratePermission.Scope.SignPayloadRaw
        }

    public abstract val address: String
    public abstract val payload: SubstrateSignerPayload

    public data class Submit internal constructor(
        override val id: String,
        override val version: String,
        override val blockchainIdentifier: String,
        override val senderId: String,
        override val appMetadata: AppMetadata?,
        override val origin: Connection.Id,
        override val destination: Connection.Id?,
        override val accountId: String?,
        override val address: String,
        override val payload: SubstrateSignerPayload,
    ) : SignPayloadSubstrateRequest()

    public data class SubmitAndReturn internal constructor(
        override val id: String,
        override val version: String,
        override val blockchainIdentifier: String,
        override val senderId: String,
        override val appMetadata: AppMetadata?,
        override val origin: Connection.Id,
        override val destination: Connection.Id?,
        override val accountId: String?,
        override val address: String,
        override val payload: SubstrateSignerPayload,
    ) : SignPayloadSubstrateRequest()

    public data class Return internal constructor(
        override val id: String,
        override val version: String,
        override val blockchainIdentifier: String,
        override val senderId: String,
        override val appMetadata: AppMetadata?,
        override val origin: Connection.Id,
        override val destination: Connection.Id?,
        override val accountId: String?,
        override val address: String,
        override val payload: SubstrateSignerPayload,
    ) : SignPayloadSubstrateRequest()

    public companion object {}
}

@Suppress("FunctionName")
public suspend fun <T> SignPayloadSubmitSubstrateRequest(
    address: String,
    payload: SubstrateSignerPayload,
    producer: T,
    transportType: Connection.Type = Connection.Type.P2P,
) : SignPayloadSubstrateRequest.Submit where T : BeaconClient<*>, T : BeaconProducer {
    val requestMetadata = producer.prepareRequest(transportType)

    return SignPayloadSubstrateRequest.Submit(
        id = requestMetadata.id,
        version = SubstrateBeaconConfiguration.MESSAGE_VERSION,
        blockchainIdentifier = Substrate.IDENTIFIER,
        senderId = requestMetadata.senderId,
        appMetadata = producer.ownAppMetadata(),
        origin = requestMetadata.origin,
        destination = requestMetadata.destination,
        accountId = requestMetadata.account?.accountId ?: failWithActiveAccountNotSet(),
        address = address,
        payload = payload,
    )
}

@Suppress("FunctionName")
public suspend fun <T> SignPayloadSubmitAndReturnSubstrateRequest(
    address: String,
    payload: SubstrateSignerPayload,
    producer: T,
    transportType: Connection.Type = Connection.Type.P2P,
) : SignPayloadSubstrateRequest.SubmitAndReturn where T : BeaconClient<*>, T : BeaconProducer {
    val requestMetadata = producer.prepareRequest(transportType)

    return SignPayloadSubstrateRequest.SubmitAndReturn(
        id = requestMetadata.id,
        version = SubstrateBeaconConfiguration.MESSAGE_VERSION,
        blockchainIdentifier = Substrate.IDENTIFIER,
        senderId = requestMetadata.senderId,
        appMetadata = producer.ownAppMetadata(),
        origin = requestMetadata.origin,
        destination = requestMetadata.destination,
        accountId = requestMetadata.account?.accountId ?: failWithActiveAccountNotSet(),
        address = address,
        payload = payload,
    )
}

@Suppress("FunctionName")
public suspend fun <T> SignPayloadReturnSubstrateRequest(
    address: String,
    payload: SubstrateSignerPayload,
    producer: T,
    transportType: Connection.Type = Connection.Type.P2P,
) : SignPayloadSubstrateRequest.Return where T : BeaconClient<*>, T : BeaconProducer {
    val requestMetadata = producer.prepareRequest(transportType)

    return SignPayloadSubstrateRequest.Return(
        id = requestMetadata.id,
        version = SubstrateBeaconConfiguration.MESSAGE_VERSION,
        blockchainIdentifier = Substrate.IDENTIFIER,
        senderId = requestMetadata.senderId,
        appMetadata = producer.ownAppMetadata(),
        origin = requestMetadata.origin,
        destination = requestMetadata.destination,
        accountId = requestMetadata.account?.accountId ?: failWithActiveAccountNotSet(),
        address = address,
        payload = payload,
    )
}