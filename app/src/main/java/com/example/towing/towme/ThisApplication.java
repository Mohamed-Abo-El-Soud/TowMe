package com.example.towing.towme;

import android.app.Application;

import com.parse.Parse;
import com.parse.ParseObject;

/**
 * Created by ahmedabdalla on 14-11-28.
 */
public class ThisApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Parse.initialize(this, "zHvdP3Em9tLrPXURI2NZ4rjbtyj7wlVtVKn6lqON", "vgUhuOT2agiIYjhcf0lCODXDMtBbc1fVA7FJItNn");
//        ParseObject testObject = new ParseObject("TestObject");
//        testObject.put("foo", "bar");
//        testObject.saveInBackground();
    }

}
