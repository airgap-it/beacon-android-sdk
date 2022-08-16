package it.airgap.beaconsdk.core.internal.utils

import android.content.Context
import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.internal.BeaconSdk
import it.airgap.beaconsdk.core.internal.blockchain.BlockchainRegistry
import it.airgap.beaconsdk.core.internal.compat.Compat
import it.airgap.beaconsdk.core.internal.compat.CoreCompat
import it.airgap.beaconsdk.core.internal.compat.VersionedCompat
import it.airgap.beaconsdk.core.internal.data.BeaconApplication
import it.airgap.beaconsdk.core.internal.di.DependencyRegistry
import it.airgap.beaconsdk.core.scope.BeaconScope
import kotlinx.serialization.json.Json

@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public val beaconSdk: BeaconSdk
    get() = BeaconSdk.instance

@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public val applicationContext: Context
    get() = beaconSdk.applicationContext

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun dependencyRegistry(beaconScope: BeaconScope): DependencyRegistry =
    beaconSdk.dependencyRegistry(beaconScope)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun app(beaconScope: BeaconScope): BeaconApplication =
    beaconSdk.app(beaconScope)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun blockchainRegistry(beaconScope: BeaconScope? = null): BlockchainRegistry =
    beaconScope?.let { dependencyRegistry(beaconScope).blockchainRegistry } ?: beaconSdk.beaconScopes.fold(BlockchainRegistry(emptyMap())) { acc, next ->
        val blockchainRegistry = dependencyRegistry(next).blockchainRegistry
        BlockchainRegistry(acc.factories + blockchainRegistry.factories, acc.blockchains + blockchainRegistry.blockchains)
    }

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun compat(beaconScope: BeaconScope? = null): Compat<VersionedCompat> =
    beaconScope?.let { dependencyRegistry(it).compat } ?: CoreCompat()

public fun json(beaconScope: BeaconScope? = null): Json =
    beaconScope?.let { dependencyRegistry(it).json } ?: Json.Default