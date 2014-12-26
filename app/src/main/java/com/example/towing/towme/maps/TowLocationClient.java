package com.example.towing.towme.maps;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.location.*;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.widget.Toast;

import com.example.towing.towme.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.*;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ahmedabdalla on 14-12-23.
 */
public class TowLocationClient implements GoogleApiClient.ConnectionCallbacks {


    private final static int
            CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    // Milliseconds per second
    private static final int MILLISECONDS_PER_SECOND = 1000;
    // Update frequency in seconds
    public static final int UPDATE_INTERVAL_IN_SECONDS = 1;
    // Update frequency in milliseconds
    private static final long UPDATE_INTERVAL =
            MILLISECONDS_PER_SECOND * UPDATE_INTERVAL_IN_SECONDS;
    // The fastest update frequency, in seconds
    private static final int FASTEST_INTERVAL_IN_SECONDS = 5;
    // A fast frequency ceiling in milliseconds
    private static final long FASTEST_INTERVAL =
            MILLISECONDS_PER_SECOND * FASTEST_INTERVAL_IN_SECONDS;
    // Amount of milliseconds in a day
    private static final long DAY_IN_MILLIS = 1000 * 60 * 60 * 24;
    private static long lastTimeChecked = System.currentTimeMillis();


    private static final String LOG_TAG = TowLocationClient.class.getSimpleName();
    // Define an object that holds accuracy and frequency parameters
    static com.google.android.gms.location.LocationRequest mLocationRequest;
    // context
    private static FragmentActivity mActivity;
    private static SharedPreferences mPrefs;
    private static SharedPreferences.Editor mEditor;
    private static boolean mUpdatesRequested;
    private static LocationClient mLocationClient;
    private static boolean isConnected;
    private static Location mCurrentLocation;

    public static interface TowLocationListener{
        void onLocationChanged(Location location);
    }

    private static List<TowLocationListener> locationListeners = new ArrayList<>();

    public static void addLocationListener(TowLocationListener listener){
        locationListeners.add(listener);
    }

    public static void removeLocationListener(TowLocationListener listener){
        locationListeners.remove(listener);
    }

    public static boolean hasLocationListener(TowLocationListener listener){
        return locationListeners.contains(listener);
    }
//
//    public TowLocationClient(FragmentActivity activity){
//        mActivity = activity;
//        onCreate();
//    }

    public static void setActivity(FragmentActivity activity) {
        if(mActivity==null)
            mActivity = activity;
        onCreate();
    }

    public static void onCreate() {
        if(mLocationClient==null) {
            // Create the LocationRequest object
            mLocationRequest = com.google.android.gms.location.LocationRequest.create();
            // Use high accuracy
            mLocationRequest.setPriority(
                    LocationRequest.PRIORITY_HIGH_ACCURACY);
            // Set the update interval to 5 seconds
            mLocationRequest.setInterval(UPDATE_INTERVAL);
            // Set the fastest update interval to 1 second
            mLocationRequest.setFastestInterval(FASTEST_INTERVAL);

            // Open the shared preferences
            mPrefs = PreferenceManager.getDefaultSharedPreferences(mActivity);
            // Get a SharedPreferences editor
            mEditor = mPrefs.edit();
            // Start with updates turned off
            mUpdatesRequested = false;

        /*
         * Create a new location client, using the enclosing class to
         * handle callbacks.
         */
            mLocationClient = new LocationClient(mActivity, mCallbacks, mFailedListener);
        }
    }


    private static FragmentManager getSupportFragmentManager(){
        return mActivity.getSupportFragmentManager();
    }

    public static void start(){
        // Connect the client.
        mLocationClient.connect();
    }

    public static void stop(){
        // Disconnecting the client invalidates it.
        isConnected = false;

        if (mLocationClient.isConnected()) {
            Location cow = mLocationClient.getLastLocation();
            if (cow == null)
                Log.v(LOG_TAG, "no updates found");
            /*
             * Remove location updates for a listener.
             * The current Activity is the listener, so
             * the argument is "this".
             */
            mLocationClient.removeLocationUpdates(mLocationListener);
        }
        mLocationClient.disconnect();
    }

    public static boolean isConnected(){
        return isConnected;
    }


