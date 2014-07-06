package com.bklimt.surgetracker.model;

import android.content.Context;
import android.text.format.DateFormat;

import com.bklimt.surgetracker.backbone.Model;
import com.parse.DeleteCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.SaveCallback;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Surge extends Model {
    private Logger logger = Logger.getLogger(getClass().getName());

    ParseObject parseObject;

    public Surge() {
        Date start = new Date();
        set("start", start);

        parseObject = new ParseObject("Surge");
        pin();
    }

    public Surge(ParseObject obj) {
        set("start", obj.get("start"));
        if (obj.has("end")) {
            set("end", obj.get("end"));
        }

        parseObject = obj;

        logger.log(Level.INFO, "Loaded Surge with start=" + getStart() + ", end=" + getEnd());
    }

    private void pin() {
        Date start;
        Date end;
        synchronized (lock) {
            start = getStart();
            end = getEnd();
        }

        parseObject.put("start", start);

        if (end == null) {
            parseObject.remove("end");
        } else {
            parseObject.put("end", end);
        }

        logger.log(Level.INFO, "Pinning Surge with start=" + start + ", end=" + end);
        parseObject.pinInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    logger.log(Level.INFO, "Pinned successfully.");
                } else {
                    logger.log(Level.SEVERE, "Unable to pin Surge.", e);
                }
            }
        });
    }

    public void unpin() {
        parseObject.unpinInBackground(new DeleteCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    logger.log(Level.INFO, "Unpinned successfully.");
                } else {
                    logger.log(Level.SEVERE, "Unable to unpin Surge.", e);
                }
            }
        });
    }

    /*
    public Surge(JSONObject json) {
        JSONObject start = json.optJSONObject("start");
        JSONObject end = json.optJSONObject("end");
        if (start != null) {
            set("start", start);
        }
        if (end != null) {
            set("end", end);
        }
    }
    */

    public void setStart(Date start) {
        synchronized (lock) {
            long duration = getDate("end").getTime() - getDate("start").getTime();
            Date end = new Date(start.getTime() + duration);
            set("start", start);
            set("end", end);
            pin();
        }
    }

    public void stop() {
        synchronized (lock) {
            Date end = new Date();
            set("end", end);
            pin();
        }
    }

    public void setDurationSeconds(int duration) {
        synchronized (lock) {
            Date start = getStart();
            Date end = new Date(start.getTime() + duration * 1000);
            set("end", end);
            pin();
        }
    }

    public Date getStart() {
        return getDate("start");
    }

    public Date getEnd() {
        return getDate("end");
    }

    public int getSecondsSincePrevious() {
        return getInt("secondsSincePrevious");
    }

    public void setPrevious(Surge previous) {
        if (previous == null) {
            unset("secondsSincePrevious");
            return;
        }
        long millisecondsSincePrevious = getStart().getTime() - previous.getStart().getTime();
        set("secondsSincePrevious", (int) (millisecondsSincePrevious / 1000));
    }

    public int getDurationSeconds() {
        Date end = getEnd();
        if (end == null) {
            end = new Date();
        }

        long durationMilliseconds = end.getTime() - getStart().getTime();
        return (int)(durationMilliseconds / 1000L);
    }

    public String getDurationString() {
        int durationSeconds = getDurationSeconds();
        int durationMinutes = durationSeconds / 60;
        durationSeconds %= 60;
        return String.format("%02d:%02d", durationMinutes, durationSeconds);
    }

    public String getFrequency() {
        int frequencySeconds = getSecondsSincePrevious();
        int frequencyMinutes = frequencySeconds / 60;
        frequencySeconds %= 60;
        return String.format("%02d:%02d", frequencyMinutes, frequencySeconds);
    }

    public String getStartDay(Context context) {
        return DateFormat.getDateFormat(context).format(getStart());
    }

    public String getStartTime(Context context) {
        return DateFormat.getTimeFormat(context).format(getStart());
    }
}
