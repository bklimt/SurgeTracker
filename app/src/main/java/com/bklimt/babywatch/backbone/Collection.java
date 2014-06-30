package com.bklimt.babywatch.backbone;

import android.os.Handler;
import android.os.Looper;
import android.widget.ArrayAdapter;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.WeakHashMap;
import java.util.logging.Logger;

import org.json.JSONArray;

import bolts.Capture;

public class Collection<T extends Model> {
    protected Object lock = new Object();
    private Logger log = Logger.getLogger(getClass().getName());

    private ArrayList<T> items = new ArrayList<T>();

    private ArrayList<CollectionListener<? extends Collection<T>, T>> listeners =
            new ArrayList<CollectionListener<? extends Collection<T>, T>>();

    public Collection() {
    }

    public JSONArray toJSON() {
        synchronized (lock) {
            JSONArray json = new JSONArray();
            for (T item : items) {
                json.put(item.toJSON());
            }
            return json;
        }
    }

    public int size() {
        synchronized (lock) {
            return items.size();
        }
    }

    public void add(T item) {
        synchronized (lock) {
            int position = items.size();
            insert(item, position);
        }
    }

    public void insert(T item, int position) {
        synchronized (lock) {
            items.add(position, item);
            notifyAdded(item, position);
        }
    }

    public void remove(T item) {
        synchronized (lock) {
            items.remove(item);
            notifyRemoved(item);
        }
    }

    public T get(int position) {
        synchronized (lock) {
            return items.get(position);
        }
    }

    protected void notifyAdded(final T item, final int position) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    notifyAdded(item, position);
                }
            });
            return;
        }

        synchronized (lock) {
            log.info("Firing add event for " + item);
            ArrayList<CollectionListener> listenersCopy = new ArrayList<CollectionListener>(listeners);
            for (CollectionListener listener : listenersCopy) {
                listener.onAdd(this, item, position);
            }
        }
    }

    protected void notifyRemoved(final T item) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    notifyRemoved(item);
                }
            });
            return;
        }

        synchronized (lock) {
            log.info("Firing remove event for " + item);
            ArrayList<CollectionListener> listenersCopy = new ArrayList<CollectionListener>(listeners);
            for (CollectionListener listener : listenersCopy) {
                listener.onRemove(this, item);
            }
        }
    }

    public void each(Visitor<T> visitor) {
        synchronized (lock) {
            for (T item : items) {
                visitor.visit(item);
            }
        }
    }

    public void clear() {
        synchronized (lock) {
            final ArrayList<T> toRemove = new ArrayList<T>(items);
            for (T item : toRemove) {
                remove(item);
            }
        }
    }

    public <T1 extends Collection<T>> void addListener(CollectionListener<T1, T> listener) {
        synchronized (lock) {
            listeners.add(listener);
        }
    }

    public <T1 extends Collection<T>> void removeListener(CollectionListener<T1, T> listener) {
        synchronized (lock) {
            listeners.remove(listener);
        }
    }

    public void bindToArrayAdapter(ArrayAdapter<T> adapter) {
        synchronized (lock) {
            adapter.clear();
            adapter.addAll(items);

            final WeakReference<ArrayAdapter<T>> weakAdapter = new WeakReference<ArrayAdapter<T>>(adapter);
            final Capture<CollectionListener<Collection<T>, T>> weakListener = new Capture<CollectionListener<Collection<T>, T>>();
            weakListener.set(new CollectionListener<Collection<T>, T>() {
                @Override
                public void onAdd(Collection<T> collection, T item, int position) {
                    ArrayAdapter<T> adapter = weakAdapter.get();
                    if (adapter == null) {
                        removeListener(weakListener.get());
                        return;
                    }
                    adapter.insert(item, position);
                }

                @Override
                public void onRemove(Collection<T> collection, T item) {
                    ArrayAdapter<T> adapter = weakAdapter.get();
                    if (adapter == null) {
                        removeListener(weakListener.get());
                        return;
                    }
                    adapter.remove(item);
                }
            });
            addListener(weakListener.get());
        }
    }
}
