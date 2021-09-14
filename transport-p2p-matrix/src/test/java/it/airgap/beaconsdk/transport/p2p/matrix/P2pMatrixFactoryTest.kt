package it.airgap.beaconsdk.transport.p2p.matrix

import io.mockk.*
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.core.internal.migration.Migration
import it.airgap.beaconsdk.core.internal.storage.MockSecureStorage
import it.airgap.beaconsdk.core.internal.storage.MockStorage
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.utils.AccountUtils
import it.airgap.beaconsdk.core.network.provider.HttpProvider
import it.airgap.beaconsdk.transport.p2p.matrix.internal.BeaconP2pMatrixConfiguration
import it.airgap.beaconsdk.transport.p2p.matrix.internal.di.ExtendedDependencyRegistry
import it.airgap.beaconsdk.transport.p2p.matrix.internal.storage.MockP2pMatrixStoragePlugin
import it.airgap.beaconsdk.transport.p2p.matrix.internal.storage.sharedpreferences.SharedPreferencesMatrixStoragePlugin
import it.airgap.beaconsdk.transport.p2p.matrix.storage.ExtendedP2pMatrixStoragePlugin
import mockApp
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertTrue

internal class P2pMatrixFactoryTest {

    @MockK(relaxed = true)
    private lateinit var dependencyRegistry: ExtendedDependencyRegistry

    @MockK(relaxed = true)
    private lateinit var migration: Migration

    @MockK
    private lateinit var accountUtils: AccountUtils

    private lateinit var storageManager: StorageManager

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        mockApp()

        storageManager = spyk(StorageManager(MockStorage(), MockSecureStorage(), accountUtils))

        every { dependencyRegistry.storageManager } returns storageManager
        every { dependencyRegistry.migration } returns migration
    }

    @After
    fun cleanUp() {
        unmockkAll()
    }

    @Test
    fun `creates P2pMatrix instance with defaults`() {
        val factory = P2pMatrix.Factory()
        val p2pMatrix = factory.create(dependencyRegistry)

        assertTrue(storageManager.hasPlugin<ExtendedP2pMatrixStoragePlugin>(), "Expected ExtendedP2pMatrixStoragePlugin to be registered.")

        verify(exactly = 1) { storageManager.addPlugins(any<SharedPreferencesMatrixStoragePlugin>()) }

        verify(exactly = 1) { dependencyRegistry.matrixClient(null) }
        verify(exactly = 1) { dependencyRegistry.p2pStore(null, BeaconP2pMatrixConfiguration.defaultNodes) }
    }

    @Test
    fun `creates P2pMatrix instance as builder function with defaults`() {
        val factory = p2pMatrix()
        val p2pMatrix = factory.create(dependencyRegistry)

        assertTrue(storageManager.hasPlugin<ExtendedP2pMatrixStoragePlugin>(), "Expected ExtendedP2pMatrixStoragePlugin to be registered.")

        verify(exactly = 1) { storageManager.addPlugins(any<SharedPreferencesMatrixStoragePlugin>()) }

        verify(exactly = 1) { dependencyRegistry.matrixClient(null) }
        verify(exactly = 1) { dependencyRegistry.p2pStore(null, BeaconP2pMatrixConfiguration.defaultNodes) }
    }

    @Test
    fun `creates P2pMatrix instance with custom storage plugin`() {
        val plugin = MockP2pMatrixStoragePlugin().extend()
        val factory = P2pMatrix.Factory(storagePlugin = plugin)
        val p2pMatrix = factory.create(dependencyRegistry)

        assertTrue(storageManager.hasPlugin<ExtendedP2pMatrixStoragePlugin>(), "Expected ExtendedP2pMatrixStoragePlugin to be registered.")

        verify(exactly = 1) { storageManager.addPlugins(plugin) }

        verify(exactly = 1) { dependencyRegistry.matrixClient(null) }
        verify(exactly = 1) { dependencyRegistry.p2pStore(null, BeaconP2pMatrixConfiguration.defaultNodes) }
    }

    @Test
    fun `creates P2pMatrix instance as builder function with custom storage plugin`() {
        val plugin = MockP2pMatrixStoragePlugin().extend()
        val factory = p2pMatrix(storagePlugin = plugin)
        val p2pMatrix = factory.create(dependencyRegistry)

        assertTrue(storageManager.hasPlugin<ExtendedP2pMatrixStoragePlugin>(), "Expected ExtendedP2pMatrixStoragePlugin to be registered.")

        verify(exactly = 1) { storageManager.addPlugins(plugin) }

        verify(exactly = 1) { dependencyRegistry.matrixClient(null) }
        verify(exactly = 1) { dependencyRegistry.p2pStore(null, BeaconP2pMatrixConfiguration.defaultNodes) }
    }

    @Test
    fun `creates P2pMatrix instance with custom nodes`() {
        val nodes = listOf("node1", "node2")
        val factory = P2pMatrix.Factory(matrixNodes = nodes)
        val p2pMatrix = factory.create(dependencyRegistry)

        assertTrue(storageManager.hasPlugin<ExtendedP2pMatrixStoragePlugin>(), "Expected ExtendedP2pMatrixStoragePlugin to be registered.")

        verify(exactly = 1) { dependencyRegistry.matrixClient(null) }
        verify(exactly = 1) { dependencyRegistry.p2pStore(null, nodes) }
    }

    @Test
    fun `creates P2pMatrix instance as builder function with custom nodes`() {
        val nodes = listOf("node1", "node2")
        val factory = p2pMatrix(matrixNodes = nodes)
        val p2pMatrix = factory.create(dependencyRegistry)

        assertTrue(storageManager.hasPlugin<ExtendedP2pMatrixStoragePlugin>(), "Expected ExtendedP2pMatrixStoragePlugin to be registered.")

        verify(exactly = 1) { dependencyRegistry.matrixClient(null) }
        verify(exactly = 1) { dependencyRegistry.p2pStore(null, nodes) }
    }

    @Test
    fun `creates P2pMatrix instance with custom HttpProvider`() {
        val httpProvider = mockkClass(HttpProvider::class)
        val factory = P2pMatrix.Factory(httpProvider = httpProvider)
        val p2pMatrix = factory.create(dependencyRegistry)

        assertTrue(storageManager.hasPlugin<ExtendedP2pMatrixStoragePlugin>(), "Expected ExtendedP2pMatrixStoragePlugin to be registered.")

        verify(exactly = 1) { dependencyRegistry.matrixClient(httpProvider) }
        verify(exactly = 1) { dependencyRegistry.p2pStore(httpProvider, BeaconP2pMatrixConfiguration.defaultNodes) }
    }

    @Test
    fun `creates P2pMatrix instance as builder function with custom HttpProvider`() {
        val httpProvider = mockkClass(HttpProvider::class)
        val factory = p2pMatrix(httpProvider = httpProvider)
        val p2pMatrix = factory.create(dependencyRegistry)

        assertTrue(storageManager.hasPlugin<ExtendedP2pMatrixStoragePlugin>(), "Expected ExtendedP2pMatrixStoragePlugin to be registered.")

        verify(exactly = 1) { dependencyRegistry.matrixClient(httpProvider) }
        verify(exactly = 1) { dependencyRegistry.p2pStore(httpProvider, BeaconP2pMatrixConfiguration.defaultNodes) }
    }
}