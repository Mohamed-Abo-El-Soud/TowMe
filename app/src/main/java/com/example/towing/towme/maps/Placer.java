package com.example.towing.towme.maps;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.LayoutTransition;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.towing.towme.LocationPost;
import com.example.towing.towme.R;
import com.example.towing.towme.Utilites;
import com.example.towing.towme.directions.GoogleDirection;
import com.example.towing.towme.dispatch.DispatchActivity;
import com.example.towing.towme.dispatch.DispatchActivity.simpleCallback;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.PolylineOptions;
import com.parse.DeleteCallback;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import org.json.JSONObject;
import org.w3c.dom.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Mohamed on 14-11-30.
 * {@link Placer} class is designed to handle all map-related actions. the major functions are the
 * {@link #Placer(GoogleMap, Context, View)}
 */
public class Placer {

    private static final String LOG_TAG = Placer.class.getSimpleName();
    private static final int MAX_POST_SEARCH_DISTANCE = 100;
    private static final int MAX_POST_COUNT = 5;

    private GmapInteraction interaction;
    private View mRootView;
    private Context mContext;
    private boolean once = true;
    private boolean viewCreated = false;
    private Location mLocation;
    Marker selfMarker;
    Marker destinationMarker;
    ArrayList<Marker> otherMarkers = new ArrayList<>();

    private clientResultInflater mClientInflater;
    private towTruckerResultInflater mTowInflater;
    private loadingInflater mLoadingInflater;

    /**
     * default constructor, which calls the initializer function
     * {@link #initialize(GoogleMap,Context,View)}.
     * */
    public Placer(GoogleMap map,Context context,View rootView){
        initialize( map, context, rootView);
    }

    /**
     * initializes all the necessary fields to manipulate the map.
     * */
    public void initialize (GoogleMap map,Context context,View rootView){
        mRootView = rootView;
        mContext = context;
        interaction = new GmapInteraction(map);
        map.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                mapQuery(cameraPosition.target);
            }
        });
    }

    public void onViewCreated(){
        // initialize the panel controls
        if(mClientInflater==null && mTowInflater==null && mLoadingInflater==null) {
            // retrieving the views
            LinearLayout clientResultsPanel =
                    (LinearLayout) mRootView.findViewById(R.id.results_panel);
            LinearLayout towTruckerResultsPanel
                    = (LinearLayout) mRootView.findViewById(R.id.trucker_panel);
            LinearLayout loadingResultsPanel =
                    (LinearLayout) mRootView.findViewById(R.id.loading_panel);
            // initializing the panel inflaters
            if (clientResultsPanel != null)
                mClientInflater = new clientResultInflater(clientResultsPanel);
            if (towTruckerResultsPanel != null)
                mTowInflater = new towTruckerResultInflater(towTruckerResultsPanel);
            if (loadingResultsPanel != null)
                mLoadingInflater = new loadingInflater(loadingResultsPanel);

            // set button click listeners
            mClientInflater.setAccept(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    acceptRequest(GmapInteraction.getLocation(FindATowPost.getmDestination())
                            ,mLocation,mClientInflater);
                }
            });
            mClientInflater.setReject(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    rejectRequest(mClientInflater,mLoadingInflater);
                }
            });
            mTowInflater.setReject(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    cancelTow(mTowInflater);
                }
            });
            mLoadingInflater.setReject(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    FindATowPost.removePost(connectivityIssues);
                    cancelTow(mLoadingInflater);
                }
            });
        }

        if(destinationMarker==null) {
            // initialize the destination marker
            // we will give it a dummy location, as well as dummy text for now
            destinationMarker = interaction.addVisual("dummy title", null
                    , GmapInteraction.getLocation(new LatLng(43.6599447, -79.4043498)));
//            destinationMarker = interaction.addVisual("dummy title", "dummy snippet"
//                    , GmapInteraction.getLocation(new LatLng(43.6599447, -79.4043498)));
            // change the color of the marker
            GmapInteraction.setMarkerIcon(destinationMarker, BitmapDescriptorFactory.defaultMarker(
                    BitmapDescriptorFactory.HUE_AZURE));
            // hide it for now
            destinationMarker.setVisible(false);
        }
        viewCreated = true;
    }

    /**
     * usually called when the location gets updated. Updates the map ui of the new map location
     * as well as check server for updates and other background processes
     * */
    public void update(Location currentSelfLocation){
        mLocation = currentSelfLocation;
        defineSelfLocation(currentSelfLocation);

        // update any ongoing requests/posts
        FindATowPost.updatePost(GmapInteraction.getGeoPoint(currentSelfLocation),clientFound
                ,connectivityIssues, new simpleCallback() {
            @Override
            public void done(Object first, Object second) {
                if(mClientInflater!=null)mClientInflater.hidePanel();
                Toast.makeText(mContext,"the user has cancelled the tow request",Toast.LENGTH_LONG)
                        .show();
                //  hide the marker shown
                destinationMarker.setVisible(false);
                // return back to the old view
                if(mLocation!=null)
                    interaction.viewLocation(mLocation);
                if(mLoadingInflater!=null)mLoadingInflater.displayPanel();
            }
        });

        LocationRequest.trackRequest(towFound,connectivityIssues,retrying);

//        LocationRequest.trackRequest(mContext);
//        if(once)
//            requestOtherMarkersPositions(currentSelfLocation);
//              getClosestTruck(currentSelfLocation);
//              LocationRequest.createRequest(Utilites.getUser()
//                      , interaction.getGeoPoint(currentSelfLocation), mContext);
//                LocationRequest.sendRequest(Utilites.getUser()
//                        ,interaction.getGeoPoint(currentSelfLocation),mContext);

        uploadLocation(currentSelfLocation);
        once = false;
//        extra();
    }

    public void getATow(Location currentSelfLocation){
        if(currentSelfLocation==null) return;

        // make sure the ui has been properly initialized
        if(!viewCreated) return;

        LocationRequest.trackRequest(towFound,connectivityIssues,retrying);

        LocationRequest.sendRequest(Utilites.getUser()
                ,GmapInteraction.getGeoPoint(currentSelfLocation)
                ,towRequested,connectivityIssues,noTrucksCallback
        );
    }

    /**
     * this callback is triggered whenever a trucker receives a request from a client, along with
     * their required information.
     * */
    private simpleCallback clientFound = new simpleCallback() {
        @Override
        public void done(Object bundle, Object second) {

            // hiding the loading panel if it is visible
            if(mLoadingInflater!=null)mLoadingInflater.hidePanel();

            Bundle items = (Bundle)bundle;
            String firstName = items.getString(DispatchActivity.FIRST_NAME);
            String lastName = items.getString(DispatchActivity.LAST_NAME);
            String carYear = items.getString(DispatchActivity.CAR_YEAR);
            String carMake = items.getString(DispatchActivity.CAR_MAKE);
            String carModel = items.getString(DispatchActivity.CAR_MODEL);
            Location location = items.getParcelable(FindATowPost.LOCATION_COLUMN);

            // put in all the required information
            mClientInflater.fillInViews(firstName, lastName, carYear, carMake, carModel);
            // view the panel
            if(mClientInflater!=null)mClientInflater.displayPanel();
            // adjusting the view of the map to accommodate both current location
            // and the location of the client
            setDestinationLocation(firstName + " " + lastName
                    , (carYear + " " + carMake + " " + carModel), location);
            interaction.viewLocationArea(mLocation, location, new simpleCallback() {
                @Override
                public void done(Object first, Object second) {
                    // after the first animation is done,
                    // move the view a little up to accommodate for the window at the bottom
//                    interaction.moveView(0, 200);
                }
            });
            createRoute(mLocation,location);
        }
    };

    private simpleCallback towRequested = new simpleCallback() {
        @Override
        public void done(Object first, Object second) {
            String standBy = "LOADING";
//            String searching = "Searching for available truckers...";
            String searching = "awaiting response...";
            if(mLoadingInflater!=null) {
                mLoadingInflater.displayPanel();
                mLoadingInflater.fillInViews(standBy, searching, null, null, null);
            }

        }
    };

    private simpleCallback noTrucksCallback = new simpleCallback() {
        @Override
        public void done(Object first, Object second) {

            // hiding the loading panel if it is visible
            if(mLoadingInflater!=null)mLoadingInflater.hidePanel();

            Toast.makeText(mContext, "there are no trucks near your current location"
                    , Toast.LENGTH_LONG).show();
        }
    };

    private simpleCallback retrying = new simpleCallback() {
        @Override
        public void done(Object first, Object second) {

            String searching = "tow cancelled, retrying...";
            if(mLoadingInflater!=null) {
                mLoadingInflater.displayPanel();
                mLoadingInflater.fillInViews(null, searching, null, null, null);
            }
            simpleCallback retryCallback = (simpleCallback)first;

            HashMap<String,simpleCallback> callbacks = new HashMap<>();
            callbacks.put(LocationRequest.ON_CONNECTIVITY_KEY,connectivityIssues);
            callbacks.put(LocationRequest.ON_SUCCESS_KEY,towRequested);
            callbacks.put(LocationRequest.ON_NO_TRUCKERS,noTrucksCallback);
            retryCallback.done(GmapInteraction.getGeoPoint(mLocation),callbacks);

        }
    };

    private simpleCallback waitingForClients = new simpleCallback() {
        @Override
        public void done(Object first, Object second) {
            String standBy = "STAND BY";
            String searching = "Searching for nearby clients...";
            if (mLoadingInflater != null) {
                mLoadingInflater.displayPanel();
                mLoadingInflater.fillInViews(standBy, searching, null, null, null);
            }
        }
    };

    private simpleCallback towFound = new simpleCallback() {
        @Override
        public void done(Object bundle, Object unUsed) {

            // hiding the loading panel if it is visible
            if(mLoadingInflater!=null)mLoadingInflater.hidePanel();

            // display the view
            if(mTowInflater!=null)mTowInflater.displayPanel();

            Bundle items = (Bundle)bundle;
            String firstName = items.getString(DispatchActivity.FIRST_NAME);
            String lastName = items.getString(DispatchActivity.LAST_NAME);
            Location location = items.getParcelable(LocationRequest.LOCATION_COLUMN);

            // put in all the required information
            mTowInflater.fillInViews(firstName, lastName, null, null, null);
            // adjusting the view of the map to accommodate both current location
            // and the location of the client
            setDestinationLocation(firstName + " " + lastName
                    , "Tow truck driver", location);
            interaction.viewLocationArea(mLocation, location, new simpleCallback() {
                @Override
                public void done(Object first, Object second) {
                    // after the first animation is done,
                    // move the view a little up to accommodate for the window at the bottom
                    interaction.moveView(0, 200);
                }
            });


        }
    };

    private simpleCallback connectivityIssues = new simpleCallback() {
        @Override
        public void done(Object exception, Object second) {
            if (exception != null) {
                ParseException e = (ParseException) exception;
                Log.e(LOG_TAG, "Error: " + e);
                e.printStackTrace();
            }
            Toast.makeText(mContext, "the app could not connect to the internet"
                    , Toast.LENGTH_LONG).show();
        }
    };

    /**
     * a helper function for the the trucker user to set another marker at a potential client
     * */
    public void setDestinationLocation(String title, String snippet, Location location){
        // make sure the ui has been properly initialized
        if(!viewCreated) return;
            // make the marker visible, move it
            // to the appropriate location and change
            // its text contents
            if(!destinationMarker.isVisible())
                destinationMarker.setVisible(true);
            GmapInteraction.moveMarker(destinationMarker,location);
            if(snippet!=null)
                GmapInteraction.setMarkerSnippet(destinationMarker,snippet);
            if(title!=null)
                GmapInteraction.setMarkerTitle(destinationMarker, title);
    }

    public void findAClient(Location currentSelfLocation){
        if(currentSelfLocation==null) return;

        if(!viewCreated) return;
        FindATowPost.createPost(Utilites.getUser(),GmapInteraction.getGeoPoint(currentSelfLocation)
                ,connectivityIssues,waitingForClients);
    }

    private void acceptRequest(Location destination, Location currentLocation
            , PanelInflater inflater){
        if(inflater!=null)inflater.hidePanel();
        if(destination!=null && currentLocation!=null) {
            Location sAddress =  currentLocation;
            Location dAddress =  destination;
            final Intent intent = new Intent(Intent.ACTION_VIEW
                    , Uri.parse("http://maps.google.com/maps?" + "saddr="
                    + sAddress.getLatitude()
                    + "," + sAddress.getLongitude() + "&daddr="
                    + dAddress.getLatitude()
                    + "," + dAddress.getLongitude()));
            intent.setClassName("com.google.android.apps.maps"
                    , "com.google.android.maps.MapsActivity");
            // signal to the car driver (who made the request) that the
            // request has been acknowledged
            FindATowPost.acceptRequest(connectivityIssues,new simpleCallback() {
                @Override
                public void done(Object first, Object second) {
                    // then go to the navigation activity
                    mContext.startActivity(intent);
                }
            });
        }
    }

    private void rejectRequest(PanelInflater inflater, PanelInflater secondPanel){
        if(inflater!=null)inflater.hidePanel();
        //  hide the marker shown
        destinationMarker.setVisible(false);
        // remove the polylines
        interaction.removeRoute();
        // return back to the old view
        if(mLocation!=null)
            interaction.viewLocation(mLocation);
        FindATowPost.rejectRequest(connectivityIssues);

        if(secondPanel!=null)secondPanel.displayPanel();
    }

    private void cancelTow(PanelInflater inflater){
        if(inflater!=null)inflater.hidePanel();
        LocationRequest.removeRequest(new simpleCallback() {
            @Override
            public void done(Object first, Object second) {
                if(!(Boolean)first)
                    connectivityIssues.done(second,null);
            }
        });
        //  hide the marker shown
        destinationMarker.setVisible(false);
        // return back to the old view
        if(mLocation!=null)
            interaction.viewLocation(mLocation);
    }

    public void onOrientationChanged(){
        if(mClientInflater.isVisible()){
            mClientInflater.displayPanel();
        } else
            mClientInflater.hidePanel();

        if(mLoadingInflater.isVisible()){
            mLoadingInflater.displayPanel();
        } else
            mLoadingInflater.hidePanel();

        if(mTowInflater.isVisible()){
            mTowInflater.displayPanel();
        } else
            mTowInflater.hidePanel();
    }

    /**
     * fetches the address of the current location. Also moves the marker to the current location.
     * */
    public void defineSelfLocation(Location location){
        final ProgressBar bar = (ProgressBar)mRootView.findViewById(R.id.address_progress);
        if(bar.getVisibility()!=View.VISIBLE)
            bar.setVisibility(View.VISIBLE);
        simpleCallback onSuccess = new simpleCallback() {
            @Override
            public void done(Object first, Object second) {
                Location location = (Location)first;
                String address = (String)second;
                if(location==null) return;
                if(bar.getVisibility()!=View.GONE)
                    bar.setVisibility(View.GONE);
                if(selfMarker!=null){
                    GmapInteraction.moveMarker(selfMarker, location);
                }
                else{
                    selfMarker = interaction.addVisual(mContext.getString(
                            R.string.your_location_title),address,location);
                    interaction.viewLocation(location);
                }
            }
        };
        simpleCallback onFail = new simpleCallback() {
            @Override
            public void done(Object first, Object second) {
                Location location = (Location)first;
                if(location==null) return;
                if(bar.getVisibility()!=View.GONE)
                    bar.setVisibility(View.GONE);
                if(selfMarker!=null){
                    GmapInteraction.moveMarker(selfMarker, location);
                }
                else{
                    selfMarker = interaction.addVisual(mContext.getString(
                            R.string.your_location_title), null, location);
                    interaction.viewLocation(location);
                }
            }
        };
        new GetAddressTask(mContext, location, onSuccess, onFail).execute();
    }

    /**
     *
     * */
    @SuppressWarnings("unchecked")
    public void requestOtherMarkersPositions(final Location location){
        ParseQuery<ParseObject> mapQuery = ParseQuery.getQuery("TowTruckers");
        mapQuery.whereWithinKilometers("location"
                , interaction.getGeoPoint(location), MAX_POST_SEARCH_DISTANCE);
        mapQuery.setLimit(MAX_POST_COUNT);
        mapQuery.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> parseObjects, ParseException e) {
                if(e == null) {
                    Toast.makeText(mContext, "received all truckers", Toast.LENGTH_LONG).show();
                    putMarkerPositions(parseObjects);
                }
                else {
                    Log.e(LOG_TAG,"Error: "+e);
                    e.printStackTrace();
                }
            }
        });


