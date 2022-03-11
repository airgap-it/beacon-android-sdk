package it.airgap.beaconsdk.blockchain.tezos.message.response

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.blockchain.tezos.data.TezosAccount
import it.airgap.beaconsdk.blockchain.tezos.data.TezosPermission
import it.airgap.beaconsdk.blockchain.tezos.message.request.PermissionTezosRequest
import it.airgap.beaconsdk.core.data.Origin
import it.airgap.beaconsdk.core.exception.BeaconException
import it.airgap.beaconsdk.core.message.PermissionBeaconResponse

/**
 * Message responding to [PermissionTezosRequest].
 *
 * @property [id] The value that identifies the request to which the message is responding.
 * @property [version] The message version.
 * @property [requestOrigin] The origination data of the request.
 * @property [blockchainIdentifier] The unique name of the blockchain that specifies the request.
 * @property [account] The account that is granting the permissions.
 * @property [scopes] The list of granted permissions.
 */
public data class PermissionTezosResponse internal constructor(
    override val id: String,
    override val version: String,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) override val requestOrigin: Origin,
    override val blockchainIdentifier: String,
    public val account: TezosAccount,
    public val scopes: List<TezosPermission.Scope>,
) : PermissionBeaconResponse() {

    public companion object {

        /**
         * Creates a new instance of [PermissionTezosResponse] from the [request]
         * with the specified [account] and optional [scopes].
         *
         * The response will have an id matching the one of the [request].
         * If no custom [scopes] are provided, the values will be also taken from the [request].
         */
        @Throws(BeaconException::class)
        public fun from(
            request: PermissionTezosRequest,
            account: TezosAccount,
            scopes: List<TezosPermission.Scope> = request.scopes,
        ): PermissionTezosResponse =
            PermissionTezosResponse(
                request.id,
                request.version,
                request.origin,
                request.blockchainIdentifier,
                account,
                scopes,
            )
    }
}