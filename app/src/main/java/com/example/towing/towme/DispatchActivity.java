package com.example.towing.towme;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.LogInCallback;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.List;
import java.util.zip.Inflater;

/**
 * Created by Mohamed on 14-11-29.
 */
public class DispatchActivity extends FragmentActivity {

    public static final String LOG_TAG = DispatchActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dispatch);

    }

    @Override
    protected void onStart() {
        super.onStart();

        ProgressBar progressBar = (ProgressBar)findViewById(R.id.dispatch_progress);
        progressBar.setVisibility(View.VISIBLE);
        TextView startingText = (TextView)findViewById(R.id.dispatch_starting_text);
        startingText.setVisibility(View.VISIBLE);
        userInitialization();
    }

    private void userInitialization(){
        ParseUser currentUser = ParseUser.getCurrentUser();
        if (currentUser != null) {
            // do stuff with the user
            Utilites.setUser(currentUser);
            getEntries();
            moveOn(true);
        } else {
            // show the signup or login screen
            createAnonymousUser();
        }
    }

    private void createAnonymousUser(){
        ParseAnonymousUtils.logIn(new LogInCallback() {
            @Override
            public void done(ParseUser user, ParseException e) {
                if (e != null) {
                    Log.d(LOG_TAG, "Anonymous login failed.");
                } else {
                    Log.d(LOG_TAG, "Anonymous user logged in.");
                    Utilites.setUser(user);
                    createNewLocationPost(user);
                    moveOn(true);
                }
            }
        });
    }


    private void getEntries(){
        ParseQuery<LocationPost> query = LocationPost.getQuery();
        query.whereEqualTo("user", Utilites.getUser());
        query.findInBackground(new FindCallback<LocationPost>() {
            public void done(List<LocationPost> scoreList, ParseException e) {
                if (e == null) {
                    Log.d(LOG_TAG, "Retrieved " + scoreList.size() + " scores");
                    if (scoreList.size() >= 1) {
                        clearMostEntries(scoreList);
                        Utilites.setLocationPost(scoreList.get(0));
                    }
                    else {
                        createNewLocationPost(Utilites.getUser());
                    }
                } else {
                    Log.d(LOG_TAG, "Error: " + e.getMessage());
                    createNewLocationPost(Utilites.getUser());
                }
            }
        });
    }

    private void clearMostEntries(List<LocationPost> list){
        for (int i = 1; i < list.size(); i++) {
            list.get(i).deleteInBackground();
        }
    }

    private void createNewLocationPost(ParseUser user){
        LocationPost locationPost = new LocationPost();
        locationPost.setUser(user);
        Utilites.setLocationPost(locationPost);
    }

    private void moveOn(boolean ifUser){
        ProgressBar progressBar = (ProgressBar)findViewById(R.id.dispatch_progress);
        progressBar.setVisibility(View.GONE);
        TextView startingText = (TextView)findViewById(R.id.dispatch_starting_text);
        startingText.setVisibility(View.GONE);
        Intent intent = new Intent(this,MapsActivity.class);
        intent.putExtra(Intent.EXTRA_TEXT,ifUser);
        startActivity(intent);
    }



}
