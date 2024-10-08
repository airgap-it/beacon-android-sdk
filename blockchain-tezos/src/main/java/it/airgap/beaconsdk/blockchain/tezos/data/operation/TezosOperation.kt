package it.airgap.beaconsdk.blockchain.tezos.data.operation

import it.airgap.beaconsdk.core.internal.utils.KJsonSerializer
import it.airgap.beaconsdk.core.internal.utils.getSerializable
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Required
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.jsonObject

/**
 * Base for Tezos operations supported in Beacon.
 */
@Serializable(with = TezosOperation.Serializer::class)
public sealed class TezosOperation {
    public abstract val kind: Kind

    public companion object {}

    @Serializable
    public enum class Kind {
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

    @OptIn(ExperimentalSerializationApi::class)
    internal object Serializer : KJsonSerializer<TezosOperation> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("TezosOperation") {
            element<String>("kind")
        }

        override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): TezosOperation {
            val kind = jsonElement.jsonObject.getSerializable(descriptor.getElementName(0), jsonDecoder, Kind.serializer())

            return when (kind) {
                Kind.ActivateAccount -> jsonDecoder.json.decodeFromJsonElement(TezosActivateAccountOperation.serializer(), jsonElement)
                Kind.Ballot -> jsonDecoder.json.decodeFromJsonElement(TezosBallotOperation.serializer(), jsonElement)
                Kind.Delegation -> jsonDecoder.json.decodeFromJsonElement(TezosDelegationOperation.serializer(), jsonElement)
                Kind.DoubleBakingEvidence -> jsonDecoder.json.decodeFromJsonElement(TezosDoubleBakingEvidenceOperation.serializer(), jsonElement)
                Kind.Endorsement -> jsonDecoder.json.decodeFromJsonElement(TezosEndorsementOperation.serializer(), jsonElement)
                Kind.DoubleEndorsementEvidence -> jsonDecoder.json.decodeFromJsonElement(TezosDoubleEndorsementEvidenceOperation.serializer(), jsonElement)
                Kind.Origination -> jsonDecoder.json.decodeFromJsonElement(TezosOriginationOperation.serializer(), jsonElement)
                Kind.Proposals -> jsonDecoder.json.decodeFromJsonElement(TezosProposalsOperation.serializer(), jsonElement)
                Kind.Reveal -> jsonDecoder.json.decodeFromJsonElement(TezosRevealOperation.serializer(), jsonElement)
                Kind.SeedNonceRevelation -> jsonDecoder.json.decodeFromJsonElement(TezosSeedNonceRevelationOperation.serializer(), jsonElement)
                Kind.Transaction -> jsonDecoder.json.decodeFromJsonElement(TezosTransactionOperation.serializer(), jsonElement)
            }
        }

        override fun serialize(jsonEncoder: JsonEncoder, value: TezosOperation) {
            when (value) {
                is TezosActivateAccountOperation -> jsonEncoder.encodeSerializableValue(TezosActivateAccountOperation.serializer(), value)
                is TezosBallotOperation -> jsonEncoder.encodeSerializableValue(TezosBallotOperation.serializer(), value)
                is TezosDelegationOperation -> jsonEncoder.encodeSerializableValue(TezosDelegationOperation.serializer(), value)
                is TezosDoubleBakingEvidenceOperation -> jsonEncoder.encodeSerializableValue(TezosDoubleBakingEvidenceOperation.serializer(), value)
                is TezosEndorsementOperation -> jsonEncoder.encodeSerializableValue(TezosEndorsementOperation.serializer(), value)
                is TezosDoubleEndorsementEvidenceOperation -> jsonEncoder.encodeSerializableValue(TezosDoubleEndorsementEvidenceOperation.serializer(), value)
                is TezosOriginationOperation -> jsonEncoder.encodeSerializableValue(TezosOriginationOperation.serializer(), value)
                is TezosProposalsOperation -> jsonEncoder.encodeSerializableValue(TezosProposalsOperation.serializer(), value)
                is TezosRevealOperation -> jsonEncoder.encodeSerializableValue(TezosRevealOperation.serializer(), value)
                is TezosSeedNonceRevelationOperation -> jsonEncoder.encodeSerializableValue(TezosSeedNonceRevelationOperation.serializer(), value)
                is TezosTransactionOperation -> jsonEncoder.encodeSerializableValue(TezosTransactionOperation.serializer(), value)
            }
        }

    }
}

