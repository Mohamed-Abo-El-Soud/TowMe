package com.example.towing.towme;

import android.content.Context;
import android.location.Location;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;
import com.parse.FunctionCallback;
import com.parse.GetCallback;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Mohamed on 14-11-30.
 */
public class Placer {

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
    }

    public void update(Location currentSelfLocation){
        defineSelfLocation(currentSelfLocation);
        if(once)
            requestOtherMarkersPositions();
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
                }
                if(once)
                    interaction.viewLocation(location);
            }
        }).execute();
    }

    @SuppressWarnings("unchecked")
    public void requestOtherMarkersPositions(){
//        ParseCloud.callFunctionInBackground("requestTowTruckers",new HashMap<String, Object>(),new FunctionCallback<Object>() {
//            @Override
//            public void done(Object o, ParseException e) {
//                if (e == null) {
//                    // result is "Hello world!"
//                    Toast.makeText(mContext,"received all truckers", Toast.LENGTH_LONG).show();
//                    ArrayList<ParseObject list;
//                    putMarkerPositions((ArrayList<ParseObject>) o);
//                }
//            }
//        });
        ParseCloud.callFunctionInBackground("requestTowTruckers",new HashMap<String, Object>(),new FunctionCallback<ArrayList<ParseObject>>() {
            @Override
            public void done(ArrayList<ParseObject> o, ParseException e) {
                if (e == null) {
                    Toast.makeText(mContext,"received all truckers", Toast.LENGTH_LONG).show();
                    putMarkerPositions(o);
                }
            }
        });

    }

    public void putMarkerPositions(ArrayList<ParseObject> list){
        for(ParseObject trucker:list){
            ParseGeoPoint geoPoint = trucker.getParseGeoPoint("location");
            otherMarkers.add(interaction.addMarker("Tow truck",null,geoPoint));
        }
    }

    public void uploadLocation(final Location location){
        LocationPost locationPost = Utilites.getLocationPost();
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
                    Toast.makeText(mContext, "Could not get location", Toast.LENGTH_LONG).show();
//                    Log.e(LOG_TAG, "Error: " + e);
//                    e.printStackTrace();
                }
            }
        });
    }

    public void extra(){

    }

}
