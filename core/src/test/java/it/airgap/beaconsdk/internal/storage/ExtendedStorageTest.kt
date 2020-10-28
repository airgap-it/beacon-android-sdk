package it.airgap.beaconsdk.internal.storage

import androidx.annotation.IntRange
import it.airgap.beaconsdk.data.account.AccountInfo
import it.airgap.beaconsdk.data.network.Network
import it.airgap.beaconsdk.data.p2p.P2pPeerInfo
import it.airgap.beaconsdk.data.permission.PermissionInfo
import it.airgap.beaconsdk.data.sdk.AppMetadata
import it.airgap.beaconsdk.data.sdk.Origin
import it.airgap.beaconsdk.internal.utils.splitAt
import it.airgap.beaconsdk.storage.BeaconStorage
import it.airgap.beaconsdk.storage.MockBeaconStorage
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExtendedStorageTest {

    private lateinit var storage: BeaconStorage
    private lateinit var extendedStorage: ExtendedStorage

    @Before
    fun setup() {
        storage = MockBeaconStorage()
        extendedStorage = ExtendedStorage(storage)
    }

    @Test
    fun `finds account based on predicate`() {
        val accounts = generateAccounts(3)
        val expected = accounts.shuffled().first().copy()

        runBlocking { storage.setAccounts(accounts) }

        val found =
            runBlocking { extendedStorage.findAccount { it.accountIdentifier == expected.accountIdentifier } }

        assertEquals(expected, found)
    }

    @Test
    fun `finds app metadata based on predicate`() {
        val appMetadata = generateAppMetadata(3)
        val expected = appMetadata.shuffled().first().copy()

        runBlocking { storage.setAppsMetadata(appMetadata) }

        val found = runBlocking { extendedStorage.findAppMetadata { it.name == expected.name } }

        assertEquals(expected, found)
    }

    @Test
    fun `finds permissions based on predicate`() {
        val permissions = generatePermissions(3)
        val expected = permissions.shuffled().first().copy()

        runBlocking { storage.setPermissions(permissions) }

        val found =
            runBlocking { extendedStorage.findPermission { it.accountIdentifier == expected.accountIdentifier } }

        assertEquals(expected, found)
    }

    @Test
    fun `finds P2P peers based on predicate`() {
        val peers = generateP2pPeers(3)
        val expected = peers.shuffled().first().copy()

        runBlocking { storage.setP2pPeers(peers) }

        val found = runBlocking { extendedStorage.findP2pPeer { it.name == expected.name } }

        assertEquals(expected, found)
    }

    @Test
    fun `adds new accounts and overwrites if already exists based on predicate`() {
        val accounts = generateAccounts(8)
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
            extendedStorage.addAccounts(
                changed + toAdd,
                overwrite = true
            ) { a, b -> a.accountIdentifier == b.accountIdentifier }
        }

        val fromStorage = runBlocking { storage.getAccounts() }

        assertEquals(expected, fromStorage)
    }

    @Test
    fun `adds new app metadata and overwrites if already exists based on predicate`() {
        val appMetadata = generateAppMetadata(8)
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
            extendedStorage.addAppsMetadata(
                changed + toAdd,
                overwrite = true
            ) { a, b -> a.name == b.name }
        }

        val fromStorage = runBlocking { storage.getAppsMetadata() }

        assertEquals(expected, fromStorage)
    }

    @Test
    fun `adds new permissions and overwrites if already exists based on predicate`() {
        val permissions = generatePermissions(8)
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
            extendedStorage.addPermissions(
                changed + toAdd,
                overwrite = true
            ) { a, b -> a.accountIdentifier == b.accountIdentifier }
        }

        val fromStorage = runBlocking { storage.getPermissions() }

        assertEquals(expected, fromStorage)
    }

    @Test
    fun `adds new P2P requests and overwrites if already exists based on predicate`() {
        val peers = generateP2pPeers(8)
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
            extendedStorage.addP2pPeers(
                changed + toAdd,
                overwrite = true
            ) { a, b -> a.name == b.name }
        }

        val fromStorage = runBlocking { storage.getP2pPeers() }

        assertEquals(expected, fromStorage)
    }

    @Test
    fun `adds new accounts and does not overwrite if exists base on predicate`() {
        val accounts = generateAccounts(8)

        val (toStorage, toAdd) = accounts.splitAt { it.size / 2 }
        val changed = toStorage
            .shuffled()
            .takeHalf()
            .mapIndexed { index, accountInfo -> accountInfo.copy(senderId = "${accountInfo.senderId}${index}") }

        runBlocking { storage.setAccounts(toStorage) }
        runBlocking {
            extendedStorage.addAccounts(changed + toAdd, overwrite = false) { a, b ->
                a.accountIdentifier == b.accountIdentifier
            }
        }

        val fromStorage = runBlocking { storage.getAccounts() }

        assertEquals(toStorage + toAdd, fromStorage)
    }

    @Test
    fun `adds new app metadata and does not overwrite if exists base on predicate`() {
        val appMetadata = generateAppMetadata(8)

        val (toStorage, toAdd) = appMetadata.splitAt { it.size / 2 }
        val changed = toStorage
            .shuffled()
            .takeHalf()
            .mapIndexed { index, appMedata -> appMedata.copy(senderId = "${appMedata.senderId}${index}") }

        runBlocking { storage.setAppsMetadata(toStorage) }
        runBlocking {
            extendedStorage.addAppsMetadata(changed + toAdd, overwrite = false) { a, b ->
                a.name == b.name
            }
        }

        val fromStorage = runBlocking { storage.getAppsMetadata() }

        assertEquals(toStorage + toAdd, fromStorage)
    }

    @Test
    fun `adds new permissions and does not overwrite if exists base on predicate`() {
        val permissions = generatePermissions(8)

        val (toStorage, toAdd) = permissions.splitAt { it.size / 2 }
        val changed = toStorage
            .shuffled()
            .takeHalf()
            .mapIndexed { index, permission -> permission.copy(senderId = "${permission.senderId}${index}") }

        runBlocking { storage.setPermissions(toStorage) }
        runBlocking {
            extendedStorage.addPermissions(changed + toAdd, overwrite = false) { a, b ->
                a.accountIdentifier == b.accountIdentifier
            }
        }

        val fromStorage = runBlocking { storage.getPermissions() }

        assertEquals(toStorage + toAdd, fromStorage)
    }

    @Test
    fun `adds new P2P requests and does not overwrite if exists base on predicate`() {
        val peers = generateP2pPeers(8)

        val (toStorage, toAdd) = peers.splitAt { it.size / 2 }
        val changed = toStorage
            .shuffled()
            .takeHalf()
            .mapIndexed { index, peer -> peer.copy(publicKey = "${peer.publicKey}${index}") }

        runBlocking { storage.setP2pPeers(toStorage) }
        runBlocking {
            extendedStorage.addP2pPeers(changed + toAdd, overwrite = false) { a, b ->
                a.name == b.name
            }
        }

        val fromStorage = runBlocking { storage.getP2pPeers() }

        assertEquals(toStorage + toAdd, fromStorage)
    }

    @Test
    fun `removes accounts based on predicate`() {
        val accounts = generateAccounts(4)
        val (toKeep, toRemove) = accounts.splitAt { it.size / 2 }
        val accountIdentifiersToRemove = toRemove.map { it.accountIdentifier }

        runBlocking { storage.setAccounts(accounts) }
        runBlocking { extendedStorage.removeAccounts { accountIdentifiersToRemove.contains(it.accountIdentifier) } }
        val fromStorage = runBlocking { storage.getAccounts() }

        assertEquals(toKeep, fromStorage)
    }

    @Test
    fun `removes app metadata based on predicate`() {
        val appMetadata = generateAppMetadata(4)
        val (toKeep, toRemove) = appMetadata.splitAt { it.size / 2 }
        val namesToRemove = toRemove.map { it.name }

        runBlocking { storage.setAppsMetadata(appMetadata) }
        runBlocking { extendedStorage.removeAppsMetadata() { namesToRemove.contains(it.name) } }
        val fromStorage = runBlocking { storage.getAppsMetadata() }

        assertEquals(toKeep, fromStorage)
    }

    @Test
    fun `removes permissions based on predicate`() {
        val permissions = generatePermissions(4)
        val (toKeep, toRemove) = permissions.splitAt { it.size / 2 }
        val accountIdentifiersToRemove = toRemove.map { it.accountIdentifier }

        runBlocking { storage.setPermissions(permissions) }
        runBlocking { extendedStorage.removePermissions { accountIdentifiersToRemove.contains(it.accountIdentifier) } }
        val fromStorage = runBlocking { storage.getPermissions() }

        assertEquals(toKeep, fromStorage)
    }

    @Test
    fun `removes P2P peers based on predicate`() {
        val peers = generateP2pPeers(4)
        val (toKeep, toRemove) = peers.splitAt { it.size / 2 }
        val namesToRemove = toRemove.map { it.name }

        runBlocking { storage.setP2pPeers(peers) }
        runBlocking { extendedStorage.removeP2pPeers { namesToRemove.contains(it.name) } }
        val fromStorage = runBlocking { storage.getP2pPeers() }

        assertEquals(toKeep, fromStorage)
    }

    @Test
    fun `removes all accounts`() {
        val accounts = generateAccounts(2)
        runBlocking { storage.setAccounts(accounts) }
        runBlocking { extendedStorage.removeAccounts() }

        val fromStorage = runBlocking { storage.getAccounts() }

        assertTrue(fromStorage.isEmpty())
    }

    @Test
    fun `removes all app metadata`() {
        val appMetadata = generateAppMetadata(2)
        runBlocking { storage.setAppsMetadata(appMetadata) }
        runBlocking { extendedStorage.removeAppsMetadata() }

        val fromStorage = runBlocking { storage.getAppsMetadata() }

        assertTrue(fromStorage.isEmpty())
    }

    @Test
    fun `removes all permissions`() {
        val permissions = generatePermissions(2)
        runBlocking { storage.setPermissions(permissions) }
        runBlocking { extendedStorage.removePermissions() }

        val fromStorage = runBlocking { storage.getPermissions() }

        assertTrue(fromStorage.isEmpty())
    }

    @Test
    fun `removes all P2P peers`() {
        val peers = generateP2pPeers(2)
        runBlocking { storage.setP2pPeers(peers) }
        runBlocking { extendedStorage.removeP2pPeers() }

        val fromStorage = runBlocking { storage.getP2pPeers() }

        assertTrue(fromStorage.isEmpty())
    }

    @Test
    fun `notifies when new accounts are added`() {
        val toAdd = generateAccounts(2)

        runBlocking {
            val newAccounts = async {
                extendedStorage.accounts
                    .take(toAdd.size)
                    .toList()
            }
            extendedStorage.addAccounts(toAdd)

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
                extendedStorage.activeAccountIdentifier.first()
            }
            extendedStorage.setActiveAccountIdentifier(toSet)

            assertEquals(toSet, newAccountIdentifier.await())
        }
    }

    @Test
    fun `notifies when new app metadata is added`() {
        val toAdd = generateAppMetadata(2)

        runBlocking {
            val newAppMetadata = async {
                extendedStorage.appMetadata
                    .take(toAdd.size)
                    .toList()
            }
            extendedStorage.addAppsMetadata(toAdd)

            assertEquals(toAdd.sortedBy { it.name }, newAppMetadata.await().sortedBy { it.name })
        }
    }

    @Test
    fun `notifies when new permissions are added`() {
        val toAdd = generatePermissions(2)

        runBlocking {
            val newPermissions = async {
                extendedStorage.permissions
                    .take(toAdd.size)
                    .toList()
            }
            extendedStorage.addPermissions(toAdd)

            assertEquals(
                toAdd.sortedBy { it.accountIdentifier },
                newPermissions.await().sortedBy { it.accountIdentifier })
        }
    }

    @Test
    fun `notifies when new P2P peers are added`() {
        val toAdd = generateP2pPeers(2)

        runBlocking {
            val newPeers = async {
                extendedStorage.p2pPeers
                    .take(toAdd.size)
                    .toList()
            }
            extendedStorage.addP2pPeers(toAdd)

            assertEquals(toAdd.sortedBy { it.name }, newPeers.await().sortedBy { it.name })
        }
    }

    @Test
    fun `notifies when accounts are updated`() {
        val toStorage = generateAccounts(2)

        runBlocking {
            storage.setAccounts(toStorage)
            val subscribed = async {
                extendedStorage.accounts
                    .drop(toStorage.size)
                    .take(toStorage.size)
                    .toList()
            }

            val updated = toStorage.mapIndexed { index, accountInfo ->
                accountInfo.copy(accountIdentifier = "${accountInfo.accountIdentifier}$index")
            }
            extendedStorage.addAccounts(updated)

            assertEquals(
                updated.sortedBy { it.accountIdentifier },
                subscribed.await().sortedBy { it.accountIdentifier })
        }
    }

    @Test
    fun `notifies when new app metadata is updated`() {
        val toStorage = generateAppMetadata(2)

        runBlocking {
            storage.setAppsMetadata(toStorage)
            val subscribed = async {
                extendedStorage.appMetadata
                    .drop(toStorage.size)
                    .take(toStorage.size)
                    .toList()
            }

            val updated = toStorage.mapIndexed { index, appMetadata ->
                appMetadata.copy(name = "${appMetadata.name}$index")
            }
            extendedStorage.addAppsMetadata(updated)

            assertEquals(
                updated.sortedBy { it.name },
                subscribed.await().sortedBy { it.name })
        }
    }

    @Test
    fun `notifies when permissions are updated`() {
        val toStorage = generatePermissions(2)

        runBlocking {
            storage.setPermissions(toStorage)
            val subscribed = async {
                extendedStorage.permissions
                    .drop(toStorage.size)
                    .take(toStorage.size)
                    .toList()
            }

            val updated = toStorage.mapIndexed { index, permission ->
                permission.copy(accountIdentifier = "${permission.accountIdentifier}$index")
            }
            extendedStorage.addPermissions(updated)

            assertEquals(
                updated.sortedBy { it.accountIdentifier },
                subscribed.await().sortedBy { it.accountIdentifier })
        }
    }

    @Test
    fun `notifies when P2P peers are updated`() {
        val toStorage = generateP2pPeers(2)

        runBlocking {
            storage.setP2pPeers(toStorage)
            val subscribed = async {
                extendedStorage.p2pPeers
                    .drop(toStorage.size)
                    .take(toStorage.size)
                    .toList()
            }

            val updated = toStorage.mapIndexed { index, peer ->
                peer.copy(name = "${peer.name}$index")
            }
            extendedStorage.addP2pPeers(updated)

            assertEquals(
                updated.sortedBy { it.name },
                subscribed.await().sortedBy { it.name })
        }
    }

    private fun generateAccounts(@IntRange(from = 1) n: Int): List<AccountInfo> =
        (0 until n).map {
            AccountInfo(
                "identifier#$it",
                "address#$it",
                Network.Custom(),
                emptyList(),
                "sender#$it",
                Origin.P2P("origin#$it"),
                "publicKey#$it",
                it.toLong(),
            )
        }

    private fun generateAppMetadata(@IntRange(from = 1) n: Int): List<AppMetadata> =
        (0 until n).map {
            AppMetadata(
                "sender#$it",
                "#$it",
            )
        }

    private fun generatePermissions(@IntRange(from = 1) n: Int): List<PermissionInfo> =
        (0 until n).map {
            PermissionInfo(
                "identifier#$it",
                "address#$it",
                Network.Custom(),
                emptyList(),
                "sender#$it",
                AppMetadata("sender#$it", "#$it"),
                "website#$it",
                "publicKey#$it",
                it.toLong(),
            )
        }

    private fun generateP2pPeers(@IntRange(from = 1) n: Int): List<P2pPeerInfo> =
        (0 until n).map {
            P2pPeerInfo(
                "#$it",
                "publicKey#$it",
                "relayServer#$it",
            )
        }

    private fun <T> List<T>.takeHalf(): List<T> = take(size / 2)
}