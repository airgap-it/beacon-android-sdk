package it.airgap.beaconsdk.data.tezos

import it.airgap.beaconsdk.internal.utils.failWithExpectedJsonDecoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
sealed class TezosOperation(internal val kind: Kind) {
    @Serializable
    data class ActivateAccount(val pkh: String, val secret: String) : TezosOperation(Kind.ActivateAccount) {
        companion object {}
    }

    @Serializable
    data class Ballot(val source: String, val period: String, val proposal: String, val ballot: Type) : TezosOperation(Kind.Ballot) {

        @Serializable
        enum class Type {
            @SerialName("nay") Nay,
            @SerialName("yay") Yay,
            @SerialName("pass") Pass,
        }

        companion object {}
    }

    @Serializable
    data class Delegation(
        val source: String,
        val fee: String,
        val counter: String,
        @SerialName("gas_limit") val gasLimit: String,
        @SerialName("storage_limit") val storageLimit: String,
        val delegate: String?
    ) : TezosOperation(Kind.Delegation) {
        companion object {}
    }

    @Serializable
    data class DoubleBakingEvidence(
        val bh1: TezosBlockHeader,
        val bh2: TezosBlockHeader
    ) : TezosOperation(Kind.DoubleBakingEvidence) {
        companion object {}
    }

    @Serializable
    data class Endorsement(val level: String) : TezosOperation(Kind.Endorsement) {
        companion object {}
    }

    @Serializable
    data class DoubleEndorsementEvidence(
        val op1: TezosInlinedEndorsement,
        val op2: TezosInlinedEndorsement
    ) : TezosOperation(Kind.DoubleEndorsementEvidence) {
        companion object {}
    }

    @Serializable
    data class Origination(
        val source: String,
        val fee: String,
        val counter: String,
        @SerialName("gas_limit") val gasLimit: String,
        @SerialName("storage_limit") val storageLimit: String,
        val balance: String,
        val script: String,
        val delegate: String?,
    ) : TezosOperation(Kind.Origination) {
        companion object {}
    }

    @Serializable
    data class Proposals(val period: String, val proposals: List<String>) : TezosOperation(Kind.Proposals) {
        companion object {}
    }

    @Serializable
    data class Reveal(
        val source: String,
        val fee: String,
        val counter: String,
        @SerialName("gas_limit") val gasLimit: String,
        @SerialName("storage_limit") val storageLimit: String,
        val publicKey: String
    ) : TezosOperation(Kind.Reveal) {
        companion object {}
    }

    @Serializable
    data class SeedNonceRevelation(val level: String, val nonce: String) : TezosOperation(Kind.SeedNonceRevelation) {
        companion object {}
    }

    @Serializable
    data class Transaction(
        val source: String,
        val fee: String,
        val counter: String,
        @SerialName("gas_limit") val gasLimit: String,
        @SerialName("storage_limit") val storageLimit: String,
        val amount: String,
        val destination: String,
        val parameters: Parameters
    ) : TezosOperation(Kind.Transaction) {

        @Serializable
        data class Parameters(val entrypoint: String, val value: MichelineMichelsonV1Expression) {
            companion object {}
        }

        companion object {}
    }

    companion object {}

    @Serializable
    internal enum class Kind {
        @SerialName("endorsement") Endorsement,
        @SerialName("seed_nonce_revelation") SeedNonceRevelation,
        @SerialName("double_endorsement_evidence") DoubleEndorsementEvidence,
        @SerialName("double_baking_evidence") DoubleBakingEvidence,
        @SerialName("activate_account") ActivateAccount,
        @SerialName("proposals") Proposals,
        @SerialName("ballot") Ballot,
        @SerialName("reveal") Reveal,
        @SerialName("transaction") Transaction,
        @SerialName("origination") Origination,
        @SerialName("delegation") Delegation,
    }

    class Serializer : KSerializer<TezosOperation> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("TezosOperation", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): TezosOperation {
            val jsonDecoder = decoder as? JsonDecoder ?: failWithExpectedJsonDecoder(decoder::class)
            val jsonElement = jsonDecoder.decodeJsonElement()

            val kindField = "kind"
            val kindSerialized = jsonElement.jsonObject[kindField]?.jsonPrimitive?.content ?: failWithMissingField(kindField)
            val kind = jsonDecoder.json.decodeFromString(Kind.serializer(), kindSerialized)

            return when (kind) {
                Kind.ActivateAccount -> jsonDecoder.json.decodeFromJsonElement(ActivateAccount.serializer(), jsonElement)
                Kind.Ballot -> jsonDecoder.json.decodeFromJsonElement(Ballot.serializer(), jsonElement)
                Kind.Delegation -> jsonDecoder.json.decodeFromJsonElement(Delegation.serializer(), jsonElement)
                Kind.DoubleBakingEvidence -> jsonDecoder.json.decodeFromJsonElement(DoubleBakingEvidence.serializer(), jsonElement)
                Kind.Endorsement -> jsonDecoder.json.decodeFromJsonElement(Endorsement.serializer(), jsonElement)
                Kind.DoubleEndorsementEvidence -> jsonDecoder.json.decodeFromJsonElement(DoubleEndorsementEvidence.serializer(), jsonElement)
                Kind.Origination -> jsonDecoder.json.decodeFromJsonElement(Origination.serializer(), jsonElement)
                Kind.Proposals -> jsonDecoder.json.decodeFromJsonElement(Proposals.serializer(), jsonElement)
                Kind.Reveal -> jsonDecoder.json.decodeFromJsonElement(Reveal.serializer(), jsonElement)
                Kind.SeedNonceRevelation -> jsonDecoder.json.decodeFromJsonElement(SeedNonceRevelation.serializer(), jsonElement)
                Kind.Transaction -> jsonDecoder.json.decodeFromJsonElement(Transaction.serializer(), jsonElement)
            }
        }

        override fun serialize(encoder: Encoder, value: TezosOperation) {
            when (value) {
                is ActivateAccount -> encoder.encodeSerializableValue(ActivateAccount.serializer(), value)
                is Ballot -> encoder.encodeSerializableValue(Ballot.serializer(), value)
                is Delegation -> encoder.encodeSerializableValue(Delegation.serializer(), value)
                is DoubleBakingEvidence -> encoder.encodeSerializableValue(DoubleBakingEvidence.serializer(), value)
                is Endorsement -> encoder.encodeSerializableValue(Endorsement.serializer(), value)
                is DoubleEndorsementEvidence -> encoder.encodeSerializableValue(DoubleEndorsementEvidence.serializer(), value)
                is Origination -> encoder.encodeSerializableValue(Origination.serializer(), value)
                is Proposals -> encoder.encodeSerializableValue(Proposals.serializer(), value)
                is Reveal -> encoder.encodeSerializableValue(Reveal.serializer(), value)
                is SeedNonceRevelation -> encoder.encodeSerializableValue(SeedNonceRevelation.serializer(), value)
                is Transaction -> encoder.encodeSerializableValue(Transaction.serializer(), value)
            }
        }

        private fun failWithMissingField(name: String): Nothing =
            throw SerializationException("Could not deserialize, `$name` field is missing")

    }
}