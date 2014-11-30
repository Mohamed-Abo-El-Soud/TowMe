package com.example.towing.towme;

import android.location.Location;
import android.util.Log;
import android.widget.Toast;

import com.parse.LogInCallback;
import com.parse.ParseClassName;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SignUpCallback;

/**
 * Created by Mohamed on 14-11-29.
 */

@ParseClassName("LocationPost")
public class LocationPost extends ParseObject {
//
//    public String getText() {
//        return getString("text");
//    }
//
//    public void setText(String value) {
//        put("text", value);
//    }


    public static LocationPost create(User user,Location location){
        LocationPost locationPost = new LocationPost();
        String username = user.getUserName();
        String password = user.getPassWordByPass();
        ParseUser parseUser = new ParseUser();
        parseUser.setUsername(username);
        parseUser.setPassword(password);
        locationPost.setUser(parseUser);
        locationPost.LogIn(username,password);
        return locationPost;
    }

    public Boolean LogIn(String username, String password){
//        String password = user.getPassWordByPass();
//        String username = user.getUserName();
        boolean check = true;
        ParseUser.logInInBackground(username, password, new LogInCallback() {
            public void done(ParseUser user, ParseException e) {
                if (user != null) {
                    // Hooray! The user is logged in.
                } else {
                    // Login failed. Look at the ParseException to see what happened.
                    Log.v("ParseUser", "cannot log in");
//                    user.signUpInBackground(new SignUpCallback() {
//                        @Override
//                        public void done(ParseException e) {
//                            if(user!=null){
//
//                            }else {
//                                Log.v("ParseUser", "cannot log in");
//                            }
//                        }
//                    }
//                  );
//                    ParseUser parseUser = new ParseUser();
//                    parseUser.setUsername(username);
//                    parseUser.setPassword(password);
                }
            }
        });
        return true;
    }

    public ParseUser getUser() {
        return getParseUser("user");
    }

    public void setUser(ParseUser value) {
        put("user", value);
    }

    public void setUser(User user) {
        ParseUser parseUser = new ParseUser();
        parseUser.setUsername(user.getUserName());
        parseUser.setPassword(user.getPassWordByPass());
        setUser(parseUser);
    }

    public ParseGeoPoint getLocation() {
        return getParseGeoPoint("location");
    }

    public void setLocation(ParseGeoPoint value) {
        put("location", value);
    }

    public void setLocation(Location value) {

        ParseGeoPoint geoPoint = new ParseGeoPoint();
        geoPoint.setLatitude(value.getLatitude());
        geoPoint.setLongitude(value.getLongitude());
        setLocation(geoPoint);
    }

    public static ParseQuery<LocationPost> getQuery() {
        return ParseQuery.getQuery(LocationPost.class);
    }
}