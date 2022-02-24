
import android.content.Context
import io.mockk.*
import it.airgap.beaconsdk.core.internal.BeaconSdk
import it.airgap.beaconsdk.core.internal.blockchain.BlockchainRegistry
import it.airgap.beaconsdk.core.internal.blockchain.MockBlockchain
import it.airgap.beaconsdk.core.internal.data.BeaconApplication
import it.airgap.beaconsdk.core.internal.di.DependencyRegistry
import it.airgap.beaconsdk.core.internal.utils.currentTimestamp
import it.airgap.beaconsdk.core.internal.utils.logDebug
import it.airgap.beaconsdk.core.internal.utils.logError
import it.airgap.beaconsdk.core.internal.utils.logInfo

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

internal fun mockDependencyRegistry(): DependencyRegistry =
    mockkClass(DependencyRegistry::class).also {
        val blockchainRegistry = mockkClass(BlockchainRegistry::class)
        val mockBlockchain = MockBlockchain()

        every { blockchainRegistry.get(any()) } returns mockBlockchain
        every { blockchainRegistry.getOrNull(any()) } returns mockBlockchain
        every { it.blockchainRegistry } returns blockchainRegistry

        mockBeaconSdk(dependencyRegistry = it)
    }

internal fun mockBlockchainRegistry(): BlockchainRegistry =
    mockkClass(BlockchainRegistry::class).also {
        val dependencyRegistry = mockkClass(DependencyRegistry::class)
        val mockBlockchain = MockBlockchain()

        every { it.get(any()) } returns mockBlockchain
        every { it.getOrNull(any()) } returns mockBlockchain
        every { dependencyRegistry.blockchainRegistry } returns it

        mockBeaconSdk(dependencyRegistry = dependencyRegistry)
    }

internal fun mockLog() {
    mockkStatic("it.airgap.beaconsdk.core.internal.utils.LogKt")

    every { logInfo(any(), any()) } answers {
        println("[INFO] ${firstArg<String>()}: ${secondArg<String>()}")
    }

    every { logDebug(any(), any()) } answers {
        println("[DEBUG] ${firstArg<String>()}: ${secondArg<String>()}")
    }

    every { logError(any(), any()) } answers {
        println("[ERROR] ${firstArg<String>()}: ${secondArg<Throwable>()}")
    }
}

internal fun mockTime(currentTimeMillis: Long = 1) {
    mockkStatic("it.airgap.beaconsdk.core.internal.utils.TimeKt")
    every { currentTimestamp() } returns currentTimeMillis
}
