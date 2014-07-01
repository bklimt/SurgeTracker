package com.bklimt.babywatch;

import android.content.Context;
import android.text.format.DateFormat;

import com.bklimt.babywatch.backbone.Model;
import com.bklimt.babywatch.backbone.ModelListener;

import org.json.JSONObject;

import java.util.Date;

public class Surge extends Model {
    public Surge() {
        set("start", new Date());
    }

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
        set("secondsSincePrevious", (int)(millisecondsSincePrevious / 1000));
    }

    public void stop() {
        set("end", new Date());
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
