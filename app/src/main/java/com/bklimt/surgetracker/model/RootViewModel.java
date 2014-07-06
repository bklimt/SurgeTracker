package com.bklimt.surgetracker.model;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.bklimt.surgetracker.backbone.Model;
import com.bklimt.surgetracker.backbone.Visitor;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

public class RootViewModel extends Model {
    private static RootViewModel instance = new RootViewModel();
    public static RootViewModel get() {
        return instance;
    }

    private SurgeCollection surges;
    private AggregateCollection aggregates;

    private RootViewModel() {
        surges = new SurgeCollection();
        aggregates = new AggregateCollection(surges);
    }

    public SurgeCollection getSurges() {
        return surges;
    }

    public AggregateCollection getAggregates() {
        return aggregates;
    }

    public Surge getCurrentSurge() {
        return (Surge) getModel("currentSurge");
    }

    public void selectSurge(Surge surge) {
        set("selectedSurge", surge);
    }

    public Surge getSelectedSurge() {
        return (Surge) getModel("selectedSurge");
    }

    public void startSurge() {
        synchronized (lock) {
            if (getModel("currentSurge") != null) {
                throw new RuntimeException("Tried to start a surge when one is already in progress.");
            }
            Surge surge = new Surge();
            set("currentSurge", surge);
            getSurges().add(surge);
        }
    }

    public void stopSurge() {
        synchronized (lock) {
            if (getModel("currentSurge") == null) {
                throw new RuntimeException("Tried to stop a surge when one isn't happening.");
            }
            getCurrentSurge().stop();
            unset("currentSurge");
        }
    }

    private void writeCsv(final Context context, final Writer writer) throws IOException {
        SurgeCollection surges = getSurges();
        AggregateCollection aggregates = getAggregates();

        writer.write("Duration (mm:ss),Time Between (mm:ss),Start time\n");
        surges.each(new Visitor<Surge>() {
            @Override
            public void visit(Surge surge) throws Exception {
                writer.write(surge.getDurationString());
                writer.write(",");
                writer.write(surge.getFrequency());
                writer.write(",");
                writer.write(surge.getStartDay(context));
                writer.write(" ");
                writer.write(surge.getStartTime(context));
                writer.write("\n");
            }
        });
    }

    public void sendEmail(Context context) throws IOException {
        StringWriter writer = new StringWriter();
        writeCsv(context, writer);

        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", "", null));
        intent.putExtra(Intent.EXTRA_SUBJECT, "Surge Report");
        intent.putExtra(Intent.EXTRA_TEXT, writer.getBuffer().toString());
        context.startActivity(Intent.createChooser(intent, "Send email..."));
    }
}
