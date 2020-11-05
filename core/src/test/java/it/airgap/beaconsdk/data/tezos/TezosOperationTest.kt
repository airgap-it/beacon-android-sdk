package it.airgap.beaconsdk.data.tezos

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Test
import kotlin.test.assertEquals

internal class TezosOperationTest {

    @Test
    fun `is deserialized from JSON`() {
        listOf(
            activateAccountWithJson(),
            ballotWithJson(),
            delegationWithJson(),
            delegationWithJson(includeNulls = true),
            doubleBakingEvidenceWithJson(),
            endorsementWithJson(),
            doubleBakingEvidenceWithJson(),
            doubleEndorsementEvidenceWithJson(),
            originationWithJson(),
            originationWithJson(includeNulls = true),
            proposalsWithJson(),
            revealWithJson(),
            seedNonceRevelationWithJson(),
            transactionWithJson(),
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
            doubleBakingEvidenceWithJson(),
            endorsementWithJson(),
            doubleBakingEvidenceWithJson(),
            doubleEndorsementEvidenceWithJson(),
            originationWithJson(),
            proposalsWithJson(),
            revealWithJson(),
            seedNonceRevelationWithJson(),
            transactionWithJson(),
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
        source: String = "source",
        fee: String = "fee",
        counter: String = "counter",
        gasLimit: String = "gasLimit",
        storageLimit: String = "storageLimit",
        delegate: String? = null,
        includeNulls: Boolean = false,
    ) : Pair<TezosDelegationOperation, String> {
        val  json = if (delegate != null || includeNulls) {
            """
                {
                    "kind": "delegation",
                    "source": "$source",
                    "fee": "$fee",
                    "counter": "$counter",
                    "gas_limit": "$gasLimit",
                    "storage_limit": "$storageLimit",
                    "delegate": ${Json.encodeToString(delegate)}
                }
            """.trimIndent()
        } else {
            """
               {
                    "kind": "delegation",
                    "source": "$source",
                    "fee": "$fee",
                    "counter": "$counter",
                    "gas_limit": "$gasLimit",
                    "storage_limit": "$storageLimit"
               } 
            """.trimIndent()
        }

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
        op1: TezosInlinedEndorsement = TezosInlinedEndorsement("branch#1", TezosEndorsementOperation("level#1")),
        op2: TezosInlinedEndorsement = TezosInlinedEndorsement("branch#2", TezosEndorsementOperation("level#2")),
    ) : Pair<TezosDoubleEndorsementEvidenceOperation, String> =
        TezosDoubleEndorsementEvidenceOperation(op1, op2) to """
            {
                "kind": "double_endorsement_evidence",
                "op1": ${Json.encodeToString(op1)},
                "op2": ${Json.encodeToString(op2)}
            }
        """.trimIndent()

    private fun originationWithJson(
        source: String = "source",
        fee: String = "fee",
        counter: String = "counter",
        gasLimit: String = "gasLimit",
        storageLimit: String = "storageLimit",
        balance: String = "balance",
        script: String = "script",
        delegate: String? = null,
        includeNulls: Boolean = false,
    ) : Pair<TezosOriginationOperation, String> {
        val  json = if (delegate != null || includeNulls) {
            """
                {
                    "kind": "origination",
                    "source": "$source",
                    "fee": "$fee",
                    "counter": "$counter",
                    "gas_limit": "$gasLimit",
                    "storage_limit": "$storageLimit",
                    "balance": "$balance",
                    "script": "$script",
                    "delegate": ${Json.encodeToString(delegate)}
                }
            """.trimIndent()
        } else {
            """
               {
                    "kind": "origination",
                    "source": "$source",
                    "fee": "$fee",
                    "counter": "$counter",
                    "gas_limit": "$gasLimit",
                    "storage_limit": "$storageLimit",
                    "balance": "$balance",
                    "script": "$script"
               } 
            """.trimIndent()
        }

        return TezosOriginationOperation(
            source,
            fee,
            counter,
            gasLimit,
            storageLimit,
            balance,
            script,
            delegate,
        ) to json
    }

    private fun proposalsWithJson(
        period: String = "period",
        proposals: List<String> = emptyList(),
    ) : Pair<TezosProposalsOpertion, String> =
        TezosProposalsOpertion(period, proposals) to """
            {
                "kind": "proposals",
                "period": "$period",
                "proposals": ${Json.encodeToString(proposals)}
            }
        """.trimIndent()

    private fun revealWithJson(
        source: String = "source",
        fee: String = "fee",
        counter: String = "counter",
        gasLimit: String = "gasLimit",
        storageLimit: String = "storageLimit",
        publicKey: String = "publicKey",
    ) : Pair<TezosRevealOperation, String> =
        TezosRevealOperation(
            source,
            fee,
            counter,
            gasLimit,
            storageLimit,
            publicKey,
        ) to """
            {
                "kind": "reveal",
                "source": "$source",
                "fee": "$fee",
                "counter": "$counter",
                "gas_limit": "$gasLimit",
                "storage_limit": "$storageLimit",
                "public_key": "$publicKey"
            }
        """.trimIndent()

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
        source: String = "source",
        fee: String = "fee",
        counter: String = "counter",
        gasLimit: String = "gasLimit",
        storageLimit: String = "storageLimit",
        amount: String = "amount",
        destination: String = "destination",
        parameters: TezosTransactionOperation.Parameters = TezosTransactionOperation.Parameters(
            "entrypoint",
            MichelinePrimitiveString("string"),
        ),
    ) : Pair<TezosTransactionOperation, String> =
        TezosTransactionOperation(
            source,
            fee,
            counter,
            gasLimit,
            storageLimit,
            amount,
            destination,
            parameters,
        ) to """
           {
                "kind": "transaction",
                "source": "$source",
                "fee": "$fee",
                "counter": "$counter",
                "gas_limit": "$gasLimit",
                "storage_limit": "$storageLimit",
                "amount": "$amount",
                "destination": "$destination",
                "parameters": ${Json.encodeToString(parameters)}
           } 
        """.trimIndent()

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