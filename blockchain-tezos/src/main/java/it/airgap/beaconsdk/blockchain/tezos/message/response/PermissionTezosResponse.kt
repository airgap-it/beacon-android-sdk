package it.airgap.beaconsdk.blockchain.tezos.message.response

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.blockchain.tezos.data.TezosAccount
import it.airgap.beaconsdk.blockchain.tezos.data.TezosAppMetadata
import it.airgap.beaconsdk.blockchain.tezos.data.TezosNotification
import it.airgap.beaconsdk.blockchain.tezos.data.TezosPermission
import it.airgap.beaconsdk.blockchain.tezos.data.TezosThreshold
import it.airgap.beaconsdk.blockchain.tezos.extension.ownAppMetadata
import it.airgap.beaconsdk.blockchain.tezos.message.request.PermissionTezosRequest
import it.airgap.beaconsdk.core.client.BeaconClient
import it.airgap.beaconsdk.core.client.BeaconConsumer
import it.airgap.beaconsdk.core.data.Connection
import it.airgap.beaconsdk.core.exception.BeaconException
import it.airgap.beaconsdk.core.message.PermissionBeaconResponse

/**
 * Message responding to [PermissionTezosRequest].
 *
 * @property [id] The value that identifies the request to which the message is responding.
 * @property [version] The message version.
 * @property [destination] The origination data of the request.
 * @property [blockchainIdentifier] The unique name of the blockchain that specifies the request.
 * @property [account] The account that is granting the permissions.
 * @property [scopes] The list of granted permissions.
 */
public data class PermissionTezosResponse internal constructor(
    override val id: String,
    override val version: String,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) override val destination: Connection.Id,
    override val blockchainIdentifier: String,
    public val account: TezosAccount,
    public val scopes: List<TezosPermission.Scope>,
    public val appMetadata: TezosAppMetadata?,
    public val threshold: TezosThreshold?,
    public val notification: TezosNotification?
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
            threshold: TezosThreshold? = null,
            notification: TezosNotification? = null
        ): PermissionTezosResponse =
            PermissionTezosResponse(
                request.id,
                request.version,
                request.origin,
                request.blockchainIdentifier,
                account,
                scopes,
                null,
                threshold,
                notification
            )

        /**
         * Creates a new instance of [PermissionTezosResponse] from the [request]
         * with the specified [account] and optional [scopes]. The [appMetadata] is provided
         * by the [consumer].
         *
         * The response will have an id matching the one of the [request].
         * If no custom [scopes] are provided, the values will be also taken from the [request].
         */
        @Throws(BeaconException::class)
        public fun <T> from(
            request: PermissionTezosRequest,
            account: TezosAccount,
            consumer: T,
            scopes: List<TezosPermission.Scope> = request.scopes,
            threshold: TezosThreshold? = null,
            notification: TezosNotification? = null
        ): PermissionTezosResponse where T : BeaconConsumer, T : BeaconClient<*> =
            PermissionTezosResponse(
                request.id,
                request.version,
                request.origin,
                request.blockchainIdentifier,
                account,
                scopes,
                consumer.ownAppMetadata(),
                threshold,
                notification
            )
    }
}