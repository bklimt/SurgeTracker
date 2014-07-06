package com.bklimt.surgetracker.view;

import android.app.Fragment;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.bklimt.surgetracker.R;
import com.bklimt.surgetracker.TimerThread;
import com.bklimt.surgetracker.model.RootViewModel;
import com.bklimt.surgetracker.model.Surge;

import java.lang.ref.WeakReference;

public class SurgeListAdapter extends ArrayAdapter<Surge> {
    private Fragment fragment;

    public SurgeListAdapter(Context context, Fragment aFragment) {
        super(context, 0);
        fragment = aFragment;
        TimerThread.atLeastEverySecond(new SurgeListAdapterListener(this));
    }

    /**
     * Every 500ms, this listener gets called, and if there's a surge in progress, the list will
     * be updated so that we can see the second timer tick up. This is a static class so that we
     * can have a weak reference to "this". Otherwise, the timer thread would keep the adapter
     * around forever.
     */
    private static class SurgeListAdapterListener implements Runnable {
        WeakReference<SurgeListAdapter> weakAdapter;

        SurgeListAdapterListener(SurgeListAdapter adapter) {
            weakAdapter = new WeakReference<SurgeListAdapter>(adapter);
        }

        @Override
        public void run() {
            SurgeListAdapter adapter = weakAdapter.get();
            if (adapter == null) {
                TimerThread.removeListener(this);
                return;
            }

            if (RootViewModel.get().getCurrentSurge() != null) {
                adapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        if (view == null) {
            view = View.inflate(getContext(), R.layout.list_item_surge, null);
        }

        final RootViewModel root = RootViewModel.get();
        final Surge surge = getItem(position);

        final TextView durationView = (TextView) view.findViewById(R.id.duration);
        final TextView frequencyView = (TextView) view.findViewById(R.id.frequency);
        final TextView startDateView = (TextView) view.findViewById(R.id.start_date);
        final TextView startTimeView = (TextView) view.findViewById(R.id.start_time);

        durationView.setText(surge.getDurationString());
        frequencyView.setText(surge.getTimeBetweenString());
        startDateView.setText(surge.getStartDay(this.getContext()));
        startTimeView.setText(surge.getStartTime(this.getContext()));

        durationView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (surge.getEnd() == null) {
                    return;
                }
                root.selectSurge(surge);
                SurgeDurationDialogFragment dialog = new SurgeDurationDialogFragment();
                dialog.show(fragment.getFragmentManager(), "surge_duration");
            }
        });

        startDateView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (surge.getEnd() == null) {
                    return;
                }
                root.selectSurge(surge);
                SurgeStartDialogFragment dialog = new SurgeStartDialogFragment();
                dialog.show(fragment.getFragmentManager(), "surge_start");
            }
        });

        startTimeView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (surge.getEnd() == null) {
                    return;
                }
                root.selectSurge(surge);
                SurgeStartDialogFragment dialog = new SurgeStartDialogFragment();
                dialog.show(fragment.getFragmentManager(), "surge_start");
            }
        });

        View.OnLongClickListener onLongClickListener = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (surge.getEnd() == null) {
                    return false;
                }
                root.selectSurge(surge);
                SurgeDeletionDialogFragment dialog = new SurgeDeletionDialogFragment();
                dialog.show(fragment.getFragmentManager(), "surge_delete");
                return true;
            }
        };
        view.setOnLongClickListener(onLongClickListener);
        durationView.setOnLongClickListener(onLongClickListener);
        startDateView.setOnLongClickListener(onLongClickListener);
        startTimeView.setOnLongClickListener(onLongClickListener);

        /*
         * No need to bind these UI elements, because the Collection is already bound to the list
         * adapter, and that will fire change events whenever a surge changes. We wouldn't want to
         * bind these UI elements anyway, because the view can get reused.
         */

        return view;
    }
}
