package com.example.towing.towme.maps;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
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
import com.example.towing.towme.dispatch.DispatchActivity;
import com.example.towing.towme.dispatch.DispatchActivity.simpleCallback;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
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
    }

    /**
     * fetches the address of the current location. Also moves the marker to the current location.
     * */
    public void defineSelfLocation(Location location){
        final ProgressBar bar = (ProgressBar)mRootView.findViewById(R.id.address_progress);
        if(bar.getVisibility()!=View.VISIBLE)
            bar.setVisibility(View.VISIBLE);
        new GetAddressTask(mContext
                ,interaction
                ,(ProgressBar)mRootView.findViewById(R.id.address_progress)
                ,location
                ,new GetAddressTask.OnTaskCompleted() {
            @Override
            public void onTaskCompleted(Location location, String address) {
                if(location==null) return;
                if(bar.getVisibility()!=View.GONE)
                    bar.setVisibility(View.GONE);
                if(selfMarker!=null){
                    interaction.moveMarker(selfMarker,location);
                }
                else{
                    selfMarker = interaction.addVisual(mContext.getString(
                                    R.string.your_location_title),address,location);
                    interaction.viewLocation(location);
                }
            }
        }).execute();
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


}


/**
 * Requests the closest Truck Driver to the current location and tracks
 * the progress of the request.
 * */
class LocationRequest{
    private final static String LOG_TAG = LocationRequest.class.getSimpleName();
    private static final int MAX_POST_SEARCH_DISTANCE = 100;
    public final static String LOCATION_REQUEST_CLASS_NAME = "LocationRequest";
    public final static String USER_COLUMN = "user";
    public final static String LOCATION_COLUMN = "location";
    public final static String TRUCKER_COLUMN = "Trucker";
    private static boolean requestCreated = false;
    private static ParseObject mLocationRequest;
    private static ParseObject mActiveTrucker;

    // keys
    public final static String ON_CONNECTIVITY_KEY = "connectivity";
    public final static String ON_SUCCESS_KEY = "success";
    public final static String ON_NO_TRUCKERS = "noTruckers";

    public static void sendRequest(final ParseUser user, final ParseGeoPoint point
            , final simpleCallback onSuccess
            , final simpleCallback onConnectivityIssues, final simpleCallback onNoTruckers){
        createRequest(user,point,new simpleCallback() {
            @Override
            public void done(Object first, Object second) {
                if((Boolean)first){
                    // object creation went through


                    initializeRequest(point,onSuccess,onConnectivityIssues,onNoTruckers);

//
//                    getClosestTruckDriver(point,new simpleCallback() {
//                        @Override
//                        public void done(Object first, Object second) {
//                            if((Boolean)first){
//                                // closest truck has been found
//                                ParseObject towTrucker = (ParseObject)second;
//                                linkRequest(towTrucker,mLocationRequest, new simpleCallback() {
//                                    @Override
//                                    public void done(Object first, Object second) {
//                                        if((Boolean)first) {
//                                            // a tow-truck has been requested
//                                            if (onSuccess != null)
//                                                onSuccess.done(null,null);
//                                        } else {
//                                            if (onConnectivityIssues != null)
//                                                onConnectivityIssues.done(second,null);
//                                        }
//                                    }
//                                });
//                            } else {
//                                // no trucks are nearby
//                                if (onNoTruckers != null)
//                                    onNoTruckers.done(null,null);
//                                // remove the request object if there are no tow trucks
//                                removeRequest(new simpleCallback() {
//                                    @Override
//                                    public void done(Object first, Object second) {
//                                        if(!(Boolean)first) {
//                                            // if the removal request didn't go through
//                                            if (onConnectivityIssues != null)
//                                                onConnectivityIssues.done(second, null);
//                                        }
//                                    }
//                                });
//                            }
//                        }
//                    });
                } else{
                    // connectivity issues
                    if (onConnectivityIssues != null)
                        onConnectivityIssues.done(second, null);
                }
            }
        });
    }

