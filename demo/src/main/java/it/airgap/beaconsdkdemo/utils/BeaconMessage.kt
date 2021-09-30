package it.airgap.beaconsdkdemo.utils

import it.airgap.beaconsdk.blockchain.tezos.message.request.*
import it.airgap.beaconsdk.blockchain.tezos.message.response.*
import it.airgap.beaconsdk.core.data.BeaconError
import it.airgap.beaconsdk.core.internal.utils.failWithIllegalArgument
import it.airgap.beaconsdk.core.message.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement

fun BeaconMessage.toJson(json: Json = Json.Default): JsonElement =
    when (this) {
        is BeaconRequest -> toJson(json)
        is BeaconResponse -> toJson(json)
        is DisconnectBeaconMessage -> toJson(json)
    }

fun BeaconRequest.toJson(json: Json = Json.Default): JsonElement =
    when (this) {
        is PermissionBeaconRequest -> toJson(json)
        is BlockchainBeaconRequest -> toJson(json)
    }

fun PermissionBeaconRequest.toJson(json: Json = Json.Default): JsonElement =
    when (this) {
        is PermissionTezosRequest -> toJson(json)
        else -> failWithUnknownPermissionBeaconRequest(this)
    }

fun BlockchainBeaconRequest.toJson(json: Json = Json.Default): JsonElement =
    when (this) {
        is BlockchainTezosRequest -> toJson(json)
        else -> failWithUnknownBlockchainBeaconRequest(this)
    }

fun PermissionTezosRequest.toJson(json: Json = Json.Default): JsonElement =
    JsonObject(
        mapOf(
            "type" to json.encodeToJsonElement("tezos_permission_request"),
            "id" to json.encodeToJsonElement(id),
            "version" to json.encodeToJsonElement(version),
            "blockchainIdentifier" to json.encodeToJsonElement(blockchainIdentifier),
            "senderId" to json.encodeToJsonElement(senderId),
            "appMetadata" to json.encodeToJsonElement(appMetadata),
            "origin" to json.encodeToJsonElement(origin),
            "network" to json.encodeToJsonElement(network),
            "scopes" to json.encodeToJsonElement(scopes),
        )
    )

fun BlockchainTezosRequest.toJson(json: Json = Json.Default): JsonElement =
    when (this) {
        is OperationTezosRequest -> toJson(json)
        is SignPayloadTezosRequest -> toJson(json)
        is BroadcastTezosRequest -> toJson(json)
    }

fun OperationTezosRequest.toJson(json: Json = Json.Default): JsonElement =
    JsonObject(
        mapOf(
            "type" to json.encodeToJsonElement("tezos_operation_request"),
            "id" to json.encodeToJsonElement(id),
            "version" to json.encodeToJsonElement(version),
            "blockchainIdentifier" to json.encodeToJsonElement(blockchainIdentifier),
            "senderId" to json.encodeToJsonElement(senderId),
            "appMetadata" to json.encodeToJsonElement(appMetadata),
            "origin" to json.encodeToJsonElement(origin),
            "network" to json.encodeToJsonElement(network),
            "operationDetails" to json.encodeToJsonElement(operationDetails),
            "sourceAddress" to json.encodeToJsonElement(sourceAddress),
        )
    )

fun SignPayloadTezosRequest.toJson(json: Json = Json.Default): JsonElement =
    JsonObject(
        mapOf(
            "type" to json.encodeToJsonElement("tezos_sign_payload_request"),
            "id" to json.encodeToJsonElement(id),
            "version" to json.encodeToJsonElement(version),
            "blockchainIdentifier" to json.encodeToJsonElement(blockchainIdentifier),
            "senderId" to json.encodeToJsonElement(senderId),
            "appMetadata" to json.encodeToJsonElement(appMetadata),
            "origin" to json.encodeToJsonElement(origin),
            "signingType" to json.encodeToJsonElement(signingType),
            "payload" to json.encodeToJsonElement(payload),
            "sourceAddress" to json.encodeToJsonElement(sourceAddress),
        )
    )

fun BroadcastTezosRequest.toJson(json: Json = Json.Default): JsonElement =
    JsonObject(
        mapOf(
            "type" to json.encodeToJsonElement("tezos_broadcast_request"),
            "id" to json.encodeToJsonElement(id),
            "version" to json.encodeToJsonElement(version),
            "blockchainIdentifier" to json.encodeToJsonElement(blockchainIdentifier),
            "senderId" to json.encodeToJsonElement(senderId),
            "appMetadata" to json.encodeToJsonElement(appMetadata),
            "origin" to json.encodeToJsonElement(origin),
            "network" to json.encodeToJsonElement(network),
            "signedTransaction" to json.encodeToJsonElement(signedTransaction),
        )
    )

fun BeaconResponse.toJson(json: Json = Json.Default): JsonElement =
    when (this) {
        is PermissionBeaconResponse -> toJson(json)
        is BlockchainBeaconResponse -> toJson(json)
        is AcknowledgeBeaconResponse -> toJson(json)
        is ErrorBeaconResponse -> toJson(json)
    }

