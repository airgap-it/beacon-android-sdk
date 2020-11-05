package it.airgap.beaconsdk.internal.transport.p2p.utils

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.internal.crypto.Crypto
import it.airgap.beaconsdk.internal.utils.HexString
import it.airgap.beaconsdk.internal.utils.Success
import it.airgap.beaconsdk.internal.utils.asHexString
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class P2pServerUtilsTest {

    @MockK
    private lateinit var crypto: Crypto

    private lateinit var p2pServerUtils: P2pServerUtils

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        every { crypto.hashKey(any<HexString>()) } answers { Success(firstArg<HexString>().asByteArray()) }
        every { crypto.hash(any<String>(), any()) } answers { Success(firstArg<String>().encodeToByteArray()) }
    }

    @Test
    fun `finds relay server for specified ByteArray public key`() {
        val publicKey = 10.asHexString().asByteArray()
        val matrixNodes: List<String> = listOf(
            5.asHexString().decodeToString(),
            9.asHexString().decodeToString(),
            13.asHexString().decodeToString(),
        )
        p2pServerUtils = P2pServerUtils(crypto, matrixNodes)

        val relayServer = p2pServerUtils.getRelayServer(publicKey)

        assertEquals(9.asHexString().decodeToString(), relayServer)
    }

    @Test
    fun `finds relay server for specified HexString public key`() {
        val publicKey = 10.asHexString()
        val matrixNodes: List<String> = listOf(
            5.asHexString().decodeToString(),
            9.asHexString().decodeToString(),
            13.asHexString().decodeToString(),
        )
        p2pServerUtils = P2pServerUtils(crypto, matrixNodes)

        val relayServer = p2pServerUtils.getRelayServer(publicKey)

        assertEquals(9.asHexString().decodeToString(), relayServer)
    }

    @Test
    fun `returns empty relay server if matrix nodes are empty`() {
        val publicKey = byteArrayOf(0)
        p2pServerUtils = P2pServerUtils(crypto, emptyList())

        val relayServer = p2pServerUtils.getRelayServer(publicKey)

        assertTrue(relayServer.isBlank(), "Expected relay server to be blank")
    }
}