package com.example.towing.towme.maps;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.*;
import android.util.Log;
import android.widget.Toast;

import com.example.towing.towme.MapsActivity;
import com.example.towing.towme.R;
import com.example.towing.towme.dispatch.DispatchActivity;

public class LocationTrackingService extends Service implements
        TowLocationClient.TowLocationListener{
    public LocationTrackingService() {
    }

    @Override
    public void onLocationChanged(Location location) {

        Log.d("locationTrack","location has changed, and I'm a service");
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();
        startLocationTrack();
        // If we get killed, after returning from here, don't restart
        return START_NOT_STICKY;
    }

    public void startLocationTrack(){
        if(isTracking) return;
        isTracking = true;
        if(!TowLocationClient.hasLocationListener(LocationTrackingService.this)) {
            TowLocationClient.addLocationListener(LocationTrackingService.this);
        }
        if(!TowLocationClient.isConnected()){
            TowLocationClient.resume();
            TowLocationClient.start();
        }
        locationTrack();
    }

    static boolean isTracking = false;

    public void locationTrack(){

        Log.d("locationTrack","service started");

        Intent i=new Intent(this, MapsActivity.class);

        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|
                Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pi= PendingIntent.getActivity(this, 0, i, 0);

        Notification.Builder builder = new Notification.Builder(this);
        // if the android version is Lollipop or newer
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setColor(getResources().getColor(R.color.primary_light));
        }
        builder = builder
                .setSmallIcon(R.drawable.ic_launcher_white)
                .setSubText("click here to stop it")
                .setWhen(System.currentTimeMillis())
                .setContentTitle("Location tracking service")
                .setContentText("Location tracking right now...")
                .setContentIntent(pi);

        Notification note = builder.build();

        note.flags|= Notification.FLAG_NO_CLEAR;

        startForeground(1337, note);
    }



    public void stopLocationTrack(){
        isTracking = false;
        TowLocationClient.removeLocationListener(this);
        if(TowLocationClient.isConnected()) {
            TowLocationClient.pause();
            TowLocationClient.stop();
        }
        stopForeground(true);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

    @Override
    public void onDestroy() {
        Log.d("locationTrack","service destroyed");
        stopLocationTrack();
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
    }

}
