package com.bklimt.surgetracker.model;

import com.bklimt.surgetracker.backbone.Collection;
import com.bklimt.surgetracker.backbone.CollectionListener;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewDataInterface;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SurgeCollection extends Collection<Surge> {
    private Logger log = Logger.getLogger(getClass().getName());

    public SurgeCollection() {
        setComparator(new Comparator<Surge>() {
            @Override
            public int compare(Surge lhs, Surge rhs) {
                return rhs.getStart().compareTo(lhs.getStart());
            }
        });

        addListener(new CollectionListener<Collection<Surge>, Surge>() {
            @Override
            public void onAdd(Collection<Surge> collection, Surge item, int position) {
                updatePrevious();
            }

            @Override
            public void onRemove(Collection<Surge> collection, Surge item) {
                updatePrevious();
            }

            @Override
            public void onChanged(Surge model, String key, Object oldValue, Object newValue) {
                updatePrevious();
            }
        });

        ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Surge");
        query.fromLocalDatastore();
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> surges, ParseException e) {
                if (e != null) {
                    log.log(Level.SEVERE, "Unable to load existing surges.", e);
                    return;
                }
                for (ParseObject obj : surges) {
                    add(new Surge(obj));
                }
            }
        });
    }

    private void updatePrevious() {
        synchronized (lock) {
            Surge previous = null;
            for (int i = size() - 1; i >= 0; --i) {
                Surge current = get(i);
                current.setPrevious(previous);
                previous = current;
            }
        }
    }

    public GraphViewDataInterface[] getDurationGraphViewData() {
        synchronized (lock) {
            int size = size();
            final GraphViewDataInterface[] data = new GraphViewDataInterface[size];
            for (int i = 0; i < size; ++i) {
                Surge surge = get((size - i) - 1);
                long time = surge.getStart().getTime();
                double duration = surge.getDurationSeconds();
                data[i] = new GraphView.GraphViewData(time, duration);
            }
            return data;
        }
    }

    public GraphViewDataInterface[] getFrequencyGraphViewData() {
        synchronized (lock) {
            int size = size();
            final GraphViewDataInterface[] data = new GraphViewDataInterface[size];
            for (int i = 0; i < size; ++i) {
                Surge surge = get((size - i) - 1);
                long time = surge.getStart().getTime();
                double frequency = -surge.getSecondsSincePrevious();
                data[i] = new GraphView.GraphViewData(time, frequency);
            }
            return data;
        }
    }

    /*
    public SurgeCollection(JSONArray array) {
        for (int i = 0; i < array.length(); ++i) {
            add(new Surge(array.optJSONObject(i)));
        }
    }
    */
}
