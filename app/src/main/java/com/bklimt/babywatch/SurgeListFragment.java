package com.bklimt.babywatch;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;

import com.bklimt.babywatch.backbone.ModelListener;

import java.lang.ref.WeakReference;

import bolts.Capture;

/**
 * A placeholder fragment containing a simple view.
 */
public class SurgeListFragment extends Fragment {
    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static SurgeListFragment newInstance() {
        SurgeListFragment fragment = new SurgeListFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public SurgeListFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        final RootViewModel root = RootViewModel.get();

        final SurgeListAdapter surgeListAdapter = new SurgeListAdapter(getActivity());
        final ListView surgeListView = (ListView) rootView.findViewById(R.id.surge_list);
        surgeListView.setAdapter(surgeListAdapter);
        root.getSurges().bindToArrayAdapter(surgeListAdapter);

        Button startButton = (Button) rootView.findViewById(R.id.start_button);
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
        });

        return rootView;
    }
}
