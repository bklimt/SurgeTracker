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

    public void stop() {
        set("end", new Date());
    }

    public String getDuration() {
        Date end = getEnd();
        if (end == null) {
            end = new Date();
        }

        long durationMilliseconds = end.getTime() - getStart().getTime();
        int durationSeconds = (int)(durationMilliseconds / 1000L);
        int durationMinutes = durationSeconds / 60;
        durationSeconds %= 60;
        return String.format("%02d:%02d", durationMinutes, durationSeconds);
    }

    public String getStartDay(Context context) {
        return DateFormat.getDateFormat(context).format(getStart());
    }

    public String getStartTime(Context context) {
        return DateFormat.getTimeFormat(context).format(getStart());
    }
}
