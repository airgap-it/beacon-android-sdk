package it.airgap.beaconsdk.chain.tezos.data

import it.airgap.beaconsdk.chain.tezos.message.BroadcastTezosRequest
import it.airgap.beaconsdk.chain.tezos.message.OperationTezosRequest
import it.airgap.beaconsdk.chain.tezos.message.SignPayloadTezosRequest
import it.airgap.beaconsdk.core.data.BeaconError
import it.airgap.beaconsdk.core.internal.utils.KJsonSerializer
import it.airgap.beaconsdk.core.internal.utils.failWithIllegalArgument
import it.airgap.beaconsdk.core.internal.utils.failWithUnexpectedJsonType
import it.airgap.beaconsdk.core.message.PermissionBeaconRequest
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.json.*

/**
 * Types of Tezos errors supported in Beacon
 */
@Serializable(with = TezosError.Serializer::class)
public sealed class TezosError : BeaconError() {
    /**
     * Indicates that the transaction broadcast failed.
     *
     * Applicable to [OperationTezosRequest] and [BroadcastTezosRequest].
     */
    public object BroadcastError : TezosError() {
        internal const val IDENTIFIER = "BROADCAST_ERROR"
    }

    /**
     * Indicates that a private key matching the address provided in the request could not be found.
     *
     * Applicable to [SignPayloadTezosRequest].
     */
    public object NoPrivateKeyFound : TezosError() {
        internal const val IDENTIFIER = "NO_PRIVATE_KEY_FOUND_ERROR"
    }

    /**
     * Indicates that the signature was blocked and could not be completed ([SignPayloadTezosRequest])
     * or the permissions requested by the dApp were rejected ([PermissionBeaconRequest]).
     *
     * Applicable to [SignPayloadTezosRequest] and [PermissionBeaconRequest].
     */
    public object NotGranted : TezosError() {
        internal const val IDENTIFIER = "NOT_GRANTED_ERROR"
    }

    /**
     * Indicates that any of the provided parameters are invalid.
     *
     * Applicable to [OperationTezosRequest].
     */
    public object ParametersInvalid : TezosError() {
        internal const val IDENTIFIER = "PARAMETERS_INVALID_ERROR"
    }

    /**
     * Indicates that too many operation details were included in the request
     * and they could not be included into a single operation group.
     *
     * Applicable to [OperationTezosRequest].
     */
    public object TooManyOperations : TezosError() {
        internal const val IDENTIFIER = "TOO_MANY_OPERATIONS_ERROR"
    }

    /**
     * Indicates that the transaction included in the request could not be parsed or was rejected by the node.
     *
     * Applicable to [BroadcastTezosRequest].
     */
    public object TransactionInvalid : TezosError() {
        internal const val IDENTIFIER = "TRANSACTION_INVALID_ERROR"
    }

    /**
     * Indicates that the requested type of signature is not supported in the client.
     *
     * Applicable to [SignPayloadTezosRequest].
     */
    public object SignatureTypeNotSupported : TezosError() {
        internal const val IDENTIFIER = "SIGNATURE_TYPE_NOT_SUPPORTED"
    }

    public companion object {}
    
    internal object Serializer : KJsonSerializer<TezosError> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("TezosError", PrimitiveKind.STRING)

        override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): TezosError {
            if (jsonElement !is JsonPrimitive) failWithUnexpectedJsonType(jsonElement::class)

            val value = jsonElement.jsonPrimitive.content
            
            return when (value) {
                BroadcastError.IDENTIFIER -> BroadcastError
                NoPrivateKeyFound.IDENTIFIER -> NoPrivateKeyFound
                NotGranted.IDENTIFIER -> NotGranted
                ParametersInvalid.IDENTIFIER -> ParametersInvalid
                TooManyOperations.IDENTIFIER -> TooManyOperations
                TransactionInvalid.IDENTIFIER -> TransactionInvalid
                SignatureTypeNotSupported.IDENTIFIER -> SignatureTypeNotSupported
                else -> failWithUnknownValue(value)
            }
        }

        override fun serialize(jsonEncoder: JsonEncoder, value: TezosError) {
            when (value) {
                BroadcastError -> jsonEncoder.encodeString(BroadcastError.IDENTIFIER)
                NoPrivateKeyFound -> jsonEncoder.encodeString(NoPrivateKeyFound.IDENTIFIER)
                NotGranted -> jsonEncoder.encodeString(NotGranted.IDENTIFIER)
                ParametersInvalid -> jsonEncoder.encodeString(ParametersInvalid.IDENTIFIER)
                TooManyOperations -> jsonEncoder.encodeString(TooManyOperations.IDENTIFIER)
                TransactionInvalid -> jsonEncoder.encodeString(TransactionInvalid.IDENTIFIER)
                SignatureTypeNotSupported -> jsonEncoder.encodeString(TransactionInvalid.IDENTIFIER)
            }
        }

        private fun failWithUnknownValue(value: String): Nothing = failWithIllegalArgument("Unknown TezosError value \"$value\"")
    }
}
