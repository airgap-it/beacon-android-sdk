package it.airgap.beaconsdk.blockchain.tezos.data.operation

import fromValues
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.blockchain.tezos.Tezos
import it.airgap.beaconsdk.blockchain.tezos.internal.creator.*
import it.airgap.beaconsdk.blockchain.tezos.internal.serializer.*
import it.airgap.beaconsdk.blockchain.tezos.internal.wallet.TezosWallet
import it.airgap.beaconsdk.core.internal.BeaconConfiguration
import it.airgap.beaconsdk.core.internal.compat.CoreCompat
import it.airgap.beaconsdk.core.internal.di.DependencyRegistry
import it.airgap.beaconsdk.core.internal.serializer.coreJson
import it.airgap.beaconsdk.core.internal.storage.MockSecureStorage
import it.airgap.beaconsdk.core.internal.storage.MockStorage
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.utils.IdentifierCreator
import it.airgap.beaconsdk.core.scope.BeaconScope
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import mockDependencyRegistry
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

internal class TezosOperationTest {

    @MockK
    private lateinit var wallet: TezosWallet

    @MockK
    private lateinit var identifierCreator: IdentifierCreator

    private lateinit var dependencyRegistry: DependencyRegistry
    private lateinit var storageManager: StorageManager

    private lateinit var json: Json

    private val beaconScope: BeaconScope = BeaconScope.Global

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        storageManager = StorageManager(beaconScope, MockStorage(), MockSecureStorage(), identifierCreator, BeaconConfiguration(ignoreUnsupportedBlockchains = false))
        val tezos = Tezos(
            wallet,
            TezosCreator(
                DataTezosCreator(storageManager, identifierCreator),
                V1BeaconMessageTezosCreator(),
                V2BeaconMessageTezosCreator(),
                V3BeaconMessageTezosCreator(),
            ),
            TezosSerializer(
                DataTezosSerializer(),
                V1BeaconMessageTezosSerializer(),
                V2BeaconMessageTezosSerializer(),
                V3BeaconMessageTezosSerializer(),
            ),
        )

        dependencyRegistry = mockDependencyRegistry(tezos)
        json = coreJson(dependencyRegistry.blockchainRegistry, CoreCompat(beaconScope))
    }

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
            json.decodeFromString<TezosOperation>(it.second) to it.first
        }.forEach {
            assertEquals(it.second, it.first)
        }
    }

    @Test
    fun `deserializes ballot type from JSON`() {
        ballotTypesWithJsonStrings()
            .map { json.decodeFromString<TezosBallotOperation.Type>(it.second) to it.first }
            .forEach { assertEquals(it.second, it.first) }
    }

    @Test
    fun `deserializes transaction parameters from JSON`() {
        listOf(transactionParametersWithJson())
            .map { json.decodeFromString<TezosTransactionOperation.Parameters>(it.second) to it.first }
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
            json.decodeFromString(JsonObject.serializer(), json.encodeToString(it.first)) to
                    json.decodeFromString(JsonObject.serializer(), it.second)
        }.forEach {
            assertEquals(it.second, it.first)
        }
    }

    @Test
    fun `serializes ballot type to JSON`() {
        ballotTypesWithJsonStrings()
            .map { json.encodeToString(it.first) to it.second }
            .forEach { assertEquals(it.second, it.first) }
    }

    @Test
    fun `serializes transaction parameters to JSON`() {
        listOf(transactionParametersWithJson())
            .map {
                json.decodeFromString(JsonObject.serializer(), json.encodeToString(it.first)) to
                        json.decodeFromString(JsonObject.serializer(), it.second)
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
                "ballot": ${json.encodeToString(ballot)}
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

        val jsonObject = JsonObject.fromValues(values, includeNulls).toString()

        return TezosDelegationOperation(source, fee, counter, gasLimit, storageLimit, delegate) to jsonObject
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
                "bh1": ${json.encodeToString(bh1)},
                "bh2": ${json.encodeToString(bh2)}
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
                "op1": ${json.encodeToString(op1)},
                "op2": ${json.encodeToString(op2)}
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
            "script" to  json.encodeToJsonElement(script),
        )

        val jsonObject = JsonObject.fromValues(values, includeNulls).toString()

        return TezosOriginationOperation(
            source,
            fee,
            counter,
            gasLimit,
            storageLimit,
            balance,
            delegate,
            script,
        ) to jsonObject
    }

    private fun proposalsWithJson(
        period: String = "period",
        proposals: List<String> = emptyList(),
    ) : Pair<TezosProposalsOperation, String> =
        TezosProposalsOperation(period, proposals) to """
            {
                "kind": "proposals",
                "period": "$period",
                "proposals": ${json.encodeToString(proposals)}
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

        val jsonObject = JsonObject.fromValues(values, includeNulls).toString()

        return TezosRevealOperation(
            source,
            fee,
            counter,
            gasLimit,
            storageLimit,
            publicKey,
        ) to jsonObject
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
        destination: String = "receiverId",
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
            "parameters" to parameters?.let { json.encodeToJsonElement(it) },
        )

        val jsonObject = JsonObject.fromValues(values, includeNulls).toString()

        return TezosTransactionOperation(
            source,
            fee,
            counter,
            gasLimit,
            storageLimit,
            amount,
            destination,
            parameters,
        ) to jsonObject
    }

    private fun transactionParametersWithJson(
        entrypoint: String = "entrypoint",
        value: MichelineMichelsonV1Expression = MichelinePrimitiveString("string"),
    ): Pair<TezosTransactionOperation.Parameters, String> =
        TezosTransactionOperation.Parameters(entrypoint, value) to """
            {
                "entrypoint": "$entrypoint",
                "value": ${json.encodeToString(value)}
            }
        """.trimIndent()
}