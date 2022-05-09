package com.example.sample_plugin

import io.flutter.plugin.common.EventChannel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import timber.log.Timber

class SharedFlowStreamHandler<T>(
    private val coroutineScope: CoroutineScope,
    private val sharedFlow: SharedFlow<T>,
) : EventChannel.StreamHandler {

    constructor(
        coroutineScope: CoroutineScope, flow: Flow<T>, started: SharingStarted, replay: Int = 0,
    ) : this(
        coroutineScope, flow.shareIn(coroutineScope, started, replay)
    )

    private var collectJob: Job? = null

    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
        Timber.d("onListen: arguments = $arguments")
        collectJob = coroutineScope.launch(Dispatchers.Main) {
            try {
                sharedFlow.collect {
                    events?.success(it)
                }
            } catch (e: CancellationException) {
                events?.endOfStream()
            }
        }
    }

    override fun onCancel(arguments: Any?) {
        Timber.d("onCancel: arguments = $arguments")
        collectJob?.cancel()
    }
}
