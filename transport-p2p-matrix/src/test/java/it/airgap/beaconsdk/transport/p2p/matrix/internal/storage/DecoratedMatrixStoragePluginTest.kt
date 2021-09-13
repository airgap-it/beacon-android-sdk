package it.airgap.beaconsdk.transport.p2p.matrix.internal.storage

import it.airgap.beaconsdk.transport.p2p.matrix.data.MatrixRoom
import it.airgap.beaconsdk.transport.p2p.matrix.internal.storage.decorator.DecoratedP2pMatrixStoragePlugin
import it.airgap.beaconsdk.transport.p2p.matrix.storage.P2pMatrixStoragePlugin
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class DecoratedMatrixStoragePluginTest {
    private lateinit var plugin: P2pMatrixStoragePlugin
    private lateinit var decoratedPlugin: DecoratedP2pMatrixStoragePlugin

    @Before
    fun setup() {
        plugin = MockP2pMatrixStoragePlugin()
        decoratedPlugin = DecoratedP2pMatrixStoragePlugin(plugin)
    }

    @Test
    fun `removes Matrix relay server`() {
        runBlocking { plugin.setMatrixRelayServer("relayServer") }
        runBlocking { decoratedPlugin.removeMatrixRelayServer() }

        val fromStorage = runBlocking { plugin.getMatrixRelayServer() }

        assertNull(fromStorage)
    }

    @Test
    fun `removes Matrix channels`() {
        runBlocking { plugin.setMatrixChannels(mapOf("sender" to "channel")) }
        runBlocking { decoratedPlugin.removeMatrixChannels() }

        val fromStorage = runBlocking { plugin.getMatrixChannels() }

        assertEquals(emptyMap(), fromStorage)
    }

    @Test
    fun `removes Matrix sync token`() {
        runBlocking { plugin.setMatrixSyncToken("syncToken") }
        runBlocking { decoratedPlugin.removeMatrixSyncToken() }

        val fromStorage = runBlocking { plugin.getMatrixSyncToken() }

        assertNull(fromStorage)
    }

    @Test
    fun `removes Matrix rooms`() {
        runBlocking {
            plugin.setMatrixRooms(
                listOf(MatrixRoom.Unknown("id", emptyList())),
            )
        }
        runBlocking { decoratedPlugin.removeMatrixRooms() }

        val fromStorage = runBlocking { plugin.getMatrixRooms() }

        assertEquals(emptyList(), fromStorage)
    }
}