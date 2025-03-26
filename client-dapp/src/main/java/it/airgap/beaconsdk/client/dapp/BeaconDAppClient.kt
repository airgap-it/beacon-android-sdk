package it.airgap.beaconsdk.client.dapp

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.builder.InitBuilder
import it.airgap.beaconsdk.core.client.BeaconClient
import it.airgap.beaconsdk.core.client.BeaconProducer
import it.airgap.beaconsdk.core.data.Connection
import it.airgap.beaconsdk.core.exception.BeaconException
import it.airgap.beaconsdk.core.internal.BeaconConfiguration
import it.airgap.beaconsdk.core.internal.controller.connection.ConnectionController
import it.airgap.beaconsdk.core.internal.controller.message.MessageController
import it.airgap.beaconsdk.core.internal.crypto.Crypto
import it.airgap.beaconsdk.core.internal.data.BeaconApplication
import it.airgap.beaconsdk.core.internal.serializer.Serializer
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.storage.sharedpreferences.SharedPreferencesSecureStorage
import it.airgap.beaconsdk.core.internal.utils.*
import it.airgap.beaconsdk.core.scope.BeaconScope
import it.airgap.beaconsdk.core.storage.SecureStorage
import it.airgap.beaconsdk.core.transport.data.PairingRequest
import it.airgap.beaconsdk.core.transport.data.PairingResponse
import it.airgap.beaconsdk.client.dapp.internal.controller.account.AccountController
import it.airgap.beaconsdk.client.dapp.internal.di.ExtendedDependencyRegistry
import it.airgap.beaconsdk.client.dapp.internal.di.extend
import it.airgap.beaconsdk.client.dapp.internal.storage.sharedpreferences.SharedPreferencesDAppClientStorage
import it.airgap.beaconsdk.client.dapp.storage.DAppClientStorage
import it.airgap.beaconsdk.core.data.Account
import it.airgap.beaconsdk.core.message.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile

public class BeaconDAppClient @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) constructor(
    app: BeaconApplication,
    beaconId: String,
    beaconScope: BeaconScope,
    connectionController: ConnectionController,
    messageController: MessageController,
    private val accountController: AccountController,
    storageManager: StorageManager,
    crypto: Crypto,
    serializer: Serializer,
    identifierCreator: IdentifierCreator,
    configuration: BeaconConfiguration,
) : BeaconClient<BeaconResponse>(app, beaconId, beaconScope, connectionController, messageController, storageManager, crypto, serializer, configuration, identifierCreator), BeaconProducer {

    /**
     * Sends the [request] to the previously paired peer.
     *
     * @throws [BeaconException] if processing and sending the [request] failed.
     */
    @Throws(BeaconException::class)
    override suspend fun request(request: BeaconRequest) {
        send(request, isTerminal = false)
            .takeIfNotIgnored()
            ?.mapException { BeaconException.from(it) }
            ?.getOrThrow()
    }

    public suspend fun getActiveAccount(): Account? =
        accountController.getActiveAccount()?.account

    public suspend fun clearActiveAccount() {
        accountController.clearActiveAccount()
    }

    public suspend fun reset() {
        accountController.clearAll()
    }

    /**
     * Prepares and triggers peer pairing via transport specified with the [connectionType]. Returns
     * the pairing request, which, depending on the transport used, may require additional handling.
     *
     * @throws [BeaconException] if the process failed.
     */
    @Throws(BeaconException::class)
    override suspend fun pair(connectionType: Connection.Type): PairingRequest {
        val pairingRequestDeferred = CompletableDeferred<Result<PairingRequest>>()

        CoroutineScope(CoroutineName("collectPairingMessages") + Dispatchers.Default).launch {
            connectionController.pair(connectionType)
                .onEachSuccess {
                    when (it) {
                        is PairingRequest -> pairingRequestDeferred.complete(Result.success(it))
                        is PairingResponse -> accountController.onPairingResponse(it)
                    }
                }
                .onEachFailure {
                    if (!pairingRequestDeferred.isCompleted) pairingRequestDeferred.complete(Result.failure(it))
                }
                .takeWhile { it.isFailure || it.getOrNull() !is PairingResponse }
                .collect()
        }

        return pairingRequestDeferred.await()
            .mapException { BeaconException.from(it) }
            .getOrThrow()
    }

    override suspend fun prepareRequest(connectionType: Connection.Type): BeaconProducer.RequestMetadata =
        BeaconProducer.RequestMetadata(
            id = crypto.guid().getOrThrow(),
            version = BeaconConfiguration.BEACON_VERSION,
            origin = Connection.Id.fromType(connectionType),
            destination = accountController.getActivePeer()?.toConnectionId(),
            senderId = senderId,
            account = accountController.getActiveAccount()?.account,
        )

    override suspend fun processMessage(origin: Connection.Id, message: BeaconMessage): Result<Unit> =
        runCatchingFlat {
            when (message) {
                is PermissionBeaconResponse -> accountController.onPermissionResponse(origin, message).getOrThrow()
                is DisconnectBeaconMessage -> {
                    // TODO: remove active account and peer and communicate it to the user
                    // (the behavior is already implemented and will fire in `super.processMessage(origin, message)`

                    return Result.success()
                }
                else -> { /* no action */ }
            }

            super.processMessage(origin, message)
        }

    override suspend fun transformMessage(message: BeaconMessage): BeaconResponse? =
        when (message) {
            is BeaconResponse -> message
            else -> null
        }

    private fun Connection.Id.Companion.fromType(type: Connection.Type): Connection.Id =
        when (type) {
            Connection.Type.P2P -> Connection.Id.P2P(app.keyPair.publicKey.toHexString().asString())
        }

    public companion object {}

    public class Builder(
        name: String,
        clientId: String? = null
    ) : InitBuilder<BeaconDAppClient, DAppClientStorage, SecureStorage, Builder>(
        name,
        BeaconScope(clientId),
        { SharedPreferencesDAppClientStorage(applicationContext) },
        { SharedPreferencesSecureStorage(applicationContext) },
    ) {

        private var _extendedDependencyRegistry: ExtendedDependencyRegistry? = null
        private val extendedDependencyRegistry: ExtendedDependencyRegistry
            get() = _extendedDependencyRegistry ?: dependencyRegistry(beaconScope).extend().also { _extendedDependencyRegistry = it }

        /**
         * Creates a new instance of [BeaconDAppClient].
         */
        override suspend fun createInstance(configuration: BeaconConfiguration): BeaconDAppClient =
            extendedDependencyRegistry.dAppClient(storage, connections, configuration)
    }
}

public suspend fun BeaconDAppClient(
    name: String,
    clientId: String? = null,
    builderAction: BeaconDAppClient.Builder.() -> Unit = {}
): BeaconDAppClient = BeaconDAppClient.Builder(name, clientId).apply(builderAction).build()