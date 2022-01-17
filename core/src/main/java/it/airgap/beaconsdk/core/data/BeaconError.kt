package it.airgap.beaconsdk.core.data

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.internal.utils.KJsonSerializer
import it.airgap.beaconsdk.core.internal.utils.blockchainRegistry
import it.airgap.beaconsdk.core.internal.utils.failWithIllegalArgument
import it.airgap.beaconsdk.core.internal.utils.failWithUnexpectedJsonType
import it.airgap.beaconsdk.core.message.BeaconRequest
import it.airgap.beaconsdk.core.message.PermissionBeaconRequest
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.json.*

/**
 * Base for errors supported in Beacon.
 */
@Serializable(with = BeaconError.Serializer::class)
public abstract class BeaconError {
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public abstract val blockchainIdentifier: String?

    /**
     * Indicates that the request execution has been aborted by the user or the wallet.
     *
     * Applicable to every [BeaconRequest].
     */
    public object Aborted : BeaconError() {
        internal const val IDENTIFIER = "ABORTED_ERROR"

        override val blockchainIdentifier: String? = null
    }

    /**
     * Indicates that an unexpected error occurred.
     *
     * Applicable to every [BeaconRequest].
     */
    public object Unknown : BeaconError() {
        internal const val IDENTIFIER = "UNKNOWN_ERROR"

        override val blockchainIdentifier: String? = null
    }

    public companion object {
        public fun serializer(blockchainIdentifier: String? = null): KSerializer<BeaconError> = Serializer(blockchainIdentifier)
    }

    internal class Serializer(private val blockchainIdentifier: String? = null) : KJsonSerializer<BeaconError> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BeaconError", PrimitiveKind.STRING)

        override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): BeaconError {
            if (jsonElement !is JsonPrimitive) failWithUnexpectedJsonType(jsonElement::class)

            val value = jsonElement.jsonPrimitive.content

            return when (value) {
                Aborted.IDENTIFIER -> Aborted
                Unknown.IDENTIFIER -> Unknown
                else -> blockchainIdentifier?.let { blockchainRegistry.getOrNull(it)?.serializer?.data?.error?.deserialize(jsonDecoder) } ?: failWithUnknownValue(value)
            }
        }

        override fun serialize(jsonEncoder: JsonEncoder, value: BeaconError) {
            when (value) {
                Aborted -> jsonEncoder.encodeString(Aborted.IDENTIFIER)
                Unknown -> jsonEncoder.encodeString(Unknown.IDENTIFIER)
                else -> blockchainIdentifier?.let { blockchainRegistry.getOrNull(it)?.serializer?.data?.error?.serialize(jsonEncoder, value) } ?: failWithUnknownValue(value)
            }
        }

        private fun failWithUnknownValue(value: String): Nothing = failWithIllegalArgument("Unknown BeaconError value \"$value\"")
        private fun failWithUnknownValue(value: BeaconError): Nothing = failWithIllegalArgument("Unknown BeaconError value \"$value\"")
    }
}