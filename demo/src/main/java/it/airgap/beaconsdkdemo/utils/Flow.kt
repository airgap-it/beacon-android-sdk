package it.airgap.beaconsdkdemo.utils

import kotlinx.coroutines.flow.MutableStateFlow

fun <T> MutableStateFlow<T>.emit(default: T, update: T.() -> T) {
    val state = value ?: default
    value = state.update()
}