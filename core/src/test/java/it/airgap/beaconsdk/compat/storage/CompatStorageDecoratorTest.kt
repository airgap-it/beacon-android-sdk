package it.airgap.beaconsdk.compat.storage

import io.mockk.spyk
import io.mockk.verify
import it.airgap.beaconsdk.compat.internal.CompatStorageDecorator
import it.airgap.beaconsdk.compat.storage.BeaconCompatStorage
import it.airgap.beaconsdk.data.account.AccountInfo
import it.airgap.beaconsdk.data.network.Network
import it.airgap.beaconsdk.data.p2p.P2pPairingRequest
import it.airgap.beaconsdk.data.permission.PermissionInfo
import it.airgap.beaconsdk.data.sdk.AppMetadata
import it.airgap.beaconsdk.data.sdk.Origin
import it.airgap.beaconsdk.storage.MockBeaconCompatStorage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CompatStorageDecoratorTest {

    private lateinit var storage: BeaconCompatStorage
    private lateinit var decorator: CompatStorageDecorator

    @Before
    fun setup() {
        storage = spyk(MockBeaconCompatStorage())
        decorator = CompatStorageDecorator(storage)
    }

    @Test
    fun `sets and gets P2P peers`() {
        runBlocking {
            decorator.setP2pPeers(mockPeers)
            val peers = decorator.getP2pPeers()

            verify(exactly = 1) { storage.setP2pPeers(mockPeers, any()) }
            verify(exactly = 1) { storage.getP2pPeers(any()) }
            assertEquals(mockPeers, peers)
        }
    }

    @Test
    fun `sets and gets accounts`() {
        runBlocking {
            decorator.setAccounts(mockAccounts)
            val accounts = decorator.getAccounts()

            verify(exactly = 1) { storage.setAccounts(mockAccounts, any()) }
            verify(exactly = 1) { storage.getAccounts(any()) }
            assertEquals(mockAccounts, accounts)
        }
    }

    @Test
    fun `sets and gets active account identifier`() {
        runBlocking {
            decorator.setActiveAccountIdentifier(mockActiveAccountIdentifier)
            val identifier = decorator.getActiveAccountIdentifier()

            verify(exactly = 1) { storage.setActiveAccountIdentifier(mockActiveAccountIdentifier, any()) }
            verify(exactly = 1) { storage.getActiveAccountIdentifier(any()) }
            assertEquals(mockActiveAccountIdentifier, identifier)
        }
    }

    @Test
    fun `sets and gets apps metadata`() {
        runBlocking {
            decorator.setAppsMetadata(mockAppsMetadata)
            val metadata = decorator.getAppsMetadata()

            verify(exactly = 1) { storage.setAppsMetadata(mockAppsMetadata, any()) }
            verify(exactly = 1) { storage.getAppsMetadata(any()) }
            assertEquals(mockAppsMetadata, metadata)
        }
    }

    @Test
    fun `sets and gets permissions`() {
        runBlocking {
            decorator.setPermissions(mockPermissions)
            val permissions = decorator.getPermissions()

            verify(exactly = 1) { storage.setPermissions(mockPermissions, any()) }
            verify(exactly = 1) { storage.getPermissions(any()) }
            assertEquals(mockPermissions, permissions)
        }
    }

    @Test
    fun `sets and gets sdk secret seed`() {
        runBlocking {
            decorator.setSdkSecretSeed(mockSdkSecretSeed)
            val seed = decorator.getSdkSecretSeed()

            verify(exactly = 1) { storage.setSdkSecretSeed(mockSdkSecretSeed, any()) }
            verify(exactly = 1) { storage.getSdkSecretSeed(any()) }
            assertEquals(mockSdkSecretSeed, seed)
        }
    }

    @Test
    fun `sets and gets sdk version`() {
        runBlocking {
            decorator.setSdkVersion(mockSdkVersion)
            val sdkVersion = decorator.getSdkVersion()

            verify(exactly = 1) { storage.setSdkVersion(mockSdkVersion, any()) }
            verify(exactly = 1) { storage.getSdkVersion(any()) }
            assertEquals(mockSdkVersion, sdkVersion)
        }
    }

    private val mockPeers: List<P2pPairingRequest> = listOf(
        P2pPairingRequest(
            "name",
            "publicKey",
            "relayServer"
        )
    )
    private val mockAccounts: List<AccountInfo> = listOf(
        AccountInfo(
            "accountIdentifier",
            "address",
            Network.Custom(),
            emptyList(),
            "senderId",
            Origin.P2P("id"),
            "publicKey",
            1
        )
    )

    private val mockActiveAccountIdentifier: String = "activeAccountIdentifier"

    private val mockAppsMetadata: List<AppMetadata> = listOf(
        AppMetadata("senderId", "name")
    )

    private val mockPermissions: List<PermissionInfo> = listOf(
        PermissionInfo(
            "accountIdentifier",
            "address",
            Network.Custom(),
            emptyList(),
            "senderId",
            AppMetadata("senderId", "name"),
            "website",
            "publicKey",
            1
        )
    )

    private val mockSdkSecretSeed: String = "sdkSecretSeed"

    private val mockSdkVersion: String = "sdkVersion"
}