package it.airgap.beaconsdk.internal.protocol

import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.internal.crypto.Crypto
import it.airgap.beaconsdk.internal.utils.Base58Check
import org.junit.Before
import org.junit.Test
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class ProtocolRegistryTest {

    @MockK
    private lateinit var crypto: Crypto

    @MockK
    private lateinit var base58Check: Base58Check

    private lateinit var protocolRegistry: ProtocolRegistry

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        protocolRegistry = ProtocolRegistry(crypto, base58Check)
    }

    @Test
    fun `returns specified protocol`() {
        typesWithClasses
            .forEach {
                val protocol = protocolRegistry.get(it.first)

                assertTrue(
                    it.second.isInstance(protocol),
                    "Expected protocol of type ${it.first} to be an instance of ${it.second}"
                )
            }
    }

    @Test
    fun `creates only one instance of each protocol`() {
        typesWithClasses
            .forEach {
                val protocol1 = protocolRegistry.get(it.first)
                val protocol2 = protocolRegistry.get(it.first)

                assertEquals(protocol1, protocol2)
            }
    }

    private val typesWithClasses: List<Pair<Protocol.Type, KClass<out Protocol>>> = listOf(
        Protocol.Type.Tezos to Tezos::class,
    )
}