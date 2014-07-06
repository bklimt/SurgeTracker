package com.bklimt.surgetracker.view;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.DatePicker;
import android.widget.TimePicker;

import com.bklimt.surgetracker.R;
import com.bklimt.surgetracker.model.RootViewModel;
import com.bklimt.surgetracker.model.Surge;

import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by klimt on 7/3/14.
 */
public class SurgeStartDialogFragment extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Logger logger = Logger.getLogger(getClass().getName());

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage("Start");

        View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_surge_start, null);
        builder.setView(view);

        RootViewModel root = RootViewModel.get();
        final Surge surge = root.getSelectedSurge();

        final Date date = surge.getStart();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        final DatePicker datePicker = (DatePicker) view.findViewById(R.id.date);
        datePicker.init(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH), null);

        final TimePicker timePicker = (TimePicker) view.findViewById(R.id.time);
        timePicker.setCurrentHour(calendar.get(Calendar.HOUR_OF_DAY));
        timePicker.setCurrentMinute(calendar.get(Calendar.MINUTE));

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(date);

                calendar.set(Calendar.YEAR, datePicker.getYear());
                calendar.set(Calendar.MONTH, datePicker.getMonth());
                calendar.set(Calendar.DAY_OF_MONTH, datePicker.getDayOfMonth());
                calendar.set(Calendar.HOUR_OF_DAY, timePicker.getCurrentHour());
                calendar.set(Calendar.MINUTE, timePicker.getCurrentMinute());

                logger.log(Level.INFO, "Setting start to " + calendar);
                surge.setStart(calendar.getTime());
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });

        return builder.create();
    }
}
