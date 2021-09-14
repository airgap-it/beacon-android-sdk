package it.airgap.beaconsdk.core.message

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.data.*
import it.airgap.beaconsdk.core.internal.utils.KJsonSerializer
import it.airgap.beaconsdk.core.internal.utils.chainRegistry
import it.airgap.beaconsdk.core.internal.utils.getSerializable
import it.airgap.beaconsdk.core.internal.utils.getString
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.json.*
import kotlin.reflect.jvm.internal.impl.types.ErrorType

/**
 * Base class for messages used in the Beacon communication.
 *
 * @property [id] A unique value used to identify the pair of request and response messages.
 */
@Serializable
public sealed class BeaconMessage {
    public abstract val id: String

    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    public abstract val version: String

    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    public abstract val associatedOrigin: Origin

    public companion object {}
}

// -- request --

/**
 * Base class for request messages used in the Beacon communication.
 *
 * @property [senderId] A unique value used to identify the sender of the request.
 * @property [origin] An origination data used to identify the source of the request.
 */
@Serializable
@SerialName("request")
public sealed class BeaconRequest : BeaconMessage() {
    public abstract val senderId: String
    public abstract val appMetadata: AppMetadata?
    public abstract val identifier: String
    public abstract val origin: Origin

    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    override val associatedOrigin: Origin
        get() = origin

    public companion object {}
}

/**
 * Message requesting the granting of the specified [permissions][scopes] to the [sender dApp][appMetadata].
 *
 * Expects [PermissionBeaconResponse] as a response.
 *
 * @property [id] The value that identifies this request.
 * @property [senderId] The value that identifies the sender of this request.
 * @property [appMetadata] The metadata describing the dApp asking for permissions.
 * @property [identifier] A unique name of the chain that specifies this request.
 * @property [network] The network to which the permissions apply.
 * @property [scopes] The list of permissions asked to be granted.
 * @property [origin] The origination data of this request.
 */
@Serializable
@SerialName("permission_request")
public data class PermissionBeaconRequest @RestrictTo(RestrictTo.Scope.LIBRARY) constructor(
    override val id: String,
    override val senderId: String,
    override val appMetadata: AppMetadata,
    override val identifier: String,
    public val network: Network,
    public val scopes: List<Permission.Scope>,
    override val origin: Origin,

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override val version: String,
) : BeaconRequest() {

    @Serializable
    public abstract class Details {}

    public companion object {}
}

/**
 * Message requesting a chain specific operation provided in the [payload].
 *
 * Expects [ChainBeaconResponse] as a response.
 *
 * @property [id] The value that identifiers this request.
 * @property [senderId] The value that identifiers the sender of this request.
 * @property [appMetadata] The metadata describing the dApp asking for the broadcast. May be `null` if the [senderId] is unknown.
 * @property [identifier] A unique name of the chain that specifies this request.
 * @property [payload] The content of this request.
 * @property [origin] The origination data of this request.
 */
