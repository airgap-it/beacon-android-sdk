package it.airgap.client.dapp

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.builder.InitBuilder
import it.airgap.beaconsdk.core.client.BeaconClient
import it.airgap.beaconsdk.core.client.BeaconProducer
import it.airgap.beaconsdk.core.data.Connection
import it.airgap.beaconsdk.core.exception.BeaconException
import it.airgap.beaconsdk.core.internal.BeaconConfiguration
import it.airgap.beaconsdk.core.internal.controller.ConnectionController
import it.airgap.beaconsdk.core.internal.controller.MessageController
import it.airgap.beaconsdk.core.internal.crypto.Crypto
import it.airgap.beaconsdk.core.internal.data.BeaconApplication
import it.airgap.beaconsdk.core.internal.serializer.Serializer
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.storage.sharedpreferences.SharedPreferencesSecureStorage
import it.airgap.beaconsdk.core.internal.utils.IdentifierCreator
import it.airgap.beaconsdk.core.internal.utils.applicationContext
import it.airgap.beaconsdk.core.internal.utils.delegate.default
import it.airgap.beaconsdk.core.internal.utils.dependencyRegistry
import it.airgap.beaconsdk.core.internal.utils.mapException
import it.airgap.beaconsdk.core.message.BeaconMessage
import it.airgap.beaconsdk.core.message.BeaconRequest
import it.airgap.beaconsdk.core.message.BeaconResponse
import it.airgap.beaconsdk.core.scope.BeaconScope
import it.airgap.beaconsdk.core.scope.Scope
import it.airgap.beaconsdk.core.storage.SecureStorage
import it.airgap.beaconsdk.core.storage.Storage
import it.airgap.beaconsdk.core.transport.data.PairingRequest
import it.airgap.beaconsdk.core.transport.data.PairingResponse
import it.airgap.client.dapp.internal.controller.AccountController
import it.airgap.client.dapp.internal.di.ExtendedDependencyRegistry
import it.airgap.client.dapp.internal.di.extend
import it.airgap.client.dapp.internal.storage.sharedpreferences.SharedPreferencesDAppClientStorage
import it.airgap.client.dapp.storage.DAppClientStorage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

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

    override suspend fun pair(connectionType: Connection.Type): Result<PairingRequest> {
        val pairingRequestDeferred = CompletableDeferred<Result<PairingRequest>>()

        CoroutineScope(CoroutineName("collectPairingMessages")).launch {
            connectionController.pair(connectionType)
                .takeWhile { it.isFailure || it.getOrNull() is PairingResponse }
                .take(5 /* pairing request + pairing response + 3 errors */)
                .collect { result ->
                    try {
                        when (val message = result.getOrThrow()) {
                            is PairingRequest -> pairingRequestDeferred.complete(Result.success(message))
                            is PairingResponse -> accountController.onPairingResponse(message)
                        }
                    } catch (e: Exception) {
                        if (!pairingRequestDeferred.isCompleted) pairingRequestDeferred.complete(Result.failure(e))
                        else { /* TODO: emit an error event */ }
                    }
                }
        }

        return pairingRequestDeferred.await()
    }

    override fun prepareRequest(connectionType: Connection.Type): BeaconProducer.RequestMetadata {
        TODO("Not yet implemented")
    }

    override suspend fun transformMessage(message: BeaconMessage): BeaconResponse? =
        when (message) {
            is BeaconResponse -> message
            else -> null
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