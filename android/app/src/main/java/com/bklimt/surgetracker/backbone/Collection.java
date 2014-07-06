package com.bklimt.surgetracker.backbone;

import android.os.Handler;
import android.os.Looper;
import android.widget.ArrayAdapter;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.logging.Logger;

import org.json.JSONArray;

import bolts.Capture;

public class Collection<T extends Model> {
    protected Object lock = new Object();
    private Logger log = Logger.getLogger(getClass().getName());

    private ArrayList<T> items = new ArrayList<T>();

    private ArrayList<CollectionListener<? extends Collection<T>, T>> listeners =
            new ArrayList<CollectionListener<? extends Collection<T>, T>>();

    private Comparator<T> comparator = null;

    private ModelListener<T> modelListener = new ModelListener<T>() {
        @Override
        public void onChanged(T model, String key, Object oldValue, Object newValue) {
            notifyChanged(model, key, oldValue, newValue);
        }
    };

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

    public void setComparator(Comparator<T> newComparator) {
        synchronized (lock) {
            comparator = newComparator;
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
            if (comparator != null) {
                for (position = 0; position < items.size(); ++position) {
                    if (comparator.compare(items.get(position), item) > 0) {
                        break;
                    }
                }
            }
            items.add(position, item);
            notifyAdded(item, position);
            item.addListener(modelListener);
        }
    }

    public void insert(T item, int position) {
        synchronized (lock) {
            if (comparator != null) {
                throw new RuntimeException("Attempted to insert into a sorted list. Use add()");
            }
            items.add(position, item);
            notifyAdded(item, position);
            item.addListener(modelListener);
        }
    }

    public void remove(T item) {
        synchronized (lock) {
            items.remove(item);
            item.removeListener(modelListener);
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

    protected void notifyChanged(final T model, final String key, final Object oldValue, final Object newValue) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                notifyChanged(model, key, oldValue, newValue);
                }
            });
            return;
        }

        synchronized (lock) {
            log.info("Firing collection change event for " + model);
            ArrayList<CollectionListener> listenersCopy = new ArrayList<CollectionListener>(listeners);
            for (CollectionListener listener : listenersCopy) {
                listener.onChanged(model, key, oldValue, newValue);
            }
        }
    }

    public void each(Visitor<T> visitor) {
        synchronized (lock) {
            for (T item : items) {
                try {
                    visitor.visit(item);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
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

                @Override
                public void onChanged(T model, String key, Object oldValue, Object newValue) {
                    ArrayAdapter<T> adapter = weakAdapter.get();
                    if (adapter == null) {
                        removeListener(weakListener.get());
                        return;
                    }
                    adapter.notifyDataSetChanged();
                }
            });
            addListener(weakListener.get());
        }
    }
}
