package com.bklimt.babywatch;

import com.bklimt.babywatch.backbone.Model;

public class RootViewModel extends Model {
    private static RootViewModel instance = new RootViewModel();
    public static RootViewModel get() {
        return instance;
    }

    private RootViewModel() {
        set("surges", new SurgeCollection());
    }

    public SurgeCollection getSurges() {
        return (SurgeCollection) getCollection("surges");
    }

    public Surge getCurrentSurge() {
        return (Surge) getModel("currentSurge");
    }

    public void startSurge() {
        synchronized (lock) {
            if (getModel("currentSurge") != null) {
                throw new RuntimeException("Tried to start a surge when one is already in progress.");
            }
            Surge surge = new Surge();
            set("currentSurge", surge);
            getSurges().add(surge);
        }
    }

    public void stopSurge() {
        synchronized (lock) {
            if (getModel("currentSurge") == null) {
                throw new RuntimeException("Tried to stop a surge when one isn't happening.");
            }
            getCurrentSurge().stop();
            unset("currentSurge");
        }
    }
}
