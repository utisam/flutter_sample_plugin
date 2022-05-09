package com.example.sample_plugin

import androidx.annotation.NonNull
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.embedding.engine.plugins.lifecycle.FlutterLifecycleAdapter
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import timber.log.Timber


/** SamplePlugin */
class SamplePlugin : FlutterPlugin, MethodCallHandler, ActivityAware {

    private lateinit var pluginComponent: SamplePluginComponent

    private var _pluginScope: CoroutineScope? = null
    private val pluginScope get() = requireNotNull(_pluginScope) { "Plugin is not attached" }

    private lateinit var eventChannel: EventChannel
    private lateinit var methodChannel: MethodChannel

    private var _lifecycle: Lifecycle? = null
    private val lifecycle get() = requireNotNull(_lifecycle) { "Activity is not attached" }

    override fun onAttachedToEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        Timber.i("onAttachedToEngine")

        pluginComponent = DaggerSamplePluginComponent.create()

        _pluginScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        eventChannel = EventChannel(binding.binaryMessenger, "com.example.sample_plugin/sample")
            .apply {
                setStreamHandler(SharedFlowStreamHandler(pluginScope, flow {
                    repeat(100) { i ->
                        emit(i)
                        delay(100)
                    }
                }, SharingStarted.Eagerly))
            }

        methodChannel = MethodChannel(binding.binaryMessenger, "com.example.sample_plugin")
        methodChannel.setMethodCallHandler(this)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        Timber.i("onAttachedToActivity")
        onReattachedToActivityForConfigChanges(binding)
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        Timber.i("onReattachedToActivityForConfigChanges")
        // ForConfigChanges はバックグラウンドからの復帰や画面の回転で Activity を再作成する際に呼び出される。
        // 入力途中のデータが消えたりすると困るので、内部状態は初期化しないように維持すべき。
        // _activity = binding.activity
        _lifecycle = FlutterLifecycleAdapter.getActivityLifecycle(binding)
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        // 全ての Method は LifecycleScope で実行する。
        // Dispatchers.Main.immediate で起動して Destroyed で Cancel される。
        // lifecycle.repeatOnLifecycle で状態遷移に応じて停止・再起動できる。
        lifecycle.coroutineScope.launch {
            try {
                result.success(invokeMethod(call))
            } catch (e: NotImplementedError) {
                result.notImplemented()
            } catch (e: Throwable) {
                result.error(e.javaClass.simpleName, e.message, null)
            }
        }
    }

    override fun onDetachedFromActivityForConfigChanges() {
        Timber.i("onDetachedFromActivityForConfigChanges")

        _lifecycle = null
    }

    override fun onDetachedFromActivity() {
        Timber.i("onDetachedFromActivity")
        onDetachedFromActivityForConfigChanges()
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        Timber.i("onDetachedFromEngine")

        methodChannel.setMethodCallHandler(null)

        eventChannel.setStreamHandler(null)

        pluginScope.cancel()
        _pluginScope = null
    }

    /* ---------------- Method の実装 ---------------- */

    private fun invokeMethod(call: MethodCall): Any =
        when (call.method) {
            "getPlatformVersion" -> getPlatformVersion()
            else -> throw NotImplementedError()
        }

    @VisibleForTesting
    internal fun getPlatformVersion(): String {
        Timber.d("getPlatformVersion")
        return "Android ${android.os.Build.VERSION.RELEASE}"
    }
}
