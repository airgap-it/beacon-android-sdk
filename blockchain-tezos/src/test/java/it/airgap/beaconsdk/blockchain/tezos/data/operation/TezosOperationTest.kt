package it.airgap.beaconsdk.blockchain.tezos.data.operation

import fromValues
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import org.junit.Test
import kotlin.test.assertEquals

internal class TezosOperationTest {

    @Test
    fun `is deserialized from JSON`() {
        listOf(
            activateAccountWithJson(),
            ballotWithJson(),

            delegationWithJson(),
            delegationWithJson("source", "fee", "counter", "gasLimit", "storageLimit", "delegate"),
            delegationWithJson(includeNulls = true),

            doubleBakingEvidenceWithJson(),

            endorsementWithJson(),

            doubleBakingEvidenceWithJson(),

            doubleEndorsementEvidenceWithJson(),

            originationWithJson(),
            originationWithJson("source", "fee", "counter", "gasLimit", "storageLimit", delegate = "delegate"),
            originationWithJson(includeNulls = true),

            proposalsWithJson(),

            revealWithJson(),
            revealWithJson("source", "fee", "counter", "gasLimit", "storageLimit"),
            revealWithJson(includeNulls = true),

            seedNonceRevelationWithJson(),

            transactionWithJson(),
            transactionWithJson(
                "source",
                "fee",
                "counter",
                "gasLimit",
                "storageLimit",
                parameters = TezosTransactionOperation.Parameters("entrypoint", MichelinePrimitiveString("string"))
            ),
            transactionWithJson(includeNulls = true),
        ).map {
            Json.decodeFromString<TezosOperation>(it.second) to it.first
        }.forEach {
            assertEquals(it.second, it.first)
        }
    }

    @Test
    fun `deserializes ballot type from JSON`() {
        ballotTypesWithJsonStrings()
            .map { Json.decodeFromString<TezosBallotOperation.Type>(it.second) to it.first }
            .forEach { assertEquals(it.second, it.first) }
    }

    @Test
    fun `deserializes transaction parameters from JSON`() {
        listOf(transactionParametersWithJson())
            .map { Json.decodeFromString<TezosTransactionOperation.Parameters>(it.second) to it.first }
            .forEach { assertEquals(it.second, it.first) }
    }

    @Test
    fun `serializes to JSON`() {
        listOf(
            activateAccountWithJson(),

            ballotWithJson(),

            delegationWithJson(),
            delegationWithJson("source", "fee", "counter", "gasLimit", "storageLimit", "delegate"),

            doubleBakingEvidenceWithJson(),

            endorsementWithJson(),

            doubleBakingEvidenceWithJson(),

            doubleEndorsementEvidenceWithJson(),

            originationWithJson(),
            originationWithJson("source", "fee", "counter", "gasLimit", "storageLimit", delegate = "delegate"),

            proposalsWithJson(),

            revealWithJson(),
            revealWithJson("source", "fee", "counter", "gasLimit", "storageLimit"),

            seedNonceRevelationWithJson(),

            transactionWithJson(),
            transactionWithJson(
                "source",
                "fee",
                "counter",
                "gasLimit",
                "storageLimit",
                parameters = TezosTransactionOperation.Parameters("entrypoint", MichelinePrimitiveString("string"))
            ),
        ).map {
            Json.decodeFromString(JsonObject.serializer(), Json.encodeToString(it.first)) to
                    Json.decodeFromString(JsonObject.serializer(), it.second)
        }.forEach {
            assertEquals(it.second, it.first)
        }
    }

    @Test
    fun `serializes ballot type to JSON`() {
        ballotTypesWithJsonStrings()
            .map { Json.encodeToString(it.first) to it.second }
            .forEach { assertEquals(it.second, it.first) }
    }

    @Test
    fun `serializes transaction parameters to JSON`() {
        listOf(transactionParametersWithJson())
            .map {
                Json.decodeFromString(JsonObject.serializer(), Json.encodeToString(it.first)) to
                        Json.decodeFromString(JsonObject.serializer(), it.second)
            }.forEach {
                assertEquals(it.second, it.first)
            }
    }