fun PermissionBeaconResponse.toJson(json: Json = Json.Default): JsonElement =
    when (this) {
        is PermissionTezosResponse -> toJson(json)
        else -> failWithUnknownPermissionBeaconResponse(this)
    }

fun BlockchainBeaconResponse.toJson(json: Json = Json.Default): JsonElement =
    when (this) {
        is BlockchainTezosResponse -> toJson(json)
        else -> failWithUnknownBlockchainBeaconResponse(this)
    }

fun AcknowledgeBeaconResponse.toJson(json: Json = Json.Default): JsonElement =
    JsonObject(
        mapOf(
            "type" to json.encodeToJsonElement("acknowledge_response"),
            "id" to json.encodeToJsonElement(id),
            "senderId" to json.encodeToJsonElement(senderId),
            "version" to json.encodeToJsonElement(version),
        )
    )

fun ErrorBeaconResponse.toJson(json: Json = Json.Default): JsonElement =
    JsonObject(
        mapOf(
            "type" to json.encodeToJsonElement("error_response"),
            "id" to json.encodeToJsonElement(id),
            "blockchainIdentifier" to json.encodeToJsonElement(blockchainIdentifier),
            "errorType" to json.encodeToJsonElement(BeaconError.serializer(blockchainIdentifier), errorType),
            "version" to json.encodeToJsonElement(version),
        )
    )

fun PermissionTezosResponse.toJson(json: Json = Json.Default): JsonElement =
    JsonObject(
        mapOf(
            "type" to json.encodeToJsonElement("tezos_permission_response"),
            "id" to json.encodeToJsonElement(id),
            "version" to json.encodeToJsonElement(version),
            "blockchainIdentifier" to json.encodeToJsonElement(blockchainIdentifier),
            "publicKey" to json.encodeToJsonElement(publicKey),
            "network" to json.encodeToJsonElement(network),
            "scopes" to json.encodeToJsonElement(scopes),
            "threshold" to json.encodeToJsonElement(threshold),
        )
    )

fun BlockchainTezosResponse.toJson(json: Json = Json.Default): JsonElement =
    when (this) {
        is OperationTezosResponse -> toJson(json)
        is SignPayloadTezosResponse -> toJson(json)
        is BroadcastTezosResponse -> toJson(json)
    }

fun OperationTezosResponse.toJson(json: Json = Json.Default): JsonElement =
    JsonObject(
        mapOf(
            "type" to json.encodeToJsonElement("tezos_operation_response"),
            "id" to json.encodeToJsonElement(id),
            "version" to json.encodeToJsonElement(version),
            "blockchainIdentifier" to json.encodeToJsonElement(blockchainIdentifier),
            "transactionHash" to json.encodeToJsonElement(transactionHash),
        )
    )

fun SignPayloadTezosResponse.toJson(json: Json = Json.Default): JsonElement =
    JsonObject(
        mapOf(
            "type" to json.encodeToJsonElement("tezos_sign_payload_response"),
            "id" to json.encodeToJsonElement(id),
            "version" to json.encodeToJsonElement(version),
            "blockchainIdentifier" to json.encodeToJsonElement(blockchainIdentifier),
            "signingType" to json.encodeToJsonElement(signingType),
            "signature" to json.encodeToJsonElement(signature),
        )
    )

fun BroadcastTezosResponse.toJson(json: Json = Json.Default): JsonElement =
    JsonObject(
        mapOf(
            "type" to json.encodeToJsonElement("tezos_broadcast_response"),
            "id" to json.encodeToJsonElement(id),
            "version" to json.encodeToJsonElement(version),
            "blockchainIdentifier" to json.encodeToJsonElement(blockchainIdentifier),
            "transactionHash" to json.encodeToJsonElement(transactionHash),
        )
    )

fun DisconnectBeaconMessage.toJson(json: Json = Json.Default): JsonElement =
    JsonObject(
        mapOf(
            "id" to json.encodeToJsonElement(id),
            "senderId" to json.encodeToJsonElement(senderId),
            "version" to json.encodeToJsonElement(version),
            "origin" to json.encodeToJsonElement(origin),
        )
    )

private fun failWithUnknownPermissionBeaconRequest(request: PermissionBeaconRequest): Nothing =
    failWithIllegalArgument("Unknown Beacon permission request of type ${request::class}")

private fun failWithUnknownBlockchainBeaconRequest(request: BlockchainBeaconRequest): Nothing =
    failWithIllegalArgument("Unknown Beacon blockchain request of type ${request::class}")

private fun failWithUnknownPermissionBeaconResponse(response: PermissionBeaconResponse): Nothing =
    failWithIllegalArgument("Unknown Beacon permission response of type ${response::class}")

private fun failWithUnknownBlockchainBeaconResponse(response: BlockchainBeaconResponse): Nothing =
    failWithIllegalArgument("Unknown Beacon blockchain response of type ${response::class}")