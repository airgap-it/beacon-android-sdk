package it.airgap.beaconsdk.internal.servicelocator

import it.airgap.beaconsdk.internal.client.ConnectionClient
import it.airgap.beaconsdk.internal.client.SdkClient
import it.airgap.beaconsdk.internal.crypto.Crypto
import it.airgap.beaconsdk.internal.controller.MessageController
import it.airgap.beaconsdk.internal.protocol.Protocol
import it.airgap.beaconsdk.internal.protocol.ProtocolRegistry
import it.airgap.beaconsdk.internal.serializer.Serializer
import it.airgap.beaconsdk.internal.storage.ExtendedStorage
import it.airgap.beaconsdk.internal.transport.Transport
import it.airgap.beaconsdk.internal.utils.AccountUtils
import it.airgap.beaconsdk.internal.utils.Base58Check

internal interface ServiceLocator {
    val appName: String
    val matrixNodes: List<String>
    val extendedStorage: ExtendedStorage

    val sdkClient: SdkClient

    val messageController: MessageController

    val protocolRegistry: ProtocolRegistry

    val crypto: Crypto
    val serializer: Serializer

    val accountUtils: AccountUtils
    val base58Check: Base58Check

    fun connectionController(transportType: Transport.Type): ConnectionClient
    fun transport(type: Transport.Type): Transport
}