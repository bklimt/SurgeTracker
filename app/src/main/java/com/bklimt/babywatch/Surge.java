package com.bklimt.babywatch;

import android.content.Context;
import android.text.format.DateFormat;

import com.bklimt.babywatch.backbone.Model;
import com.parse.ParseClassName;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.SaveCallback;

import org.json.JSONObject;

import java.util.Date;

public class Surge extends Model {
    ParseObject parseObject = new ParseObject("Surge");

    public Surge() {
        Date start = new Date();
        set("start", start);
        parseObject.put("start", start);
        parseObject.pinInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
            }
        });
    }

    public Surge(ParseObject obj) {
        set("start", obj.get("start"));
        if (obj.has("end")) {
            set("end", obj.get("end"));
        }
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
        Date end = new Date();
        set("end", end);
        parseObject.put("end", end);
        parseObject.pinInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
            }
        });
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
