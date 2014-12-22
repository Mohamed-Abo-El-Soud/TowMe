package com.example.towing.towme.maps;

import android.app.Dialog;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.towing.towme.DrawerItemClickListener;
import com.example.towing.towme.MapsActivity;
import com.example.towing.towme.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;

/**
 * Created by ahmedabdalla on 14-11-26.
 */
public class MapFragment extends Fragment implements
        GooglePlayServicesClient.ConnectionCallbacks
        ,GoogleApiClient.ConnectionCallbacks
        , GooglePlayServicesClient.OnConnectionFailedListener
        , LocationListener
    ,DrawerItemClickListener.FragmentWithName
{

    public static final String LOG_TAG = MapFragment.class.getSimpleName();

    @Override
    public String getName() {
        return LOG_TAG;
    }

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


    // Define an object that holds accuracy and frequency parameters
    LocationRequest mLocationRequest;

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private Placer mPlacer;
    private LocationClient mLocationClient;
    boolean mUpdatesRequested;
    boolean isConnected = false;
    boolean once = true;
    // Global variable to hold the current location
    Location mCurrentLocation;
    private SharedPreferences mPrefs;
    private SharedPreferences.Editor mEditor;
    private View mRootview;

    public static final String ARG_OPTION_NAME = ".option_name";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeValues();
    }

    @Override
    public View onCreateView(LayoutInflater inflater
            , @Nullable ViewGroup container
            , @Nullable Bundle savedInstanceState) {
        if (mRootview == null)
            mRootview = inflater.inflate(R.layout.fragment_map, container, false);
        else
            ((ViewGroup)mRootview.getParent()).removeView(mRootview);
        return mRootview;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        setUpMapIfNeeded();
        mRootview.findViewById(R.id.request_tow).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPlacer.getATow(mCurrentLocation);
            }
        });
        mRootview.findViewById(R.id.find_client).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPlacer.findAClient(mCurrentLocation);
            }
        });
        mPlacer.onViewCreated();
        super.onViewCreated(view, savedInstanceState);
    }

    private FragmentManager getSupportFragmentManager(){
        return getActivity().getSupportFragmentManager();
    }

    @Override
    public void onStart() {
        super.onStart();
        // Connect the client.
        mLocationClient.connect();
    }

    /*
     * Called when the Activity is no longer visible.
     */
    @Override
    public void onStop() {
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
            mLocationClient.removeLocationUpdates(this);
        }
        mLocationClient.disconnect();
        super.onStop();
    }

    @Override
    public void onPause() {
        // Save the current setting for updates
        mEditor.putBoolean(getString(R.string.request_updates_key), mUpdatesRequested);
        mEditor.commit();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        /*
         * Get any previous setting for location updates
         * Gets "false" if an error occurs
         */
        String key = getString(R.string.request_updates_key);
        if (mPrefs.contains(key)) {
            mUpdatesRequested =
                    mPrefs.getBoolean(key, false);

            // Otherwise, turn off location updates
        } else {
            mEditor.putBoolean(key, false);
            mEditor.commit();
        }
    }

    private void initializeValues(){
        // Create the LocationRequest object
        mLocationRequest = LocationRequest.create();
        // Use high accuracy
        mLocationRequest.setPriority(
                LocationRequest.PRIORITY_HIGH_ACCURACY);
        // Set the update interval to 5 seconds
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        // Set the fastest update interval to 1 second
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);

        // Open the shared preferences
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        // Get a SharedPreferences editor
        mEditor = mPrefs.edit();
        // Start with updates turned off
        mUpdatesRequested = false;

        /*
         * Create a new location client, using the enclosing class to
         * handle callbacks.
         */
        mLocationClient = new LocationClient(getActivity(), this, this);
//        setUpMapIfNeeded();
    }
    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #onLocationChanged(android.location.Location)} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link com.google.android.gms.maps.SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mRootview == null) return;
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            Fragment mapFragment = getChildFragmentManager().findFragmentById(R.id.map_fragment);
            if (mapFragment == null) return;
            mMap = ((SupportMapFragment)mapFragment).getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                mPlacer = new Placer(mMap,getActivity(),mRootview);
                MapsActivity activity = (MapsActivity)getActivity();
                mPlacer.setDialogListener(activity);
//                setUpMap();
//                Toast.makeText(this, "could not find map", Toast.LENGTH_LONG);
            }
        }
    }



    @Override
    public void onLocationChanged(Location location) {
        onLocChanged(location);
    }

    public void onLocChanged(Location location){
        // Report to the UI that the location was updated
//        String msg = "Updated Location: " +
//                Double.toString(location.getLatitude()) + "," +
//                Double.toString(location.getLongitude());
        mCurrentLocation = location;
        mPlacer.update(mCurrentLocation);
//        Toast.makeText(getActivity(), "Location changed...", Toast.LENGTH_SHORT).show();
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

    public void showErrorDialog(int resultCode) {
        // Get the error dialog from Google Play services
        Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
                resultCode,
                getActivity(),
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

    @Override
    public void onConnected(Bundle bundle) {
        // Display the connection status
        Toast.makeText(getActivity(), "Connected", Toast.LENGTH_SHORT).show();
        isConnected = true;
        // If already requested, start periodic updates
        if (mUpdatesRequested) {
            mLocationClient.requestLocationUpdates(mLocationRequest,this);
        }

        Location lastLocation = mLocationClient.getLastLocation();
        if(lastLocation!=null){
            onLocChanged(lastLocation);
        }
    }


    @Override
    public void onConnectionSuspended(int i) {
        Log.e(LOG_TAG,"unable to connect");
    }

    @Override
    public void onDisconnected() {
        // Display the connection status
        isConnected = false;
        Toast.makeText(getActivity(), "Disconnected. Please re-connect.",
                Toast.LENGTH_SHORT).show();
    }

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
                        getActivity(),
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


}
