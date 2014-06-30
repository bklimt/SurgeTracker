package com.bklimt.babywatch;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;

public class TimerThread {
    private static Object lock = new Object();
    private static boolean running = false;

    private static ArrayList<Runnable> listeners = new ArrayList<Runnable>();
    private static Handler handler = new Handler(Looper.getMainLooper());

    public static void addListener(Runnable runnable) {
        synchronized (lock) {
            listeners.add(runnable);
            if (!running) {
                running = true;
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (true) {
                            try {
                                // Should run at least every second.
                                Thread.sleep(500);
                                ArrayList<Runnable> listenersCopy;
                                synchronized (lock) {
                                    listenersCopy = new ArrayList<Runnable>(listeners);
                                }
                                for (Runnable listener : listenersCopy) {
                                    handler.post(listener);
                                }
                            } catch (InterruptedException e) {
                                // Ignore it. There's really no way to kill this thread.
                            }
                        }
                    }
                });
                thread.setName(TimerThread.class.getName());
                thread.start();
            }
        }
    }

    public static void removeListener(Runnable runnable) {
        synchronized (lock) {
            listeners.remove(runnable);
        }
    }
}
