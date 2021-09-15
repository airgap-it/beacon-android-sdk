package it.airgap.beaconsdk.core.internal.storage

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.data.AppMetadata
import it.airgap.beaconsdk.core.data.Peer
import it.airgap.beaconsdk.core.data.Permission
import it.airgap.beaconsdk.core.data.selfRemoved
import it.airgap.beaconsdk.core.internal.utils.AccountUtils
import it.airgap.beaconsdk.core.internal.utils.asHexString
import it.airgap.beaconsdk.core.storage.ExtendedStorage
import it.airgap.beaconsdk.core.storage.SecureStorage
import it.airgap.beaconsdk.core.storage.Storage
import it.airgap.beaconsdk.core.storage.StoragePlugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlin.reflect.KProperty1

@OptIn(ExperimentalCoroutinesApi::class)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class StorageManager(
    @PublishedApi
    internal val storage: ExtendedStorage,

    @PublishedApi
    internal val secureStorage: SecureStorage,

    private val accountUtils: AccountUtils
) : ExtendedStorage by storage, SecureStorage by secureStorage {

    private val _plugins: MutableList<StoragePlugin> = mutableListOf()
    @PublishedApi
    internal val plugins: List<StoragePlugin> get() = _plugins

    public constructor(
        storage: Storage,
        secureStorage: SecureStorage,
        accountUtils: AccountUtils
    ) : this(storage.extend(), secureStorage, accountUtils)

    public fun addPlugins(plugins: List<StoragePlugin>) {
        _plugins.addAll(plugins)
    }

    public fun addPlugins(vararg plugins: StoragePlugin) {
        addPlugins(plugins.toList())
    }

    public inline fun <reified T : StoragePlugin> getPlugin(): T? = plugins.filterIsInstance<T>().firstOrNull()

    public inline fun <reified T : StoragePlugin> hasPlugin(): Boolean = plugins.any { it is T }

    private val removedPeers: MutableSharedFlow<Peer> by lazy { MutableSharedFlow(extraBufferCapacity = 64) }
    public val updatedPeers: Flow<Peer>
        get() = merge(storage.peers, removedPeers)

    public suspend inline fun <reified T: Peer> updatePeers(
        peers: List<T>,
        compareWith: List<KProperty1<T, *>>
    ) {
        addPeers(peers, overwrite = true) { a, b ->
            when {
                a is T && b is T -> compareWith.fold(true) { acc, next -> acc && next(a) == next(b) }
                else -> false
            }
        }
    }

    public suspend inline fun <reified T : Peer> findInstancePeer(noinline predicate: (T) -> Boolean): T? =
        storage.findPeer(T::class.java, predicate)

    public suspend fun removePeers(peers: List<Peer>) {
        removePeers { peer -> peers.any { it.publicKey == peer.publicKey } }
    }

    override suspend fun removePeers(predicate: ((Peer) -> Boolean)?) {
        runCatching {
            val peers = storage.getPeers()
            val toRemove = predicate?.let { peers.filter(it) } ?: peers

            storage.removePeers(predicate)
            removePermissions { permission ->
                toRemove.any {
                    val senderId = accountUtils.getSenderId(it.publicKey.asHexString().toByteArray()).getOrThrow()
                    senderId == permission.appMetadata.senderId
                }
            }

            CoroutineScope(Dispatchers.Default).launch {
                toRemove.map { it.selfRemoved() }.forEach {
                    removedPeers.tryEmit(it)
                }
            }
        }
    }

    public suspend fun removeAppMetadata(appsMetadata: List<AppMetadata>) {
        removeAppMetadata { metadata -> appsMetadata.any { it.senderId == metadata.senderId } }
    }

    public suspend fun removePermissions(permissions: List<Permission>) {
        removePermissions { permission -> permissions.any { it == permission } }
    }
}