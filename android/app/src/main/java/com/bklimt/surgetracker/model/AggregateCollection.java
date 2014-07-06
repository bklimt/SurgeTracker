package com.bklimt.surgetracker.model;

import com.bklimt.surgetracker.backbone.Collection;
import com.bklimt.surgetracker.backbone.CollectionListener;
import com.bklimt.surgetracker.backbone.Visitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

/**
 * Created by klimt on 7/2/14.
 */
public class AggregateCollection extends Collection<Aggregate> {
    private SurgeCollection surges;

    public AggregateCollection(SurgeCollection newSurges) {
        surges = newSurges;

        surges.addListener(new CollectionListener<Collection<Surge>, Surge>() {
            @Override
            public void onAdd(Collection<Surge> collection, Surge item, int position) {
                recompute();
            }

            @Override
            public void onRemove(Collection<Surge> collection, Surge item) {
                recompute();
            }

            @Override
            public void onChanged(Surge model, String key, Object oldValue, Object newValue) {
                recompute();
            }
        });

        recompute();
    }

    public void recompute() {
        synchronized (lock) {
            clear();
            if (surges.size() == 0) {
                return;
            }

            Surge latest = surges.get(0);
            Aggregate aggregate = new Aggregate(Collections.singletonList(latest), latest.getStart());
            add(aggregate);

            final Date since = new Date();
            while (true) {
                since.setTime(since.getTime() - (2 * 60 * 60 * 1000));

                final ArrayList<Surge> surgesSince = new ArrayList<Surge>();
                surges.each(new Visitor<Surge>() {
                    @Override
                    public void visit(Surge surge) {
                        if (since.before(surge.getStart())) {
                            surgesSince.add(surge);
                        }
                    }
                });
                if (surgesSince.size() == 0) {
                    continue;
                }
                add(new Aggregate(surgesSince, since));

                if (surgesSince.size() == surges.size()) {
                    break;
                }
            }
        }
    }
}
