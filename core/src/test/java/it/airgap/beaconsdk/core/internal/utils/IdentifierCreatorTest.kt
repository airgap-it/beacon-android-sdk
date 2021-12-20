package it.airgap.beaconsdk.core.internal.utils

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.core.blockchain.Blockchain
import it.airgap.beaconsdk.core.data.MockNetwork
import it.airgap.beaconsdk.core.data.Network
import it.airgap.beaconsdk.core.internal.blockchain.BlockchainRegistry
import it.airgap.beaconsdk.core.internal.blockchain.MockBlockchain
import it.airgap.beaconsdk.core.internal.crypto.Crypto
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

internal class IdentifierCreatorTest {

    @MockK
    private lateinit var crypto: Crypto

    @MockK
    private lateinit var base58Check: Base58Check

    @MockK
    private lateinit var blockchainRegistry: BlockchainRegistry

    private lateinit var blockchain: Blockchain
    private lateinit var identifierCreator: IdentifierCreator

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        blockchain = MockBlockchain()

        every { blockchainRegistry.getOrNull(any()) } returns blockchain

        every { crypto.hash(any<String>(), any()) } answers { Result.success(firstArg<String>().encodeToByteArray()) }
        every { crypto.hash(any<ByteArray>(), any()) } answers { Result.success(firstArg<ByteArray>().toHexString().asString().encodeToByteArray()) }
        every { base58Check.encode(any()) } answers { Result.success(firstArg<ByteArray>().decodeToString()) }

        identifierCreator = IdentifierCreator(crypto, base58Check, blockchainRegistry)
    }

    @Test
    fun `creates account identifier from address and network`() {
        expectedIdentifiers()
            .map { identifierCreator.accountId(it.first.first, it.first.second).getOrThrow() to it.second }
            .forEach { assertEquals(it.second, it.first) }
    }

    @Test
    fun `creates sender identifier from public key`() {
        val publicKey = "00"
        val senderId = identifierCreator.senderId(publicKey.asHexString().toByteArray()).getOrThrow()

        assertEquals(publicKey, senderId)
    }

    private fun expectedIdentifiers(address: String = "address"): List<Pair<Pair<String, Network>, String>> =
        listOf(
            Pair(address, MockNetwork()) to "$address-mock",
            Pair(address, MockNetwork(name = "name")) to "$address-mock-name:name",
            Pair(address, MockNetwork(rpcUrl = "rpcUrl")) to "$address-mock-rpc:rpcUrl",
            Pair(address, MockNetwork(name = "name", rpcUrl = "rpcUrl")) to "$address-mock-name:name-rpc:rpcUrl",
        )
}