    public static void pause(){
        // Save the current setting for updates
        mEditor.putBoolean(mActivity.getString(R.string.request_updates_key), mUpdatesRequested);
        mEditor.commit();
    }

    public static void resume(){
        /*
         * Get any previous setting for location updates
         * Gets "false" if an error occurs
         */
        String key = mActivity.getString(R.string.request_updates_key);
        if (mPrefs.contains(key)) {
            mUpdatesRequested =
                    mPrefs.getBoolean(key, false);

            // Otherwise, turn off location updates
        } else {
            mEditor.putBoolean(key, false);
            mEditor.commit();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        throw new UnsupportedOperationException("this isn't supposed to be called!");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e(LOG_TAG, "unable to connect");
    }

    // Define a DialogFragment that displays the error dialog
    public static class ErrorDialogFragment extends DialogFragment {
        // Global field to contain the error dialog
        private Dialog mDialog;

        // Default constructor. Sets the dialog field to null
        public ErrorDialogFragment() {
            super();
            mDialog = null;
        }

        // Set the dialog to display
        public void setDialog(Dialog dialog) {
            mDialog = dialog;
        }

        // Return a Dialog to the DialogFragment.
        @Override
        @android.support.annotation.NonNull
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return mDialog;
        }
    }

    public static void showErrorDialog(int resultCode) {
        // Get the error dialog from Google Play services
        Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
                resultCode,
                mActivity,
                CONNECTION_FAILURE_RESOLUTION_REQUEST);

        // If Google Play services can provide an error dialog
        if (errorDialog != null) {
            // Create a new DialogFragment for the error dialog
            ErrorDialogFragment errorFragment =
                    new ErrorDialogFragment();
            // Set the dialog in the DialogFragment
            errorFragment.setDialog(errorDialog);
            // Show the error dialog in the DialogFragment
            errorFragment.show(getSupportFragmentManager(),
                    "Location Updates");
        }
    }

    public static void onLocChanged(Location location){
        Log.d("locationTrack","core location updated");
        for(TowLocationListener mListener:locationListeners){
            mListener.onLocationChanged(location);
        }
        // Report to the UI that the location was updated
//        String msg = "Updated Location: " +
//                Double.toString(location.getLatitude()) + "," +
//                Double.toString(location.getLongitude());
        mCurrentLocation = location;
//        mPlacer.update(mCurrentLocation);
//        Toast.makeText(getActivity(), "Location changed...", Toast.LENGTH_SHORT).show();
    }


    private static GooglePlayServicesClient.OnConnectionFailedListener mFailedListener =
            new GooglePlayServicesClient.OnConnectionFailedListener() {
                @Override
                public void onConnectionFailed(ConnectionResult connectionResult) {
                    /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
                    if (connectionResult.hasResolution()) {
                        try {
                            // Start an Activity that tries to resolve the error
                            connectionResult.startResolutionForResult(
                                    mActivity,
                                    CONNECTION_FAILURE_RESOLUTION_REQUEST);
                /*
                 * Thrown if Google Play services canceled the original
                 * PendingIntent
                 */
                        } catch (IntentSender.SendIntentException e) {
                            // Log the error
                            e.printStackTrace();
                        }
                    } else {
            /*
             * If no resolution is available, display a dialog to the
             * user with the error.
             */
                        showErrorDialog(connectionResult.getErrorCode());
                    }
                }
            };

    private static GooglePlayServicesClient.ConnectionCallbacks mCallbacks =
            new GooglePlayServicesClient.ConnectionCallbacks() {
                @Override
                public void onConnected(Bundle bundle) {
                    // Display the connection status
                    Toast.makeText(mActivity, "Connected", Toast.LENGTH_SHORT).show();
                    isConnected = true;
                    // If already requested, start periodic updates
                    if (mUpdatesRequested) {
                        mLocationClient.requestLocationUpdates(mLocationRequest,mLocationListener);
                    }

                    Location lastLocation = mLocationClient.getLastLocation();
                    if(lastLocation!=null){
                        onLocChanged(lastLocation);
                    }
                }

                @Override
                public void onDisconnected() {
                    // Display the connection status
                    isConnected = false;
                    Toast.makeText(mActivity, "Disconnected. Please re-connect.",
                            Toast.LENGTH_SHORT).show();
                }
            };
    private static LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            onLocChanged(location);
        }
    };
}
