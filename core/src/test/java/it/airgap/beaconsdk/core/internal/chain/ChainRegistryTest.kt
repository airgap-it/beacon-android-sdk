package it.airgap.beaconsdk.core.internal.chain

import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.core.internal.di.DependencyRegistry
import org.junit.Before
import org.junit.Test
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class ChainRegistryTest {

    @MockK
    private lateinit var dependencyRegistry: DependencyRegistry

    private lateinit var chainRegistry: ChainRegistry

    private val chains: List<Chain.Factory<*>> = listOf(MockChain.Factory())
    private val chainFactories: Map<String, () -> Chain<*, *>>
        get() = chains
            .map { it.identifier to { it.create(dependencyRegistry) } }
            .toMap()

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        chainRegistry = ChainRegistry(chainFactories)
    }

    @Test
    fun `returns specified chain`() {
        identifiersWithClasses
            .forEach {
                val chain = chainRegistry.get(it.first)

                assertTrue(
                    it.second.isInstance(chain),
                    "Expected chain of type ${it.first} to be an instance of ${it.second}"
                )
            }
    }

    @Test
    fun `returns null if chain is not registered`() {
        val nullChain = chainRegistry.get("unregistered")

        assertNull(nullChain, "Expected unregistered chain to be null")
    }

    @Test
    fun `creates only one instance of each chain`() {
        identifiersWithClasses
            .forEach {
                val chain1 = chainRegistry.get(it.first)
                val chain2 = chainRegistry.get(it.first)

                assertEquals(chain1, chain2)
            }
    }

    private val identifiersWithClasses: List<Pair<String, KClass<out Chain<*, *>>>> = listOf(
        MockChain.IDENTIFIER to MockChain::class,
    )
}