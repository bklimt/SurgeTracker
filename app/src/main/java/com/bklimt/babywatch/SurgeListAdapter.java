package com.bklimt.babywatch;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.bklimt.babywatch.backbone.Model;
import com.bklimt.babywatch.backbone.ModelListener;

import java.lang.ref.WeakReference;

public class SurgeListAdapter extends ArrayAdapter<Surge> {
    public SurgeListAdapter(Context context) {
        super(context, 0);
        TimerThread.addListener(new SurgeListAdapterListener(this));
    }

    /**
     * We really need a listener that can have a "weak this".
     */
    private static class SurgeListAdapterListener implements ModelListener<Surge>, Runnable {
        WeakReference<SurgeListAdapter> weakAdapter;

        SurgeListAdapterListener(SurgeListAdapter adapter) {
            weakAdapter = new WeakReference<SurgeListAdapter>(adapter);
        }

        @Override
        public void onChanged(Surge surge, String key, Object oldValue, Object newValue) {
            SurgeListAdapter adapter = weakAdapter.get();
            if (adapter == null) {
                surge.removeListener(this);
                return;
            }

            // Since views can be reused, this is the only safe thing to do.
            adapter.notifyDataSetChanged();
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

        final Surge surge = getItem(position);

        final TextView durationView = (TextView) view.findViewById(R.id.duration);
        final TextView frequencyView = (TextView) view.findViewById(R.id.frequency);
        final TextView startDateView = (TextView) view.findViewById(R.id.start_date);
        final TextView startTimeView = (TextView) view.findViewById(R.id.start_time);

        durationView.setText(surge.getDuration());
        frequencyView.setText(surge.getDuration());
        startDateView.setText(surge.getStartDay(this.getContext()));
        startTimeView.setText(surge.getStartTime(this.getContext()));

        // TODO(klimt): This listener won't ever get removed as long as this adapter is still alive.
        surge.addListener(new SurgeListAdapterListener(this));

        return view;
    }
}
