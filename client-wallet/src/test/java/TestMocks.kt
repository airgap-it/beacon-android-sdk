
import android.content.Context
import io.mockk.*
import it.airgap.beaconsdk.core.internal.BeaconSdk
import it.airgap.beaconsdk.core.internal.chain.ChainRegistry
import it.airgap.beaconsdk.core.internal.chain.MockChain
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

        coEvery { it.init(any(), any(), any(), any(), any(), any()) } returns Unit

        every { it.applicationContext } returns contextMock
        every { it.beaconId } returns beaconId
        every { it.dependencyRegistry } returns dependencyRegistry
    }

// -- static --

internal fun mockChainRegistry(): ChainRegistry =
    mockkClass(ChainRegistry::class).also {
        val dependencyRegistry = mockkClass(DependencyRegistry::class)

        every { it.get(any()) } returns MockChain()
        every { dependencyRegistry.chainRegistry } returns it

        mockBeaconSdk(dependencyRegistry = dependencyRegistry)
    }