    private fun activateAccountWithJson(
        pkh: String = "pkh",
        secret: String = "secret",
    ): Pair<TezosActivateAccountOperation, String> =
        TezosActivateAccountOperation(pkh, secret) to """
            {
                "kind": "activate_account",
                "pkh": "$pkh",
                "secret": "$secret"
            }
        """.trimIndent()

    private fun ballotWithJson(
        source: String = "source",
        period: String = "period",
        proposal: String = "proposal",
        ballot: TezosBallotOperation.Type = TezosBallotOperation.Type.Yay,
    ): Pair<TezosBallotOperation, String> =
        TezosBallotOperation(source, period, proposal, ballot) to """
            {
                "kind": "ballot",
                "source": "$source",
                "period": "$period",
                "proposal": "$proposal",
                "ballot": ${Json.encodeToString(ballot)}
            }
        """.trimIndent()

    private fun ballotTypesWithJsonStrings(): List<Pair<TezosBallotOperation.Type, String>> = listOf(
        TezosBallotOperation.Type.Nay to "\"nay\"",
        TezosBallotOperation.Type.Yay to "\"yay\"",
        TezosBallotOperation.Type.Pass to "\"pass\"",
    )

    private fun delegationWithJson(
        source: String? = null,
        fee: String? = null,
        counter: String? = null,
        gasLimit: String? = null,
        storageLimit: String? = null,
        delegate: String? = null,
        includeNulls: Boolean = false,
    ) : Pair<TezosDelegationOperation, String> {
        val values = mapOf(
            "kind" to "delegation",
            "source" to source,
            "fee" to fee,
            "counter" to counter,
            "gas_limit" to gasLimit,
            "storage_limit" to storageLimit,
            "delegate" to delegate,
        )

        val json = JsonObject.fromValues(values, includeNulls).toString()

        return TezosDelegationOperation(source, fee, counter, gasLimit, storageLimit, delegate) to json
    }

    private fun doubleBakingEvidenceWithJson(
        bh1: TezosBlockHeader = TezosBlockHeader(
            1,
            1,
            "predecessor#1",
            "timestamp#1",
            1,
            "operationHash#1",
            emptyList(),
            "context#1",
            1,
            "proofOfWorkNonce#1",
            "signature#1"
        ),
        bh2: TezosBlockHeader = TezosBlockHeader(
            2,
            2,
            "predecessor#2",
            "timestamp#2",
            2,
            "operationHash#2",
            emptyList(),
            "context#2",
            2,
            "proofOfWorkNonce#2",
            "signature#2"
        ),
    ) : Pair<TezosDoubleBakingEvidenceOperation, String> =
        TezosDoubleBakingEvidenceOperation(bh1, bh2) to """
            {
                "kind": "double_baking_evidence",
                "bh1": ${Json.encodeToString(bh1)},
                "bh2": ${Json.encodeToString(bh2)}
            }
        """.trimIndent()

    private fun endorsementWithJson(level: String = "level") : Pair<TezosEndorsementOperation, String> =
        TezosEndorsementOperation(level) to """
            {
                "kind": "endorsement",
                "level": "level"
            }
        """.trimIndent()

    private fun doubleEndorsementEvidenceWithJson(
        op1: TezosInlinedEndorsement = TezosInlinedEndorsement(
            "branch#1",
            TezosEndorsementOperation("level#1")),
        op2: TezosInlinedEndorsement = TezosInlinedEndorsement(
            "branch#2",
            TezosEndorsementOperation("level#2")),
    ) : Pair<TezosDoubleEndorsementEvidenceOperation, String> =
        TezosDoubleEndorsementEvidenceOperation(op1, op2) to """
            {
                "kind": "double_endorsement_evidence",
                "op1": ${Json.encodeToString(op1)},
                "op2": ${Json.encodeToString(op2)}
            }
        """.trimIndent()

