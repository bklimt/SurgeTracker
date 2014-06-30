package com.bklimt.babywatch;

import com.bklimt.babywatch.backbone.Collection;

import org.json.JSONArray;

public class SurgeCollection extends Collection<Surge> {
    public SurgeCollection() {
    }

    public SurgeCollection(JSONArray array) {
        for (int i = 0; i < array.length(); ++i) {
            add(new Surge(array.optJSONObject(i)));
        }
    }
}
