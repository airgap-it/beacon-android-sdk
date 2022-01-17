package it.airgap.beaconsdk.blockchain.substrate

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.blockchain.substrate.internal.di.SubstrateDependencyRegistry
import it.airgap.beaconsdk.core.internal.di.DependencyRegistry
import it.airgap.beaconsdk.core.internal.di.findExtended
import org.junit.Before
import org.junit.Test
import kotlin.reflect.KClass

internal class SubstrateFactoryTest {
    @MockK(relaxed = true)
    private lateinit var dependencyRegistry: DependencyRegistry

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        every { dependencyRegistry.findExtended(any<KClass<SubstrateDependencyRegistry>>()) } returns null
    }

    @Test
    fun `creates Substrate instance`() {
        val factory = Substrate.Factory()
        val substrate = factory.create(dependencyRegistry)
    }

    @Test
    fun `creates Substrate instance as builder function`() {
        val factory = substrate()
        val substrate = factory.create(dependencyRegistry)
    }
}