@Serializable(with = ChainBeaconRequest.Serializer::class)
@SerialName("chain_request")
public data class ChainBeaconRequest @RestrictTo(RestrictTo.Scope.LIBRARY) constructor(
    override val id: String,
    override val senderId: String,
    override val appMetadata: AppMetadata?,
    override val identifier: String,
    val payload: Payload,
    override val origin: Origin,

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override val version: String,
) : BeaconRequest() {

    @Serializable(with = Payload.Serializer::class)
    public abstract class Payload {

        @Serializable
        public object Empty : Payload()

        public companion object {
            public fun serializer(chainIdentifier: String? = null): KSerializer<Payload> = Serializer(chainIdentifier)
        }

        internal class Serializer(private val chainIdentifier: String? = null) : KJsonSerializer<Payload>() {
            override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ChainBeaconRequestPayload")

            override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): Payload =
                chainIdentifier
                    ?.let { chainRegistry.get(it) }
                    ?.let { jsonDecoder.json.decodeFromJsonElement(it.serializer.requestPayload, jsonElement) }
                    ?: Empty

            override fun serialize(jsonEncoder: JsonEncoder, value: Payload) {
                chainIdentifier
                    ?.let { chainRegistry.get(it)?.serializer?.requestPayload }
                    ?.let { jsonEncoder.encodeSerializableValue(it, value) }
                    ?: run { jsonEncoder.encodeSerializableValue(Empty.serializer(), Empty) }
            }
        }
    }

    public companion object {}

    internal object Serializer : KJsonSerializer<ChainBeaconRequest>() {
        object Field {
            const val ID = "id"
            const val SENDER_ID = "senderId"
            const val APP_METADATA = "appMetadata"
            const val IDENTIFIER = "identifier"
            const val PAYLOAD = "payload"
            const val ORIGIN = "origin"
            const val VERSION = "version"
        }

        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ChainBeaconRequest") {
            element<String>(Field.ID)
            element<String>(Field.SENDER_ID)
            element<AppMetadata>(Field.APP_METADATA)
            element<String>(Field.IDENTIFIER)
            element<Payload>(Field.PAYLOAD)
            element<Origin>(Field.ORIGIN)
            element<String>(Field.VERSION)
        }


        override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): ChainBeaconRequest {
            val id = jsonElement.jsonObject.getString(Field.ID)
            val senderId = jsonElement.jsonObject.getString(Field.SENDER_ID)
            val appMetadata = jsonElement.jsonObject.getSerializable(Field.APP_METADATA, jsonDecoder, AppMetadata.serializer())
            val identifier = jsonElement.jsonObject.getString(Field.IDENTIFIER)
            val payload = jsonElement.jsonObject.getSerializable(Field.PAYLOAD, jsonDecoder, Payload.serializer(identifier))
            val origin = jsonElement.jsonObject.getSerializable(Field.ORIGIN, jsonDecoder, Origin.serializer())
            val version = jsonElement.jsonObject.getString(Field.VERSION)

            return ChainBeaconRequest(id, senderId, appMetadata, identifier, payload, origin, version)
        }

        override fun serialize(jsonEncoder: JsonEncoder, value: ChainBeaconRequest) {
            val jsonObject = with(value) {
                JsonObject(mapOf(
                    Field.ID to JsonPrimitive(id),
                    Field.SENDER_ID to JsonPrimitive(senderId),
                    Field.APP_METADATA to (appMetadata?.let { jsonEncoder.json.encodeToJsonElement(AppMetadata.serializer(), it) } ?: JsonNull),
                    Field.IDENTIFIER to JsonPrimitive(identifier),
                    Field.PAYLOAD to jsonEncoder.json.encodeToJsonElement(Payload.serializer(identifier), payload),
                    Field.VERSION to JsonPrimitive(version),
                    Field.ORIGIN to jsonEncoder.json.encodeToJsonElement(Origin.serializer(), origin)
                ))
            }

            jsonEncoder.encodeJsonElement(jsonObject)
        }
    }
}

// -- response --

/**
 * Base class for response messages used in the Beacon communication.
 */
@Serializable
@SerialName("response")
public sealed class BeaconResponse : BeaconMessage() {
    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    public abstract val requestOrigin: Origin

    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    override val associatedOrigin: Origin
        get() = requestOrigin

    public companion object {}
}

/**
 * Message responding to [PermissionBeaconRequest].
 *
 * @property [id] The value that identifies the request to which the message is responding.
 * @property [publicKey] The public key of the account that is granting the permissions.
 * @property [identifier] A unique name of the chain that specifies the request.
 * @property [network] The network to which the permissions apply.
 * @property [scopes] The list of granted permissions.
 * @property [threshold] An optional threshold configuration.
 */
@Serializable
@SerialName("permission_response")
public data class PermissionBeaconResponse @RestrictTo(RestrictTo.Scope.LIBRARY) constructor(
    override val id: String,
    public val publicKey: String,
    public val identifier: String,
    public val network: Network,
    public val scopes: List<Permission.Scope>,
    public val threshold: Threshold? = null,

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override val version: String,

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override val requestOrigin: Origin,
) : BeaconResponse() {

    public companion object {

        /**
         * Creates a new instance of [PermissionBeaconResponse] from the [request]
         * with the specified [publicKey] and optional [network], [scopes] and [threshold].
         *
         * The response will have an id and identifier matching the ones of the [request].
         * If no custom [network] and [scopes] are provided, the values will be also taken from the [request].
         * By default [threshold] is set to `null`.
         */
        public fun from(
            request: PermissionBeaconRequest,
            publicKey: String,
            network: Network = request.network,
            scopes: List<Permission.Scope> = request.scopes,
            threshold: Threshold? = null,
        ): PermissionBeaconResponse =
            PermissionBeaconResponse(request.id, publicKey, request.identifier, network, scopes, threshold, request.version, request.origin)
    }
}