    private static void initializeRequest(final ParseGeoPoint point
            , final simpleCallback onSuccess
            , final simpleCallback onConnectivityIssues, final simpleCallback onNoTruckers){
        getClosestTruckDriver(point,new simpleCallback() {
            @Override
            public void done(Object first, Object second) {
                if((Boolean)first){
                    // closest truck has been found
                    ParseObject towTrucker = (ParseObject)second;
                    linkRequest(towTrucker,mLocationRequest, new simpleCallback() {
                        @Override
                        public void done(Object first, Object second) {
                            if((Boolean)first) {
                                // a tow-truck has been requested
                                if (onSuccess != null)
                                    onSuccess.done(null,null);
                            } else {
                                if (onConnectivityIssues != null)
                                    onConnectivityIssues.done(second,null);
                            }
                        }
                    });
                } else {
                    // no trucks are nearby
                    if (onNoTruckers != null)
                        onNoTruckers.done(null,null);
                    // remove the request object if there are no tow trucks
                    removeRequest(new simpleCallback() {
                        @Override
                        public void done(Object first, Object second) {
                            if(!(Boolean)first) {
                                // if the removal request didn't go through
                                if (onConnectivityIssues != null)
                                    onConnectivityIssues.done(second, null);
                            }
                        }
                    });
                }
            }
        });
    }

