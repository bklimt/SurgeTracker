package com.bklimt.surgetracker.model;

import android.content.Context;
import android.text.format.DateFormat;

import com.bklimt.surgetracker.backbone.Model;

import java.util.Date;
import java.util.List;

/**
 * Created by klimt on 7/1/14.
 */
public class Aggregate extends Model {
    public Aggregate(List<Surge> surges, Date since) {
        if (surges.size() == 0) {
            throw new RuntimeException("Tried to aggregate 0 surges.");
        }

        int totalDuration = 0;
        int totalFrequency = 0;
        for (int i = 0; i < surges.size(); ++i) {
            totalDuration += surges.get(i).getDurationSeconds();
            totalFrequency += surges.get(i).getSecondsSincePrevious();
        }

        set("count", surges.size());
        set("since", since);
        set("averageDuration", (int)Math.round((double) totalDuration / surges.size()));
        set("averageTimeBetween", (int)Math.round((double) totalFrequency / surges.size()));
    }

    public int getCount() {
        return getInt("count");
    }

    public Date getSince() {
        return getDate("since");
    }

    public int getAverageDurationSeconds() {
        return getInt("averageDuration");
    }

    public int getAverageSecondsBetween() {
        return getInt("averageTimeBetween");
    }

    public String getAverageDurationString() {
        int durationSeconds = getAverageDurationSeconds();
        int durationMinutes = durationSeconds / 60;
        durationSeconds %= 60;
        return String.format("%02d:%02d", durationMinutes, durationSeconds);
    }

    public String getAverageTimeBetweenString() {
        int secondsBetween = getAverageSecondsBetween();
        int minutesBetween = secondsBetween / 60;
        secondsBetween %= 60;
        return String.format("%02d:%02d", minutesBetween, secondsBetween);
    }

    public String getSinceDay(Context context) {
        return DateFormat.getDateFormat(context).format(getSince());
    }

    public String getSinceTime(Context context) {
        return DateFormat.getTimeFormat(context).format(getSince());
    }
}
