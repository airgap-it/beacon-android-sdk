package it.airgap.client.dapp

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.client.wallet.internal.di.ExtendedDependencyRegistry
import it.airgap.beaconsdk.client.wallet.internal.di.extend
import it.airgap.beaconsdk.core.builder.InitBuilder
import it.airgap.beaconsdk.core.client.BeaconClient
import it.airgap.beaconsdk.core.exception.BeaconException
import it.airgap.beaconsdk.core.internal.BeaconConfiguration
import it.airgap.beaconsdk.core.internal.controller.ConnectionController
import it.airgap.beaconsdk.core.internal.controller.MessageController
import it.airgap.beaconsdk.core.internal.crypto.Crypto
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.utils.beaconSdk
import it.airgap.beaconsdk.core.internal.utils.dependencyRegistry
import it.airgap.beaconsdk.core.internal.utils.mapException
import it.airgap.beaconsdk.core.message.BeaconMessage
import it.airgap.beaconsdk.core.message.BeaconRequest
import it.airgap.beaconsdk.core.message.BeaconResponse
import it.airgap.beaconsdk.core.scope.BeaconScope
import it.airgap.beaconsdk.core.scope.Scope

public class BeaconDAppClient @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) constructor(
    name: String,
    beaconId: String,
    beaconScope: BeaconScope,
    connectionController: ConnectionController,
    messageController: MessageController,
    storageManager: StorageManager,
    crypto: Crypto,
    configuration: BeaconConfiguration,
) : BeaconClient<BeaconResponse>(name, beaconId, beaconScope, connectionController, messageController, storageManager, crypto, configuration) {

    public suspend fun request(request: BeaconRequest) {
        send(request, isTerminal = false)
            .takeIfNotIgnored()
            ?.mapException { BeaconException.from(it) }
            ?.getOrThrow()
    }

    override suspend fun transformMessage(message: BeaconMessage): BeaconResponse? =
        when (message) {
            is BeaconResponse -> message
            else -> null
        }

    public companion object {
        private const val SCOPE_PREFIX = "dapp_client"
    }

    public class Builder(name: String, clientId: String? = null) : InitBuilder<BeaconDAppClient, Builder>(name, Scope(clientId, SCOPE_PREFIX)) {

        private var _extendedDependencyRegistry: ExtendedDependencyRegistry? = null
        private val extendedDependencyRegistry: ExtendedDependencyRegistry
            get() = _extendedDependencyRegistry ?: dependencyRegistry(beaconScope).extend().also { _extendedDependencyRegistry = it }

        /**
         * Creates a new instance of [BeaconDAppClient].
         */
        override suspend fun createInstance(configuration: BeaconConfiguration): BeaconDAppClient =
            extendedDependencyRegistry.dAppClient(name, connections, configuration)
    }
}

public suspend fun BeaconDAppClient(
    name: String,
    clientId: String? = null,
    builderAction: BeaconDAppClient.Builder.() -> Unit = {}
): BeaconDAppClient = BeaconDAppClient.Builder(name, clientId).apply(builderAction).build()