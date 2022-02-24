package it.airgap.beaconsdk.core.internal.storage.decorator

import appMetadata
import it.airgap.beaconsdk.core.data.*
import it.airgap.beaconsdk.core.internal.BeaconConfiguration
import it.airgap.beaconsdk.core.internal.storage.MockStorage
import it.airgap.beaconsdk.core.internal.utils.splitAt
import it.airgap.beaconsdk.core.storage.Storage
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import p2pPeers
import permissions
import takeHalf
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class DecoratedStorageTest {

    private lateinit var storage: Storage
    private lateinit var decoratedStorage: DecoratedStorage

    @Before
    fun setup() {
        storage = MockStorage()
        decoratedStorage = DecoratedStorage(storage, BeaconConfiguration(ignoreUnsupportedBlockchains = false))
    }

    @Test
    fun `finds app metadata based on predicate`() {
        val appMetadata = appMetadata(3)
        val expected = appMetadata.shuffled().first().copy()

        runBlocking { storage.setAppMetadata(appMetadata) }

        val found = runBlocking { decoratedStorage.findAppMetadata { it.name == expected.name } }

        assertEquals(expected, found)
    }

    @Test
    fun `finds permissions based on predicate`() {
        val permissions = permissions(3).filterIsInstance<MockPermission>()
        val expected = permissions.shuffled().first().copy()

        runBlocking { storage.setPermissions(permissions) }

        val found = runBlocking { decoratedStorage.findPermission { it.accountId == expected.accountId } }

        assertEquals(expected, found)
    }

    @Test
    fun `finds P2P peers based on predicate`() {
        val peers = p2pPeers(3)
        val expected = peers.shuffled().first().copy()

        runBlocking { storage.setPeers(peers) }

        val found = runBlocking { decoratedStorage.findPeer { it.name == expected.name } }

        assertEquals(expected, found)
    }

    @Test
    fun `adds new app metadata and overwrites if already exists based on predicate`() {
        val appMetadata = appMetadata(8)
        val expected = appMetadata.toMutableList()

        val (toStorage, toAdd) = appMetadata.splitAt { it.size / 2 }

        val changed = toStorage
            .mapIndexed { index, accountInfo -> index to accountInfo }
            .shuffled()
            .takeHalf()
            .map { it.first to it.second.copy(senderId = "${it.second.senderId}${it.first}") }
            .onEach { expected[it.first] = it.second }
            .map { it.second }

        runBlocking { storage.setAppMetadata(toStorage) }
        runBlocking {
            decoratedStorage.addAppMetadata(
                changed + toAdd,
                overwrite = true
            ) { a, b -> a.name == b.name }
        }

        val fromStorage = runBlocking { storage.getAppMetadata() }

        assertEquals(expected, fromStorage)
    }

    @Test
    fun `adds new permissions and overwrites if already exists based on predicate`() {
        val permissions = permissions(8).filterIsInstance<MockPermission>()
        val expected = permissions.toMutableList()

        val (toStorage, toAdd) = permissions.splitAt { it.size / 2 }

        val changed = toStorage
            .mapIndexed { index, accountInfo -> index to accountInfo }
            .shuffled()
            .takeHalf()
            .map { it.first to it.second.copy(senderId = "${it.second.senderId}${it.first}") }
            .onEach { expected[it.first] = it.second }
            .map { it.second }

        runBlocking { storage.setPermissions(toStorage) }
        runBlocking {
            decoratedStorage.addPermissions(
                changed + toAdd,
                overwrite = true
            ) { a, b -> a.accountId == b.accountId }
        }

        val fromStorage = runBlocking { storage.getPermissions() }

        assertEquals(expected, fromStorage)
    }

    @Test
    fun `adds new P2P requests and overwrites if already exists based on predicate`() {
        val peers = p2pPeers(8)
        val expected = peers.toMutableList()

        val (toStorage, toAdd) = peers.splitAt { it.size / 2 }

        val changed = toStorage
            .mapIndexed { index, accountInfo -> index to accountInfo }
            .shuffled()
            .takeHalf()
            .map { it.first to it.second.copy(publicKey = "${it.second.publicKey}${it.first}") }
            .onEach { expected[it.first] = it.second }
            .map { it.second }

        runBlocking { storage.setPeers(toStorage) }
        runBlocking {
            decoratedStorage.addPeers(
                changed + toAdd,
                overwrite = true
            ) { a, b -> a.name == b.name }
        }

        val fromStorage = runBlocking { storage.getPeers() }

        assertEquals(expected, fromStorage)
    }

    @Test
    fun `adds new app metadata and does not overwrite if exists base on predicate`() {
        val appMetadata = appMetadata(8)

        val (toStorage, toAdd) = appMetadata.splitAt { it.size / 2 }
        val changed = toStorage
            .shuffled()
            .takeHalf()
            .mapIndexed { index, appMetadata -> appMetadata.copy(senderId = "${appMetadata.senderId}${index}") }

        runBlocking { storage.setAppMetadata(toStorage) }
        runBlocking {
            decoratedStorage.addAppMetadata(changed + toAdd, overwrite = false) { a, b ->
                a.name == b.name
            }
        }

        val fromStorage = runBlocking { storage.getAppMetadata() }

        assertEquals(toStorage + toAdd, fromStorage)
    }

    @Test
    fun `adds new permissions and does not overwrite if exists base on predicate`() {
        val permissions = permissions(8).filterIsInstance<MockPermission>()

        val (toStorage, toAdd) = permissions.splitAt { it.size / 2 }
        val changed = toStorage
            .shuffled()
            .takeHalf()
            .mapIndexed { index, permission -> permission.copy(senderId = "${permission.senderId}${index}") }

        runBlocking { storage.setPermissions(toStorage) }
        runBlocking {
            decoratedStorage.addPermissions(changed + toAdd, overwrite = false) { a, b ->
                a.accountId == b.accountId
            }
        }

        val fromStorage = runBlocking { storage.getPermissions() }

        assertEquals(toStorage + toAdd, fromStorage)
    }

    @Test
    fun `adds new P2P requests and does not overwrite if exists base on predicate`() {
        val peers = p2pPeers(8)

        val (toStorage, toAdd) = peers.splitAt { it.size / 2 }
        val changed = toStorage
            .shuffled()
            .takeHalf()
            .mapIndexed { index, peer -> peer.copy(publicKey = "${peer.publicKey}${index}") }

        runBlocking { storage.setPeers(toStorage) }
        runBlocking {
            decoratedStorage.addPeers(changed + toAdd, overwrite = false) { a, b ->
                a.name == b.name
            }
        }

        val fromStorage = runBlocking { storage.getPeers() }

        assertEquals(toStorage + toAdd, fromStorage)
    }

    @Test
    fun `removes app metadata based on predicate`() {
        val appMetadata = appMetadata(4)
        val (toKeep, toRemove) = appMetadata.splitAt { it.size / 2 }
        val namesToRemove = toRemove.map { it.name }

        runBlocking { storage.setAppMetadata(appMetadata) }
        runBlocking { decoratedStorage.removeAppMetadata { namesToRemove.contains(it.name) } }
        val fromStorage = runBlocking { storage.getAppMetadata() }

        assertEquals(toKeep, fromStorage)
    }

    @Test
    fun `removes permissions based on predicate`() {
        val permissions = permissions(4)
        val (toKeep, toRemove) = permissions.splitAt { it.size / 2 }
        val accountIdentifiersToRemove = toRemove.map { it.accountId }

        runBlocking { storage.setPermissions(permissions) }
        runBlocking { decoratedStorage.removePermissions { accountIdentifiersToRemove.contains(it.accountId) } }
        val fromStorage = runBlocking { storage.getPermissions() }

        assertEquals(toKeep, fromStorage)
    }

    @Test
    fun `removes P2P peers based on predicate`() {
        val peers = p2pPeers(4)
        val (toKeep, toRemove) = peers.splitAt { it.size / 2 }
        val namesToRemove = toRemove.map { it.name }

        runBlocking { storage.setPeers(peers) }
        runBlocking { decoratedStorage.removePeers { namesToRemove.contains(it.name) } }
        val fromStorage = runBlocking { storage.getPeers() }

        assertEquals(toKeep, fromStorage)
    }

    @Test
    fun `removes all app metadata`() {
        val appMetadata = appMetadata(2)
        runBlocking { storage.setAppMetadata(appMetadata) }
        runBlocking { decoratedStorage.removeAppMetadata() }

        val fromStorage = runBlocking { storage.getAppMetadata() }

        assertTrue(fromStorage.isEmpty())
    }

    @Test
    fun `removes all permissions`() {
        val permissions = permissions(2)
        runBlocking { storage.setPermissions(permissions) }
        runBlocking { decoratedStorage.removePermissions() }

        val fromStorage = runBlocking { storage.getPermissions() }

        assertTrue(fromStorage.isEmpty())
    }

    @Test
    fun `removes all P2P peers`() {
        val peers = p2pPeers(2)
        runBlocking { storage.setPeers(peers) }
        runBlocking { decoratedStorage.removePeers() }

        val fromStorage = runBlocking { storage.getPeers() }

        assertTrue(fromStorage.isEmpty())
    }

    @Test
    fun `adds set of migrations if empty`() {
        val migrations = setOf("migration")

        runBlocking { storage.setMigrations(emptySet()) }
        runBlocking { decoratedStorage.addMigrations(migrations) }

        val fromStorage = runBlocking { storage.getMigrations() }

        assertEquals(migrations, fromStorage)
    }

    @Test
    fun `adds set of migrations if not empty`() {
        val migrations = setOf("migration1", "migration2")
        val newMigrations = setOf("newMigration1", "newMigration2")

        runBlocking { storage.setMigrations(migrations) }
        runBlocking { decoratedStorage.addMigrations(newMigrations) }

        val fromStorage = runBlocking { storage.getMigrations() }

        assertEquals(migrations + newMigrations, fromStorage)
    }

    @Test
    fun `notifies when new app metadata is added`() {
        val toAdd = appMetadata(2)

        runBlocking {
            val newAppMetadata = async {
                decoratedStorage.appMetadata
                    .take(toAdd.size)
                    .toList()
            }
            decoratedStorage.addAppMetadata(toAdd)

            assertEquals(toAdd.sortedBy { it.name }, newAppMetadata.await().sortedBy { it.name })
        }
    }

    @Test
    fun `notifies when new permissions are added`() {
        val toAdd = permissions(2)

        runBlocking {
            val newPermissions = async {
                decoratedStorage.permissions
                    .take(toAdd.size)
                    .toList()
            }
            decoratedStorage.addPermissions(toAdd)

            assertEquals(
                toAdd.sortedBy { it.accountId },
                newPermissions.await().sortedBy { it.accountId })
        }
    }

    @Test
    fun `notifies when new P2P peers are added`() {
        val toAdd = p2pPeers(2)

        runBlocking {
            val newPeers = async {
                decoratedStorage.peers
                    .take(toAdd.size)
                    .toList()
            }
            decoratedStorage.addPeers(toAdd)

            assertEquals(toAdd.sortedBy { it.name }, newPeers.await().sortedBy { it.name })
        }
    }

    @Test
    fun `notifies when new app metadata is updated`() {
        val toStorage = appMetadata(2)

        runBlocking {
            storage.setAppMetadata(toStorage)
            val subscribed = async {
                decoratedStorage.appMetadata
                    .drop(toStorage.size)
                    .take(toStorage.size)
                    .toList()
            }

            val updated = toStorage.mapIndexed { index, appMetadata ->
                appMetadata.copy(name = "${appMetadata.name}$index")
            }
            decoratedStorage.addAppMetadata(updated)

            assertEquals(
                updated.sortedBy { it.name },
                subscribed.await().sortedBy { it.name })
        }
    }

    @Test
    fun `notifies when permissions are updated`() {
        val toStorage = permissions(2).filterIsInstance<MockPermission>()

        runBlocking {
            storage.setPermissions(toStorage)
            val subscribed = async {
                decoratedStorage.permissions
                    .drop(toStorage.size)
                    .take(toStorage.size)
                    .toList()
            }

            val updated = toStorage.mapIndexed { index, permission ->
                permission.copy(accountId = "${permission.accountId}$index")
            }
            decoratedStorage.addPermissions(updated)

            assertEquals(
                updated.sortedBy { it.accountId },
                subscribed.await().sortedBy { it.accountId })
        }
    }

    @Test
    fun `notifies when P2P peers are updated`() {
        val toStorage = p2pPeers(2)

        runBlocking {
            storage.setPeers(toStorage)
            val subscribed = async {
                decoratedStorage.peers
                    .drop(toStorage.size)
                    .take(toStorage.size)
                    .toList()
            }

            val updated = toStorage.mapIndexed { index, peer ->
                peer.copy(name = "${peer.name}$index")
            }
            decoratedStorage.addPeers(updated)

            assertEquals(
                updated.sortedBy { it.name },
                subscribed.await().sortedBy { it.name })
        }
    }

    private suspend fun Storage.getPeers(): List<Peer> = getMaybePeers().mapNotNull { it.getOrNull() }
    private suspend fun Storage.getAppMetadata(): List<AppMetadata> = getMaybeAppMetadata().mapNotNull { it.getOrNull() }
    private suspend fun Storage.getPermissions(): List<Permission> = getMaybePermissions().mapNotNull { it.getOrNull() }

    private fun <T> Maybe<T>.getOrNull(): T? =
        when (this) {
            is Maybe.Some -> value
            is Maybe.None -> null
            is Maybe.NoneWithError -> null
        }
}