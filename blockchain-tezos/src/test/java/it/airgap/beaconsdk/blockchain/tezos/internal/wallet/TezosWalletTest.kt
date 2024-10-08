package it.airgap.beaconsdk.blockchain.tezos.internal.wallet

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.core.internal.crypto.Crypto
import it.airgap.beaconsdk.core.internal.utils.Base58
import it.airgap.beaconsdk.core.internal.utils.Base58Check
import org.bouncycastle.crypto.digests.Blake2bDigest
import org.junit.Before
import org.junit.Test
import java.lang.Integer.min
import java.security.MessageDigest
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class TezosWalletTest {

    @MockK
    private lateinit var crypto: Crypto

    private lateinit var wallet: TezosWallet

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        every { crypto.hash(any<ByteArray>(), any()) } answers {
            val message = firstArg<ByteArray>()
            val size = secondArg<Int>()
            val blake2bDigest = Blake2bDigest(min(size * 8, 512))

            ByteArray(blake2bDigest.digestSize * 8).let {
                blake2bDigest.update(message, 0, message.size)
                blake2bDigest.doFinal(it, 0)

                Result.success(it.sliceArray(0 until size))
            }
        }

        every { crypto.hashSha256(any<ByteArray>()) } answers {
            val messageDigest = MessageDigest.getInstance("SHA-256")
            Result.success(messageDigest.digest(firstArg()))
        }

        wallet = TezosWallet(crypto, Base58Check(Base58(), crypto))
    }

    @Test
    fun `creates Tezos address from plain public key`() {
        val publicKey = "452ef5736b7973c94427561900a17822a4948c1d8ed3aafd5007fa9656fd8f39"
        val address = wallet.address(publicKey).getOrThrow()

        assertEquals("tz1MMDnKwxp6Qp54zFxxZKFnrRX6h46XbTwr", address)
    }

    @Test
    fun `creates Tezos address from encrypted public key`() {
        val publicKeysWithExpected = listOf(
            "edpkuAh8moaRkqGnVJUuwJywGcTEcuDx72o4K6j6zvYszmxNBC4V3D" to "tz1MMDnKwxp6Qp54zFxxZKFnrRX6h46XbTwr",
            "sppk7ZpH5qAjTDZn1o1TW7z2QbQZUcMHRn2wtV4rRfz15eLQrvPkt6k" to "tz2R3oTJR3cLfSyJVQiv8NGN4wXTQj58UYjp",
            "p2pk67fo5oy6byruqDtzVixbM7L3cVBDRMcFhA33XD5w2HF4fRXDJhw" to "tz3duiskLgZdaEvkgEwWYF4mUnVXde7JTtef",
        )

        publicKeysWithExpected.forEach {
            val publicKey = it.first
            val expected = it.second

            val actual = wallet.address(publicKey).getOrThrow()

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `fails to create Tezos address from invalid public key`() {
        val publicKey = "452ef5736b7973c94427561900a178"
        val address = wallet.address(publicKey)

        assertTrue(address.isFailure, "Expected address result to be a failure.")
    }
}