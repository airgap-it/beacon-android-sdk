package it.airgap.beaconsdkdemo.utils

import androidx.lifecycle.MutableLiveData

fun <T> MutableLiveData<T>.setValue(default: T, update: T.() -> T) {
    val state = value ?: default
    value = state.update()
}