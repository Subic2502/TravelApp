package com.example.travelapp;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Jedno mesto za rad sa nitima: pozadinske niti za mrežu i bazu, i Handler za glavnu (UI) nit.
public final class AppExecutor {

    private static final int THREAD_COUNT = 4;

    private static final ExecutorService BACKGROUND = Executors.newFixedThreadPool(THREAD_COUNT);
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private AppExecutor() {
    }

    public static void runInBackground(Runnable task) {
        BACKGROUND.execute(task);
    }

    // Samo glavna nit sme da menja prikaz, zato rezultat vraćamo preko Handler-a.
    public static void runOnMainThread(Runnable task) {
        MAIN_HANDLER.post(task);
    }
}
