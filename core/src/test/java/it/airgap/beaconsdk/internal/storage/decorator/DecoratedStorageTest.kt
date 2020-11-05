package it.airgap.beaconsdk.internal.storage.decorator

import accounts
import appMetadata
import it.airgap.beaconsdk.internal.storage.MockStorage
import it.airgap.beaconsdk.internal.storage.Storage
import it.airgap.beaconsdk.internal.utils.splitAt
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
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
        decoratedStorage = DecoratedStorage(storage)
    }

    @Test
    fun `finds account based on predicate`() {
        val accounts = accounts(4)
        val expected = accounts.shuffled().first().copy()

        runBlocking { storage.setAccounts(accounts) }

        val found =
            runBlocking { decoratedStorage.findAccount { it.accountIdentifier == expected.accountIdentifier } }

        assertEquals(expected, found)
    }

    @Test
    fun `finds app metadata based on predicate`() {
        val appMetadata = appMetadata(3)
        val expected = appMetadata.shuffled().first().copy()

        runBlocking { storage.setAppsMetadata(appMetadata) }

        val found = runBlocking { decoratedStorage.findAppMetadata { it.name == expected.name } }

        assertEquals(expected, found)
    }

    @Test
    fun `finds permissions based on predicate`() {
        val permissions = permissions(3)
        val expected = permissions.shuffled().first().copy()

        runBlocking { storage.setPermissions(permissions) }

        val found =
            runBlocking { decoratedStorage.findPermission { it.accountIdentifier == expected.accountIdentifier } }

        assertEquals(expected, found)
    }

    @Test
    fun `finds P2P peers based on predicate`() {
        val peers = p2pPeers(3)
        val expected = peers.shuffled().first().copy()

        runBlocking { storage.setP2pPeers(peers) }

        val found = runBlocking { decoratedStorage.findP2pPeer { it.name == expected.name } }

        assertEquals(expected, found)
    }

    @Test
    fun `adds new accounts and overwrites if already exists based on predicate`() {
        val accounts = accounts(8)
        val expected = accounts.toMutableList()

        val (toStorage, toAdd) = accounts.splitAt { it.size / 2 }

        val changed = toStorage
            .mapIndexed { index, accountInfo -> index to accountInfo }
            .shuffled()
            .takeHalf()
            .map { it.first to it.second.copy(senderId = "${it.second.senderId}${it.first}") }
            .onEach { expected[it.first] = it.second }
            .map { it.second }

        runBlocking { storage.setAccounts(toStorage) }
        runBlocking {
            decoratedStorage.addAccounts(
                changed + toAdd,
                overwrite = true
            ) { a, b -> a.accountIdentifier == b.accountIdentifier }
        }

        val fromStorage = runBlocking { storage.getAccounts() }

        assertEquals(expected, fromStorage)
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

        runBlocking { storage.setAppsMetadata(toStorage) }
        runBlocking {
            decoratedStorage.addAppsMetadata(
                changed + toAdd,
                overwrite = true
            ) { a, b -> a.name == b.name }
        }

        val fromStorage = runBlocking { storage.getAppsMetadata() }

        assertEquals(expected, fromStorage)
    }

    @Test
    fun `adds new permissions and overwrites if already exists based on predicate`() {
        val permissions = permissions(8)
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
            ) { a, b -> a.accountIdentifier == b.accountIdentifier }
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

        runBlocking { storage.setP2pPeers(toStorage) }
        runBlocking {
            decoratedStorage.addP2pPeers(
                changed + toAdd,
                overwrite = true
            ) { a, b -> a.name == b.name }
        }

        val fromStorage = runBlocking { storage.getP2pPeers() }

        assertEquals(expected, fromStorage)
    }

    @Test
    fun `adds new accounts and does not overwrite if exists base on predicate`() {
        val accounts = accounts(8)

        val (toStorage, toAdd) = accounts.splitAt { it.size / 2 }
        val changed = toStorage
            .shuffled()
            .takeHalf()
            .mapIndexed { index, accountInfo -> accountInfo.copy(senderId = "${accountInfo.senderId}${index}") }

        runBlocking { storage.setAccounts(toStorage) }
        runBlocking {
            decoratedStorage.addAccounts(changed + toAdd, overwrite = false) { a, b ->
                a.accountIdentifier == b.accountIdentifier
            }
        }

        val fromStorage = runBlocking { storage.getAccounts() }

        assertEquals(toStorage + toAdd, fromStorage)
    }

    @Test
    fun `adds new app metadata and does not overwrite if exists base on predicate`() {
        val appMetadata = appMetadata(8)

        val (toStorage, toAdd) = appMetadata.splitAt { it.size / 2 }
        val changed = toStorage
            .shuffled()
            .takeHalf()
            .mapIndexed { index, appMetadata -> appMetadata.copy(senderId = "${appMetadata.senderId}${index}") }

        runBlocking { storage.setAppsMetadata(toStorage) }
        runBlocking {
            decoratedStorage.addAppsMetadata(changed + toAdd, overwrite = false) { a, b ->
                a.name == b.name
            }
        }

        val fromStorage = runBlocking { storage.getAppsMetadata() }

        assertEquals(toStorage + toAdd, fromStorage)
    }

    @Test
    fun `adds new permissions and does not overwrite if exists base on predicate`() {
        val permissions = permissions(8)

        val (toStorage, toAdd) = permissions.splitAt { it.size / 2 }
        val changed = toStorage
            .shuffled()
            .takeHalf()
            .mapIndexed { index, permission -> permission.copy(senderId = "${permission.senderId}${index}") }

        runBlocking { storage.setPermissions(toStorage) }
        runBlocking {
            decoratedStorage.addPermissions(changed + toAdd, overwrite = false) { a, b ->
                a.accountIdentifier == b.accountIdentifier
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

        runBlocking { storage.setP2pPeers(toStorage) }
        runBlocking {
            decoratedStorage.addP2pPeers(changed + toAdd, overwrite = false) { a, b ->
                a.name == b.name
            }
        }

        val fromStorage = runBlocking { storage.getP2pPeers() }

        assertEquals(toStorage + toAdd, fromStorage)
    }

    @Test
    fun `removes accounts based on predicate`() {
        val accounts = accounts(4)
        val (toKeep, toRemove) = accounts.splitAt { it.size / 2 }
        val accountIdentifiersToRemove = toRemove.map { it.accountIdentifier }

        runBlocking { storage.setAccounts(accounts) }
        runBlocking { decoratedStorage.removeAccounts { accountIdentifiersToRemove.contains(it.accountIdentifier) } }
        val fromStorage = runBlocking { storage.getAccounts() }

        assertEquals(toKeep, fromStorage)
    }

    @Test
    fun `removes app metadata based on predicate`() {
        val appMetadata = appMetadata(4)
        val (toKeep, toRemove) = appMetadata.splitAt { it.size / 2 }
        val namesToRemove = toRemove.map { it.name }

        runBlocking { storage.setAppsMetadata(appMetadata) }
        runBlocking { decoratedStorage.removeAppsMetadata() { namesToRemove.contains(it.name) } }
        val fromStorage = runBlocking { storage.getAppsMetadata() }

        assertEquals(toKeep, fromStorage)
    }

    @Test
    fun `removes permissions based on predicate`() {
        val permissions = permissions(4)
        val (toKeep, toRemove) = permissions.splitAt { it.size / 2 }
        val accountIdentifiersToRemove = toRemove.map { it.accountIdentifier }

        runBlocking { storage.setPermissions(permissions) }
        runBlocking { decoratedStorage.removePermissions { accountIdentifiersToRemove.contains(it.accountIdentifier) } }
        val fromStorage = runBlocking { storage.getPermissions() }

        assertEquals(toKeep, fromStorage)
    }

    @Test
    fun `removes P2P peers based on predicate`() {
        val peers = p2pPeers(4)
        val (toKeep, toRemove) = peers.splitAt { it.size / 2 }
        val namesToRemove = toRemove.map { it.name }

        runBlocking { storage.setP2pPeers(peers) }
        runBlocking { decoratedStorage.removeP2pPeers { namesToRemove.contains(it.name) } }
        val fromStorage = runBlocking { storage.getP2pPeers() }

        assertEquals(toKeep, fromStorage)
    }

    @Test
    fun `removes all accounts`() {
        val accounts = accounts(2)
        runBlocking { storage.setAccounts(accounts) }
        runBlocking { decoratedStorage.removeAccounts() }

        val fromStorage = runBlocking { storage.getAccounts() }

        assertTrue(fromStorage.isEmpty())
    }

    @Test
    fun `removes all app metadata`() {
        val appMetadata = appMetadata(2)
        runBlocking { storage.setAppsMetadata(appMetadata) }
        runBlocking { decoratedStorage.removeAppsMetadata() }

        val fromStorage = runBlocking { storage.getAppsMetadata() }

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
        runBlocking { storage.setP2pPeers(peers) }
        runBlocking { decoratedStorage.removeP2pPeers() }

        val fromStorage = runBlocking { storage.getP2pPeers() }

        assertTrue(fromStorage.isEmpty())
    }

    @Test
    fun `notifies when new accounts are added`() {
        val toAdd = accounts(2)

        runBlocking {
            val newAccounts = async {
                decoratedStorage.accounts
                    .take(toAdd.size)
                    .toList()
            }
            decoratedStorage.addAccounts(toAdd)

            assertEquals(
                toAdd.sortedBy { it.accountIdentifier },
                newAccounts.await().sortedBy { it.accountIdentifier })
        }
    }

    @Test
    fun `notifies when new active account identifier is set`() {
        val toSet = "new active"

        runBlocking {
            val newAccountIdentifier = async {
                decoratedStorage.activeAccountIdentifier.first()
            }
            decoratedStorage.setActiveAccountIdentifier(toSet)

            assertEquals(toSet, newAccountIdentifier.await())
        }
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
            decoratedStorage.addAppsMetadata(toAdd)

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
                toAdd.sortedBy { it.accountIdentifier },
                newPermissions.await().sortedBy { it.accountIdentifier })
        }
    }

    @Test
    fun `notifies when new P2P peers are added`() {
        val toAdd = p2pPeers(2)

        runBlocking {
            val newPeers = async {
                decoratedStorage.p2pPeers
                    .take(toAdd.size)
                    .toList()
            }
            decoratedStorage.addP2pPeers(toAdd)

            assertEquals(toAdd.sortedBy { it.name }, newPeers.await().sortedBy { it.name })
        }
    }

    @Test
    fun `notifies when accounts are updated`() {
        val toStorage = accounts(2)

        runBlocking {
            storage.setAccounts(toStorage)
            val subscribed = async {
                decoratedStorage.accounts
                    .drop(toStorage.size)
                    .take(toStorage.size)
                    .toList()
            }

            val updated = toStorage.mapIndexed { index, accountInfo ->
                accountInfo.copy(accountIdentifier = "${accountInfo.accountIdentifier}$index")
            }
            decoratedStorage.addAccounts(updated)

            assertEquals(
                updated.sortedBy { it.accountIdentifier },
                subscribed.await().sortedBy { it.accountIdentifier })
        }
    }

    @Test
    fun `notifies when new app metadata is updated`() {
        val toStorage = appMetadata(2)

        runBlocking {
            storage.setAppsMetadata(toStorage)
            val subscribed = async {
                decoratedStorage.appMetadata
                    .drop(toStorage.size)
                    .take(toStorage.size)
                    .toList()
            }

            val updated = toStorage.mapIndexed { index, appMetadata ->
                appMetadata.copy(name = "${appMetadata.name}$index")
            }
            decoratedStorage.addAppsMetadata(updated)

            assertEquals(
                updated.sortedBy { it.name },
                subscribed.await().sortedBy { it.name })
        }
    }

    @Test
    fun `notifies when permissions are updated`() {
        val toStorage = permissions(2)

        runBlocking {
            storage.setPermissions(toStorage)
            val subscribed = async {
                decoratedStorage.permissions
                    .drop(toStorage.size)
                    .take(toStorage.size)
                    .toList()
            }

            val updated = toStorage.mapIndexed { index, permission ->
                permission.copy(accountIdentifier = "${permission.accountIdentifier}$index")
            }
            decoratedStorage.addPermissions(updated)

            assertEquals(
                updated.sortedBy { it.accountIdentifier },
                subscribed.await().sortedBy { it.accountIdentifier })
        }
    }

    @Test
    fun `notifies when P2P peers are updated`() {
        val toStorage = p2pPeers(2)

        runBlocking {
            storage.setP2pPeers(toStorage)
            val subscribed = async {
                decoratedStorage.p2pPeers
                    .drop(toStorage.size)
                    .take(toStorage.size)
                    .toList()
            }

            val updated = toStorage.mapIndexed { index, peer ->
                peer.copy(name = "${peer.name}$index")
            }
            decoratedStorage.addP2pPeers(updated)

            assertEquals(
                updated.sortedBy { it.name },
                subscribed.await().sortedBy { it.name })
        }
    }
}