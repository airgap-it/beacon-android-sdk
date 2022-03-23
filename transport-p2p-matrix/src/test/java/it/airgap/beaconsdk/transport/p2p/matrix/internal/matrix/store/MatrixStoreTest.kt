package it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.store

import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import io.mockk.spyk
import it.airgap.beaconsdk.core.internal.BeaconConfiguration
import it.airgap.beaconsdk.core.internal.storage.MockSecureStorage
import it.airgap.beaconsdk.core.internal.storage.MockStorage
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.utils.IdentifierCreator
import it.airgap.beaconsdk.transport.p2p.matrix.data.MatrixRoom
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.MatrixEvent
import it.airgap.beaconsdk.transport.p2p.matrix.internal.storage.MockP2pMatrixStoragePlugin
import it.airgap.beaconsdk.transport.p2p.matrix.internal.storage.getMatrixRooms
import it.airgap.beaconsdk.transport.p2p.matrix.internal.storage.setMatrixRooms
import it.airgap.beaconsdk.transport.p2p.matrix.internal.storage.setMatrixSyncToken
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class MatrixStoreTest {

    @MockK
    private lateinit var identifierCreator: IdentifierCreator

    private lateinit var storageManager: StorageManager
    private lateinit var matrixStore: MatrixStore

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        storageManager = spyk(StorageManager(MockStorage(), MockSecureStorage(), identifierCreator, BeaconConfiguration(ignoreUnsupportedBlockchains = false)).apply { addPlugins(MockP2pMatrixStoragePlugin()) })
        matrixStore = MatrixStore(storageManager)
    }

    @Test
    fun `initializes state at first run`() {
        runBlockingTest {
            val userId = "userId"
            val deviceId = "deviceId"
            val accessToken = "accessToken"

            matrixStore.intent(Init(userId, deviceId, accessToken)).getOrThrow()

            assertEquals(
                MatrixStoreState(
                    isPolling = false,
                    userId = userId,
                    deviceId = deviceId,
                    transactionCounter = 0,
                    accessToken = accessToken,
                    syncToken = null,
                    pollingTimeout = null,
                    pollingRetries = 0,
                    rooms = emptyMap(),
                ),
                matrixStore.state().getOrThrow(),
            )
        }
    }

    @Test
    fun `initializes state on every next run`() {
        runBlockingTest {
            val syncToken = "syncToken"
            val rooms = listOf(
                MatrixRoom.Joined("1", listOf("member#1", "member#2")),
                MatrixRoom.Invited("2", listOf("member#3", "member#4")),
                MatrixRoom.Left("3", listOf("member#4", "member#5")),
            )

            storageManager.setMatrixSyncToken(syncToken)
            storageManager.setMatrixRooms(rooms)

            val userId = "userId"
            val deviceId = "deviceId"
            val accessToken = "accessToken"

            matrixStore.intent(Init(userId, deviceId, accessToken)).getOrThrow()

            assertEquals(
                MatrixStoreState(
                    isPolling = false,
                    userId = userId,
                    deviceId = deviceId,
                    transactionCounter = 0,
                    accessToken = accessToken,
                    syncToken = syncToken,
                    pollingTimeout = null,
                    pollingRetries = 0,
                    rooms = mapOf(
                        "1" to MatrixRoom.Joined("1", listOf("member#1", "member#2")),
                        "2" to MatrixRoom.Invited("2", listOf("member#3", "member#4")),
                        "3" to MatrixRoom.Left("3", listOf("member#4", "member#5")),
                    )
                ),
                matrixStore.state().getOrThrow(),
            )
        }
    }

    @Test
    fun `sets new sync data on successful sync`() {
        runBlockingTest {
            val syncToken = "syncToken"
            val pollingTimeout = 1000L
            val rooms = listOf(
                MatrixRoom.Joined("1", listOf("member#1")),
                MatrixRoom.Invited("2", emptyList()),
                MatrixRoom.Left("3", emptyList()),
            )

            matrixStore.intent(OnSyncSuccess(syncToken, pollingTimeout, rooms, emptyList())).getOrThrow()

            assertEquals(
                MatrixStoreState(
                    isPolling = true,
                    syncToken = syncToken,
                    pollingTimeout = pollingTimeout,
                    pollingRetries = 0,
                    rooms = mapOf(
                        "1" to MatrixRoom.Joined("1", listOf("member#1")),
                        "2" to MatrixRoom.Invited("2", emptyList()),
                        "3" to MatrixRoom.Left("3", emptyList()),
                    )
                ),
                matrixStore.state().getOrThrow()
            )
        }
    }

    @Test
    fun `merges new data on successful sync`() {
        runBlockingTest {
            val storageRooms = listOf(
                MatrixRoom.Joined("1", listOf("member#1", "member#2")),
                MatrixRoom.Invited("2", listOf("member#3", "member#4")),
                MatrixRoom.Left("3", listOf("member#4", "member#5")),
            )
            storageManager.setMatrixSyncToken("storageSyncToken")
            storageManager.setMatrixRooms(storageRooms)

            val userId = "userId"
            val deviceId = "deviceId"
            val accessToken = "accessToken"

            matrixStore.intent(Init(userId, deviceId, accessToken))

            val syncToken = "syncToken"
            val pollingTimeout = 1000L
            val rooms = listOf(
                MatrixRoom.Joined("4", listOf("member#6")),
            )

            matrixStore.intent(OnSyncSuccess(syncToken, pollingTimeout, rooms, emptyList())).getOrThrow()

            assertEquals(
                MatrixStoreState(
                    isPolling = true,
                    userId = userId,
                    deviceId = deviceId,
                    accessToken = accessToken,
                    syncToken = syncToken,
                    pollingTimeout = pollingTimeout,
                    pollingRetries = 0,
                    rooms = mapOf(
                        "1" to MatrixRoom.Joined("1", listOf("member#1", "member#2")),
                        "2" to MatrixRoom.Invited("2", listOf("member#3", "member#4")),
                        "3" to MatrixRoom.Left("3", listOf("member#4", "member#5")),
                        "4" to MatrixRoom.Joined("4", listOf("member#6")),
                    )
                ),
                matrixStore.state().getOrThrow()
            )
        }
    }

    @Test
    fun `emits new events on successful sync`() {
        runBlocking {
            val node = "node"
            val roomId = "1"
            val events = listOf(
                MatrixEvent.TextMessage(node, roomId, "sender#1", "message"),
                MatrixEvent.Join(node, roomId, "sender#2"),
                MatrixEvent.Create(node, roomId, "sender#3"),
            )

            matrixStore.intent(OnSyncSuccess(
                syncToken = null,
                pollingTimeout = 0,
                rooms = emptyList(),
                events = events,
            )).getOrThrow()

            val emitted = matrixStore.events
                .take(events.size)
                .toList()

            assertEquals(
                events.sortedBy { it.toString() },
                emitted.sortedBy { it.toString() },
            )
        }
    }

    @Test
    fun `increases polling retries counter and sets polling state on error`() {
        runBlockingTest {
            val currentRetries = matrixStore.state().getOrThrow().pollingRetries
            matrixStore.intent(OnSyncError).getOrThrow()

            assertEquals(
                MatrixStoreState(
                    isPolling = false,
                    pollingRetries = currentRetries + 1,
                ),
                matrixStore.state().getOrThrow(),
            )
        }
    }

    @Test
    fun `increases transaction counter on new txn id created`() {
        runBlockingTest {
            val currentCounter = matrixStore.state().getOrThrow().transactionCounter
            matrixStore.intent(OnTxnIdCreated).getOrThrow()

            assertEquals(
                MatrixStoreState(transactionCounter = currentCounter + 1),
                matrixStore.state().getOrThrow(),
            )
        }
    }

    @Test
    fun `resets state`() {
        runBlockingTest {
            val userId = "userId"
            val deviceId = "deviceId"
            val accessToken = "accessToken"

            val syncToken = "syncToken"
            val pollingTimeout = 1000L
            val rooms = listOf(
                MatrixRoom.Joined("1", listOf("member#1")),
                MatrixRoom.Invited("2", emptyList()),
                MatrixRoom.Left("3", emptyList()),
            )

            matrixStore.intent(Init(userId, deviceId, accessToken)).getOrThrow()
            matrixStore.intent(OnSyncSuccess(syncToken, pollingTimeout, rooms, emptyList())).getOrThrow()
            matrixStore.intent(Reset).getOrThrow()

            assertEquals(
                MatrixStoreState(syncToken = syncToken),
                matrixStore.state().getOrThrow()
            )
        }
    }

    @Test
    fun `resets state hard`() {
        runBlockingTest {
            val userId = "userId"
            val deviceId = "deviceId"
            val accessToken = "accessToken"

            val syncToken = "syncToken"
            val pollingTimeout = 1000L
            val rooms = listOf(
                MatrixRoom.Joined("1", listOf("member#1")),
                MatrixRoom.Invited("2", emptyList()),
                MatrixRoom.Left("3", emptyList()),
            )

            matrixStore.intent(Init(userId, deviceId, accessToken)).getOrThrow()
            matrixStore.intent(OnSyncSuccess(syncToken, pollingTimeout, rooms, emptyList())).getOrThrow()
            matrixStore.intent(HardReset).getOrThrow()

            assertEquals(
                MatrixStoreState(syncToken = syncToken),
                matrixStore.state().getOrThrow()
            )

            assertTrue(storageManager.getMatrixRooms().isEmpty(), "Expected Matrix rooms to be empty.")
        }
    }
}