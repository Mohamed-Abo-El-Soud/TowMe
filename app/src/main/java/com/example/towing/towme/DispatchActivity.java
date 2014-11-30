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

import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseUser;

import java.util.zip.Inflater;

/**
 * Created by Mohamed on 14-11-29.
 */
public class DispatchActivity extends FragmentActivity {

    FragmentActivity that = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dispatch);

        if (savedInstanceState == null) {
//            initializeDrawer();
        }

    }

    @Override
    protected void onStart() {
        super.onStart();

        ProgressBar progressBar = (ProgressBar)findViewById(R.id.dispatch_progress);
        progressBar.setVisibility(View.VISIBLE);
        TextView startingText = (TextView)findViewById(R.id.dispatch_starting_text);
        startingText.setVisibility(View.VISIBLE);
        ThisApplication thisApplication = (ThisApplication)getApplication();
        logInUser(thisApplication.getUser());
    }

    public void logInUser(User user){
        ParseUser.logInInBackground(user.getUserName(), user.getPassWordByPass(), new LogInCallback() {
            public void done(ParseUser user, ParseException e) {
                if (user != null) {
                    // Hooray! The user is logged in.
                    Log.v("ParseUser","Log in successful");
                    ThisApplication app = (ThisApplication)getApplication();
                    app.setParseUser(user);
                    moveOn(true);
                } else {
                    // Login failed. Look at the ParseException to see what happened.
                    Toast.makeText(that, "Could not connect to the internet"
                            , Toast.LENGTH_LONG).show();
                    moveOn(false);
                }

            }
        });

    }

    public void moveOn(boolean ifUser){
        ProgressBar progressBar = (ProgressBar)findViewById(R.id.dispatch_progress);
        progressBar.setVisibility(View.GONE);
        TextView startingText = (TextView)findViewById(R.id.dispatch_starting_text);
        startingText.setVisibility(View.GONE);
        Intent intent = new Intent(this,MapsActivity.class);
        intent.putExtra(Intent.EXTRA_TEXT,ifUser);
        startActivity(intent);

    }

}
