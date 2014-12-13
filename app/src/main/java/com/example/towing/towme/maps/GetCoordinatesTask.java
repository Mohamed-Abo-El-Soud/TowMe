package com.example.towing.towme.maps;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Created by Mohamed on 14-11-30.
 */
public class GetCoordinatesTask extends GetAddressTask {


    private static final String LOG_TAG = GetAddressTask.class.getSimpleName();


    public GetCoordinatesTask(Context context, GmapInteraction interaction
            , ProgressBar activityIndicator, String address,OnTaskCompleted listener) {
        this(context, interaction, activityIndicator, null, address,listener);
    }

    public GetCoordinatesTask(Context context, GmapInteraction interaction
            , ProgressBar activityIndicator, Location location, String address,OnTaskCompleted listener) {
        super(context, interaction, activityIndicator, location, address,listener);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

    }

    @Override
    protected Void doInBackground(Void... params) {
        Geocoder geocoder =
                new Geocoder(mContext, Locale.getDefault());
        // Create a list to contain the result address
        List<Address> addresses = null;
        try {
                /*
                 * Return 1 address.
                 */
            addresses = geocoder.getFromLocationName(mAddress,1);
        } catch (IOException e1) {
            Log.e(LOG_TAG,
                    "IO Exception in getFromLocation()");
            e1.printStackTrace();
//            return ("IO Exception trying to get coordinates");
        } catch (IllegalArgumentException e2) {
            // Error message to post in the log
            String errorString = "Illegal arguments " + mAddress +
                    " passed to coordinates service";
            Log.e("LocationSampleActivity", errorString);
            e2.printStackTrace();
//            return errorString;
        }
        // If the reverse geocode returned an address
        if (addresses != null && addresses.size() > 0) {
            // Get the first address
            Address address = addresses.get(0);
                /*
                 * Format the first line of address (if available),
                 * city, and country name.
                 */
            Double latitude = address.getLatitude();
            Double longitude = address.getLongitude();
            mLocation = new Location("");
            mLocation.setLatitude(latitude);
            mLocation.setLongitude(longitude);
            return null;
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void mVoid) {
        // Set activity indicator visibility to "gone"
        mActivityIndicator.setVisibility(View.GONE);
//        if(mLocation == null) return;
//        mInteraction.addMarker("entered location",mAddress,mLocation);
        if(mListener!=null)
            mListener.onTaskCompleted(mLocation,mAddress);
    }
}
