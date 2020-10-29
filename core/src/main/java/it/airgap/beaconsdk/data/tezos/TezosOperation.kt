package it.airgap.beaconsdk.data.tezos

import it.airgap.beaconsdk.internal.utils.failWithExpectedJsonDecoder
import it.airgap.beaconsdk.internal.utils.failWithMissingField
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Required
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable(with = TezosOperation.Serializer::class)
public sealed class TezosOperation {
    internal abstract val kind: Kind

    @Serializable
    public data class ActivateAccount(
        public val pkh: String,
        public val secret: String,
    ) : TezosOperation() {
        @Required
        override val kind: Kind = Kind.ActivateAccount

        public companion object {}
    }

    @Serializable
    public data class Ballot(
        public val source: String,
        public val period: String,
        public val proposal: String,
        public val ballot: Type,
    ) : TezosOperation() {
        @Required
        override val kind: Kind = Kind.Ballot

        @Serializable
        public enum class Type {
            @SerialName("nay")
            Nay,

            @SerialName("yay")
            Yay,

            @SerialName("pass")
            Pass,
        }

        public companion object {}
    }

    @Serializable
    public data class Delegation(
        public val source: String,
        public val fee: String,
        public val counter: String,
        @SerialName("gas_limit") public val gasLimit: String,
        @SerialName("storage_limit") public val storageLimit: String,
        public val delegate: String?,
    ) : TezosOperation() {
        @Required
        override val kind: Kind = Kind.Delegation

        public companion object {}
    }

    @Serializable
    public data class DoubleBakingEvidence(
        public val bh1: TezosBlockHeader,
        public val bh2: TezosBlockHeader,
    ) : TezosOperation() {
        @Required
        override val kind: Kind = Kind.DoubleBakingEvidence

        public companion object {}
    }

    @Serializable
    public data class Endorsement(public val level: String) : TezosOperation() {
        @Required
        override val kind: Kind = Kind.Endorsement

        public companion object {}
    }

    @Serializable
    public data class DoubleEndorsementEvidence(
        public val op1: TezosInlinedEndorsement,
        public val op2: TezosInlinedEndorsement,
    ) : TezosOperation() {
        @Required
        override val kind: Kind = Kind.DoubleEndorsementEvidence

        public companion object {}
    }

    @Serializable
    public data class Origination(
        public val source: String,
        public val fee: String,
        public val counter: String,
        @SerialName("gas_limit") public val gasLimit: String,
        @SerialName("storage_limit") public val storageLimit: String,
        public val balance: String,
        public val script: String,
        public val delegate: String?,
    ) : TezosOperation() {
        @Required
        override val kind: Kind = Kind.Origination

        public companion object {}
    }

    @Serializable
    public data class Proposals(
        public val period: String,
        public val proposals: List<String>,
    ) : TezosOperation() {
        @Required
        override val kind: Kind = Kind.Proposals

        public companion object {}
    }

    @Serializable
    public data class Reveal(
        public val source: String,
        public val fee: String,
        public val counter: String,
        @SerialName("gas_limit") public val gasLimit: String,
        @SerialName("storage_limit") public val storageLimit: String,
        public val publicKey: String,
    ) : TezosOperation() {
        @Required
        override val kind: Kind = Kind.Reveal

        public companion object {}
    }

    @Serializable
    public data class SeedNonceRevelation(public val level: String, public val nonce: String) :
        TezosOperation() {
        @Required
        override val kind: Kind = Kind.SeedNonceRevelation

        public companion object {}
    }

    @Serializable
    public data class Transaction(
        public val source: String,
        public val fee: String,
        public val counter: String,
        @SerialName("gas_limit") public val gasLimit: String,
        @SerialName("storage_limit") public val storageLimit: String,
        public val amount: String,
        public val destination: String,
        public val parameters: Parameters,
    ) : TezosOperation() {
        @Required
        override val kind: Kind = Kind.Transaction

        @Serializable
        public data class Parameters(
            public val entrypoint: String,
            public val value: MichelineMichelsonV1Expression,
        ) {
            public companion object {}
        }

        public companion object {}
    }

    public companion object {}

    @Serializable
    internal enum class Kind {
        @SerialName("endorsement")
        Endorsement,

        @SerialName("seed_nonce_revelation")
        SeedNonceRevelation,

        @SerialName("double_endorsement_evidence")
        DoubleEndorsementEvidence,

        @SerialName("double_baking_evidence")
        DoubleBakingEvidence,

        @SerialName("activate_account")
        ActivateAccount,

        @SerialName("proposals")
        Proposals,

        @SerialName("ballot")
        Ballot,

        @SerialName("reveal")
        Reveal,

        @SerialName("transaction")
        Transaction,

        @SerialName("origination")
        Origination,

        @SerialName("delegation")
        Delegation,
    }

    internal class Serializer : KSerializer<TezosOperation> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("TezosOperation", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): TezosOperation {
            val jsonDecoder = decoder as? JsonDecoder ?: failWithExpectedJsonDecoder(decoder::class)
            val jsonElement = jsonDecoder.decodeJsonElement()

            val kindField = "kind"
            val kindSerialized =
                jsonElement.jsonObject[kindField]?.jsonPrimitive ?: failWithMissingField(kindField)
            val kind = jsonDecoder.json.decodeFromJsonElement(Kind.serializer(), kindSerialized)

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

    }
}