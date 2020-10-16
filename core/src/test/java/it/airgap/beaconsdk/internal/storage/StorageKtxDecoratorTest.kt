package it.airgap.beaconsdk.internal.storage

import io.mockk.coVerify
import io.mockk.spyk
import it.airgap.beaconsdk.data.account.AccountInfo
import it.airgap.beaconsdk.data.network.Network
import it.airgap.beaconsdk.data.p2p.P2pPairingRequest
import it.airgap.beaconsdk.data.permission.PermissionInfo
import it.airgap.beaconsdk.data.sdk.AppMetadata
import it.airgap.beaconsdk.data.sdk.Origin
import it.airgap.beaconsdk.storage.BeaconStorageKtx
import it.airgap.beaconsdk.storage.MockBeaconStorageKtx
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class StorageKtxDecoratorTest {
    private lateinit var storage: BeaconStorageKtx
    private lateinit var decorator: Storage.KtxDecorator

    @Before
    fun setup() {
        storage = spyk(MockBeaconStorageKtx())
        decorator = Storage.KtxDecorator(storage)
    }

    @Test
    fun `sets and gets P2P peers`() {
        runBlocking {
            decorator.setP2pPeers(mockPeers)
            val peers = decorator.getP2pPeers()

            coVerify(exactly = 1) { storage.setP2pPeers(mockPeers) }
            coVerify(exactly = 1) { storage.getP2pPeers() }
            Assert.assertEquals(mockPeers, peers)
        }
    }

    @Test
    fun `sets and gets accounts`() {
        runBlocking {
            decorator.setAccounts(mockAccounts)
            val accounts = decorator.getAccounts()

            coVerify(exactly = 1) { storage.setAccounts(mockAccounts) }
            coVerify(exactly = 1) { storage.getAccounts() }
            Assert.assertEquals(mockAccounts, accounts)
        }
    }

    @Test
    fun `sets and gets active account identifier`() {
        runBlocking {
            decorator.setActiveAccountIdentifier(mockActiveAccountIdentifier)
            val identifier = decorator.getActiveAccountIdentifier()

            coVerify(exactly = 1) { storage.setActiveAccountIdentifier(mockActiveAccountIdentifier) }
            coVerify(exactly = 1) { storage.getActiveAccountIdentifier() }
            Assert.assertEquals(mockActiveAccountIdentifier, identifier)
        }
    }

    @Test
    fun `sets and gets apps metadata`() {
        runBlocking {
            decorator.setAppsMetadata(mockAppsMetadata)
            val metadata = decorator.getAppsMetadata()

            coVerify(exactly = 1) { storage.setAppsMetadata(mockAppsMetadata) }
            coVerify(exactly = 1) { storage.getAppsMetadata() }
            Assert.assertEquals(mockAppsMetadata, metadata)
        }
    }

    @Test
    fun `sets and gets permissions`() {
        runBlocking {
            decorator.setPermissions(mockPermissions)
            val permissions = decorator.getPermissions()

            coVerify(exactly = 1) { storage.setPermissions(mockPermissions) }
            coVerify(exactly = 1) { storage.getPermissions() }
            Assert.assertEquals(mockPermissions, permissions)
        }
    }

    @Test
    fun `sets and gets sdk secret seed`() {
        runBlocking {
            decorator.setSdkSecretSeed(mockSdkSecretSeed)
            val seed = decorator.getSdkSecretSeed()

            coVerify(exactly = 1) { storage.setSdkSecretSeed(mockSdkSecretSeed) }
            coVerify(exactly = 1) { storage.getSdkSecretSeed() }
            Assert.assertEquals(mockSdkSecretSeed, seed)
        }
    }

    @Test
    fun `sets and gets sdk version`() {
        runBlocking {
            decorator.setSdkVersion(mockSdkVersion)
            val sdkVersion = decorator.getSdkVersion()

            coVerify(exactly = 1) { storage.setSdkVersion(mockSdkVersion) }
            coVerify(exactly = 1) { storage.getSdkVersion() }
            Assert.assertEquals(mockSdkVersion, sdkVersion)
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