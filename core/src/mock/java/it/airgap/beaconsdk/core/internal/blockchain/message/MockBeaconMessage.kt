package it.airgap.beaconsdk.core.internal.blockchain.message

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.data.AppMetadata
import it.airgap.beaconsdk.core.data.BeaconError
import it.airgap.beaconsdk.core.data.Origin
import it.airgap.beaconsdk.core.internal.message.v1.V1BeaconMessage
import it.airgap.beaconsdk.core.data.MockAppMetadata
import it.airgap.beaconsdk.core.internal.message.v2.V2BeaconMessage
import it.airgap.beaconsdk.core.internal.message.v3.*
import it.airgap.beaconsdk.core.message.BlockchainBeaconRequest
import it.airgap.beaconsdk.core.message.BlockchainBeaconResponse
import it.airgap.beaconsdk.core.message.PermissionBeaconRequest
import it.airgap.beaconsdk.core.message.PermissionBeaconResponse
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

internal enum class MockBeaconMessageType(val value: String) {
    Request("request"),
    Response("response");

    companion object {
        fun from(string: String): MockBeaconMessageType? = values().firstOrNull { string.contains(it.value) }
    }
}

@Serializable
public data class PermissionMockRequest(
    val type: String,
    override val id: String,
    override val version: String,
    override val blockchainIdentifier: String,
    override val senderId: String,
    override val origin: Origin,
    override val appMetadata: MockAppMetadata,
    val rest: Map<String, JsonElement> = emptyMap(),
) : PermissionBeaconRequest() {
    public fun toV1(): V1BeaconMessage =
        V1MockPermissionBeaconRequest(
            type,
            version,
            id,
            senderId,
            appMetadata,
            rest,
        )

    public fun toV2(): V2BeaconMessage =
        V2MockPermissionBeaconRequest(
            type,
            version,
            id,
            senderId,
            appMetadata,
            rest,
        )

    public fun toV3(): V3BeaconMessage.Content =
        PermissionV3BeaconRequestContent(
            blockchainIdentifier,
            V3MockPermissionBeaconRequestData(
                appMetadata,
                rest,
            ),
        )
}

@Serializable
public data class BlockchainMockRequest(
    val type: String,
    override val id: String,
    override val version: String,
    override val blockchainIdentifier: String,
    override val senderId: String,
    override val appMetadata: AppMetadata?,
    override val origin: Origin,
    override val accountId: String?,
    val rest: Map<String, JsonElement> = emptyMap(),
) : BlockchainBeaconRequest() {
    public fun toV1(): V1BeaconMessage =
        V1MockBlockchainBeaconMessage(
            type,
            version,
            id,
            senderId,
            rest,
            MockBeaconMessageType.Request,
        )

    public fun toV2(): V2BeaconMessage =
        V2MockBlockchainBeaconMessage(
            type,
            version,
            id,
            senderId,
            rest,
            MockBeaconMessageType.Request,
        )

    public fun toV3(): V3BeaconMessage.Content =
        BlockchainV3BeaconRequestContent(
            blockchainIdentifier,
            accountId ?: "",
            V3MockBlockchainBeaconRequestData(rest),
        )
}

@Serializable
public data class PermissionMockResponse(
    val type: String,
    override val id: String,
    override val version: String,
    override val requestOrigin: Origin,
    override val blockchainIdentifier: String,
    val rest: Map<String, JsonElement> = emptyMap(),
) : PermissionBeaconResponse() {
    public fun toV1(senderId: String): V1BeaconMessage =
        V1MockPermissionBeaconResponse(
            type,
            version,
            id,
            senderId,
            rest,
        )

    public fun toV2(senderId: String): V2BeaconMessage =
        V2MockPermissionBeaconResponse(
            type,
            version,
            id,
            senderId,
            rest,
        )

    public fun toV3(): V3BeaconMessage.Content =
        PermissionV3BeaconResponseContent(
            blockchainIdentifier,
            V3MockPermissionBeaconResponseData(rest),
        )
}

@Serializable
public data class BlockchainMockResponse(
    val type: String,
    override val id: String,
    override val version: String,
    override val requestOrigin: Origin,
    override val blockchainIdentifier: String,
    val rest: Map<String, JsonElement> = emptyMap(),
) : BlockchainBeaconResponse() {
    public fun toV1(senderId: String): V1BeaconMessage =
        V1MockBlockchainBeaconMessage(
            type,
            version,
            id,
            senderId,
            rest,
            MockBeaconMessageType.Response,
        )

    public fun toV2(senderId: String): V2BeaconMessage =
        V2MockBlockchainBeaconMessage(
            type,
            version,
            id,
            senderId,
            rest,
            MockBeaconMessageType.Response,
        )

    public fun toV3(): V3BeaconMessage.Content =
        BlockchainV3BeaconResponseContent(
            blockchainIdentifier,
            V3MockBlockchainBeaconResponseData(rest),
        )
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
public sealed class MockError : BeaconError()