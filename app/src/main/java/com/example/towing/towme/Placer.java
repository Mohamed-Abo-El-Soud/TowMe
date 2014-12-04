package com.example.towing.towme;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.parse.DeleteCallback;
import com.parse.FindCallback;
import com.parse.FunctionCallback;
import com.parse.GetCallback;
import com.parse.ParseACL;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseRole;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Mohamed on 14-11-30.
 */
public class Placer {

    private static final String LOG_TAG = Placer.class.getSimpleName();
    private static final int MAX_POST_SEARCH_DISTANCE = 100;
    private static final int MAX_POST_COUNT = 5;

    private GmapInteraction interaction;
    private View mRootView;
    private Context mContext;
    private Location mLocation;
    private boolean once = true;

    Marker selfMarker;
    ArrayList<Marker> otherMarkers = new ArrayList<Marker>();

    public Placer(GoogleMap map,Context context,View rootView){
        initialize( map, context, rootView);
    }

    public void initialize (GoogleMap map,Context context,View rootView){
        mRootView = rootView;
        mContext = context;
        interaction = new GmapInteraction(map);
        map.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                mapQuery(cameraPosition.target);
            }
        });
    }

    public void update(Location currentSelfLocation){
        defineSelfLocation(currentSelfLocation);
        if(once)
//            requestOtherMarkersPositions(currentSelfLocation);
              getClosestTruck(currentSelfLocation);

        uploadLocation(currentSelfLocation);
        once = false;
//        extra();
    }

    public void defineSelfLocation(Location location){
        new GetAddressTask(mContext
                ,interaction
                ,(ProgressBar)mRootView.findViewById(R.id.address_progress)
                ,location
                ,new GetAddressTask.OnTaskCompleted() {
            @Override
            public void onTaskCompleted(Location location, String address) {
                if(location==null) return;
                if(selfMarker!=null){
                    interaction.moveMarker(selfMarker,location);
                }
                else{
                    selfMarker = interaction.addVisual(mContext.getString(
                                    R.string.your_location_title),address,location);
                    interaction.viewLocation(location);
                }
            }
        }).execute();
    }

    @SuppressWarnings("unchecked")
    public void requestOtherMarkersPositions(final Location location){
        ParseQuery<ParseObject> mapQuery = ParseQuery.getQuery("TowTruckers");
        mapQuery.whereWithinKilometers("location"
                , interaction.getGeoPoint(location), MAX_POST_SEARCH_DISTANCE);
        mapQuery.setLimit(MAX_POST_COUNT);
//        mapQuery.whereGreaterThan("createdAt",ParseUser.getCurrentUser().getCreatedAt());
        mapQuery.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> parseObjects, ParseException e) {
                if(e == null) {
                    Toast.makeText(mContext, "received all truckers", Toast.LENGTH_LONG).show();
                    putMarkerPositions(parseObjects, interaction.getGeoPoint(location));
                }
                else {
                    Log.e(LOG_TAG,"Error: "+e);
                    e.printStackTrace();
                }
            }
        });


//        ParseCloud.callFunctionInBackground("requestTowTruckers"
//                ,new HashMap<String, Object>(),new FunctionCallback<ArrayList<ParseObject>>() {
//            @Override
//            public void done(ArrayList<ParseObject> o, ParseException e) {
//                if (e == null) {
//                    Toast.makeText(mContext,"received all truckers", Toast.LENGTH_LONG).show();
//                    putMarkerPositions(o,interaction.getGeoPoint(location));
//                }
//            }
//        });


//        ParseQuery<ParseUser> userQuery = ParseUser.getQuery();
//        userQuery.whereEqualTo("username","mandarin");
//        userQuery.getFirstInBackground(new GetCallback<ParseUser>() {
//            @Override
//            public void done(ParseUser parseUser, ParseException e) {
//                if(parseUser != null)
//                    parseUser.deleteInBackground(new DeleteCallback() {
//                        @Override
//                        public void done(ParseException e) {
//                            if(e!=null){
//                                Log.e(LOG_TAG,"Error: "+e);
//                                e.printStackTrace();
//                            }
//                        }
//                    });
//            }
//        });
    }

    public void getClosestTruck(final Location location){
        ParseQuery<ParseObject> mapQuery = ParseQuery.getQuery("TowTruckers");
        mapQuery.whereWithinKilometers("location"
                , interaction.getGeoPoint(location), MAX_POST_SEARCH_DISTANCE);
        mapQuery.getFirstInBackground(new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject parseObject, ParseException e) {
                if (e == null) {
                    Toast.makeText(mContext, "received all truckers", Toast.LENGTH_LONG).show();
                    ParseGeoPoint geoPoint = parseObject.getParseGeoPoint("location");
                    otherMarkers.add(interaction.addMarker((String)parseObject.get("place"),null,geoPoint));
                } else {
                    Log.e(LOG_TAG, "Error: " + e);
                    e.printStackTrace();
                }
            }
        });

    }

    public void requestNavigation(Location sAddress, Location dAddress){

        final Intent intent = new Intent(Intent.ACTION_VIEW
                , Uri.parse("http://maps.google.com/maps?" + "saddr=" + sAddress.getLatitude()
                + "," + sAddress.getLongitude() + "&daddr=" + dAddress.getLatitude()
                + "," + dAddress.getLongitude()));
        intent.setClassName("com.google.android.apps.maps","com.google.android.maps.MapsActivity");
        mContext.startActivity(intent);
    }

    public void putMarkerPositions(List<ParseObject> list,ParseGeoPoint point){
        for(ParseObject trucker:list){
            ParseGeoPoint geoPoint = trucker.getParseGeoPoint("location");
            otherMarkers.add(interaction.addMarker((String)trucker.get("place"),null,geoPoint));
        }
    }

    public void uploadLocation(final Location location){
        final LocationPost locationPost = Utilites.getLocationPost();
        if (locationPost == null) return;
        locationPost.fetchInBackground(new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject parseObject, ParseException e) {
                if(e==null) {
                    LocationPost locationP = (LocationPost) parseObject;
                    locationP.setLocation(location);
                    locationP.saveInBackground();
                }
                else {
                    newUploadLocation(locationPost,location);
                }
            }
        });
    }

    public void newUploadLocation(LocationPost lPost,Location location){

        lPost.setLocation(location);
        lPost.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if(e!=null){
                    Toast.makeText(mContext, "Could not get location", Toast.LENGTH_LONG).show();
                    Log.e(LOG_TAG, "Error: " + e);
                    e.printStackTrace();
                }
                else {
                    Toast.makeText(mContext, "created new post", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    public void mapQuery(LatLng position){

    }

}
