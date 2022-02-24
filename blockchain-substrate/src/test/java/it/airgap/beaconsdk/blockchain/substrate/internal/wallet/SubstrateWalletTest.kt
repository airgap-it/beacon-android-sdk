package it.airgap.beaconsdk.blockchain.substrate.internal.wallet

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.core.internal.crypto.Crypto
import it.airgap.beaconsdk.core.internal.utils.Base58
import org.bouncycastle.crypto.digests.Blake2bDigest
import org.junit.Before
import org.junit.Test
import java.lang.Integer.min
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class SubstrateWalletTest {

    @MockK
    private lateinit var crypto: Crypto

    private lateinit var wallet: SubstrateWallet

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        every { crypto.hash(any<ByteArray>(), any()) } answers {
            val message = firstArg<ByteArray>()
            val size = secondArg<Int>()
            val blake2bDigest = Blake2bDigest(size * 8)

            ByteArray(blake2bDigest.digestSize).let {
                blake2bDigest.update(message, 0, message.size)
                blake2bDigest.doFinal(it, 0)

                Result.success(it.sliceArray(0 until size))
            }
        }

        wallet = SubstrateWallet(crypto, Base58())
    }

    @Test
    fun `creates Substrate address from public key`() {
        val publicKey = "a4cbbdb7a1d9703e8c0946f337c1779795d5ed8ae86045a70457bc7d7df30566"
        val address = wallet.address(publicKey, 42).getOrThrow()

        assertEquals("5FnnEXR2jDoNpRZmquLnRNL2E7H4VsGrXPxgpVoYYBsY43po", address)
    }

    @Test
    fun `fails to create Substrate address from invalid public key`() {
        val publicKey = "a4cbbdb7a1d9703e8c0946f337c177"
        val address = wallet.address(publicKey, 42)

        assertTrue(address.isFailure, "Expected address result to be a failure.")
    }
}