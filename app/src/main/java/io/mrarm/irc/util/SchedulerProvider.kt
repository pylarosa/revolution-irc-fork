package io.mrarm.irc.util

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.plus

interface DelayScheduler {
    fun schedule(delayMillis: Long, runnable: Runnable)
    fun cancel(runnable: Runnable)
}

class HandlerDelayScheduler(
    private val handler: Handler = Handler(Looper.getMainLooper())
) : DelayScheduler {
    override fun schedule(delayMillis: Long, runnable: Runnable) {
        if (delayMillis <= 0L) {
            handler.post(runnable)
        } else {
            handler.postDelayed(runnable, delayMillis)
        }
    }

    override fun cancel(runnable: Runnable) {
        handler.removeCallbacks(runnable)
    }
}

interface SchedulerProvider {
    val ioDispatcher: CoroutineDispatcher
    val reconnectionScheduler: DelayScheduler
}

class DefaultSchedulerProvider : SchedulerProvider {
    override val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    override val reconnectionScheduler: DelayScheduler = HandlerDelayScheduler()
}

object SchedulerProviderHolder {
    @Volatile
    private var provider: SchedulerProvider = DefaultSchedulerProvider()

    @JvmStatic
    fun get(): SchedulerProvider = provider

    @JvmStatic
    fun override(provider: SchedulerProvider) {
        this.provider = provider
    }

    @JvmStatic
    fun reset() {
        provider = DefaultSchedulerProvider()
    }
}

class ManagedCoroutineScope(dispatcher: CoroutineDispatcher) {
    private val job = SupervisorJob()
    val scope: CoroutineScope = CoroutineScope(job + dispatcher)

    fun cancel() {
        job.cancel()
    }
}