@Serializable
public data class TezosActivateAccountOperation(
    public val pkh: String,
    public val secret: String,
) : TezosOperation() {
    @Required
    override val kind: Kind = Kind.ActivateAccount

    public companion object {}
}

@Serializable
public data class TezosBallotOperation(
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
public data class TezosDelegationOperation(
    public val source: String? = null,
    public val fee: String? = null,
    public val counter: String? = null,
    @SerialName("gas_limit") public val gasLimit: String? = null,
    @SerialName("storage_limit") public val storageLimit: String? = null,
    public val delegate: String? = null,
) : TezosOperation() {
    @Required
    override val kind: Kind = Kind.Delegation

    public companion object {}
}

@Serializable
public data class TezosDoubleBakingEvidenceOperation(
    public val bh1: TezosBlockHeader,
    public val bh2: TezosBlockHeader,
) : TezosOperation() {
    @Required
    override val kind: Kind = Kind.DoubleBakingEvidence

    public companion object {}
}

@Serializable
public data class TezosEndorsementOperation(public val level: String) : TezosOperation() {
    @Required
    override val kind: Kind = Kind.Endorsement

    public companion object {}
}

@Serializable
public data class TezosDoubleEndorsementEvidenceOperation(
    public val op1: TezosInlinedEndorsement,
    public val op2: TezosInlinedEndorsement,
) : TezosOperation() {
    @Required
    override val kind: Kind = Kind.DoubleEndorsementEvidence

    public companion object {}
}

@Serializable
public data class TezosOriginationOperation(
    public val source: String? = null,
    public val fee: String? = null,
    public val counter: String? = null,
    @SerialName("gas_limit") public val gasLimit: String? = null,
    @SerialName("storage_limit") public val storageLimit: String? = null,
    public val balance: String,
    public val delegate: String? = null,
    public val script: Script,
) : TezosOperation() {
    @Required
    override val kind: Kind = Kind.Origination

    @Serializable
    public data class Script(
        public val code: MichelineMichelsonV1Expression,
        public val storage: MichelineMichelsonV1Expression,
    )

    public companion object {}
}

@Serializable
public data class TezosProposalsOperation(
    public val period: String,
    public val proposals: List<String>,
) : TezosOperation() {
    @Required
    override val kind: Kind = Kind.Proposals

    public companion object {}
}

@Serializable
public data class TezosRevealOperation(
    public val source: String? = null,
    public val fee: String? = null,
    public val counter: String? = null,
    @SerialName("gas_limit") public val gasLimit: String? = null,
    @SerialName("storage_limit") public val storageLimit: String? = null,
    @SerialName("public_key") public val publicKey: String,
) : TezosOperation() {
    @Required
    override val kind: Kind = Kind.Reveal

    public companion object {}
}

@Serializable
public data class TezosSeedNonceRevelationOperation(public val level: String, public val nonce: String) : TezosOperation() {
    @Required
    override val kind: Kind = Kind.SeedNonceRevelation

    public companion object {}
}

@Serializable
public data class TezosTransactionOperation(
    public val source: String? = null,
    public val fee: String? = null,
    public val counter: String? = null,
    @SerialName("gas_limit") public val gasLimit: String? = null,
    @SerialName("storage_limit") public val storageLimit: String? = null,
    public val amount: String,
    public val destination: String,
    public val parameters: Parameters? = null,
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