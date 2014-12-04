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

    public ParseUser getUser() {
        return getParseUser("user");
    }

    public void setUser(ParseUser value) {
        put("user", value);
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