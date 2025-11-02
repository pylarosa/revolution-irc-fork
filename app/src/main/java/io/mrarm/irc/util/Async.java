package io.mrarm.irc.util;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Async {
    private static final ExecutorService IO = Executors.newCachedThreadPool();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private Async() {}

    public static void io(Runnable work, Runnable uiCallback) {
        IO.execute(() -> {
            work.run();
            if (uiCallback != null)
                MAIN.post(uiCallback);
        });
    }
}
