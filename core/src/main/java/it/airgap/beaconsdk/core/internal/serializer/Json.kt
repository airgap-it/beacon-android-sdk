package it.airgap.beaconsdk.core.internal.serializer

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.data.AppMetadata
import it.airgap.beaconsdk.core.data.BeaconError
import it.airgap.beaconsdk.core.data.Network
import it.airgap.beaconsdk.core.data.Permission
import it.airgap.beaconsdk.core.internal.blockchain.BlockchainRegistry
import it.airgap.beaconsdk.core.internal.compat.Compat
import it.airgap.beaconsdk.core.internal.compat.VersionedCompat
import it.airgap.beaconsdk.core.internal.message.VersionedBeaconMessage
import it.airgap.beaconsdk.core.internal.message.v1.ErrorV1BeaconResponse
import it.airgap.beaconsdk.core.internal.message.v1.V1BeaconMessage
import it.airgap.beaconsdk.core.internal.message.v2.ErrorV2BeaconResponse
import it.airgap.beaconsdk.core.internal.message.v2.V2BeaconMessage
import it.airgap.beaconsdk.core.internal.message.v3.*
import it.airgap.beaconsdk.core.transport.data.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.modules.polymorphic

private fun serializersModule(
    blockchainRegistry: BlockchainRegistry,
    compat: Compat<VersionedCompat>,
): SerializersModule = SerializersModule {

    // -- data --

    contextual(AppMetadata.Serializer(blockchainRegistry, compat))
    contextual(BeaconError.Serializer(blockchainRegistry))
    contextual(Network.Serializer(blockchainRegistry))
    contextual(Permission.Serializer(blockchainRegistry, compat))

    // -- message versioned --

    contextual(VersionedBeaconMessage.Serializer(blockchainRegistry, compat))

    // -- message v1 --

    contextual(V1BeaconMessage.Serializer(blockchainRegistry, compat))
    contextual(ErrorV1BeaconResponse.Serializer(blockchainRegistry, compat))

    // -- message v2 --

    contextual(V2BeaconMessage.Serializer(blockchainRegistry, compat))
    contextual(ErrorV2BeaconResponse.Serializer(blockchainRegistry, compat))

    // -- message v3 --

    polymorphic(V3BeaconMessage.Content::class) {
        subclass(PermissionV3BeaconRequestContent::class, PermissionV3BeaconRequestContent.serializer(blockchainRegistry))
        subclass(BlockchainV3BeaconRequestContent::class, BlockchainV3BeaconRequestContent.serializer(blockchainRegistry))

        subclass(PermissionV3BeaconResponseContent::class, PermissionV3BeaconResponseContent.serializer(blockchainRegistry))
        subclass(BlockchainV3BeaconResponseContent::class, BlockchainV3BeaconResponseContent.serializer(blockchainRegistry))
        subclass(AcknowledgeV3BeaconResponseContent::class, AcknowledgeV3BeaconResponseContent.serializer())
        subclass(ErrorV3BeaconResponseContent::class, ErrorV3BeaconResponseContent.serializer(blockchainRegistry))

        subclass(DisconnectV3BeaconMessageContent::class, DisconnectV3BeaconMessageContent.serializer())
    }

    contextual(PermissionV3BeaconRequestContent.Serializer(blockchainRegistry))
    contextual(PermissionV3BeaconResponseContent.Serializer(blockchainRegistry))

    contextual(BlockchainV3BeaconRequestContent.Serializer(blockchainRegistry))
    contextual(BlockchainV3BeaconResponseContent.Serializer(blockchainRegistry))

    contextual(ErrorV3BeaconResponseContent.Serializer(blockchainRegistry))

    // -- pairing --

    contextual(PairingMessage.serializer())
    contextual(PairingRequest.serializer())
    contextual(PairingResponse.serializer())
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun contextualJson(blockchainRegistry: BlockchainRegistry, compat: Compat<VersionedCompat>): Json =
    Json {
        serializersModule = serializersModule(blockchainRegistry, compat)
    }