    private fun originationWithJson(
        source: String? = null,
        fee: String? = null,
        counter: String? = null,
        gasLimit: String? = null,
        storageLimit: String? = null,
        balance: String = "balance",
        delegate: String? = null,
        script: TezosOriginationOperation.Script = TezosOriginationOperation.Script(
            code = MichelinePrimitiveInt(0),
            storage = MichelinePrimitiveInt(1),
        ),
        includeNulls: Boolean = false,
    ) : Pair<TezosOriginationOperation, String> {
        val values = mapOf(
            "kind" to "origination",
            "source" to source,
            "fee" to fee,
            "counter" to counter,
            "gas_limit" to gasLimit,
            "storage_limit" to storageLimit,
            "balance" to balance,
            "delegate" to delegate,
            "script" to  Json.encodeToJsonElement(script),
        )

        val json = JsonObject.fromValues(values, includeNulls).toString()

        return TezosOriginationOperation(
            source,
            fee,
            counter,
            gasLimit,
            storageLimit,
            balance,
            delegate,
            script,
        ) to json
    }

    private fun proposalsWithJson(
        period: String = "period",
        proposals: List<String> = emptyList(),
    ) : Pair<TezosProposalsOperation, String> =
        TezosProposalsOperation(period, proposals) to """
            {
                "kind": "proposals",
                "period": "$period",
                "proposals": ${Json.encodeToString(proposals)}
            }
        """.trimIndent()

    private fun revealWithJson(
        source: String? = null,
        fee: String? = null,
        counter: String? = null,
        gasLimit: String? = null,
        storageLimit: String? = null,
        publicKey: String = "publicKey",
        includeNulls: Boolean = false
    ) : Pair<TezosRevealOperation, String> {
        val values = mapOf(
            "kind" to "reveal",
            "source" to source,
            "fee" to fee,
            "counter" to counter,
            "gas_limit" to gasLimit,
            "storage_limit" to storageLimit,
            "public_key" to publicKey,
        )

        val json = JsonObject.fromValues(values, includeNulls).toString()

        return TezosRevealOperation(
            source,
            fee,
            counter,
            gasLimit,
            storageLimit,
            publicKey,
        ) to json
    }

    private fun seedNonceRevelationWithJson(
        level: String = "level",
        nonce: String = "nonce",
    ) : Pair<TezosSeedNonceRevelationOperation, String> =
        TezosSeedNonceRevelationOperation(level, nonce) to """
            {
                "kind": "seed_nonce_revelation",
                "level": "$level",
                "nonce": "$nonce"
            }
        """.trimIndent()

    private fun transactionWithJson(
        source: String? = null,
        fee: String? = null,
        counter: String? = null,
        gasLimit: String? = null,
        storageLimit: String? = null,
        amount: String = "amount",
        destination: String = "destination",
        parameters: TezosTransactionOperation.Parameters? = null,
        includeNulls: Boolean = false,
    ) : Pair<TezosTransactionOperation, String> {
        val values = mapOf(
            "kind" to "transaction",
            "source" to source,
            "fee" to fee,
            "counter" to counter,
            "gas_limit" to gasLimit,
            "storage_limit" to storageLimit,
            "amount" to amount,
            "destination" to destination,
            "parameters" to parameters?.let { Json.encodeToJsonElement(it) },
        )

        val json = JsonObject.fromValues(values, includeNulls).toString()

        return TezosTransactionOperation(
            source,
            fee,
            counter,
            gasLimit,
            storageLimit,
            amount,
            destination,
            parameters,
        ) to json
    }

    private fun transactionParametersWithJson(
        entrypoint: String = "entrypoint",
        value: MichelineMichelsonV1Expression = MichelinePrimitiveString("string"),
    ): Pair<TezosTransactionOperation.Parameters, String> =
        TezosTransactionOperation.Parameters(entrypoint, value) to """
            {
                "entrypoint": "$entrypoint",
                "value": ${Json.encodeToString(value)}
            }
        """.trimIndent()
}