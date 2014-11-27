package com.example.towing.towme;

import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Created by ahmedabdalla on 14-11-26.
 */
public class GetAddressTask extends AsyncTask<Location,Void,String> {

    private static Context mContext;
    private static final String LOG_TAG = GetAddressTask.class.getSimpleName();
    private TextView mAddress;
    private ProgressBar mActivityIndicator;

    public GetAddressTask(Context context,TextView textView,ProgressBar activityIndicator) {
        super();
        mContext = context;
        mAddress = textView;
        mActivityIndicator = activityIndicator;
    }

    @Override
    protected void onPreExecute() {
        // Show the activity indicator
        mActivityIndicator.setVisibility(View.VISIBLE);

        try {
            // Ensure that a Geocoder services is available
            if (Build.VERSION.SDK_INT <
                    Build.VERSION_CODES.GINGERBREAD
                    &&
                    Geocoder.isPresent())
                throw new IOException("Location services are not available." +
                        " please insure you have the latest version of android" +
                        " (Gingerbread or higher)");
        }catch (IOException e){
            Log.e(LOG_TAG,"Error: "+e);
            cancel(true);
        }
        super.onPreExecute();
    }

    /**
     * Get a Geocoder instance, get the latitude and longitude
     * look up the address, and return it
     *
     * @params params One or more Location objects
     * @return A string containing the address of the current
     * location, or an empty string if no address can be found,
     * or an error message
     */
    @Override
    protected String doInBackground(Location... params) {
        Geocoder geocoder =
                new Geocoder(mContext, Locale.getDefault());
        // Get the current location from the input parameter list
        Location loc = params[0];
        // Create a list to contain the result address
        List<Address> addresses = null;
        try {
                /*
                 * Return 1 address.
                 */
            addresses = geocoder.getFromLocation(loc.getLatitude(),
                    loc.getLongitude(), 1);
        } catch (IOException e1) {
            Log.e(LOG_TAG,
                    "IO Exception in getFromLocation()");
            e1.printStackTrace();
            return ("IO Exception trying to get address");
        } catch (IllegalArgumentException e2) {
            // Error message to post in the log
            String errorString = "Illegal arguments " +
                    Double.toString(loc.getLatitude()) +
                    " , " +
                    Double.toString(loc.getLongitude()) +
                    " passed to address service";
            Log.e("LocationSampleActivity", errorString);
            e2.printStackTrace();
            return errorString;
        }
        // If the reverse geocode returned an address
        if (addresses != null && addresses.size() > 0) {
            // Get the first address
            Address address = addresses.get(0);
                /*
                 * Format the first line of address (if available),
                 * city, and country name.
                 */
            String addressText = String.format(
                    "%s, %s, %s",
                    // If there's a street address, add it
                    address.getMaxAddressLineIndex() > 0 ?
                            address.getAddressLine(0) : "",
                    // Locality is usually a city
                    address.getLocality(),
                    // The country of the address
                    address.getCountryName());
            // Return the text
            return addressText;
        } else {
            return "No address found";
        }
    }


    @Override
    protected void onPostExecute(String address) {
        // Set activity indicator visibility to "gone"
        mActivityIndicator.setVisibility(View.GONE);
        // Display the results of the lookup.
        mAddress.setText(address);
    }
}