//        ParseCloud.callFunctionInBackground("requestTowTruckers"
//                ,new HashMap<String, Object>(),new FunctionCallback<ArrayList<ParseObject>>() {
//            @Override
//            public void done(ArrayList<ParseObject> o, ParseException e) {
//                if (e == null) {
//                    Toast.makeText(mContext,"received all truckers", Toast.LENGTH_LONG).show();
//                    putMarkerPositions(o,interaction.getGeoPoint(location));
//                }
//            }
//        });


//        ParseQuery<ParseUser> userQuery = ParseUser.getQuery();
//        userQuery.whereEqualTo("username","mandarin");
//        userQuery.getFirstInBackground(new GetCallback<ParseUser>() {
//            @Override
//            public void done(ParseUser parseUser, ParseException e) {
//                if(parseUser != null)
//                    parseUser.deleteInBackground(new DeleteCallback() {
//                        @Override
//                        public void done(ParseException e) {
//                            if(e!=null){
//                                Log.e(LOG_TAG,"Error: "+e);
//                                e.printStackTrace();
//                            }
//                        }
//                    });
//            }
//        });
    }

    /**
     * Fetches the closest trucks within a 100km radius. Retrieves a list of truckers, ordered by
     * proximity. In addition, markers are created for each trucker and the UI is updated. This
     * function is designed to be called once, since the markers are created, rather than updated.
     * Updating existing markers is a more complex process, since there is a possibility that more
     * truckers are added/deleted/replaced. This means that the fetched list of truckers, have to
     * be cross-referenced with the list of local truck markers. Truckers that are not found locally
     * are then added, and the local trucks that are not fetched can be deleted. This is then an
     * expensive implementation, and requires a more comprehensive approach.
     *
     * @param location the current location when the function is called
     * */
    public void getClosestTruck(final Location location){
        ParseQuery<ParseObject> mapQuery = ParseQuery.getQuery("TowTruckers");
        mapQuery.whereWithinKilometers("location"
                , interaction.getGeoPoint(location), MAX_POST_SEARCH_DISTANCE);
        mapQuery.getFirstInBackground(new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject parseObject, ParseException e) {
                if (e == null) {
                    Toast.makeText(mContext, "received all truckers", Toast.LENGTH_LONG).show();
                    ParseGeoPoint geoPoint = parseObject.getParseGeoPoint("location");
                    otherMarkers.add(interaction.addMarker((String)parseObject.get("place"),null,geoPoint));
                    if(mListener!=null)mListener.showDialog(location,interaction.getLocation(geoPoint));
                } else {
                    Log.e(LOG_TAG, "Error: " + e);
                    e.printStackTrace();
                }
            }
        });

    }

    public void sendTruckRequest(final Location location){
        ParseQuery<ParseObject> mapQuery = ParseQuery.getQuery("TowTruckers");
        mapQuery.whereWithinKilometers("location"
                , interaction.getGeoPoint(location), MAX_POST_SEARCH_DISTANCE);
        mapQuery.getFirstInBackground(new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject towTrucker, ParseException e) {
                if(e==null){
//                    towTrucker.put();
                } else {
                    Log.e(LOG_TAG, "Error: " + e);
                    e.printStackTrace();
                    Toast.makeText(mContext,"unfortunately, an error has occurred."
                            ,Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    public interface dialogListener{
        void showDialog(Location source, Location destination);
    }

    private dialogListener mListener;

    public void setDialogListener(dialogListener listener){
        mListener = listener;
    }

    /**
     * given a list of ParseObjects, for each trucker ParseObject in the list, the coordinates
     * are fetched and markers are created in the UI. The marker is then added to the
     * {@link #otherMarkers} ArrayList field for future access.
     *
     * @param list the list of fetched ParseObjects from the server
     * */
    public void putMarkerPositions(List<ParseObject> list){
        for(ParseObject trucker:list){
            ParseGeoPoint geoPoint = trucker.getParseGeoPoint("location");
            otherMarkers.add(interaction.addMarker((String)trucker.get("place"),null,geoPoint));
        }
    }

    /**
     * A simple function that updates the current {@link LocationPost} that is currently stored in
     * the {@link Utilites} class. If the LocationPost does not exist in the server side, a
     * LocationPost is created for you.
     * */
    public void uploadLocation(final Location location){
        final LocationPost locationPost = Utilites.getLocationPost();
        if (locationPost == null) return;
        locationPost.fetchInBackground(new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject parseObject, ParseException e) {
                if(e==null) {
                    LocationPost locationP = (LocationPost) parseObject;
                    locationP.setLocation(location);
                    locationP.saveInBackground();
                }
                else {
                    newUploadLocation(locationPost,location);
                }
            }
        });
    }

    /**
     * A simple helper function that, given a {@link LocationPost} and a {@link Location}, the
     * helper sets the location of the LocationPost and updates the server.
     *
     *
     * @param location the current location when the function is called.
     * @param lPost the LocationPost that needs to be updated.
     *
     * */
    public void newUploadLocation(LocationPost lPost,Location location){

        lPost.setLocation(location);
        lPost.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if(e!=null){
                    Toast.makeText(mContext, "Could not get location", Toast.LENGTH_LONG).show();
                    Log.e(LOG_TAG, "Error: " + e);
                    e.printStackTrace();
                }
                else {
                    Toast.makeText(mContext, "created new post", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    public void mapQuery(LatLng position){

    }

    public void createRoute(Location start, Location end){
        if(mContext==null)return;
        interaction.buildRoute(start,end,mContext,null);
    }

}


interface PanelInflater{
    public void setViews(View view);
    public void setAccept(View.OnClickListener listener);
    public void setReject(View.OnClickListener listener);
    public void fillInViews(String firstName,String lastName,String carYear
            ,String carMake,String carModel);
    public void displayPanel();
    public void hidePanel();
}

class animatedInflater implements PanelInflater{

    protected LinearLayout mRootView;
    protected boolean viewsSet = false;
    private boolean isVisible = false;
    // fade in/out transition duration in milliseconds
    protected final static long ANIMATION_DURATION = 500;

    @Override
    public void setViews(View view) {
        mRootView = (LinearLayout)view;
        hidePanel();
        viewsSet = true;
    }

    @Override
    public void setAccept(View.OnClickListener listener) {

    }

    @Override
    public void setReject(View.OnClickListener listener) {

    }

    @Override
    public void fillInViews(String firstName, String lastName, String carYear, String carMake, String carModel) {

    }

    public boolean isVisible(){
        return isVisible;
    }

    @Override
    public void displayPanel() {
        isVisible = true;
        if(mRootView.getVisibility()==View.VISIBLE) return;
        // Set the view to 0% opacity but visible, so that it is visible
        // (but fully transparent) during the animation.
        mRootView.setAlpha(0f);
        mRootView.setVisibility(View.VISIBLE);

        // Animate the view to 100% opacity, and clear any animation
        // listener set on the view.
        mRootView.animate()
                .alpha(1f)
                .setDuration(ANIMATION_DURATION)
                .setListener(null);
    }

    @Override
    public void hidePanel() {
        isVisible = false;
        if(mRootView.getVisibility()==View.GONE) return;
        // Animate the view to 0% opacity. After the animation ends,
        // set its visibility to GONE as an optimization step (it won't
        // participate in layout passes, etc.)
        mRootView.animate()
                .alpha(0f)
                .setDuration(ANIMATION_DURATION)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mRootView.setVisibility(View.GONE);
                    }
                });
    }
}

class clientResultInflater extends animatedInflater{

//    private LinearLayout mRootView;

    private TextView firstNamefield;
    private TextView lastNamefield;
    private TextView carYearfield;
    private TextView carMakefield;
    private TextView carModelfield;

    private Button acceptButton;
    private Button rejectButton;

//    private boolean viewsSet = false;

    clientResultInflater(View view){
        setViews(view);
    }

    @Override
    public void setViews(View view){
        mRootView = (LinearLayout)view;

        hidePanel();

        firstNamefield = (TextView) mRootView.findViewById(R.id.first_name);
        lastNamefield = (TextView) mRootView.findViewById(R.id.last_name);
        carYearfield = (TextView) mRootView.findViewById(R.id.car_year);
        carMakefield = (TextView) mRootView.findViewById(R.id.car_make);
        carModelfield = (TextView) mRootView.findViewById(R.id.car_model);

        viewsSet = true;

        acceptButton = (Button) mRootView.findViewById(R.id.accept_button);
        rejectButton = (Button) mRootView.findViewById(R.id.reject_button);

    }

    @Override
    public void setAccept(View.OnClickListener listener){
        if(!viewsSet)return;
        acceptButton.setOnClickListener(listener);
    }

    @Override
    public void setReject(View.OnClickListener listener){
        if(!viewsSet)return;
        rejectButton.setOnClickListener(listener);
    }

    @Override
    public void fillInViews(String firstName,String lastName,String carYear
            ,String carMake,String carModel){

        if(!viewsSet) return;
        if(firstName!=null)
            firstNamefield.setText(firstName);
        if(lastName!=null)
            lastNamefield.setText(lastName);
        if(carYear!=null)
            carYearfield.setText(carYear);
        if(carMake!=null)
            carMakefield.setText(carMake);
        if(carModel!=null)
            carModelfield.setText(carModel);

    }

    @Override
    public void displayPanel(){
        super.displayPanel();
//        if(!viewsSet)return;
//        if(mRootView.getVisibility()==View.VISIBLE) return;
//        mRootView.setVisibility(View.VISIBLE);
    }

    @Override
    public void hidePanel(){
        super.hidePanel();
//        if(!viewsSet)return;
//        if(mRootView.getVisibility()==View.GONE) return;
//        mRootView.setVisibility(View.GONE);
    }

}

class towTruckerResultInflater extends animatedInflater{


    private TextView firstNamefield;
    private TextView lastNamefield;

    private Button rejectButton;

    towTruckerResultInflater(View view){
        setViews(view);
    }

    @Override
    public void setViews(View view){
        mRootView = (LinearLayout)view;

        hidePanel();

        firstNamefield = (TextView) mRootView.findViewById(R.id.first_name);
        lastNamefield = (TextView) mRootView.findViewById(R.id.last_name);
        rejectButton = (Button) mRootView.findViewById(R.id.reject_button);

        viewsSet = true;

    }

    @Override
    public void setAccept(View.OnClickListener listener) {

    }

    @Override
    public void setReject(View.OnClickListener listener){
        if(!viewsSet)return;
        rejectButton.setOnClickListener(listener);
    }

    @Override
    public void fillInViews(String firstName, String lastName
            , String carYear, String carMake, String carModel){

        if(!viewsSet) return;

        if(firstName!=null)
            firstNamefield.setText(firstName);
        if(lastName!=null)
            lastNamefield.setText(lastName);

    }


    @Override
    public void displayPanel(){
        super.displayPanel();
//        if(!viewsSet)return;
//        if(mRootView.getVisibility()==View.VISIBLE) return;
//        mRootView.setVisibility(View.VISIBLE);
    }

    @Override
    public void hidePanel(){
        super.hidePanel();
//        if(!viewsSet)return;
//        if(mRootView.getVisibility()==View.GONE) return;
//        mRootView.setVisibility(View.GONE);
    }

}

class loadingInflater extends animatedInflater{

    private TextView titleField;
    private TextView snippetField;

    private Button rejectButton;

    loadingInflater(View view){
        setViews(view);
    }

    @Override
    public void setViews(View view) {
        mRootView = (LinearLayout)view;

        hidePanel();

        titleField = (TextView) mRootView.findViewById(R.id.title_bar);
        snippetField = (TextView) mRootView.findViewById(R.id.snippet_bar);
        rejectButton = (Button) mRootView.findViewById(R.id.reject_button);

        viewsSet = true;
    }

    @Override
    public void setAccept(View.OnClickListener listener) {
        // nothing
    }

    @Override
    public void setReject(View.OnClickListener listener) {
        if(!viewsSet)return;
        rejectButton.setOnClickListener(listener);
    }

    @Override
    public void fillInViews(String title, String snippet,
                            String carYear, String carMake, String carModel) {
        if(!viewsSet) return;

        if(title!=null)
            titleField.setText(title);
        if(snippet!=null)
            snippetField.setText(snippet);
    }


    @Override
    public void displayPanel(){
        super.displayPanel();
//        if(!viewsSet)return;
//        if(mRootView.getVisibility()==View.VISIBLE) return;
//        mRootView.setVisibility(View.VISIBLE);
    }

    @Override
    public void hidePanel(){
        super.hidePanel();
//        if(!viewsSet)return;
//        if(mRootView.getVisibility()==View.GONE) return;
//        mRootView.setVisibility(View.GONE);
    }
}
