package it.airgap.beaconsdk.chain.tezos.internal

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.chain.tezos.Tezos
import it.airgap.beaconsdk.core.internal.crypto.Crypto
import it.airgap.beaconsdk.core.internal.utils.Base58Check
import it.airgap.beaconsdk.core.internal.utils.asHexString
import it.airgap.beaconsdk.core.internal.utils.toHexString
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class WalletTest {

    @MockK
    private lateinit var crypto: Crypto

    @MockK
    private lateinit var base58Check: Base58Check

    private lateinit var wallet: TezosWallet

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        every { crypto.hash(any<ByteArray>(), any()) } answers { Result.success(firstArg()) }

        every { base58Check.encode(any()) } answers { Result.success(firstArg<ByteArray>().toHexString().asString()) }
        every { base58Check.decode(any()) } answers { Result.success(Tezos.PrefixBytes.edpk + firstArg<String>().removePrefix(Tezos.Prefix.edpk).asHexString().toByteArray()) }

        wallet = TezosWallet(crypto, base58Check)
    }

    @Test
    fun `creates Tezos address from plain public key`() {
        val publicKey = "00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff"
        val address = wallet.addressFromPublicKey(publicKey).getOrThrow()

        assertEquals("${Tezos.PrefixBytes.tz1.toHexString().asString()}$publicKey", address)
    }

    @Test
    fun `creates Tezos address from entrypted public key`() {
        val publicKey = "${Tezos.Prefix.edpk}00112233445566778899aabbccddeeff001122334455667788"
        val address = wallet.addressFromPublicKey(publicKey).getOrThrow()

        assertEquals("${Tezos.PrefixBytes.tz1.toHexString().asString()}${publicKey.removePrefix(Tezos.Prefix.edpk)}", address)
    }

    @Test
    fun `fails to create Tezos address from invalid public key`() {
        val publicKey = "00112233445566778899aabbccddeeff"
        val address = wallet.addressFromPublicKey(publicKey)

        assertTrue(address.isFailure, "Expected address result to be a failure.")
    }
}