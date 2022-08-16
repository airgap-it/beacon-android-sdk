
import android.content.Context
import io.mockk.*
import it.airgap.beaconsdk.core.internal.BeaconSdk
import it.airgap.beaconsdk.core.internal.blockchain.BlockchainRegistry
import it.airgap.beaconsdk.core.internal.blockchain.MockBlockchain
import it.airgap.beaconsdk.core.internal.compat.CoreCompat
import it.airgap.beaconsdk.core.internal.crypto.data.KeyPair
import it.airgap.beaconsdk.core.internal.data.BeaconApplication
import it.airgap.beaconsdk.core.internal.di.DependencyRegistry
import it.airgap.beaconsdk.core.scope.BeaconScope

// -- class --

internal fun mockBeaconSdk(
    app: BeaconApplication = BeaconApplication(KeyPair(byteArrayOf(0), byteArrayOf(0)), "mockApp"),
    beaconId: String = "beaconId",
    dependencyRegistry: DependencyRegistry = mockk(relaxed = true),
): BeaconSdk =
    mockkClass(BeaconSdk::class).also {
        mockkObject(BeaconSdk)
        every { BeaconSdk.instance } returns it

        val contextMock = mockk<Context>(relaxed = true)

        coEvery { it.add(any(), any(), any(), any(), any(), any()) } returns Unit

        every { it.applicationContext } returns contextMock
        every { it.app(any()) } returns app
        every { it.beaconId(any()) } returns beaconId
        every { it.dependencyRegistry(any()) } returns dependencyRegistry
    }

// -- static --

internal fun mockDependencyRegistry(beaconScope: BeaconScope? = null): DependencyRegistry =
    mockkClass(DependencyRegistry::class).also {
        val blockchainRegistry = mockkClass(BlockchainRegistry::class)
        val mockBlockchain = MockBlockchain()

        every { blockchainRegistry.get(any()) } returns mockBlockchain
        every { blockchainRegistry.getOrNull(any()) } returns mockBlockchain
        every { it.blockchainRegistry } returns blockchainRegistry

        every { it.compat } returns CoreCompat(beaconScope)

        mockBeaconSdk(dependencyRegistry = it)
    }