/**
 * A message responding to [ChainBeaconRequest]
 *
 * @property [id] The value that identifies the request to which the message is responding.
 * @property [identifier] A unique name of the chain that specifies the request.
 * @property [payload] The content of the response.
 */
@Serializable
@SerialName("chain_response")
public data class ChainBeaconResponse @RestrictTo(RestrictTo.Scope.LIBRARY) constructor(
    override val id: String,
    public val identifier: String,
    public val payload: Payload,

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override val version: String,

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override val requestOrigin: Origin,
) : BeaconResponse() {

    @Serializable(with = Payload.Serializer::class)
    public abstract class Payload {

        @Serializable
        public object Empty : Payload()

        public companion object {
            public fun serializer(chainIdentifier: String? = null): KSerializer<Payload> = Serializer(chainIdentifier)
        }

        internal class Serializer(private val chainIdentifier: String? = null) : KJsonSerializer<Payload>() {
            override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ChainBeaconResponsePayload")

            override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): Payload =
                    chainIdentifier
                        ?.let { chainRegistry.get(it) }
                        ?.let { jsonDecoder.json.decodeFromJsonElement(it.serializer.responsePayload, jsonElement) }
                        ?: Empty

            override fun serialize(jsonEncoder: JsonEncoder, value: Payload) {
                chainIdentifier
                    ?.let { chainRegistry.get(it)?.serializer?.responsePayload }
                    ?.let { jsonEncoder.encodeSerializableValue(it, value) }
                    ?: run { jsonEncoder.encodeSerializableValue(Empty.serializer(), Empty) }
            }
        }
    }

    public companion object {

        /**
         * Creates a new instance of [ChainBeaconResponse] from the [request]
         * with the specified payload.
         *
         * The response will have an id matching the one of the [request].
         */
        public fun from(
            request: ChainBeaconRequest,
            payload: Payload,
        ): ChainBeaconResponse =
            ChainBeaconResponse(request.id, request.identifier, payload, request.version, request.origin)
    }

    internal object Serializer : KJsonSerializer<ChainBeaconResponse>() {
        object Field {
            const val ID = "id"
            const val IDENTIFIER = "identifier"
            const val PAYLOAD = "payload"
            const val VERSION = "version"
            const val REQUEST_ORIGIN = "requestOrigin"
        }

        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ChainBeaconResponse") {
            element<String>(Field.ID)
            element<String>(Field.IDENTIFIER)
            element<Payload>(Field.PAYLOAD)
            element<String>(Field.VERSION)
            element<Origin>(Field.REQUEST_ORIGIN)
        }

        override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): ChainBeaconResponse {
            val id = jsonElement.jsonObject.getString(Field.ID)
            val identifier = jsonElement.jsonObject.getString(Field.IDENTIFIER)
            val payload = jsonElement.jsonObject.getSerializable(Field.PAYLOAD, jsonDecoder, Payload.serializer(identifier))
            val version = jsonElement.jsonObject.getString(Field.VERSION)
            val requestOrigin = jsonElement.jsonObject.getSerializable(Field.REQUEST_ORIGIN, jsonDecoder, Origin.serializer())

            return ChainBeaconResponse(id, identifier, payload, version, requestOrigin)
        }

        override fun serialize(jsonEncoder: JsonEncoder, value: ChainBeaconResponse) {
            val jsonObject = with(value) {
                JsonObject(mapOf(
                    Field.ID to JsonPrimitive(id),
                    Field.IDENTIFIER to JsonPrimitive(identifier),
                    Field.PAYLOAD to jsonEncoder.json.encodeToJsonElement(Payload.serializer(identifier), payload),
                    Field.VERSION to JsonPrimitive(version),
                    Field.REQUEST_ORIGIN to jsonEncoder.json.encodeToJsonElement(Origin.serializer(), requestOrigin)
                ))
            }

            jsonEncoder.encodeJsonElement(jsonObject)
        }
    }
}