    private static void linkRequest(ParseObject truckPost, ParseObject request
            , final simpleCallback callback){

        // make sure the right objects are received with the right class names
        if(!truckPost.getClassName().equals(FindATowPost.ACTIVE_TRUCKER_CLASS_NAME)
                || !request.getClassName().equals(LOCATION_REQUEST_CLASS_NAME)){
            Log.e(LOG_TAG,"wrong class names");
            throw new RuntimeException("incorrect class names are received");
        }
        // set the state to active
        truckPost.put(FindATowPost.STATE_COLUMN, FindATowPost.STATE_ACTIVE);
        // link the truck post to the request
        truckPost.put(FindATowPost.REQUESTS_COLUMN,request);
        // save the truck post
        truckPost.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if(callback!=null)
                    callback.done(e==null,e);
            }
        });
    }

    private static void createRequest(ParseUser user, ParseGeoPoint point
            , final simpleCallback callback){
        // check if a RequestLocation has been created
        if(requestCreated) return;
//        {
//            if(callback!=null)
//                callback.done(true,null);
//            return;
//        }
        final ParseObject request = new ParseObject(LOCATION_REQUEST_CLASS_NAME);
        // fill in the data for the columns of the object
        request.put(USER_COLUMN,user);
        request.put(LOCATION_COLUMN, point);
        // there is also another column, which is left empty but will be populated by the trucker
        request.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e != null) {
                    if(callback!=null)
                        callback.done(false,e);
                } else {
                    requestCreated = true;
                    mLocationRequest = request;
                    if(callback!=null)
                        callback.done(true,null);
                }
            }
        });
    }

    /**
     * retrieves the closest truck to the given location. There is an option to trigger a callback
     * in order to perform an action once the function is completed.
     * */
    private static void getClosestTruckDriver
            (ParseGeoPoint point, final simpleCallback callback){
        // check if a RequestLocation has been created
        if(mActiveTrucker!=null) return;
//        {
//            if(callback!=null)
//                callback.done(true,mActiveTrucker);
//            return;
//        }
        ParseQuery<ParseObject> truckerQuery = ParseQuery.getQuery(
                FindATowPost.ACTIVE_TRUCKER_CLASS_NAME);
        truckerQuery.whereWithinKilometers(FindATowPost.LOCATION_COLUMN
                , point, MAX_POST_SEARCH_DISTANCE);
        truckerQuery.whereEqualTo(FindATowPost.STATE_COLUMN,FindATowPost.STATE_IDLE);
        truckerQuery.getFirstInBackground(new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject towTrucker, ParseException e) {
                if(towTrucker !=null){
                    mActiveTrucker = towTrucker;
                    if(callback!=null)
                        callback.done(true,towTrucker);
                } else if(e!=null){
                    if(callback!=null)
                        callback.done(false,e);
                }
            }
        });
    }

    public static void trackRequest(final simpleCallback onSuccess
            , final simpleCallback onConnectivityIssues, final simpleCallback onCancel){
        // check if a RequestLocation has been created
        if(!requestCreated) return;
        // check if tow trucker has cancelled the request
        if(mActiveTrucker!=null){
             checkOnTrucker(onConnectivityIssues,onCancel);
        }
        mLocationRequest.fetchInBackground(new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject fetchedRequest, ParseException e) {
                if(fetchedRequest!=null) {
                    ParseObject truckerPost = (ParseObject) fetchedRequest.get(TRUCKER_COLUMN);
                    if (truckerPost != null)
                        receivePost(truckerPost, onConnectivityIssues, onSuccess);
                } else {
                    if(onConnectivityIssues!=null)
                        onConnectivityIssues.done(e,null);
                }
            }
        });
    }

    private static void checkOnTrucker(final simpleCallback onConnectivityIssues,
                                       final simpleCallback onCancel){
        mActiveTrucker.fetchInBackground(new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject trucker, ParseException e) {
                if(trucker!=null){
                    String state = (String)trucker.get(FindATowPost.STATE_COLUMN);
                    if(state.equals(FindATowPost.STATE_IDLE)){
                        // the trucker has cancelled the request
                        mActiveTrucker = null;
                        // trigger the cancellation callback
                        onCancel.done(retryCallback,null);
                    }
                } else {
                    // the trucker post has been deleted
                    mActiveTrucker = null;
                    onCancel.done(retryCallback,null);
                }
            }
        });
    }

    private static simpleCallback retryCallback = new simpleCallback() {
        @Override
        public void done(Object first, Object second) {
            ParseGeoPoint point = (ParseGeoPoint)first;
            HashMap<String,simpleCallback> callbacks = (HashMap)second;
            simpleCallback onConnectivityIssues = callbacks.get(ON_CONNECTIVITY_KEY);
            simpleCallback onSuccess = callbacks.get(ON_SUCCESS_KEY);
            simpleCallback onNoTruckers = callbacks.get(ON_NO_TRUCKERS);

            initializeRequest(point, onSuccess, onConnectivityIssues, onNoTruckers);
        }
    };

    private static void receivePost(ParseObject truckerPost
            , final simpleCallback onConnectivityIssues
            , final simpleCallback onSuccess){
        truckerPost.fetchInBackground(new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject trucker, ParseException e) {
                if(e!=null) {
                    if(onConnectivityIssues!=null)
                        onConnectivityIssues.done(e,null);
                } else {
                    final ParseGeoPoint truckerLocation = trucker.getParseGeoPoint(
                            FindATowPost.LOCATION_COLUMN);
                    ParseUser truckerUser = trucker.getParseUser(
                            FindATowPost.USER_COLUMN);
                    truckerUser.fetchInBackground(new GetCallback<ParseUser>() {
                        @Override
                        public void done(ParseUser user, ParseException e) {
                            if(e!=null){
                                if(onConnectivityIssues!=null)
                                    onConnectivityIssues.done(e,null);
                            } else {
                                String firstName = (String) user.get(DispatchActivity.FIRST_NAME);
                                String lastName = (String) user.get(DispatchActivity.LAST_NAME);
                                Bundle bundle = new Bundle();
                                bundle.putParcelable(LOCATION_COLUMN
                                        ,GmapInteraction.getLocation(truckerLocation));
                                bundle.putString(DispatchActivity.FIRST_NAME, firstName);
                                bundle.putString(DispatchActivity.LAST_NAME,lastName);
                                if(onSuccess!=null)
                                    onSuccess.done(bundle,null);
                            }
                        }
                    });
                }
            }
        });
    }

    public static void removeRequest(final simpleCallback callback){
        // check if a RequestLocation has been created
        if(!requestCreated) return;
        if(mActiveTrucker!=null){
            mActiveTrucker.put(FindATowPost.STATE_COLUMN, FindATowPost.STATE_IDLE);
            mActiveTrucker.put(FindATowPost.REQUESTS_COLUMN, JSONObject.NULL);
            mActiveTrucker.saveInBackground(new SaveCallback() {
                @Override
                public void done(ParseException e) {
                    if(e==null) {
                        mActiveTrucker = null;
                        destroyRequest(callback);
                    } else {
                        callback.done(false,e);
                    }
                }
            });
        } else {
            destroyRequest(callback);
        }
    }

    private static void destroyRequest(final simpleCallback callback){
        mLocationRequest.deleteEventually(new DeleteCallback() {
            @Override
            public void done(ParseException e) {
                if (callback != null) {
                    callback.done(e == null, e);
                }
            }
        });
        mLocationRequest = null;
        requestCreated = false;
    }

}

