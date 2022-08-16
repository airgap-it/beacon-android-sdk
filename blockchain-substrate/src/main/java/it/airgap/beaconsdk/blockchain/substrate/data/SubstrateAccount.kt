package it.airgap.beaconsdk.blockchain.substrate.data

import it.airgap.beaconsdk.core.client.BeaconClient
import it.airgap.beaconsdk.core.internal.utils.dependencyRegistry
import it.airgap.beaconsdk.core.scope.BeaconScope
import kotlinx.serialization.Serializable

/**
 * Substrate account data.
 *
 * @property [accountId] The value that identifies the account in Beacon.
 * @property [network] The network on which the account is valid. Can be omitted.
 * @property [publicKey] The public key that belongs to the account.
 * @property [address] The account address.
 */
@Serializable
public data class SubstrateAccount internal constructor(
    public val accountId: String,
    public val network: SubstrateNetwork?,
    public val publicKey: String,
    public val address: String,
) {
    public companion object {}
}

/**
 * Creates a new instance of [SubstrateAccount] with the specified [publicKey], [address] and optional [network].
 */
public fun SubstrateAccount(publicKey: String, address: String, network: SubstrateNetwork? = null, beaconScope: BeaconScope): SubstrateAccount {
    val accountId = dependencyRegistry(beaconScope).identifierCreator.accountId(address, network).getOrThrow()
    return SubstrateAccount(accountId, network, publicKey, address)
}

/**
 * Creates a new instance of [SubstrateAccount] with the specified [publicKey], [address] and optional [network].
 */
public fun SubstrateAccount(publicKey: String, address: String, network: SubstrateNetwork? = null, client: BeaconClient<*>): SubstrateAccount {
    val accountId = dependencyRegistry(client.beaconScope).identifierCreator.accountId(address, network).getOrThrow()
    return SubstrateAccount(accountId, network, publicKey, address)
}

/**
 * Creates a new instance of [SubstrateAccount] with the specified [publicKey], [address] and optional [network].
 */
public fun BeaconClient<*>.SubstrateAccount(publicKey: String, address: String, network: SubstrateNetwork? = null): SubstrateAccount {
    val accountId = dependencyRegistry(beaconScope).identifierCreator.accountId(address, network).getOrThrow()
    return SubstrateAccount(accountId, network, publicKey, address)
}
