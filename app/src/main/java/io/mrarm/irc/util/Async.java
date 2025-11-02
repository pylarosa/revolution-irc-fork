package io.mrarm.irc.util;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Simple utility for running background work on a shared IO thread pool
 * and posting results back to the main thread.
 */
public class Async {

    private static final ExecutorService IO = Executors.newCachedThreadPool();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private Async() {
    }

    /**
     * Fire-and-forget variant: run work on background thread, then run optional UI callback.
     */
    public static void io(Runnable work, Runnable uiCallback) {
        IO.execute(() -> {
            work.run();
            if (uiCallback != null)
                MAIN.post(uiCallback);
        });
    }

    /**
     * Generic variant: run background work that returns a value, then deliver result on UI thread.
     */
    public static <T> void io(Supplier<T> work, Consumer<T> uiCallback) {
        IO.execute(() -> {
            T result = null;
            try {
                result = work.get();
            } catch (Exception e) {
                Log.e("AsyncOperationFailed", "Async work failed", e);
            }
            final T finalResult = result;
            if (uiCallback != null)
                MAIN.post(() -> uiCallback.accept(finalResult));
        });
    }
}

