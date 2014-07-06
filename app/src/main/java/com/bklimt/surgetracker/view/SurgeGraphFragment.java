package com.bklimt.surgetracker.view;

import android.os.Bundle;
import android.app.Fragment;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.bklimt.surgetracker.R;
import com.bklimt.surgetracker.TimerThread;
import com.bklimt.surgetracker.backbone.CollectionListener;
import com.bklimt.surgetracker.model.RootViewModel;
import com.bklimt.surgetracker.model.Surge;
import com.bklimt.surgetracker.model.SurgeCollection;
import com.jjoe64.graphview.CustomLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewDataInterface;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.LineGraphView;

import java.lang.ref.WeakReference;
import java.util.Date;

import bolts.Capture;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SurgeGraphFragment#newInstance} factory method to create an instance of this
 * fragment.
 */
public class SurgeGraphFragment extends Fragment {
    private boolean isFrequency = false;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment SurgeGraphFragment.
     */
    public static SurgeGraphFragment newInstance(boolean isFrequency) {
        SurgeGraphFragment fragment = new SurgeGraphFragment();
        Bundle args = new Bundle();
        args.putBoolean("isFrequency", isFrequency);
        fragment.setArguments(args);
        return fragment;
    }

    public SurgeGraphFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            isFrequency = args.getBoolean("isFrequency", false);
        }
    }

    private String getGraphTitle() {
        return isFrequency ? "Surge Frequency" : "Surge Duration";
    }

    private GraphViewDataInterface[] getGraphViewData(SurgeCollection surges) {
        return isFrequency ? surges.getFrequencyGraphViewData() : surges.getDurationGraphViewData();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_surge_graph, container, false);

        final SurgeCollection surges = RootViewModel.get().getSurges();
        final GraphViewSeries series = new GraphViewSeries(getGraphViewData(surges));

        final GraphView graphView = new LineGraphView(view.getContext(), getGraphTitle());
        graphView.addSeries(series);

        graphView.setPadding(20, 20, 20, 20);

        graphView.setCustomLabelFormatter(new CustomLabelFormatter() {
            @Override
            public String formatLabel(double value, boolean isX) {
                if (isX) {
                    Date date = new Date();
                    date.setTime((long) value);
                    return DateFormat.getTimeFormat(graphView.getContext()).format(date);
                } else {
                    int seconds = (int) value;
                    int minutes = seconds / 60;
                    seconds = seconds % 60;
                    return String.format("%02d:%02d", minutes, seconds);
                }
            }
        });

        final WeakReference<GraphView> weakGraphView = new WeakReference<GraphView>(graphView);
        final Capture<CollectionListener<SurgeCollection, Surge>> weakCollectionListener =
                new Capture<CollectionListener<SurgeCollection, Surge>>();
        weakCollectionListener.set(new CollectionListener<SurgeCollection, Surge>() {
            @Override
            public void onAdd(SurgeCollection collection, Surge item, int position) {
                GraphView graphView = weakGraphView.get();
                if (graphView == null) {
                    surges.removeListener(weakCollectionListener.get());
                    return;
                }
                series.resetData(getGraphViewData(surges));
                graphView.invalidate();
            }

            @Override
            public void onRemove(SurgeCollection collection, Surge item) {
                GraphView graphView = weakGraphView.get();
                if (graphView == null) {
                    surges.removeListener(weakCollectionListener.get());
                    return;
                }
                series.resetData(getGraphViewData(surges));
                graphView.invalidate();
            }

            @Override
            public void onChanged(Surge model, String key, Object oldValue, Object newValue) {
                GraphView graphView = weakGraphView.get();
                if (graphView == null) {
                    surges.removeListener(weakCollectionListener.get());
                    return;
                }
                series.resetData(getGraphViewData(surges));
                graphView.invalidate();
            }
        });
        surges.addListener(weakCollectionListener.get());

        final Capture<Runnable> weakTimerListener = new Capture<Runnable>();
        weakTimerListener.set(new Runnable() {
            @Override
            public void run() {
                GraphView graphView = weakGraphView.get();
                if (graphView == null) {
                    TimerThread.removeListener(weakTimerListener.get());
                    return;
                }
                if (RootViewModel.get().getCurrentSurge() != null) {
                    series.resetData(getGraphViewData(surges));
                    graphView.invalidate();
                }
            }
        });
        TimerThread.atLeastEverySecond(weakTimerListener.get());

        FrameLayout layout = (FrameLayout) view;
        layout.addView(graphView);

        return view;
    }
}
