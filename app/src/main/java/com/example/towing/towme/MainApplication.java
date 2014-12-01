package com.example.towing.towme;

import android.app.Application;

import com.parse.Parse;
import com.parse.ParseObject;

/**
 * Created by Mohamed on 14-11-30.
 */
public class MainApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ParseObject.registerSubclass(LocationPost.class);
        Parse.initialize(this, getString(R.string.parse_application_id)
                , getString(R.string.parse_client_key));
    }
}
