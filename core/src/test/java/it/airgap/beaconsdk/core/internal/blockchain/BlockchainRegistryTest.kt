package it.airgap.beaconsdk.core.internal.blockchain

import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.core.blockchain.Blockchain
import it.airgap.beaconsdk.core.internal.di.DependencyRegistry
import org.junit.Before
import org.junit.Test
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class BlockchainRegistryTest {

    @MockK
    private lateinit var dependencyRegistry: DependencyRegistry

    private lateinit var blockchainRegistry: BlockchainRegistry

    private val blockchains: List<Blockchain.Factory<*>> = listOf(MockBlockchain.Factory())
    private val blockchainFactories: Map<String, () -> Blockchain>
        get() = blockchains
            .map { it.identifier to { it.create(dependencyRegistry) } }
            .toMap()

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        blockchainRegistry = BlockchainRegistry(blockchainFactories)
    }

    @Test
    fun `returns specified blockchain`() {
        identifiersWithClasses
            .forEach {
                val blockchain = blockchainRegistry.getOrNull(it.first)

                assertTrue(
                    it.second.isInstance(blockchain),
                    "Expected blockchain of type ${it.first} to be an instance of ${it.second}"
                )
            }
    }

    @Test
    fun `returns null if blockchain is not registered`() {
        val nullBlockchain = blockchainRegistry.getOrNull("unregistered")

        assertNull(nullBlockchain, "Expected unregistered blockchain to be null")
    }

    @Test
    fun `creates only one instance of each blockchain`() {
        identifiersWithClasses
            .forEach {
                val blockchain1 = blockchainRegistry.getOrNull(it.first)
                val blockchain2 = blockchainRegistry.getOrNull(it.first)

                assertEquals(blockchain1, blockchain2)
            }
    }

    private val identifiersWithClasses: List<Pair<String, KClass<out Blockchain>>> = listOf(
        MockBlockchain.IDENTIFIER to MockBlockchain::class,
    )
}