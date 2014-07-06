package com.bklimt.surgetracker.view;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.NumberPicker;

import com.bklimt.surgetracker.R;
import com.bklimt.surgetracker.model.RootViewModel;
import com.bklimt.surgetracker.model.Surge;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by klimt on 7/3/14.
 */
public class SurgeDurationDialogFragment extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Logger logger = Logger.getLogger(getClass().getName());

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage("Duration");

        View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_surge_duration, null);
        builder.setView(view);

        RootViewModel root = RootViewModel.get();
        final Surge surge = root.getSelectedSurge();

        final NumberPicker minutesPicker = (NumberPicker) view.findViewById(R.id.minutes);
        minutesPicker.setMinValue(0);
        minutesPicker.setMaxValue(600);
        minutesPicker.setValue(surge.getDurationSeconds() / 60);

        final NumberPicker secondsPicker = (NumberPicker) view.findViewById(R.id.seconds);
        secondsPicker.setMinValue(0);
        secondsPicker.setMaxValue(59);
        secondsPicker.setValue(surge.getDurationSeconds() % 60);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                int minutes = minutesPicker.getValue();
                int seconds = secondsPicker.getValue();
                int duration = minutes * 60 + seconds;
                logger.log(Level.INFO, "Setting duration to " + duration);
                surge.setDurationSeconds(duration);
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });

        return builder.create();
    }
}
