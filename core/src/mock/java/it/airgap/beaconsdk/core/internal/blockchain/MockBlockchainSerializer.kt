package it.airgap.beaconsdk.core.internal.blockchain

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.blockchain.Blockchain
import it.airgap.beaconsdk.core.data.AppMetadata
import it.airgap.beaconsdk.core.data.BeaconError
import it.airgap.beaconsdk.core.data.MockAppMetadata
import it.airgap.beaconsdk.core.data.MockNetwork
import it.airgap.beaconsdk.core.data.MockPermission
import it.airgap.beaconsdk.core.data.Network
import it.airgap.beaconsdk.core.data.Permission
import it.airgap.beaconsdk.core.internal.blockchain.message.MockError
import it.airgap.beaconsdk.core.internal.blockchain.message.V1MockBlockchainBeaconMessage
import it.airgap.beaconsdk.core.internal.blockchain.message.V1MockPermissionBeaconRequest
import it.airgap.beaconsdk.core.internal.blockchain.message.V1MockPermissionBeaconResponse
import it.airgap.beaconsdk.core.internal.blockchain.message.V2MockBlockchainBeaconMessage
import it.airgap.beaconsdk.core.internal.blockchain.message.V2MockPermissionBeaconRequest
import it.airgap.beaconsdk.core.internal.blockchain.message.V2MockPermissionBeaconResponse
import it.airgap.beaconsdk.core.internal.blockchain.message.V3MockBlockchainBeaconRequestData
import it.airgap.beaconsdk.core.internal.blockchain.message.V3MockBlockchainBeaconResponseData
import it.airgap.beaconsdk.core.internal.blockchain.message.V3MockPermissionBeaconRequestData
import it.airgap.beaconsdk.core.internal.blockchain.message.V3MockPermissionBeaconResponseData
import it.airgap.beaconsdk.core.internal.blockchain.serializer.DataBlockchainSerializer
import it.airgap.beaconsdk.core.internal.blockchain.serializer.V1BeaconMessageBlockchainSerializer
import it.airgap.beaconsdk.core.internal.blockchain.serializer.V2BeaconMessageBlockchainSerializer
import it.airgap.beaconsdk.core.internal.blockchain.serializer.V3BeaconMessageBlockchainSerializer
import it.airgap.beaconsdk.core.internal.message.v1.V1BeaconMessage
import it.airgap.beaconsdk.core.internal.message.v2.V2BeaconMessage
import it.airgap.beaconsdk.core.internal.message.v3.BlockchainV3BeaconRequestContent
import it.airgap.beaconsdk.core.internal.message.v3.BlockchainV3BeaconResponseContent
import it.airgap.beaconsdk.core.internal.message.v3.PermissionV3BeaconRequestContent
import it.airgap.beaconsdk.core.internal.message.v3.PermissionV3BeaconResponseContent
import it.airgap.beaconsdk.core.internal.utils.KJsonSerializer
import it.airgap.beaconsdk.core.internal.utils.SuperClassSerializer
import it.airgap.beaconsdk.core.internal.utils.failWithIllegalArgument
import it.airgap.beaconsdk.core.internal.utils.getString
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.jsonObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class MockBlockchainSerializer : Blockchain.Serializer {
    override val data: DataBlockchainSerializer = object : DataBlockchainSerializer {
        override val network: KSerializer<Network>
            get() = SuperClassSerializer(MockNetwork.serializer())

        override val permission: KSerializer<Permission>
            get() = SuperClassSerializer(MockPermission.serializer())

        override val appMetadata: KSerializer<AppMetadata>
            get() = SuperClassSerializer(MockAppMetadata.serializer())

        override val error: KSerializer<BeaconError>
            get() = SuperClassSerializer(MockError.serializer())
    }

    override val v1: V1BeaconMessageBlockchainSerializer = object : V1BeaconMessageBlockchainSerializer {
        override val message: KSerializer<V1BeaconMessage> = object : KJsonSerializer<V1BeaconMessage> {
            override val descriptor: SerialDescriptor = buildClassSerialDescriptor("MockV1BeaconMessage") {
                element<String>("type")
            }

            override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): V1BeaconMessage {
                val type = jsonElement.jsonObject.getString(descriptor.getElementName(0))

                return when (type) {
                    "permission_request" -> jsonDecoder.json.decodeFromJsonElement(V1MockPermissionBeaconRequest.serializer(), jsonElement)
                    "permission_response" -> jsonDecoder.json.decodeFromJsonElement(V1MockPermissionBeaconResponse.serializer(), jsonElement)
                    else -> jsonDecoder.json.decodeFromJsonElement(V1MockBlockchainBeaconMessage.serializer(), jsonElement)
                }
            }

            override fun serialize(jsonEncoder: JsonEncoder, value: V1BeaconMessage) {
                when (value) {
                    is V1MockPermissionBeaconRequest -> jsonEncoder.encodeSerializableValue(V1MockPermissionBeaconRequest.serializer(), value)
                    is V1MockPermissionBeaconResponse -> jsonEncoder.encodeSerializableValue(V1MockPermissionBeaconResponse.serializer(), value)
                    is V1MockBlockchainBeaconMessage -> jsonEncoder.encodeSerializableValue(V1MockBlockchainBeaconMessage.serializer(), value)
                    else -> failWithIllegalArgument()
                }
            }
        }

    }

    override val v2: V2BeaconMessageBlockchainSerializer = object : V2BeaconMessageBlockchainSerializer {
        override val message: KSerializer<V2BeaconMessage> =
            object : KJsonSerializer<V2BeaconMessage> {
                override val descriptor: SerialDescriptor = buildClassSerialDescriptor("MockV2BeaconMessage") {
                    element<String>("type")
                }

                override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): V2BeaconMessage {
                    val type = jsonElement.jsonObject.getString(descriptor.getElementName(0))

                    return when (type) {
                        "permission_request" -> jsonDecoder.json.decodeFromJsonElement(V2MockPermissionBeaconRequest.serializer(), jsonElement)
                        "permission_response" -> jsonDecoder.json.decodeFromJsonElement(V2MockPermissionBeaconResponse.serializer(), jsonElement)
                        else -> jsonDecoder.json.decodeFromJsonElement(V2MockBlockchainBeaconMessage.serializer(), jsonElement)
                    }
                }

                override fun serialize(jsonEncoder: JsonEncoder, value: V2BeaconMessage) {
                    when (value) {
                        is V2MockPermissionBeaconRequest -> jsonEncoder.encodeSerializableValue(V2MockPermissionBeaconRequest.serializer(), value)
                        is V2MockPermissionBeaconResponse -> jsonEncoder.encodeSerializableValue(V2MockPermissionBeaconResponse.serializer(), value)
                        is V2MockBlockchainBeaconMessage -> jsonEncoder.encodeSerializableValue(V2MockBlockchainBeaconMessage.serializer(), value)
                        else -> failWithIllegalArgument()
                    }
                }
            }
        }

    override val v3: V3BeaconMessageBlockchainSerializer = object : V3BeaconMessageBlockchainSerializer {
        override val permissionRequestData: KSerializer<PermissionV3BeaconRequestContent.BlockchainData>
            get() = SuperClassSerializer(V3MockPermissionBeaconRequestData.serializer())

        override val blockchainRequestData: KSerializer<BlockchainV3BeaconRequestContent.BlockchainData>
            get() = SuperClassSerializer(V3MockBlockchainBeaconRequestData.serializer())

        override val permissionResponseData: KSerializer<PermissionV3BeaconResponseContent.BlockchainData>
            get() = SuperClassSerializer(V3MockPermissionBeaconResponseData.serializer())

        override val blockchainResponseData: KSerializer<BlockchainV3BeaconResponseContent.BlockchainData>
            get() = SuperClassSerializer(V3MockBlockchainBeaconResponseData.serializer())

    }
}