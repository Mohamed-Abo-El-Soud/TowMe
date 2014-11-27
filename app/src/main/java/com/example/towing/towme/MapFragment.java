package com.example.towing.towme;

import android.app.Dialog;
import android.content.Intent;
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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * Created by ahmedabdalla on 14-11-26.
 */
public class MapFragment extends Fragment implements
        GooglePlayServicesClient.ConnectionCallbacks
        ,GoogleApiClient.ConnectionCallbacks
        , GooglePlayServicesClient.OnConnectionFailedListener
        , LocationListener{

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
    private static final String LOG_TAG = MapsActivity.class.getSimpleName();


    // Define an object that holds accuracy and frequency parameters
    LocationRequest mLocationRequest;

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
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
    public void onDestroy() {
        super.onDestroy();
    }

    //    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        int id = item.getItemId();
//        if (id == R.id.action_settings) {
//            startActivity(new Intent(getActivity(), SettingsActivity.class));
//            return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }

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
//        mPrefs = getSharedPreferences("SharedPreferences",
//                Context.MODE_PRIVATE);

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
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
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
//            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_fragment))
//                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
//                setUpMap();
//                Toast.makeText(this, "could not find map", Toast.LENGTH_LONG);
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
//        Location m = mMap.getMyLocation();
//        if (m== null)
//            Log.v(LOG_TAG,"try again");
        mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));

    }

    private final static int
            CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    @Override
    public void onLocationChanged(Location location) {
        // Report to the UI that the location was updated
        String msg = "Updated Location: " +
                Double.toString(location.getLatitude()) + "," +
                Double.toString(location.getLongitude());
        mCurrentLocation = location;
        if(once) {
            getAddress();
            once = false;
        }
//        Log.v(LOG_TAG, "Location changed...");
//        Log.v(LOG_TAG, msg);
        Toast.makeText(getActivity(), "Location changed...", Toast.LENGTH_SHORT).show();
//        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
//        if(mMap != null)
//            mMap.addMarker(new MarkerOptions().position(new LatLng(mCurrentLocation.getLatitude()
//                    , mCurrentLocation.getLongitude())));
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
//            servicesConnected();
//            mCurrentLocation = mLocationClient.getLastLocation();
//            if (mCurrentLocation != null)
//                mMap.addMarker(new MarkerOptions().position(new LatLng(mCurrentLocation.getLatitude()
//                        , mCurrentLocation.getLongitude())));
//            Boolean cow = mLocationClient.isConnected();//.getLastLocation();
//            if (cow == null)
//                Log.v(LOG_TAG, "began requests");
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


    /*
    * Handle results returned to the FragmentActivity
    * by Google Play services
    */
//    @Override
//    protected void onActivityResult(
//            int requestCode, int resultCode, Intent data) {
//        // Decide what to do based on the original request code
//        switch (requestCode) {
//            case CONNECTION_FAILURE_RESOLUTION_REQUEST:
//            /*
//             * If the result code is Activity.RESULT_OK, try
//             * to connect again
//             */
//                switch (resultCode) {
//                    case Activity.RESULT_OK:
//                    /*
//                     * Try the request again
//                     */
//                        break;
//                }
//        }
//    }

    private boolean servicesConnected() {
        // Check that Google Play services is available
        int resultCode =
                GooglePlayServicesUtil.
                        isGooglePlayServicesAvailable(getActivity());
        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {
            // In debug mode, log the status
            Log.d("Location Updates",
                    "Google Play services is available.");
            // Continue
            return true;
            // Google Play services was not available for some reason
        } else {
            // Get the error code
            int errorCode = resultCode;//connectionResult.getErrorCode();
            // Get the error dialog from Google Play services
            Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
                    errorCode,
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
                errorFragment.show(
                        getSupportFragmentManager(),
                        "Location Updates");

            }
        }
        return false;
    }

    /**
     * The "Get Address" button in the UI is defined with
     * android:onClick="getAddress". The method is invoked whenever the
     * user clicks the button.
     */
    public void getAddress() {
        GetAddressTask task =
                new GetAddressTask(getActivity()
                        ,(TextView)mRootview.findViewById(R.id.address_location)
                        ,(ProgressBar)mRootview.findViewById(R.id.address_progress));
        task.execute(mCurrentLocation);
    }

}
