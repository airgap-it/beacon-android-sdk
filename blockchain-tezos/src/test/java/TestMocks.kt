import android.content.Context
import io.mockk.*
import it.airgap.beaconsdk.blockchain.tezos.Tezos
import it.airgap.beaconsdk.blockchain.tezos.internal.creator.*
import it.airgap.beaconsdk.blockchain.tezos.internal.di.ExtendedDependencyRegistry
import it.airgap.beaconsdk.blockchain.tezos.internal.di.extend
import it.airgap.beaconsdk.blockchain.tezos.internal.serializer.*
import it.airgap.beaconsdk.core.internal.BeaconSdk
import it.airgap.beaconsdk.core.internal.blockchain.BlockchainRegistry
import it.airgap.beaconsdk.core.internal.data.BeaconApplication
import it.airgap.beaconsdk.core.internal.di.DependencyRegistry
import it.airgap.beaconsdk.core.internal.utils.currentTimestamp

// -- class --

internal fun mockBeaconSdk(
    beaconId: String = "beaconId",
    app: BeaconApplication = mockkClass(BeaconApplication::class),
    dependencyRegistry: DependencyRegistry = mockk(relaxed = true),
): BeaconSdk =
    mockkClass(BeaconSdk::class).also {
        mockkObject(BeaconSdk)
        every { BeaconSdk.instance } returns it

        val contextMock = mockk<Context>(relaxed = true)

        coEvery { it.init(any(), any(), any(), any(), any()) } returns Unit

        every { it.applicationContext } returns contextMock
        every { it.app } returns app
        every { it.beaconId } returns beaconId
        every { it.dependencyRegistry } returns dependencyRegistry
    }

// -- static --

internal fun mockTime(currentTimeMillis: Long = 1) {
    mockkStatic("it.airgap.beaconsdk.core.internal.utils.TimeKt")
    every { currentTimestamp() } returns currentTimeMillis
}

internal fun mockDependencyRegistry(tezos: Tezos? = null): DependencyRegistry =
    mockkClass(DependencyRegistry::class).also {
        mockkStatic("it.airgap.beaconsdk.blockchain.tezos.internal.di.ExtendedDependencyRegistryKt")
        val extendedDependencyRegistry = mockkClass(ExtendedDependencyRegistry::class)
        every { it.extend() } returns extendedDependencyRegistry

        if (tezos != null) {
            val blockchainRegistry = mockkClass(BlockchainRegistry::class)

            every { blockchainRegistry.get(any()) } returns tezos
            every { blockchainRegistry.getOrNull(any()) } returns tezos
            every { it.blockchainRegistry } returns blockchainRegistry

            every { extendedDependencyRegistry.tezosWallet } returns tezos.wallet
            every { extendedDependencyRegistry.tezosCreator } returns tezos.creator as TezosCreator
            every { extendedDependencyRegistry.tezosSerializer } returns tezos.serializer as TezosSerializer
        }

        mockBeaconSdk(dependencyRegistry = it)
    }
