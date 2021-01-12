package it.airgap.beaconsdk.internal.transport.p2p.matrix.store

import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.internal.storage.MockSecureStorage
import it.airgap.beaconsdk.internal.storage.MockStorage
import it.airgap.beaconsdk.internal.storage.StorageManager
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.MatrixEvent
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.MatrixRoom
import it.airgap.beaconsdk.internal.utils.AccountUtils
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

internal class MatrixStoreTest {

    @MockK
    private lateinit var accountUtils: AccountUtils

    private lateinit var storageManager: StorageManager
    private lateinit var matrixStore: MatrixStore

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        storageManager = StorageManager(MockStorage(), MockSecureStorage(), accountUtils)
        matrixStore = MatrixStore(storageManager)
    }

    @Test
    fun `initializes state at first run`() {
        runBlockingTest {
            val userId = "userId"
            val deviceId = "deviceId"
            val accessToken = "accessToken"

            matrixStore.intent(Init(userId, deviceId, accessToken))

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
                matrixStore.state(),
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

            matrixStore.intent(Init(userId, deviceId, accessToken))

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
                matrixStore.state(),
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

            matrixStore.intent(OnSyncSuccess(syncToken, pollingTimeout, rooms, emptyList()))

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
                matrixStore.state()
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

            matrixStore.intent(OnSyncSuccess(syncToken, pollingTimeout, rooms, emptyList()))

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
                matrixStore.state()
            )
        }
    }

    @Test
    fun `emits new events on successful sync`() {
        runBlocking {
            val roomId = "1"
            val events = listOf(
                MatrixEvent.TextMessage(roomId, "sender#1", "message"),
                MatrixEvent.Join(roomId, "sender#2"),
                MatrixEvent.Create(roomId, "sender#3"),
            )

            val emitted = matrixStore.events
                .onSubscription {
                    matrixStore.intent(OnSyncSuccess(
                        syncToken = null,
                        pollingTimeout = 0,
                        rooms = emptyList(),
                        events = events,
                    ))
                }
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
            val currentRetries = matrixStore.state().pollingRetries
            matrixStore.intent(OnSyncError)

            assertEquals(
                MatrixStoreState(
                    isPolling = false,
                    pollingRetries = currentRetries + 1,
                ),
                matrixStore.state(),
            )
        }
    }

    @Test
    fun `increases transaction counter on new txn id created`() {
        runBlockingTest {
            val currentCounter = matrixStore.state().transactionCounter
            matrixStore.intent(OnTxnIdCreated)

            assertEquals(
                MatrixStoreState(transactionCounter = currentCounter + 1),
                matrixStore.state(),
            )
        }
    }
}