package com.example.towing.towme.maps;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Created by ahmedabdalla on 14-11-26.
 */
public class GetAddressTask extends AsyncTask<Void,Void,Void> {

    protected static Context mContext;
    private static final String LOG_TAG = GetAddressTask.class.getSimpleName();
    protected ProgressBar mActivityIndicator;
    protected GmapInteraction mInteraction;
    protected Location mLocation;
    protected String mAddress = null;
    protected OnTaskCompleted mListener;

    public interface OnTaskCompleted{
        void onTaskCompleted(Location location,String address);
    }

    public GetAddressTask(Context context,GmapInteraction interaction
            ,ProgressBar activityIndicator,Location location,OnTaskCompleted listener) {
        this(context,interaction
                ,activityIndicator,location,null,listener);
    }

    public GetAddressTask(Context context,GmapInteraction interaction
            ,ProgressBar activityIndicator,Location location,String address,OnTaskCompleted listener) {
        super();
        mInteraction = interaction;
        mContext = context;
        mActivityIndicator = activityIndicator;
        mLocation = location;
        mAddress = address;
        mListener = listener;
    }

    @Override
    protected void onPreExecute() {
        // Show the activity indicator
        if(mActivityIndicator!=null)
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
    protected Void doInBackground(Void... params) {
        Geocoder geocoder =
                new Geocoder(mContext, Locale.getDefault());
        // Get the current location from the input parameter list
        Location loc = mLocation;// = params[0];
        // Create a list to contain the result address
        List<Address> addresses = null;
        try {
                /*
                 * Return 1 address.
                 */
            addresses = geocoder.getFromLocation(loc.getLatitude(),
                    loc.getLongitude(), 1);
//            geocoder.getFromLocationName("53 Lenthall Ave",1);
        } catch (IOException e1) {
            Log.e(LOG_TAG,
                    "IO Exception in getFromLocation()");
            e1.printStackTrace();
//            return ("IO Exception trying to get address");
        } catch (IllegalArgumentException e2) {
            // Error message to post in the log
            String errorString = "Illegal arguments " +
                    Double.toString(loc.getLatitude()) +
                    " , " +
                    Double.toString(loc.getLongitude()) +
                    " passed to address service";
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
            String subLocality = address.getSubLocality();
            String locality = address.getLocality();
            String adminArea = address.getAdminArea();
            String addressText = String.format(
                    "%s, %s, %s",
                    // If there's a street address, add it
                    address.getMaxAddressLineIndex() > 0 ?
                            address.getAddressLine(0) : "",
                    // Locality is usually a city
                    (subLocality!=null?subLocality:
                            (locality!=null?locality:
                                    (adminArea!=null?adminArea:""))),
                    // The country of the address
                    address.getCountryName());
            // Return the text

            mAddress = addressText;
            return null;
        }
//        else {
//              return "No address found";
//        }
        return null;
    }


    @Override
    protected void onPostExecute(Void mVoid) {
        // Set activity indicator visibility to "gone"
        if(mActivityIndicator!=null)
            mActivityIndicator.setVisibility(View.GONE);
        if(mAddress == null) return;
//        mInteraction.addMarker("Your Location",mAddress,mLocation);
//        mInteraction.viewLocation(mLocation);
        if(mListener!=null)
            mListener.onTaskCompleted(mLocation,mAddress);
    }
}
