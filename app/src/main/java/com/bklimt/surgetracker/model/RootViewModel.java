package com.bklimt.surgetracker.model;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.Html;

import com.bklimt.surgetracker.backbone.Model;
import com.bklimt.surgetracker.backbone.Visitor;
import com.parse.ParseUser;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import bolts.Continuation;
import bolts.Task;

public class RootViewModel extends Model {
    private static RootViewModel instance = new RootViewModel();
    public static RootViewModel get() {
        return instance;
    }

    private Logger log = Logger.getLogger(getClass().getName());
    private SurgeCollection surges;
    private AggregateCollection aggregates;

    private RootViewModel() {
        surges = new SurgeCollection();
        aggregates = new AggregateCollection(surges);

        ParseUser currentUser = ParseUser.getCurrentUser();
        if (currentUser == null) {
            log.info("Loading surges without user.");
        } else {
            log.info("Loading surges for user " + ParseUser.getCurrentUser().getObjectId());
        }
        SurgeParseObject.loadAsync().continueWith(new Continuation<List<Surge>, Void>() {
            @Override
            public Void then(Task<List<Surge>> task) throws Exception {
                if (task.isFaulted()) {
                    log.log(Level.SEVERE, "Unable to load existing surges.", task.getError());
                } else if (task.isCancelled()) {
                    log.log(Level.SEVERE, "Loading surges was cancelled.");
                } else {
                    List<Surge> newSurges = task.getResult();
                    log.info("Loaded " + newSurges.size() + " surges.");
                    for (Surge surge : newSurges) {
                        if (surge.getEnd() == null) {
                            set("currentSurge", surge);
                        }
                        surges.add(surge);
                    }
                }
                return null;
            }
        }, Task.UI_THREAD_EXECUTOR);
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

    private void writeHtml(final Context context, final Writer writer) throws IOException {
        SurgeCollection surges = getSurges();
        AggregateCollection aggregates = getAggregates();

        writer.write("<table style=\"border: solid black 1px; border-collapse: collapse\">\n");
        writer.write("  <tr>\n");
        writer.write("    <td>Duration (mm:ss)</td>\n");
        writer.write("    <td>Time Between (mm:ss)</td>\n");
        writer.write("    <td>Start time</td>\n");
        writer.write("  </tr>\n");
        surges.each(new Visitor<Surge>() {
            @Override
            public void visit(Surge surge) throws Exception {
                writer.write("  <tr>\n");
                writer.write("    <td>" + surge.getDurationString() + "</td>\n");
                writer.write("    <td>" + surge.getTimeBetweenString() + "</td>\n");
                writer.write("    <td>" + surge.getStartDay(context) + " ");
                writer.write(surge.getStartTime(context) + "</td>\n");
                writer.write("  </tr>\n");
            }
        });
        writer.write("</table>");
    }

    public void sendEmail(Context context) throws IOException {
        StringWriter writer = new StringWriter();
        writeHtml(context, writer);

        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", "", null));
        intent.putExtra(Intent.EXTRA_SUBJECT, "Surge Report");
        intent.putExtra(Intent.EXTRA_TEXT, writer.getBuffer().toString());
        context.startActivity(Intent.createChooser(intent, "Send email..."));
    }
}
