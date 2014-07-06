package com.bklimt.surgetracker.view;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import com.bklimt.surgetracker.model.RootViewModel;
import com.bklimt.surgetracker.model.Surge;

/**
 * Created by klimt on 7/5/14.
 */
public class SurgeDeletionDialogFragment extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage("Are you sure you want to delete this surge?");

        builder.setPositiveButton("Delete!", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                RootViewModel root = RootViewModel.get();
                Surge surge = root.getSelectedSurge();
                root.getSurges().remove(surge);
                surge.unpin();
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });

        return builder.create();
    }
}