/**
 * Message responding to every [BeaconRequest], sent to confirm receiving of the request.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@Serializable
@SerialName("acknowledge")
public data class AcknowledgeBeaconResponse(
    override val id: String,
    val senderId: String,
    override val version: String,
    override val requestOrigin: Origin,
) : BeaconResponse() {

    public companion object {

        /**
         * Creates a new instance of [AcknowledgeBeaconResponse] from the [request]
         * with the specified [senderId].
         *
         * The response will have an id matching the one of the [request].
         */
        public fun from(request: BeaconRequest, senderId: String): AcknowledgeBeaconResponse =
            AcknowledgeBeaconResponse(request.id, senderId, request.version, request.origin)
    }
}

/**
 * Message responding to every [BeaconRequest] and informing that the request could not be completed due to an error.
 *
 * @property [id] The value that identifies the request to which the message is responding.
 * @property [identifier] A unique name of the chain that specifies the request.
 * @property [errorType] The type of the error.
 */
@Serializable(with = ErrorBeaconResponse.Serializer::class)
@SerialName("error")
public data class ErrorBeaconResponse @RestrictTo(RestrictTo.Scope.LIBRARY) constructor(
    override val id: String,
    public val identifier: String,
    public val errorType: BeaconError,

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override val version: String,

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override val requestOrigin: Origin,
) : BeaconResponse() {

    public companion object {

        /**
         * Creates a new instance of [ErrorBeaconResponse] from the [request]
         * with the specified [errorType].
         *
         * The response will have an id and chain identifier matching the ones of the [request].
         */
        public fun from(request: BeaconRequest, errorType: BeaconError): ErrorBeaconResponse =
            ErrorBeaconResponse(request.id, request.identifier, errorType, request.version, request.origin)
    }

    internal object Serializer : KJsonSerializer<ErrorBeaconResponse>() {
        object Field {
            const val ID = "id"
            const val IDENTIFIER = "identifier"
            const val ERROR_TYPE = "errorType"
            const val VERSION = "version"
            const val REQUEST_ORIGIN = "requestOrigin"
        }

        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ErrorBeaconResponse") {
            element<String>(Field.ID)
            element<String>(Field.IDENTIFIER)
            element<ErrorType>(Field.ERROR_TYPE)
            element<String>(Field.VERSION)
            element<Origin>(Field.REQUEST_ORIGIN)
        }


        override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): ErrorBeaconResponse {
            val id = jsonElement.jsonObject.getString(Field.ID)
            val identifier = jsonElement.jsonObject.getString(Field.IDENTIFIER)
            val errorType = jsonElement.jsonObject.getSerializable(Field.ERROR_TYPE, jsonDecoder, BeaconError.serializer(identifier))
            val version = jsonElement.jsonObject.getString(Field.VERSION)
            val requestOrigin = jsonElement.jsonObject.getSerializable(Field.REQUEST_ORIGIN, jsonDecoder, Origin.serializer())

            return ErrorBeaconResponse(id, identifier, errorType, version, requestOrigin)
        }

        override fun serialize(jsonEncoder: JsonEncoder, value: ErrorBeaconResponse) {
            val jsonObject = with(value) {
                JsonObject(mapOf(
                    Field.ID to JsonPrimitive(id),
                    Field.IDENTIFIER to JsonPrimitive(identifier),
                    Field.ERROR_TYPE to jsonEncoder.json.encodeToJsonElement(BeaconError.serializer(identifier), errorType),
                    Field.VERSION to JsonPrimitive(version),
                    Field.REQUEST_ORIGIN to jsonEncoder.json.encodeToJsonElement(Origin.serializer(), requestOrigin)
                ))
            }

            jsonEncoder.encodeJsonElement(jsonObject)
        }

    }
}

// -- other --

/**
 * Message informing that its sender has closed the connection.
 *
 * @property [id] The value that identifies this message.
 * @property [senderId] The value that identifies the sender of this message.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@Serializable
@SerialName("disconnect")
public data class DisconnectBeaconMessage(
    override val id: String,
    val senderId: String,
    override val version: String,
    val origin: Origin,
) : BeaconMessage() {

    override val associatedOrigin: Origin
        get() = origin
}