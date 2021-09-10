package it.airgap.beaconsdk.core.internal.di

import it.airgap.beaconsdk.core.data.beacon.Connection
import it.airgap.beaconsdk.core.internal.chain.ChainRegistry
import it.airgap.beaconsdk.core.internal.controller.ConnectionController
import it.airgap.beaconsdk.core.internal.controller.MessageController
import it.airgap.beaconsdk.core.internal.crypto.Crypto
import it.airgap.beaconsdk.core.internal.migration.Migration
import it.airgap.beaconsdk.core.internal.network.HttpClient
import it.airgap.beaconsdk.core.internal.serializer.Serializer
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.transport.Transport
import it.airgap.beaconsdk.core.internal.utils.AccountUtils
import it.airgap.beaconsdk.core.internal.utils.Base58Check
import it.airgap.beaconsdk.core.internal.utils.Poller
import it.airgap.beaconsdk.core.network.provider.HttpProvider

public interface DependencyRegistry {
    public val storageManager: StorageManager

    // -- chain --

    public val chainRegistry: ChainRegistry

    // -- controller --

    public val messageController: MessageController
    public fun connectionController(connections: List<Connection>): ConnectionController

    // -- transport --

    public fun transport(connection: Connection): Transport

    // -- utils --

    public val crypto: Crypto
    public val serializer: Serializer

    public val accountUtils: AccountUtils
    public val base58Check: Base58Check
    public val poller: Poller

    // -- network --

    public fun httpClient(httpProvider: HttpProvider?): HttpClient

    // -- Migration --

    public val migration: Migration
}