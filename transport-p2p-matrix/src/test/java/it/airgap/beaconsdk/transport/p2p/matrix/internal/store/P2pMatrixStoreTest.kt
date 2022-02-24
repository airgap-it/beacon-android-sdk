package it.airgap.beaconsdk.transport.p2p.matrix.internal.store

import io.mockk.*
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.core.data.P2pPeer
import it.airgap.beaconsdk.core.internal.BeaconConfiguration
import it.airgap.beaconsdk.core.internal.crypto.data.KeyPair
import it.airgap.beaconsdk.core.internal.data.BeaconApplication
import it.airgap.beaconsdk.core.internal.migration.Migration
import it.airgap.beaconsdk.core.internal.storage.MockSecureStorage
import it.airgap.beaconsdk.core.internal.storage.MockStorage
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.transport.p2p.data.P2pIdentifier
import it.airgap.beaconsdk.core.internal.utils.IdentifierCreator
import it.airgap.beaconsdk.core.internal.utils.asHexString
import it.airgap.beaconsdk.core.internal.utils.success
import it.airgap.beaconsdk.transport.p2p.matrix.internal.P2pMatrixCommunicator
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.MatrixClient
import it.airgap.beaconsdk.transport.p2p.matrix.internal.migration.migrateMatrixRelayServer
import it.airgap.beaconsdk.transport.p2p.matrix.internal.storage.*
import kotlinx.coroutines.test.runBlockingTest
import mockLog
import org.junit.After
import org.junit.Before
import org.junit.Test
import p2pPeers
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class P2pMatrixStoreTest {

    @MockK
    private lateinit var identifierCreator: IdentifierCreator

    @MockK
    private lateinit var p2pProtocol: P2pMatrixCommunicator

    @MockK
    private lateinit var matrixClient: MatrixClient

    @MockK
    private lateinit var migration: Migration

    private lateinit var storageManager: StorageManager
    private lateinit var p2pStore: P2pMatrixStore

    private val app: BeaconApplication = BeaconApplication(
        KeyPair(byteArrayOf(0), byteArrayOf(0)),
        "mockApp",
        "mockAppIcon",
        "mockAppUrl",
    )

    private val matrixNodes: List<String> = listOf("node1", "node2")

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        mockLog()

        coEvery { matrixClient.isUp(any()) } returns true
        coEvery { migration.migrate(any()) } returns Result.success()
        every { p2pProtocol.recipientIdentifier(any(), any()) } answers {
            Result.success(P2pIdentifier(firstArg(), secondArg()))
        }

        storageManager = spyk(StorageManager(MockStorage(), MockSecureStorage(), identifierCreator, BeaconConfiguration(ignoreUnsupportedBlockchains = false)).apply { addPlugins(MockP2pMatrixStoragePlugin()) })
        p2pStore = P2pMatrixStore(app, p2pProtocol, matrixClient, matrixNodes, storageManager, migration)
    }

    @After
    fun cleanUp() {
        unmockkAll()
    }

    @Test
    fun `initializes state with cached relay server`() {
        runBlockingTest {
            val cachedNode = "cachedNode"

            val recipient = "recipient"
            val channelId = "channelId"

            storageManager.setMatrixRelayServer(cachedNode)

            p2pStore.intent(OnChannelCreated(recipient, channelId)).getOrThrow()

            assertEquals(
                P2pMatrixStoreState(
                    relayServer = cachedNode,
                    availableNodes = matrixNodes.size,
                    activeChannels = mapOf(
                        recipient to channelId,
                    ),
                ),
                p2pStore.state().getOrThrow(),
            )
            coVerify(exactly = 1) { migration.migrateMatrixRelayServer(matrixNodes) }
        }
    }

    @Test
    fun `initializes state with first active node`() {
        val upNode = "upNode"
        val downNode = "downNode"

        p2pStore = P2pMatrixStore(app, p2pProtocol, matrixClient, listOf(downNode, upNode), storageManager, migration)

        coEvery { matrixClient.isUp(upNode) } returns true
        coEvery { matrixClient.isUp(downNode) } returns false

        runBlockingTest {
            val recipient = "recipient"
            val channelId = "channelId"

            storageManager.setMatrixRelayServer(null)

            p2pStore.intent(OnChannelCreated(recipient, channelId)).getOrThrow()

            assertEquals(
                P2pMatrixStoreState(
                    relayServer = upNode,
                    availableNodes = matrixNodes.size,
                    activeChannels = mapOf(
                        recipient to channelId,
                    ),
                ),
                p2pStore.state().getOrThrow(),
            )
            assertEquals(upNode, storageManager.getMatrixRelayServer())
        }
    }

    @Test
    fun `fails if all nodes are down`() {
        coEvery { matrixClient.isUp(any()) } returns false

        runBlockingTest {
            val recipient = "recipient"
            val channelId = "channelId"

            storageManager.setMatrixRelayServer(null)

            assertTrue(p2pStore.intent(OnChannelCreated(recipient, channelId)).isFailure)
        }
    }

    @Test
    fun `updates state on new channel created`() {
        runBlockingTest {
            val cachedNode = "cachedNode"

            val recipient1 = "recipient1"
            val channelId1 = "channelId1"

            val recipient2 = "recipient2"
            val channelId2 = "channelId2"

            with(storageManager) {
                setMatrixRelayServer(cachedNode)
                setMatrixChannels(mapOf(recipient1 to channelId1))
            }

            p2pStore.intent(OnChannelCreated(recipient2, channelId2)).getOrThrow()

            val expectedActiveChannels = mapOf(
                recipient1 to channelId1,
                recipient2 to channelId2,
            )

            assertEquals(
                P2pMatrixStoreState(
                    relayServer = cachedNode,
                    availableNodes = matrixNodes.size,
                    activeChannels = expectedActiveChannels,
                ),
                p2pStore.state().getOrThrow(),
            )
            assertEquals(expectedActiveChannels, storageManager.getMatrixChannels())
        }
    }

    @Test
    fun `updates state on new channel event`() {
        runBlockingTest {
            val cachedNode = "cachedNode"

            val publicKey = "00"

            val node = "node"
            val p2pPeer = p2pPeers(1).first().copy(publicKey = publicKey, relayServer = node)

            val nodeNew = "nodeNew"
            val p2pPeerNew = p2pPeer.copy(relayServer = nodeNew)

            val recipient = p2pPeerNew.identifier.asString()

            val channelId = "channelId"
            val channelIdNew = "channelIdNew"

            with(storageManager) {
                setPeers(listOf(p2pPeer))
                setMatrixRelayServer(cachedNode)
                setMatrixChannels(mapOf(recipient to channelId))
            }

            p2pStore.intent(OnChannelEvent(recipient, channelIdNew)).getOrThrow()

            val expectedActiveChannels = mapOf(
                recipient to channelIdNew,
            )

            val expectedInactiveChannels = setOf(channelId)

            assertEquals(
                P2pMatrixStoreState(
                    relayServer = cachedNode,
                    availableNodes = matrixNodes.size,
                    activeChannels = expectedActiveChannels,
                    inactiveChannels = expectedInactiveChannels,
                ),
                p2pStore.state().getOrThrow(),
            )
            assertEquals(expectedActiveChannels, storageManager.getMatrixChannels())
            assertEquals(
                listOf(p2pPeerNew),
                storageManager.getPeers(),
            )
        }
    }

    @Test
    fun `updates state on channel closed`() {
        runBlockingTest {
            val cachedNode = "cachedNode"

            val recipient1 = "recipient1"
            val channelId1 = "channelId1"

            val recipient2 = "recipient2"
            val channelId2 = "channelId2"

            with(storageManager) {
                setMatrixRelayServer(cachedNode)
                setMatrixChannels(mapOf(
                    recipient1 to channelId1,
                    recipient2 to channelId2,
                ))
            }

            p2pStore.intent(OnChannelClosed(channelId1)).getOrThrow()

            val expectedActiveChannels = mapOf(
                recipient2 to channelId2,
            )

            val expectedInactiveChannels = setOf(channelId1)

            assertEquals(
                P2pMatrixStoreState(
                    relayServer = cachedNode,
                    availableNodes = matrixNodes.size,
                    activeChannels = expectedActiveChannels,
                    inactiveChannels = expectedInactiveChannels,
                ),
                p2pStore.state().getOrThrow(),
            )
            assertEquals(expectedActiveChannels, storageManager.getMatrixChannels())
        }
    }

    @Test
    fun `resets state hard`() {
        runBlockingTest {
            val cachedNode = "cachedNode"
            with(storageManager) {
                setMatrixRelayServer(cachedNode)
            }

            p2pStore.intent(HardReset).getOrThrow()

            assertNull(storageManager.getMatrixRelayServer())
            assertEquals(emptyMap(), storageManager.getMatrixChannels())

            assertEquals(
                P2pMatrixStoreState(
                    relayServer = matrixNodes.first(),
                    availableNodes = matrixNodes.size,
                ),
                p2pStore.state().getOrThrow(),
            )
        }
    }

    private val P2pPeer.identifier: P2pIdentifier
        get() = P2pIdentifier(publicKey.asHexString().toByteArray(), relayServer)
}