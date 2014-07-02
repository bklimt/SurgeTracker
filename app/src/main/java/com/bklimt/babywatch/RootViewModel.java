package com.bklimt.babywatch;

import com.bklimt.babywatch.backbone.Model;

public class RootViewModel extends Model {
    private static RootViewModel instance = new RootViewModel();
    public static RootViewModel get() {
        return instance;
    }

    private SurgeCollection surges;
    private AggregateCollection aggregates;

    private RootViewModel() {
        surges = new SurgeCollection();
        aggregates = new AggregateCollection(surges);
    }

    public SurgeCollection getSurges() {
        return surges;
    }

    public AggregateCollection getAggregates() {
        return aggregates;
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