/**
 * TRUCKER - RELATED CLASS
 *  Submits a post indicating the Tow-Trucker is active and ready to receive requests for tows.
 *  the post is regularly updated with the most recent location of the user.
 * */
class FindATowPost{
    private final static String LOG_TAG = FindATowPost.class.getSimpleName();
    public final static String ACTIVE_TRUCKER_CLASS_NAME = "ActiveTruckers";
    public final static String USER_COLUMN = "trucker";
    public final static String LOCATION_COLUMN = "location";
    public final static String REQUESTS_COLUMN = "requests";
    public final static String STATE_COLUMN = "state";
    public final static String STATE_IDLE = "IDLE";
    public final static String STATE_ACTIVE = "ACTIVE";
    private static boolean postCreated = false;
    private static boolean requestReceived = false;
    private static ParseObject mTruckerPost;
    private static ParseGeoPoint mDestination;

    public static void createPost(ParseUser user,ParseGeoPoint point
            ,final simpleCallback onConnectivityIssues
            ,final simpleCallback onSuccess){
        // check if an ActiveTruckers post has been created
        if(postCreated) return;
        final ParseObject post = new ParseObject(ACTIVE_TRUCKER_CLASS_NAME);
        post.put(USER_COLUMN, user);
        post.put(LOCATION_COLUMN, point);
        post.put(STATE_COLUMN, STATE_IDLE);
        post.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if(e!=null) {
                    if (onConnectivityIssues != null)
                        onConnectivityIssues.done(e, null);
                }else {
                    postCreated = true;
                    mTruckerPost = post;
                    if (onSuccess != null)
                        onSuccess.done(null,null);
                }
            }
        });

    }
    public static void updatePost(final ParseGeoPoint point
            , final simpleCallback onSuccess, final simpleCallback onConnectivityIssues
            , final simpleCallback onCancel){
        // check if an ActiveTruckers post has been created
        if(!postCreated) return;
        // get the post from the network
        mTruckerPost.fetchInBackground(new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject parseObject, ParseException e) {
                // check if an ActiveTruckers post has been created
                if(!postCreated) return;
                if(e!=null) {
                    if (onConnectivityIssues != null)
                        onConnectivityIssues.done(e, null);
                }else {
                    // the post has been found, it should then be updated...
                    parseObject.put(LOCATION_COLUMN, point);
                    parseObject.saveInBackground();
                    // track any updates to the post
//                    trackPost(parseObject, context, callback);
                    trackPost(parseObject,onConnectivityIssues,onSuccess,onCancel);
                }
            }
        });
    }

    private static void trackPost(@NonNull ParseObject post
            , final simpleCallback onConnectivityIssues
            , final simpleCallback onSuccess
            , final simpleCallback onCancel){
        post.fetchInBackground(new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject postObject, ParseException e) {
                // check if an ActiveTruckers post has been created
                if(!postCreated) return;
                if (postObject != null) {
                    // divert to checkRequest if needed
                    if(requestReceived){
                        checkRequest(postObject,onCancel,onConnectivityIssues,onSuccess);
                        return;
                    }
                    String state = (String) postObject.get(STATE_COLUMN);
                    // check if a client has requested services from this user
                    if (state.equals(STATE_ACTIVE)) {
                        ParseObject request = (ParseObject) postObject.get(REQUESTS_COLUMN);
                        // it is implied that the request object exists
                        // trigger a response if a request has been attached to the post
                        receiveRequest(request, onConnectivityIssues, onSuccess);
                    }
                }
            }
        });
    }

    private static void checkRequest(ParseObject postObject, final simpleCallback onCancel
            ,final simpleCallback onConnectivityIssues, final simpleCallback onSuccess){
        // check if the request still stands
        String state = (String) postObject.get(STATE_COLUMN);
        // if the state has been changed, the request has been canceled
        if (state.equals(STATE_IDLE)){
            requestReceived = false;
            onCancel.done(null,null);
        } else if(state.equals(STATE_ACTIVE)) {
            ParseObject request = (ParseObject) postObject.get(REQUESTS_COLUMN);
            // it is implied that the request object exists
            // trigger a response if a request has been attached to the post
            updateRequest(request,onConnectivityIssues,onSuccess);
        }
    }

    public static void removePost(final simpleCallback onConnectivityIssues){
        // check if an ActiveTruckers post has been created
        if(!postCreated) return;
        mTruckerPost.deleteEventually(new DeleteCallback() {
            @Override
            public void done(ParseException e) {
                if (e != null) {
                    // the post was not deleted, notify the user of connectivity issues
                    if(onConnectivityIssues!=null)
                        onConnectivityIssues.done(e,null);
                }
            }
        });
        postCreated = false;
        mTruckerPost = null;
    }

    private static void updateRequest(@NonNull ParseObject request
            , final simpleCallback onConnectivityIssues, final simpleCallback onSuccess){
        request.fetchInBackground(new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject fetchedRequest, ParseException e) {
                if(fetchedRequest!=null) {
                    requestReceived = true;
                    // set a private field as the destination location so it could be accessed
                    mDestination = (ParseGeoPoint) fetchedRequest.get(
                            LocationRequest.LOCATION_COLUMN);
                    Bundle bundle = new Bundle();
                    bundle.putParcelable(LOCATION_COLUMN, GmapInteraction.
                            getLocation(mDestination));
                    if (onSuccess != null)
                        onSuccess.done(bundle,null);
                } else {
                    if (onConnectivityIssues != null)
                        onConnectivityIssues.done(e,null);
                }
            }
        });
    }
    private static void receiveRequest(@NonNull ParseObject request
            , final simpleCallback onConnectivityIssues
            , final simpleCallback onSuccess){
        request.fetchInBackground(new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject fetchedRequest, ParseException e) {
                if(fetchedRequest!=null) {
                    requestReceived = true;
                    // set a private field as the destination location so it could be accessed
                    mDestination = (ParseGeoPoint) fetchedRequest.get(
                            LocationRequest.LOCATION_COLUMN);
                    ParseUser user = (ParseUser) fetchedRequest.get(LocationRequest.USER_COLUMN);
                    user.fetchInBackground(new GetCallback<ParseUser>() {
                        @Override
                        public void done(ParseUser client, ParseException e) {
                            if (client != null) {
                                String firstName = (String) client.get(DispatchActivity.FIRST_NAME);
                                String lastName = (String) client.get(DispatchActivity.LAST_NAME);
                                String carYear = (String) client.get(DispatchActivity.CAR_YEAR);
                                String carMake = (String) client.get(DispatchActivity.CAR_MAKE);
                                String carModel = (String) client.get(DispatchActivity.CAR_MODEL);
                                Bundle bundle = new Bundle();
                                bundle.putString(DispatchActivity.FIRST_NAME, firstName);
                                bundle.putString(DispatchActivity.LAST_NAME, lastName);
                                bundle.putString(DispatchActivity.CAR_YEAR, carYear);
                                bundle.putString(DispatchActivity.CAR_MAKE, carMake);
                                bundle.putString(DispatchActivity.CAR_MODEL, carModel);
                                bundle.putParcelable(LOCATION_COLUMN, GmapInteraction.
                                        getLocation(mDestination));
                                if (onSuccess != null)
                                    onSuccess.done(bundle,null);
                            } else {
                                if (onConnectivityIssues != null)
                                    onConnectivityIssues.done(e,null);
                            }
                        }
                    });
                }
            }
        });
    }

    public static ParseGeoPoint getmDestination(){
        return mDestination;
    }

    public static void rejectRequest(final simpleCallback onConnectivityIssues){
        if(mTruckerPost==null) return;
        mTruckerPost.put(STATE_COLUMN, STATE_IDLE);
        mTruckerPost.put(REQUESTS_COLUMN,JSONObject.NULL);
        requestReceived = false;
        mTruckerPost.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if(e!=null)
                    if(onConnectivityIssues!=null)
                        onConnectivityIssues.done(e,null);
            }
        });
    }

    public static void acceptRequest(final simpleCallback onConnectivityIssues
            ,final simpleCallback onSuccess){
        if(mTruckerPost==null) return;
        ParseObject request = (ParseObject)mTruckerPost.get(REQUESTS_COLUMN);
        if(request!=null) {
            request.put(LocationRequest.TRUCKER_COLUMN, mTruckerPost);
            request.saveInBackground(new SaveCallback() {
                @Override
                public void done(ParseException e) {
                    if(e!=null) {
                        if (onConnectivityIssues != null)
                            onConnectivityIssues.done(e, null);
                    } else {
                        if (onSuccess != null)
                            onSuccess.done(null, null);
                    }
                }
            });
        }
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

class clientResultInflater implements PanelInflater{

    private LinearLayout mRootView;

    private TextView firstNamefield;
    private TextView lastNamefield;
    private TextView carYearfield;
    private TextView carMakefield;
    private TextView carModelfield;

    private Button acceptButton;
    private Button rejectButton;

    private boolean viewsSet = false;

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
        if(!viewsSet)return;
        if(mRootView.getVisibility()==View.VISIBLE) return;
        mRootView.setVisibility(View.VISIBLE);
    }

    @Override
    public void hidePanel(){
        if(!viewsSet)return;
        if(mRootView.getVisibility()==View.GONE) return;
        mRootView.setVisibility(View.GONE);
    }

}

class towTruckerResultInflater implements PanelInflater{

    private LinearLayout mRootView;

    private TextView firstNamefield;
    private TextView lastNamefield;

    private Button rejectButton;

    private boolean viewsSet = false;

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
        if(!viewsSet)return;
        if(mRootView.getVisibility()==View.VISIBLE) return;
        mRootView.setVisibility(View.VISIBLE);
    }

    @Override
    public void hidePanel(){
        if(!viewsSet)return;
        if(mRootView.getVisibility()==View.GONE) return;
        mRootView.setVisibility(View.GONE);
    }

}

class loadingInflater implements PanelInflater{


    private LinearLayout mRootView;

    private TextView titleField;
    private TextView snippetField;

    private Button rejectButton;

    private boolean viewsSet = false;


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
    public void displayPanel() {
        if(!viewsSet)return;
        if(mRootView.getVisibility()==View.VISIBLE) return;
        mRootView.setVisibility(View.VISIBLE);
    }

    @Override
    public void hidePanel() {
        if(!viewsSet)return;
        if(mRootView.getVisibility()==View.GONE) return;
        mRootView.setVisibility(View.GONE);
    }
}
