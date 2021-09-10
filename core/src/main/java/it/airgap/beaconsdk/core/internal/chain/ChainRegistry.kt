package it.airgap.beaconsdk.core.internal.chain

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.internal.utils.getAndDispose
import it.airgap.beaconsdk.core.internal.utils.getOrPutIfNotNull

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class ChainRegistry(factories: Map<String, () -> Chain<*, *>>) {
    private val factories: MutableMap<String, () -> Chain<*, *>> = factories.toMutableMap()
    private val chainMap: MutableMap<String, Chain<*, *>> = mutableMapOf()

    fun get(type: String): Chain<*, *>? = chainMap.getOrPutIfNotNull(type) {
        factories.getAndDispose(type)?.invoke()
    }
}