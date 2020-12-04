package it.airgap.beaconsdk.internal.utils

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.data.beacon.Network
import it.airgap.beaconsdk.internal.crypto.Crypto
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

internal class AccountUtilsTest {

    @MockK
    private lateinit var crypto: Crypto

    @MockK
    private lateinit var base58Check: Base58Check

    private lateinit var accountUtils: AccountUtils

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        every { crypto.hash(any<String>(), any()) } answers { Success(firstArg<String>().encodeToByteArray()) }
        every { crypto.hash(any<HexString>(), any()) } answers { Success(firstArg<HexString>().asString().encodeToByteArray()) }
        every { base58Check.encode(any()) } answers { Success(firstArg<ByteArray>().decodeToString()) }

        accountUtils = AccountUtils(crypto, base58Check)
    }

    @Test
    fun `creates account identifier from address and network`() {
        expectedIdentifiers()
            .map { accountUtils.getAccountIdentifier(it.first.first, it.first.second).get() to it.second }
            .forEach { assertEquals(it.second, it.first) }
    }

    @Test
    fun `creates sender id from public key`() {
        val publicKey = "00"
        val senderId = accountUtils.getSenderId(HexString.fromString(publicKey)).get()

        assertEquals(publicKey, senderId)
    }

    private fun expectedIdentifiers(address: String = "address"): List<Pair<Pair<String, Network>, String>> =
        listOf(
            Pair(address, Network.Mainnet()) to "$address-mainnet",
            Pair(address, Network.Mainnet(name = "name")) to "$address-mainnet-name:name",
            Pair(address, Network.Mainnet(rpcUrl = "rpcUrl")) to "$address-mainnet-rpc:rpcUrl",
            Pair(address, Network.Mainnet(name = "name", rpcUrl = "rpcUrl")) to "$address-mainnet-name:name-rpc:rpcUrl",
            Pair(address, Network.Carthagenet()) to "$address-carthagenet",
            Pair(address, Network.Carthagenet(name = "name")) to "$address-carthagenet-name:name",
            Pair(address, Network.Carthagenet(rpcUrl = "rpcUrl")) to "$address-carthagenet-rpc:rpcUrl",
            Pair(address, Network.Carthagenet(name = "name", rpcUrl = "rpcUrl")) to "$address-carthagenet-name:name-rpc:rpcUrl",
            Pair(address, Network.Custom()) to "$address-custom",
            Pair(address, Network.Custom(name = "name")) to "$address-custom-name:name",
            Pair(address, Network.Custom(rpcUrl = "rpcUrl")) to "$address-custom-rpc:rpcUrl",
            Pair(address, Network.Custom(name = "name", rpcUrl = "rpcUrl")) to "$address-custom-name:name-rpc:rpcUrl",
        )
}