package it.airgap.beaconsdk.chain.tezos.internal

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.chain.tezos.Tezos
import it.airgap.beaconsdk.chain.tezos.tezos
import it.airgap.beaconsdk.core.internal.crypto.Crypto
import it.airgap.beaconsdk.core.internal.di.DependencyRegistry
import it.airgap.beaconsdk.core.internal.utils.Base58Check
import org.junit.Before
import org.junit.Test

internal class TezosTest {
    @MockK
    private lateinit var dependencyRegistry: DependencyRegistry

    @MockK
    private lateinit var crypto: Crypto

    @MockK
    private lateinit var base58Check: Base58Check

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        every { dependencyRegistry.crypto } returns crypto
        every { dependencyRegistry.base58Check } returns base58Check
    }

    @Test
    fun `creates Tezos instance`() {
        val factory = Tezos.Factory()
        val tezos = factory.create(dependencyRegistry)
    }

    @Test
    fun `creates Tezos instance with builder function`() {
        val tezos = tezos()
    }
}