
import android.content.Context
import io.mockk.*
import it.airgap.beaconsdk.core.internal.BeaconSdk
import it.airgap.beaconsdk.core.internal.blockchain.BlockchainRegistry
import it.airgap.beaconsdk.core.internal.blockchain.MockBlockchain
import it.airgap.beaconsdk.core.internal.di.DependencyRegistry

// -- class --

internal fun mockBeaconSdk(
    beaconId: String = "beaconId",
    dependencyRegistry: DependencyRegistry = mockk(relaxed = true),
): BeaconSdk =
    mockkClass(BeaconSdk::class).also {
        mockkObject(BeaconSdk)
        every { BeaconSdk.instance } returns it

        val contextMock = mockk<Context>(relaxed = true)

        coEvery { it.init(any(), any(), any(), any(), any()) } returns Unit

        every { it.applicationContext } returns contextMock
        every { it.beaconId } returns beaconId
        every { it.dependencyRegistry } returns dependencyRegistry
    }

// -- static --

internal fun mockDependencyRegistry(): DependencyRegistry =
    mockkClass(DependencyRegistry::class).also {
        val blockchainRegistry = mockkClass(BlockchainRegistry::class)
        val mockBlockchain = MockBlockchain()

        every { blockchainRegistry.get(any()) } returns mockBlockchain
        every { blockchainRegistry.getOrNull(any()) } returns mockBlockchain
        every { it.blockchainRegistry } returns blockchainRegistry

        mockBeaconSdk(dependencyRegistry = it)
    }
