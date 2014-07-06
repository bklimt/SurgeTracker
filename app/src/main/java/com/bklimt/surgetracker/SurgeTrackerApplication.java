package com.bklimt.surgetracker;

import android.app.Application;

import com.parse.Parse;

/**
 * Created by klimt on 7/3/14.
 */
public class SurgeTrackerApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Parse.enableLocalDatastore(this);
        Parse.initialize(this, "aMFzL8nmO5fTwjjvevmKLwgisLkvqBJpxFq8hp2J",
                "uU0LtN9kLHf0sMnN0Q2o3mBFa5UPvYEzncdZcFIB");
    }
}
