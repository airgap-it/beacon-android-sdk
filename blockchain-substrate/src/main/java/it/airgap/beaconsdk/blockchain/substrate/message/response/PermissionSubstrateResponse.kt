package it.airgap.beaconsdk.blockchain.substrate.message.response

import it.airgap.beaconsdk.blockchain.substrate.data.SubstrateAccount
import it.airgap.beaconsdk.blockchain.substrate.data.SubstrateAppMetadata
import it.airgap.beaconsdk.blockchain.substrate.data.SubstratePermission
import it.airgap.beaconsdk.blockchain.substrate.extensions.ownAppMetadata
import it.airgap.beaconsdk.blockchain.substrate.message.request.PermissionSubstrateRequest
import it.airgap.beaconsdk.core.client.BeaconClient
import it.airgap.beaconsdk.core.client.BeaconConsumer
import it.airgap.beaconsdk.core.data.Connection
import it.airgap.beaconsdk.core.message.PermissionBeaconResponse

public data class PermissionSubstrateResponse internal constructor(
    override val id: String,
    override val version: String,
    override val destination: Connection.Id,
    override val blockchainIdentifier: String,
    public val appMetadata: SubstrateAppMetadata,
    public val scopes: List<SubstratePermission.Scope>,
    public val accounts: List<SubstrateAccount>,
) : PermissionBeaconResponse() {
    public companion object {

        /**
         * Creates a new instance of [PermissionSubstrateResponse] from the [request]
         * with the specified [accounts], [appMetadata] and optional [scopes].
         *
         * The response will have an id matching the one of the [request].
         * If no custom [scopes] are provided, the values will be also taken from the [request].
         */
        public fun from(
            request: PermissionSubstrateRequest,
            accounts: List<SubstrateAccount>,
            appMetadata: SubstrateAppMetadata,
            scopes: List<SubstratePermission.Scope> = request.scopes,
        ): PermissionSubstrateResponse =
            PermissionSubstrateResponse(
                request.id,
                request.version,
                request.origin,
                request.blockchainIdentifier,
                appMetadata,
                scopes,
                accounts,
            )

        /**
         * Creates a new instance of [PermissionSubstrateResponse] from the [request]
         * with the specified [accounts] and optional [scopes]. The [appMetadata] is provided by
         * the [consumer].
         *
         * The response will have an id matching the one of the [request].
         * If no custom [scopes] are provided, the values will be also taken from the [request].
         */
        public fun <T> from(
            request: PermissionSubstrateRequest,
            accounts: List<SubstrateAccount>,
            consumer: T,
            scopes: List<SubstratePermission.Scope> = request.scopes,
        ): PermissionSubstrateResponse where T : BeaconConsumer, T : BeaconClient<*> =
            PermissionSubstrateResponse(
                request.id,
                request.version,
                request.origin,
                request.blockchainIdentifier,
                consumer.ownAppMetadata(),
                scopes,
                accounts,
            )
    }
}
