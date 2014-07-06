package com.bklimt.surgetracker.view;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.bklimt.surgetracker.R;
import com.bklimt.surgetracker.model.RootViewModel;

/**
 * A placeholder fragment containing a simple view.
 */
public class AggregateListFragment extends Fragment {
    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static AggregateListFragment newInstance() {
        AggregateListFragment fragment = new AggregateListFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public AggregateListFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_aggregates, container, false);

        final RootViewModel root = RootViewModel.get();

        final AggregateListAdapter aggregateListAdapter = new AggregateListAdapter(getActivity());
        final ListView aggregateListView = (ListView) rootView.findViewById(R.id.aggregate_list);
        aggregateListView.setAdapter(aggregateListAdapter);
        root.getAggregates().bindToArrayAdapter(aggregateListAdapter);

        /*Button startButton = (Button) rootView.findViewById(R.id.start_button);
        final WeakReference<Button> weakStartButton = new WeakReference<Button>(startButton);
        final Capture<ModelListener<RootViewModel>> weakListener = new Capture<ModelListener<RootViewModel>>();
        weakListener.set(new ModelListener<RootViewModel>() {
            @Override
            public void onChanged(RootViewModel model, String key, Object oldValue, Object newValue) {
                Button startButton = weakStartButton.get();
                if (startButton == null) {
                    root.removeListener(weakListener.get());
                    return;
                }

                if (key.equals("currentSurge")) {
                    if (newValue == null) {
                        startButton.setText(R.string.start);
                    } else {
                        startButton.setText(R.string.stop);
                    }
                }
            }
        });
        root.addListener(weakListener.get());

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (root.getCurrentSurge() == null) {
                    root.startSurge();
                } else {
                    root.stopSurge();
                }
            }
        });*/

        return rootView;
    }
}
