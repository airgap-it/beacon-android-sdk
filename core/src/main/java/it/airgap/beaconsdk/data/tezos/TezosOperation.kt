package it.airgap.beaconsdk.data.tezos

import kotlinx.serialization.Serializable

@Serializable
sealed class TezosOperation(internal val kind: Kind) {
    @Serializable
    data class ActivateAccount(val pkh: String, val secret: String) : TezosOperation(Kind.ActivateAccount) {
        companion object {}
    }

    @Serializable
    data class Ballot(val source: String, val period: String, val proposal: String, val ballot: Type) : TezosOperation(Kind.Ballot) {
        enum class Type { Nay, Yay, Pass }

        companion object {}
    }

    @Serializable
    data class DelegationOperation(
        val source: String,
        val fee: String,
        val counter: String,
        val gasLimit: String,
        val storageLimit: String,
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
        val gasLimit: String,
        val storageLimit: String,
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
        val gasLimit: String,
        val storageLimit: String,
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
        val gasLimit: String,
        val storageLimit: String,
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

    internal enum class Kind {
        Endorsement,
        SeedNonceRevelation,
        DoubleEndorsementEvidence,
        DoubleBakingEvidence,
        ActivateAccount,
        Proposals,
        Ballot,
        Reveal,
        Transaction,
        Origination,
        Delegation
    }
}