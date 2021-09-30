package it.airgap.beaconsdk.core.blockchain

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.data.BeaconError
import it.airgap.beaconsdk.core.data.Network
import it.airgap.beaconsdk.core.data.Permission
import it.airgap.beaconsdk.core.internal.di.DependencyRegistry
import it.airgap.beaconsdk.core.internal.message.v1.V1BeaconMessage
import it.airgap.beaconsdk.core.internal.message.v2.V2BeaconMessage
import it.airgap.beaconsdk.core.message.BeaconMessage
import it.airgap.beaconsdk.core.message.PermissionBeaconRequest
import it.airgap.beaconsdk.core.message.PermissionBeaconResponse
import kotlinx.serialization.KSerializer

public interface Blockchain {
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val identifier: String

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val wallet: Wallet

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val creator: Creator

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val serializer: Serializer

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public interface Wallet {
        public fun addressFromPublicKey(publicKey: String): Result<String>
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public interface Creator {

        // -- data --

        public suspend fun extractPermission(request: PermissionBeaconRequest, response: PermissionBeaconResponse): Result<Permission>

        // -- VersionedBeaconMessage --

        public fun v1From(senderId: String, content: BeaconMessage): Result<V1BeaconMessage>
        public fun v2From(senderId: String, content: BeaconMessage): Result<V2BeaconMessage>
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public interface Serializer {

        // -- data --

        public val network: KSerializer<Network>
        public val permission: KSerializer<Permission>
        public val error: KSerializer<BeaconError>

        // -- VersionedBeaconMessage --

        public val v1: KSerializer<V1BeaconMessage>
        public val v2: KSerializer<V2BeaconMessage>
    }

    public interface Factory<T : Blockchain> {
        public val identifier: String

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun create(dependencyRegistry: DependencyRegistry): T
    }
}
