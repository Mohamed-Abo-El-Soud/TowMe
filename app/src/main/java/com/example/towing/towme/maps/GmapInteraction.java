package com.example.towing.towme.maps;

import android.location.Location;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.ParseGeoPoint;

import java.security.Provider;

/**
 * Created by Mohamed on 14-11-30.
 */
public class GmapInteraction {

    private GoogleMap mMap;
    private UiSettings uiSettings;

    public GmapInteraction(GoogleMap map){
        setMap(map);
    }

    public GmapInteraction(){
    }

    public void setMap(GoogleMap map) {

        mMap = map;
        if(mMap !=null){
            uiSettings = mMap.getUiSettings();
            uiSettings.setRotateGesturesEnabled(false);
            uiSettings.setCompassEnabled(false);
            uiSettings.setTiltGesturesEnabled(false);
            mMap.setMyLocationEnabled(true);
        }
    }


    public static LatLng getLatLng(Location location){
        if(location == null) return null;
        return new LatLng(location.getLatitude(),location.getLongitude());
    }

    public static LatLng getLatLng(ParseGeoPoint location){
        if(location == null) return null;
        return new LatLng(location.getLatitude(),location.getLongitude());
    }

    public static ParseGeoPoint getGeoPoint(Location location){
        if (location == null) return null;
        return new ParseGeoPoint(location.getLatitude(),location.getLongitude());
    }

    public static Location getLocation(ParseGeoPoint parseGeoPoint){
        Location result = new Location("");
        result.setLatitude(parseGeoPoint.getLatitude());
        result.setLongitude(parseGeoPoint.getLongitude());
        return result;

    }

    public void viewLocation(Location location) {
        if (mMap == null)return;
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(getLatLng(location), 10));
    }


    public Marker addMarker(String title, String snippet, Location position) {
        if (mMap == null)return null;
        Marker marker = mMap.addMarker(new MarkerOptions()
                        .position(getLatLng(position))
                        .title(title)
                        .snippet(snippet)
                        .flat(true)
        );
        return marker;
    }

    public Marker addMarker(String title, String snippet, ParseGeoPoint position) {
        if (mMap == null)return null;
        Marker marker = mMap.addMarker(new MarkerOptions()
                        .position(getLatLng(position))
                        .title(title)
                        .snippet(snippet)
                        .flat(true)
        );
        return marker;
    }

    public Marker addVisual(String title, String snippet, Location position) {
        if (mMap == null)return null;
        Marker marker = mMap.addMarker(new MarkerOptions()
                        .position(getLatLng(position))
                        .title(title)
                        .snippet(snippet)
                        .flat(true)
        );
        marker.showInfoWindow();
        return marker;
    }

    public void moveMarker(Marker marker,Location location){
        marker.setPosition(getLatLng(location));
    }



    public void buildRoute(Location start, Location end) {
        if (mMap == null)return;

    }


}
