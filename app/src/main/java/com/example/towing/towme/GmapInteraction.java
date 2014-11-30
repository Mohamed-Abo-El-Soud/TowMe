package com.example.towing.towme;

import android.location.Location;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * Created by Mohamed on 14-11-30.
 */
public class GmapInteraction {

    private GoogleMap mMap;


    public void setMap(GoogleMap map) {
        mMap = map;
    }


    private LatLng getLatLng(Location location){
        if(location == null) return null;
        return new LatLng(location.getLatitude(),location.getLongitude());
    }


    public void viewLocation(Location location) {
        if (mMap == null)return;
        mMap.moveCamera(CameraUpdateFactory.newLatLng(getLatLng(location)));
    }


    public void addMarker(String title, String snippet, Location position) {
        if (mMap == null)return;
        mMap.addMarker(new MarkerOptions()
                .position(getLatLng(position))
                .title(title)
                .snippet(snippet)
                .flat(true)
        );
    }


    public void buildRoute(Location start, Location end) {
        if (mMap == null)return;

    }


}
