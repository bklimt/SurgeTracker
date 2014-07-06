package com.bklimt.surgetracker;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;

public class TimerThread {
    private static Object lock = new Object();
    private static boolean running = false;

    private static ArrayList<Runnable> minuteListeners = new ArrayList<Runnable>();
    private static ArrayList<Runnable> secondListeners = new ArrayList<Runnable>();
    private static Handler handler = new Handler(Looper.getMainLooper());

    private static void ensureThreadRunning() {
        synchronized (lock) {
            if (!running) {
                running = true;
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        int seconds = 0;
                        while (true) {
                            try {
                                // Should run at least every second.
                                Thread.sleep(500);

                                ArrayList<Runnable> secondListenersCopy;
                                synchronized (lock) {
                                    secondListenersCopy = new ArrayList<Runnable>(secondListeners);
                                }
                                for (Runnable listener : secondListenersCopy) {
                                    handler.post(listener);
                                }

                                if (++seconds < 60) {
                                    continue;
                                }
                                seconds = 0;

                                ArrayList<Runnable> minuteListenersCopy;
                                synchronized (lock) {
                                    minuteListenersCopy = new ArrayList<Runnable>(minuteListeners);
                                }
                                for (Runnable listener : minuteListenersCopy) {
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

    public static void atLeastEveryMinute(Runnable runnable) {
        synchronized (lock) {
            minuteListeners.add(runnable);
            ensureThreadRunning();
        }
    }

    public static void atLeastEverySecond(Runnable runnable) {
        synchronized (lock) {
            secondListeners.add(runnable);
            ensureThreadRunning();
        }
    }

    public static void removeListener(Runnable runnable) {
        synchronized (lock) {
            minuteListeners.remove(runnable);
            secondListeners.remove(runnable);
        }
    }
}
