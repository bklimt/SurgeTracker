package com.bklimt.surgetracker.model;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.text.Html;
import android.text.format.DateFormat;

import com.bklimt.surgetracker.backbone.Model;
import com.bklimt.surgetracker.backbone.Visitor;
import com.parse.ParseUser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Date;
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

    private Logger logger = Logger.getLogger(getClass().getName());
    private SurgeCollection surges;
    private AggregateCollection aggregates;

    private RootViewModel() {
        surges = new SurgeCollection();
        aggregates = new AggregateCollection(surges);

        ParseUser currentUser = ParseUser.getCurrentUser();
        if (currentUser == null) {
            logger.info("Loading surges without user.");
        } else {
            logger.info("Loading surges for user " + ParseUser.getCurrentUser().getObjectId());
        }
        SurgeParseObject.loadAsync().continueWith(new Continuation<List<Surge>, Void>() {
            @Override
            public Void then(Task<List<Surge>> task) throws Exception {
                if (task.isFaulted()) {
                    logger.log(Level.SEVERE, "Unable to load existing surges.", task.getError());
                } else if (task.isCancelled()) {
                    logger.log(Level.SEVERE, "Loading surges was cancelled.");
                } else {
                    List<Surge> newSurges = task.getResult();
                    logger.info("Loaded " + newSurges.size() + " surges.");
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

    private void write(Writer writer, String str) throws IOException {
        writer.write(str.replaceAll("\n", "<br>").replaceAll(" ", "&nbsp;"));
    }

    private void writeText(final Context context, final Writer writer) throws IOException {
        SurgeCollection surges = getSurges();
        AggregateCollection aggregates = getAggregates();

        write(writer, "<tt>Summary\n\n");
        write(writer, "   | Avg | Avg |\n");
        write(writer, " # | Len | Gap | Since\n");
        write(writer, "---|-----|-----|------\n");
        aggregates.each(new Visitor<Aggregate>() {
            @Override
            public void visit(Aggregate aggregate) throws Exception {
                write(writer, String.format("%2d |%s|%s| %s\n", aggregate.getCount(),
                        aggregate.getAverageDurationString(),
                        aggregate.getAverageTimeBetweenString(),
                        aggregate.getSinceTime(context)));
            }
        });
        write(writer, "\n\n");

        write(writer, "Details\n\n");
        write(writer, "       |  Time   | Start\n");
        write(writer, "Length | Between | Time\n");
        write(writer, "-------|---------|------\n");
        surges.each(new Visitor<Surge>() {
            @Override
            public void visit(Surge surge) throws Exception {
                write(writer, String.format(" %s |  %s  | %s\n", surge.getDurationString(),
                        surge.getTimeBetweenString(), surge.getStartTime(context)));
            }
        });
        write(writer, "\n\n");
    }

    private void writeHtml(final Context context, final Writer writer) throws IOException {
        SurgeCollection surges = getSurges();
        AggregateCollection aggregates = getAggregates();

        writer.write("<html>\n");
        writer.write("<head>\n");
        writer.write("  <style>\n");
        writer.write("    th { text-align: center; background-color: #94DBFF }\n");
        writer.write("    td { text-align: center }\n");
        writer.write("  </style>\n");
        writer.write("</head>\n");
        writer.write("<body>\n");

        writer.write("<h2>Summary</h2>\n");
        writer.write("<table style=\"border: solid black 1px; border-collapse: collapse\">\n");
        writer.write("  <tr>\n");
        writer.write("    <th># of<br>Surges</th>\n");
        writer.write("    <th>Since</th>\n");
        writer.write("    <th>Avg Duration<br>(mm:ss)</th>\n");
        writer.write("    <th>Avg Time<br>Between (mm:ss)</th>\n");
        writer.write("  </tr>\n");
        aggregates.each(new Visitor<Aggregate>() {
            @Override
            public void visit(Aggregate aggregate) throws Exception {
                writer.write("  <tr>\n");
                writer.write("    <td>" + aggregate.getCount() + "</td>\n");
                writer.write("    <td>" + aggregate.getSinceDay(context) + "<br>" +
                        aggregate.getSinceTime(context) + "</td>\n");
                writer.write("    <td>" + aggregate.getAverageDurationString() + "</td>\n");
                writer.write("    <td>" + aggregate.getAverageTimeBetweenString() + "</td>\n");
                writer.write("  </tr>\n");
            }
        });
        writer.write("</table>\n\n");

        writer.write("<h2>Details</h2>\n");
        writer.write("<table style=\"border: solid black 1px; border-collapse: collapse\">\n");
        writer.write("  <tr>\n");
        writer.write("    <th>Duration<br>(mm:ss)</th>\n");
        writer.write("    <th>Time Between<br>(mm:ss)</th>\n");
        writer.write("    <th>Start time</th>\n");
        writer.write("  </tr>\n");
        surges.each(new Visitor<Surge>() {
            @Override
            public void visit(Surge surge) throws Exception {
                writer.write("  <tr>\n");
                writer.write("    <td>" + surge.getDurationString() + "</td>\n");
                writer.write("    <td>" + surge.getTimeBetweenString() + "</td>\n");
                writer.write("    <td>" + surge.getStartDay(context) + "<br>");
                writer.write(surge.getStartTime(context) + "</td>\n");
                writer.write("  </tr>\n");
            }
        });
        writer.write("</table>\n\n");

        writer.write("</body>\n</html>\n\n");
    }

    private File writeHtmlToFile(Context context) {
        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            logger.warning("No external media is mounted.");
            return null;
        }

        Date now = new Date();
        String filename = "surges " + DateFormat.getDateFormat(context).format(now) + " " +
                DateFormat.getTimeFormat(context).format(now) + ".html";
        filename = filename.replaceAll("[^A-Za-z0-9.]", "-");

        File path = Environment.getExternalStorageDirectory();
        File file = new File(path, filename);

        logger.info("Writing HTML file to " + file);
        logger.info("HTML URI: " + Uri.fromFile(file));
        try {
            path.mkdirs();
            FileOutputStream stream = new FileOutputStream(file);
            PrintWriter writer = new PrintWriter(stream);
            writeHtml(context, writer);
            writer.flush();
            writer.close();
            return file;

        } catch (IOException ioe) {
            logger.log(Level.SEVERE, "Unable to create HTML file.", ioe);
            return null;
        }
    }

    public void sendEmail(Context context) throws IOException {
        StringWriter writer = new StringWriter();
        writeText(context, writer);

        File attachment = writeHtmlToFile(context);

        Intent intent = new Intent(Intent.ACTION_SEND);//, Uri.fromParts("mailto", "", null));
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Surge Report");
        intent.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(writer.getBuffer().toString()));
        if (attachment != null) {
            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(attachment));
        }
        context.startActivity(Intent.createChooser(intent, "Send email..."));
    }
}
