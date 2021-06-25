package it.airgap.beaconsdk.internal.storage

import it.airgap.beaconsdk.data.beacon.AppMetadata
import it.airgap.beaconsdk.data.beacon.Peer
import it.airgap.beaconsdk.data.beacon.Permission
import it.airgap.beaconsdk.data.beacon.selfRemoved
import it.airgap.beaconsdk.internal.utils.AccountUtils
import it.airgap.beaconsdk.internal.utils.HexString
import it.airgap.beaconsdk.internal.utils.tryResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlin.reflect.KProperty1

internal class StorageManager(
    private val storage: ExtendedStorage,
    private val secureStorage: SecureStorage,
    private val accountUtils: AccountUtils
) : ExtendedStorage by storage, SecureStorage by secureStorage {

    constructor(
        storage: Storage,
        secureStorage: SecureStorage,
        accountUtils: AccountUtils
    ) : this(storage.extend(), secureStorage, accountUtils)

    private val removedPeers: MutableSharedFlow<Peer> by lazy { MutableSharedFlow(extraBufferCapacity = 64) }
    val updatedPeers: Flow<Peer>
        get() = merge(storage.peers, removedPeers)

    suspend inline fun <reified T: Peer> updatePeers(
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

    suspend inline fun <reified T : Peer> findInstancePeer(noinline predicate: (T) -> Boolean): T? =
        storage.findPeer(T::class.java, predicate)

    suspend fun removePeers(peers: List<Peer>) {
        removePeers { peer -> peers.any { it.publicKey == peer.publicKey } }
    }

    override suspend fun removePeers(predicate: ((Peer) -> Boolean)?) {
        tryResult {
            val peers = storage.getPeers()
            val toRemove = predicate?.let { peers.filter(it) } ?: peers

            storage.removePeers(predicate)
            removePermissions { permission ->
                toRemove.any {
                    val senderId = accountUtils.getSenderId(HexString.fromString(it.publicKey)).get()
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

    suspend fun removeAppMetadata(appsMetadata: List<AppMetadata>) {
        removeAppMetadata { metadata -> appsMetadata.any { it.senderId == metadata.senderId } }
    }

    suspend fun removePermissions(permissions: List<Permission>) {
        removePermissions { permission -> permissions.any { it == permission } }
    }
}