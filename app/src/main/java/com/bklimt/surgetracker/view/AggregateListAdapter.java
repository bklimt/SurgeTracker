package com.bklimt.surgetracker.view;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.bklimt.surgetracker.R;
import com.bklimt.surgetracker.TimerThread;
import com.bklimt.surgetracker.model.Aggregate;
import com.bklimt.surgetracker.model.RootViewModel;

import java.lang.ref.WeakReference;

public class AggregateListAdapter extends ArrayAdapter<Aggregate> {
    public AggregateListAdapter(Context context) {
        super(context, 0);
        TimerThread.atLeastEveryMinute(new AggregateListAdapterListener(this));
    }

    /**
     * Every 500ms, this listener gets called, and if there's a surge in progress, the list will
     * be updated so that we can see the second timer tick up. This is a static class so that we
     * can have a weak reference to "this". Otherwise, the timer thread would keep the adapter
     * around forever.
     */
    private static class AggregateListAdapterListener implements Runnable {
        WeakReference<AggregateListAdapter> weakAdapter;

        AggregateListAdapterListener(AggregateListAdapter adapter) {
            weakAdapter = new WeakReference<AggregateListAdapter>(adapter);
        }

        @Override
        public void run() {
            AggregateListAdapter adapter = weakAdapter.get();
            if (adapter == null) {
                TimerThread.removeListener(this);
                return;
            }

            // TODO(klimt): This doesn't need to happen this often.
            RootViewModel.get().getAggregates().recompute();
            //if (RootViewModel.get().getCurrentSurge() != null) {
                adapter.notifyDataSetChanged();
            //}
        }
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        if (view == null) {
            view = View.inflate(getContext(), R.layout.list_item_aggregate, null);
        }

        final Aggregate aggregate = getItem(position);

        final TextView surgeCountView = (TextView) view.findViewById(R.id.surge_count);
        final TextView averageDurationView = (TextView) view.findViewById(R.id.average_duration);
        final TextView averageFrequencyView = (TextView) view.findViewById(R.id.average_frequency);
        final TextView sinceView = (TextView) view.findViewById(R.id.since);

        surgeCountView.setText(String.format("%d", aggregate.getCount()));
        averageDurationView.setText(aggregate.getAverageDurationString());
        averageFrequencyView.setText(aggregate.getAverageFrequencyString());
        sinceView.setText(aggregate.getSinceTime(this.getContext()));

        /*
         * No need to bind these UI elements, because the Collection is already bound to the list
         * adapter, and that will fire change events whenever a surge changes. We wouldn't want to
         * bind these UI elements anyway, because the view can get reused.
         */

        return view;
    }
}
