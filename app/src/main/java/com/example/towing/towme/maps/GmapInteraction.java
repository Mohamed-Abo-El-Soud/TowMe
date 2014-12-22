package com.example.towing.towme.maps;

import android.location.Location;
import android.support.annotation.NonNull;

import com.example.towing.towme.dispatch.DispatchActivity;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
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

    public static Location getLocation(LatLng latLng){
        Location result = new Location("");
        result.setLatitude(latLng.latitude);
        result.setLongitude(latLng.longitude);
        return result;
    }

    public void viewLocation(Location location) {
        if (mMap == null)return;
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(getLatLng(location), 10));
    }

    public void viewLocationArea(Location firstLocation, Location secondLocation
            , final DispatchActivity.simpleCallback callback) {
        if (mMap == null)return;
        LatLngBounds bounds = new LatLngBounds.Builder().include(getLatLng(firstLocation))
                .include(getLatLng(secondLocation)).build();
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds,300)
        ,new GoogleMap.CancelableCallback() {
            @Override
            public void onFinish() {
                if(callback!=null)
                    callback.done(null,null);
            }
            @Override
            public void onCancel() {

            }
        });
    }

    public void moveView(float xPixel,float yPixel){
        if (mMap == null)return;
        mMap.stopAnimation();
        mMap.animateCamera(CameraUpdateFactory.scrollBy(xPixel,yPixel));
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

    public void removeMarker(Marker marker) {
        if (mMap == null)return;
        marker.remove();
    }

    public static void setMarkerIcon(Marker marker,BitmapDescriptor icon){
        marker.setIcon(icon);
    }

    public static void moveMarker(Marker marker,Location location){
        marker.setPosition(getLatLng(location));
    }

    public static void setMarkerTitle(Marker marker,String title){
        marker.setTitle(title);
    }

    public static void setMarkerSnippet(Marker marker,String snippet){
        marker.setTitle(snippet);
    }

    public static Location getMarkerLocation(@NonNull Marker marker){
        return getLocation(marker.getPosition());
    }

    public void buildRoute(Location start, Location end) {
        if (mMap == null)return;
        //TODO: add a function that builds a route to the destination
    }


}
