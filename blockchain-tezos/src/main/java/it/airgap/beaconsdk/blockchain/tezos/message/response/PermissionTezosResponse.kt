package it.airgap.beaconsdk.blockchain.tezos.message.response

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.blockchain.tezos.data.TezosNetwork
import it.airgap.beaconsdk.blockchain.tezos.data.TezosPermission
import it.airgap.beaconsdk.blockchain.tezos.message.request.PermissionTezosRequest
import it.airgap.beaconsdk.core.data.Origin
import it.airgap.beaconsdk.core.data.Threshold
import it.airgap.beaconsdk.core.message.PermissionBeaconResponse

/**
 * Message responding to [PermissionTezosRequest].
 *
 * @property [id] The value that identifies the request to which the message is responding.
 * @property [version] The message version.
 * @property [requestOrigin] The origination data of the request.
 * @property [blockchainIdentifier] The unique name of the blockchain that specifies the request.
 * @property [publicKey] The public key of the account that is granting the permissions.
 * @property [network] The network to which the permissions apply.
 * @property [scopes] The list of granted permissions.
 * @property [threshold] An optional threshold configuration.
 */
public data class PermissionTezosResponse internal constructor(
    override val id: String,
    override val version: String,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) override val requestOrigin: Origin,
    override val blockchainIdentifier: String,
    override val publicKey: String,
    public val network: TezosNetwork,
    public val scopes: List<TezosPermission.Scope>,
    override val threshold: Threshold? = null,
) : PermissionBeaconResponse() {

    public companion object {

        /**
         * Creates a new instance of [PermissionTezosResponse] from the [request]
         * with the specified [publicKey] and optional [network], [scopes] and [threshold].
         *
         * The response will have an id matching the one of the [request].
         * If no custom [network] and [scopes] are provided, the values will be also taken from the [request].
         * By default [threshold] is set to `null`.
         */
        public fun from(
            request: PermissionTezosRequest,
            publicKey: String,
            network: TezosNetwork = request.network,
            scopes: List<TezosPermission.Scope> = request.scopes,
            threshold: Threshold? = null,
        ): PermissionTezosResponse =
            PermissionTezosResponse(request.id, request.version, request.origin, request.blockchainIdentifier, publicKey, network, scopes, threshold)
    }
}