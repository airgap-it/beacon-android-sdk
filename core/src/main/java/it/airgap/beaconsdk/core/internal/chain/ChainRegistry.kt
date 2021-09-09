package it.airgap.beaconsdk.core.internal.chain

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.internal.utils.getOrPutIfNotNull

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class ChainRegistry(private val factories: Map<String, () -> Chain<*, *>>) {
    private val chainMap: MutableMap<String, Chain<*, *>> = mutableMapOf()

    fun get(type: String): Chain<*, *>? = chainMap.getOrPutIfNotNull(type) { factories[type]?.invoke() }
}