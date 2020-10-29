package it.airgap.beaconsdk.internal.crypto

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.internal.crypto.provider.CryptoProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

internal class CryptoTest {
    @MockK
    private lateinit var cryptoProvider: CryptoProvider

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        every { cryptoProvider.generateRandomBytes(any()) } answers { (0 until firstArg()).map(Int::toByte).toByteArray() }
    }

    @Test
    fun `creates random seed as UUID`() {
        val crypto = Crypto(cryptoProvider)
        val seed = crypto.generateRandomSeed()

        assertEquals("00010203-0405-0607-0809-0a0b0c0d0e0f", seed.value())
    }

    // Crypto#getKeyPairFromSeed correctness heavily depends on a provider
    // The method should be tested against each provider separately

    // LazySodiumCryptoProvider can't be tested here because of its native dependencies (JNA)
}