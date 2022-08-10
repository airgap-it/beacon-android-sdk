package it.airgap.client.dapp

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.builder.InitBuilder
import it.airgap.beaconsdk.core.client.BeaconClient
import it.airgap.beaconsdk.core.client.BeaconProducer
import it.airgap.beaconsdk.core.data.Connection
import it.airgap.beaconsdk.core.data.Origin
import it.airgap.beaconsdk.core.exception.BeaconException
import it.airgap.beaconsdk.core.internal.BeaconConfiguration
import it.airgap.beaconsdk.core.internal.controller.ConnectionController
import it.airgap.beaconsdk.core.internal.controller.MessageController
import it.airgap.beaconsdk.core.internal.crypto.Crypto
import it.airgap.beaconsdk.core.internal.data.BeaconApplication
import it.airgap.beaconsdk.core.internal.serializer.Serializer
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.storage.sharedpreferences.SharedPreferencesSecureStorage
import it.airgap.beaconsdk.core.internal.utils.*
import it.airgap.beaconsdk.core.message.BeaconMessage
import it.airgap.beaconsdk.core.message.BeaconRequest
import it.airgap.beaconsdk.core.message.BeaconResponse
import it.airgap.beaconsdk.core.message.PermissionBeaconResponse
import it.airgap.beaconsdk.core.scope.BeaconScope
import it.airgap.beaconsdk.core.scope.Scope
import it.airgap.beaconsdk.core.storage.SecureStorage
import it.airgap.beaconsdk.core.transport.data.PairingRequest
import it.airgap.beaconsdk.core.transport.data.PairingResponse
import it.airgap.client.dapp.internal.controller.account.AccountController
import it.airgap.client.dapp.internal.di.ExtendedDependencyRegistry
import it.airgap.client.dapp.internal.di.extend
import it.airgap.client.dapp.internal.storage.sharedpreferences.SharedPreferencesDAppClientStorage
import it.airgap.client.dapp.storage.DAppClientStorage
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
    configuration: BeaconConfiguration,
    identifierCreator: IdentifierCreator,
) : BeaconClient<BeaconResponse>(app, beaconId, beaconScope, connectionController, messageController, storageManager, crypto, serializer, configuration), BeaconProducer {

    override val senderId: String = identifierCreator.senderId(app.keyPair.publicKey).getOrThrow()

    override suspend fun request(request: BeaconRequest) {
        send(request, isTerminal = false)
            .takeIfNotIgnored()
            ?.mapException { BeaconException.from(it) }
            ?.getOrThrow()
    }

    public suspend fun getActiveAccount(): String? =
        accountController.getActiveAccountId()

    public suspend fun clearActiveAccount() {
        accountController.clearActiveAccountId()
    }

    public suspend fun reset() {
        accountController.clearAll()
    }

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

        return pairingRequestDeferred.await().getOrThrow()
    }

    override suspend fun prepareRequest(connectionType: Connection.Type): BeaconProducer.RequestMetadata =
        BeaconProducer.RequestMetadata(
            id = crypto.guid().getOrThrow(),
            version = BeaconConfiguration.BEACON_VERSION,
            origin = connectionType.toOrigin(),
            destination = accountController.getRequestDestination(),
            senderId = senderId,
            accountId = accountController.getActiveAccountId()
        )

    override suspend fun processMessage(origin: Origin, message: BeaconMessage): Result<Unit> =
        runCatchingFlat {
            when (message) {
                is PermissionBeaconResponse -> accountController.onPermissionResponse(origin, message).getOrThrow()
                else -> { /* no action */ }
            }

            super.processMessage(origin, message)
        }

    override suspend fun transformMessage(message: BeaconMessage): BeaconResponse? =
        when (message) {
            is BeaconResponse -> message
            else -> null
        }

    private fun Connection.Type.toOrigin(): Origin =
        when (this) {
            Connection.Type.P2P -> Origin.P2P(app.keyPair.publicKey.toHexString().asString())
        }

    public companion object {
        private const val SCOPE_PREFIX = "dapp_client"
    }

    public class Builder(
        name: String,
        clientId: String? = null
    ) : InitBuilder<BeaconDAppClient, DAppClientStorage, SecureStorage, Builder>(
        name,
        Scope(clientId, SCOPE_PREFIX),
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