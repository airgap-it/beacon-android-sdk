package it.airgap.beaconsdk.core.internal.storage

import it.airgap.beaconsdk.core.data.AppMetadata
import it.airgap.beaconsdk.core.data.Maybe
import it.airgap.beaconsdk.core.data.Peer
import it.airgap.beaconsdk.core.data.Permission
import it.airgap.beaconsdk.core.storage.Storage

public class MockStorage : Storage {
    private var p2pPeers: List<Peer> = emptyList()
    private var appsMetadata: List<AppMetadata> = emptyList()
    private var permissions: List<Permission> = emptyList()
    private var sdkVersion: String? = null
    private var migrations: Set<String> = emptySet()

    override suspend fun getMaybePeers(): List<Maybe<Peer>> = p2pPeers.map { Maybe.Some(it) }
    override suspend fun setPeers(p2pPeers: List<Peer>) {
        this.p2pPeers = p2pPeers
    }

    override suspend fun getMaybeAppMetadata(): List<Maybe<AppMetadata>> = appsMetadata.map { Maybe.Some(it) }
    override suspend fun setAppMetadata(appMetadata: List<AppMetadata>) {
        this.appsMetadata = appMetadata
    }

    override suspend fun getMaybePermissions(): List<Maybe<Permission>> = permissions.map { Maybe.Some(it) }
    override suspend fun setPermissions(permissions: List<Permission>) {
        this.permissions = permissions
    }

    override suspend fun getSdkVersion(): String? = sdkVersion
    override suspend fun setSdkVersion(sdkVersion: String) {
        this.sdkVersion = sdkVersion
    }

    override suspend fun getMigrations(): Set<String> = migrations
    override suspend fun setMigrations(migrations: Set<String>) {
        this.migrations = migrations
    }
}