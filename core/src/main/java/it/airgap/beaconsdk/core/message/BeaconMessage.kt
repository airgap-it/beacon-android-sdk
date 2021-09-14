package it.airgap.beaconsdk.core.message

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.data.*
import it.airgap.beaconsdk.core.internal.utils.KJsonSerializer
import it.airgap.beaconsdk.core.internal.utils.chainRegistry
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
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
public data class ChainBeaconRequest<T : ChainBeaconRequest.Payload> @RestrictTo(RestrictTo.Scope.LIBRARY) constructor(
    override val id: String,
    override val senderId: String,
    override val appMetadata: AppMetadata?,
    override val identifier: String,
    val payload: T,
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

        internal class Serializer(private val chainIdentifier: String? = null) : KJsonSerializer<Payload> {
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

    internal object Serializer : KJsonSerializer<ChainBeaconRequest<*>> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ChainBeaconRequest") {
            element<String>("id")
            element<String>("senderId")
            element<AppMetadata>("appMetadata")
            element<String>("identifier")
            element<Payload>("payload")
            element<Origin>("origin")
            element<String>("version")
        }


        override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): ChainBeaconRequest<*> =
            jsonDecoder.decodeStructure(descriptor) {
                val id = decodeStringElement(descriptor, 0)
                val senderId = decodeStringElement(descriptor, 1)
                val appMetadata = decodeSerializableElement(descriptor, 2, AppMetadata.serializer())
                val identifier = decodeStringElement(descriptor, 3)
                val payload = decodeSerializableElement(descriptor, 4, Payload.serializer(identifier))
                val origin = decodeSerializableElement(descriptor, 5, Origin.serializer())
                val version = decodeStringElement(descriptor, 6)

                return ChainBeaconRequest(id, senderId, appMetadata, identifier, payload, origin, version)
            }

        override fun serialize(jsonEncoder: JsonEncoder, value: ChainBeaconRequest<*>) {
            jsonEncoder.encodeStructure(descriptor) {
                with(value) {
                    encodeStringElement(descriptor, 0, id)
                    encodeStringElement(descriptor, 1, senderId)
                    appMetadata?.let { encodeSerializableElement(descriptor, 2, AppMetadata.serializer(), appMetadata) }
                    encodeStringElement(descriptor, 3, identifier)
                    encodeSerializableElement(descriptor, 4, Payload.serializer(identifier), payload)
                    encodeSerializableElement(descriptor, 5, Origin.serializer(), origin)
                    encodeStringElement(descriptor, 6, version)
                }
            }
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
public data class ChainBeaconResponse<T : ChainBeaconResponse.Payload> @RestrictTo(RestrictTo.Scope.LIBRARY) constructor(
    override val id: String,
    public val identifier: String,
    public val payload: T,

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

        internal class Serializer(private val chainIdentifier: String? = null) : KJsonSerializer<Payload> {
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
        public fun <T : Payload> from(
            request: ChainBeaconRequest<*>,
            payload: T,
        ): ChainBeaconResponse<T> =
            ChainBeaconResponse(request.id, request.identifier, payload, request.version, request.origin)
    }

    internal object Serializer : KJsonSerializer<ChainBeaconResponse<*>> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ChainBeaconResponse") {
            element<String>("id")
            element<String>("identifier")
            element<Payload>("payload")
            element<String>("version")
            element<Origin>("requestOrigin")
        }

        override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): ChainBeaconResponse<*> =
            jsonDecoder.decodeStructure(descriptor) {
                val id = decodeStringElement(descriptor, 0)
                val identifier = decodeStringElement(descriptor, 1)
                val payload = decodeSerializableElement(descriptor, 2, Payload.serializer(identifier))
                val version = decodeStringElement(descriptor, 3)
                val requestOrigin = decodeSerializableElement(descriptor, 4, Origin.serializer())

                return ChainBeaconResponse(id, identifier, payload, version, requestOrigin)
            }

        override fun serialize(jsonEncoder: JsonEncoder, value: ChainBeaconResponse<*>) {
            jsonEncoder.encodeStructure(descriptor) {
                with(value) {
                    encodeStringElement(descriptor, 0, id)
                    encodeStringElement(descriptor, 1, identifier)
                    encodeSerializableElement(descriptor, 2, Payload.serializer(identifier), payload)
                    encodeStringElement(descriptor, 3, version)
                    encodeSerializableElement(descriptor, 4, Origin.serializer(), requestOrigin)
                }
            }
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

    internal object Serializer : KJsonSerializer<ErrorBeaconResponse> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ErrorBeaconResponse") {
            element<String>("id")
            element<String>("identifier")
            element<ErrorType>("errorType")
            element<String>("version")
            element<Origin>("requestOrigin")
        }

        override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): ErrorBeaconResponse =
            jsonDecoder.decodeStructure(descriptor) {
                val id = decodeStringElement(descriptor, 0)
                val identifier = decodeStringElement(descriptor, 1)
                val errorType = decodeSerializableElement(descriptor, 2, BeaconError.serializer(identifier))
                val version = decodeStringElement(descriptor, 3)
                val requestOrigin = decodeSerializableElement(descriptor, 4, Origin.serializer())

                return ErrorBeaconResponse(id, identifier, errorType, version, requestOrigin)
            }

        override fun serialize(jsonEncoder: JsonEncoder, value: ErrorBeaconResponse) {
            jsonEncoder.encodeStructure(descriptor) {
                with(value) {
                    encodeStringElement(descriptor, 0, id)
                    encodeStringElement(descriptor, 1, identifier)
                    encodeSerializableElement(descriptor, 2, BeaconError.serializer(identifier), errorType)
                    encodeStringElement(descriptor, 3, version)
                    encodeSerializableElement(descriptor, 4, Origin.serializer(), requestOrigin)
                }
            }
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