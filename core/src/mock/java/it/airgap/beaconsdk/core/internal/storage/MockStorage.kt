package it.airgap.beaconsdk.core.internal.storage

import it.airgap.beaconsdk.core.data.AppMetadata
import it.airgap.beaconsdk.core.data.Maybe
import it.airgap.beaconsdk.core.data.Peer
import it.airgap.beaconsdk.core.data.Permission
import it.airgap.beaconsdk.core.scope.BeaconScope
import it.airgap.beaconsdk.core.storage.Storage
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

public class MockStorage : Storage {
    private val mutex: Mutex = Mutex()

    private var p2pPeers: List<Peer> = emptyList()
    private var appsMetadata: List<AppMetadata> = emptyList()
    private var permissions: List<Permission> = emptyList()
    private var sdkVersion: String? = null
    private var migrations: Set<String> = emptySet()

    override suspend fun getMaybePeers(): List<Maybe<Peer>> = mutex.withLock { p2pPeers.map { Maybe.Some(it) } }
    override suspend fun setPeers(p2pPeers: List<Peer>) {
        mutex.withLock {
            this.p2pPeers = p2pPeers
        }
    }

    override suspend fun getMaybeAppMetadata(): List<Maybe<AppMetadata>> = mutex.withLock { appsMetadata.map { Maybe.Some(it) } }
    override suspend fun setAppMetadata(appMetadata: List<AppMetadata>) {
        mutex.withLock {
            this.appsMetadata = appMetadata
        }
    }

    override suspend fun getMaybePermissions(): List<Maybe<Permission>> = mutex.withLock { permissions.map { Maybe.Some(it) } }
    override suspend fun setPermissions(permissions: List<Permission>) {
        mutex.withLock {
            this.permissions = permissions
        }
    }

    override suspend fun getSdkVersion(): String? = mutex.withLock { sdkVersion }
    override suspend fun setSdkVersion(sdkVersion: String) {
        mutex.withLock {
            this.sdkVersion = sdkVersion
        }
    }

    override suspend fun getMigrations(): Set<String> = mutex.withLock { migrations }
    override suspend fun setMigrations(migrations: Set<String>) {
        mutex.withLock {
            this.migrations = migrations
        }
    }

    override fun scoped(beaconScope: BeaconScope): Storage = this
}