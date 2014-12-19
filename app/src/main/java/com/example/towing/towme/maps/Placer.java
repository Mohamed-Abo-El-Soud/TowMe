package com.example.towing.towme.maps;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.towing.towme.LocationPost;
import com.example.towing.towme.R;
import com.example.towing.towme.Utilites;
import com.example.towing.towme.dispatch.DispatchActivity;
import com.example.towing.towme.dispatch.DispatchActivity.simpleCallback;
import com.google.android.gms.maps.GoogleMap;
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

    Marker selfMarker;
    ArrayList<Marker> otherMarkers = new ArrayList<>();

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

    /**
     * usually called when the location gets updated. Updates the map ui of the new map location
     * as well as check server for updates and other background processes
     * */
    public void update(Location currentSelfLocation){
        defineSelfLocation(currentSelfLocation);
//        LocationRequest.trackRequest(mContext);
        if(once)
//            requestOtherMarkersPositions(currentSelfLocation);
//              getClosestTruck(currentSelfLocation);
//              LocationRequest.createRequest(Utilites.getUser()
//                      , interaction.getGeoPoint(currentSelfLocation), mContext);
                LocationRequest.sendRequest(Utilites.getUser()
                        ,interaction.getGeoPoint(currentSelfLocation),mContext);

        uploadLocation(currentSelfLocation);
        once = false;
//        extra();
    }

    public void getATow(Location currentSelfLocation){
        if(currentSelfLocation==null) return;
        LocationRequest.trackRequest(mContext);
        LocationRequest.sendRequest(Utilites.getUser()
                ,interaction.getGeoPoint(currentSelfLocation),mContext);
    }

    private simpleCallback clientFound = new simpleCallback() {
        @Override
        public void done(Object first, Object second) {
            if((Boolean)first){
                Bundle items = (Bundle)second;
                String firstName = items.getString(DispatchActivity.FIRST_NAME);
                String lastName = items.getString(DispatchActivity.LAST_NAME);
                Location location = items.getParcelable(FindATowPost.USER_COLUMN);
                // TODO: do stuff with this information

            }
        }
    };

    public void findAClient(Location currentSelfLocation){
        if(currentSelfLocation==null) return;
        FindATowPost.updatePost(Utilites.getUser()
                ,interaction.getGeoPoint(currentSelfLocation),mContext,clientFound);
        FindATowPost.createPost(Utilites.getUser()
                ,interaction.getGeoPoint(currentSelfLocation),mContext);
    }

    private LinearLayout resultsPanel;

    /**
     * a helper function that populates the information in the results view as well as displays it
     * */
    public void fillInResultsView(){
        displayResultsView();
        if(resultsPanel!=null){

        }
    }

    public void displayResultsView(){
        if(resultsPanel== null)
            resultsPanel = (LinearLayout)mRootView.findViewById(R.id.results_panel);
        if(resultsPanel!=null)
            resultsPanel.setVisibility(View.VISIBLE);
    }

    public void hideResultsView(){
        if(resultsPanel== null)
            resultsPanel = (LinearLayout)mRootView.findViewById(R.id.results_panel);
        if(resultsPanel!=null)
            resultsPanel.setVisibility(View.GONE);
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

    public static void sendRequest(final ParseUser user, final ParseGeoPoint point
            , final Context context){

        final simpleCallback connectivityIssues = new simpleCallback() {
            @Override
            public void done(Object first, Object second) {
                // this is a callback made to deal with any connectivity issues
                if(second!=null) {
                    ParseException e = (ParseException)second;
                    Log.e(LOG_TAG, "Error: " + e);
                    e.printStackTrace();
                }
                Toast.makeText(context, "the app could not connect to the internet"
                        , Toast.LENGTH_LONG).show();
            }
        };
        createRequest(user,point,new simpleCallback() {
            @Override
            public void done(Object first, Object second) {
                if((Boolean)first){
                    // object creation went through
                    getClosestTruckDriver(point,new simpleCallback() {
                        @Override
                        public void done(Object first, Object second) {
                            if((Boolean)first){
                                // closest truck has been found
                                ParseObject towTrucker = (ParseObject)second;
                                linkRequest(towTrucker,mLocationRequest,new simpleCallback() {
                                    @Override
                                    public void done(Object first, Object second) {
                                        // inform the user that a request has been created
                                        Toast.makeText(context, "searching for truckers..."
                                                , Toast.LENGTH_LONG).show();
                                    }
                                });
                            } else {
                                // no trucks are nearby
//                    //TODO: remove the request object if there are no tow trucks
                                Toast.makeText(context, "there are no trucks near your " +
                                        "current location"
                                        , Toast.LENGTH_LONG).show();
                                // exit the process
                                removeRequest(mLocationRequest,context);
//                                connectivityIssues.done(second,null);
                            }
                        }
                    });
                } else{
                    // connectivity issues
                    connectivityIssues.done(second, null);
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
                if (e != null) {
                    if(callback!=null)
                        callback.done(false,e);
                } else {
                    if(callback!=null)
                        callback.done(true,null);
                }
            }
        });
    }

    private static void createRequest(ParseUser user, ParseGeoPoint point
            , final simpleCallback callback){
        // check if a RequestLocation has been created
        if(requestCreated) return;
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
//                    Log.e(LOG_TAG, "Error: " + e);
//                    e.printStackTrace();
//                    Toast.makeText(context, "unfortunately, an error has occurred." +
//                            " The services of this app could not be used at the moment"
//                            , Toast.LENGTH_LONG).show();
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
        ParseQuery<ParseObject> truckerQuery = ParseQuery.getQuery(
                FindATowPost.ACTIVE_TRUCKER_CLASS_NAME);
        truckerQuery.whereWithinKilometers(FindATowPost.LOCATION_COLUMN
                , point, MAX_POST_SEARCH_DISTANCE);
        truckerQuery.whereEqualTo(FindATowPost.STATE_COLUMN,FindATowPost.STATE_IDLE);
        truckerQuery.getFirstInBackground(new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject towTrucker, ParseException e) {
                if(towTrucker !=null){
                    if(callback!=null)
                        callback.done(true,towTrucker);
                } else if(e!=null){
                    if(callback!=null)
                        callback.done(false,e);
                }
            }
        });
    }

    public static void trackRequest(final Context context){
        // check if a RequestLocation has been created
        if(!requestCreated) return;

        mLocationRequest.fetchInBackground(new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject fetchedRequest, ParseException e) {
                if(fetchedRequest!=null) {
                    ParseUser trucker = (ParseUser) fetchedRequest.get(TRUCKER_COLUMN);
                    if (trucker != null) {
                        Toast.makeText(context, "request has been processed"
                                , Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(context, "request pending..."
                                , Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }


    public static void removeRequest(@NonNull ParseObject request, final Context context){
        request.deleteEventually(new DeleteCallback() {
            @Override
            public void done(ParseException e) {
                if (e != null) {
                    // the request was not deleted, notify the user of connectivity issues
                    Log.e(LOG_TAG, "Error: " + e);
                    e.printStackTrace();
                    Toast.makeText(context, "the app could not connect to the internet"
                            , Toast.LENGTH_LONG).show();
                }
            }
        });
        requestCreated = false;
        mLocationRequest = null;
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
    private static ParseObject mActiveState;

    public static void createPost(ParseUser user,ParseGeoPoint point
            , final Context context){
        final ParseObject request = new ParseObject(ACTIVE_TRUCKER_CLASS_NAME);
        request.put(USER_COLUMN,user);
        request.put(LOCATION_COLUMN, point);
        request.put(STATE_COLUMN, STATE_IDLE);
        request.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e != null) {
                    Log.e(LOG_TAG, "Error: " + e);
                    e.printStackTrace();
                    Toast.makeText(context, "unfortunately, an error has occurred." +
                            " The services of this app could not be used at the moment"
                            , Toast.LENGTH_LONG).show();
                } else {
                    postCreated = true;
                    mActiveState = request;
                    // TODO: Delete below
                    Toast.makeText(context, "post has been created"
                            , Toast.LENGTH_LONG).show();
                }
            }
        });

    }
    public static void updatePost(final ParseUser user, final ParseGeoPoint point
            , final Context context, final simpleCallback callback){
        // check if an ActiveTruckers post has been created
        if(!postCreated) return;
        // get the post from the network
        mActiveState.fetchInBackground(new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject parseObject, ParseException e) {
                if (e == null && parseObject != null) {
                    // the post has been found, it should then be updated...
                    parseObject.put(LOCATION_COLUMN, point);
                    // track any updates to the post
                    trackPost(parseObject, context, callback);
                } else if (e != null) {
                    // something went wrong, either the post did not exist, or the application
                    // could not access the network.
                    Log.e(LOG_TAG, "Error: " + e);
                    e.printStackTrace();
                    // create a new post instead
                    createPost(user, point, context);
                }
            }
        });
    }

    public static void trackPost(@NonNull ParseObject post, final Context context
            , final simpleCallback callback){
        post.fetchInBackground(new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject postObject, ParseException e) {
                if (postObject != null) {

                    ParseUser client = (ParseUser) postObject.get(REQUESTS_COLUMN);
                    String state = (String) postObject.get(STATE_COLUMN);
                    // check if a client has requested services from this user
                    if (state.equals(STATE_ACTIVE)) {
                        // check if there is a client
                        if (client == null || client == JSONObject.NULL) {
                            Toast.makeText(context, "an error has occurred with retrieving the client"
                                    , Toast.LENGTH_LONG).show();
                            return;
                        }
                        // create a dialog popup prompting the user/tow truck driver to take action
                        // TODO: add a dialog in response to the request
                        // TODO: remove the post from the database after the request has been completed
                        recieveRequest(postObject, callback);
                    }
                }
            }
        });
    }

    public static void removePost(@NonNull ParseObject post, final Context context){
        post.deleteEventually(new DeleteCallback() {
            @Override
            public void done(ParseException e) {
                if (e != null) {
                    // the post was not deleted, notify the user of connectivity issues
                    Log.e(LOG_TAG, "Error: " + e);
                    e.printStackTrace();
                    Toast.makeText(context, "the app could not connect to the internet"
                            , Toast.LENGTH_LONG).show();
                }
            }
        });
        postCreated = false;
        mActiveState = null;
    }

    private static void recieveRequest(@NonNull ParseObject request, final simpleCallback callback){
        request.fetchInBackground(new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject fetchedRequest, ParseException e) {
                if(e!=null) {
                    final ParseGeoPoint point = (ParseGeoPoint) fetchedRequest.get(
                            LocationRequest.LOCATION_COLUMN);
                    ParseUser user = (ParseUser) fetchedRequest.get(LocationRequest.USER_COLUMN);
                    user.fetchInBackground(new GetCallback<ParseUser>() {
                        @Override
                        public void done(ParseUser client, ParseException e) {
                            if(e!=null) {
                                String firstName = (String) client.get(DispatchActivity.FIRST_NAME);
                                String lastName = (String) client.get(DispatchActivity.LAST_NAME);
                                Bundle bundle = new Bundle();
                                bundle.putString(DispatchActivity.FIRST_NAME,firstName);
                                bundle.putString(DispatchActivity.LAST_NAME,lastName);
                                bundle.putParcelable(USER_COLUMN, GmapInteraction.
                                        getLocation(point));
                                if(callback!=null)
                                    callback.done(true,bundle);
                            }
                        }
                    });
                }
            }
        });
    }

}
