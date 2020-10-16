package it.airgap.beaconsdk.compat.client

import it.airgap.beaconsdk.client.BeaconClient
import it.airgap.beaconsdk.exception.BeaconException
import it.airgap.beaconsdk.message.BeaconMessage
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

interface InitCallback {
    fun onSuccess()
    fun onError(error: Throwable)
}

interface OnNewMessageListener {
    fun onNewMessage(message: BeaconMessage.Request)
    fun onError(error: Throwable)
}

interface ResponseCallback {
    fun onSuccess()
    fun onError(error: Throwable)
}

fun BeaconClient.init(callback: InitCallback) {
    initScope {
        init()
        callback.onSuccess()
    }
}

fun BeaconClient.connect(listener: OnNewMessageListener) {
    receiveScope {
        try {
            connect().collect {
                when {
                    it.isSuccess -> listener.onNewMessage(it.getOrThrow())
                    it.isFailure -> listener.onError(it.exceptionOrNull() ?: BeaconException.Unknown())
                }
            }
        } catch (e: Exception) {
            listener.onError(e)
        }
    }
}

fun BeaconClient.respond(message: BeaconMessage.Response, callback: ResponseCallback) {
    sendScope {
        try {
            respond(message)
            callback.onSuccess()
        } catch (e: Exception) {
            callback.onError(e)
        }
    }
}

private val jobs: MutableMap<JobType, CompletableJob> = mutableMapOf()

private enum class JobType {
    Init,
    Receive,
    Send,
}

private fun initScope(block: suspend () -> Unit) {
    jobScope(JobType.Init, block)
}

private fun receiveScope(block: suspend () -> Unit) {
    jobScope(JobType.Receive, block)
}

private fun sendScope(block: suspend () -> Unit) {
    jobScope(JobType.Send, block)
}

private fun jobScope(type: JobType, block: suspend () -> Unit) {
    val job = jobs.getOrPut(type) { Job() }
    CoroutineScope(job).launch {
        block()
        job.complete()
        jobs.remove(type)
    }
}