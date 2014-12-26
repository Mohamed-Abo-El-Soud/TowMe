package com.example.towing.towme.maps;

import android.os.Bundle;
import android.util.Log;

import com.example.towing.towme.dispatch.DispatchActivity;
import com.parse.DeleteCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import org.json.JSONObject;

import java.util.HashMap;

/**
 * Requests the closest Truck Driver to the current location and tracks
 * the progress of the request.
 * */
public class LocationRequest{
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
            , final DispatchActivity.simpleCallback onSuccess
            , final DispatchActivity.simpleCallback onConnectivityIssues, final DispatchActivity.simpleCallback onNoTruckers){
        createRequest(user,point,new DispatchActivity.simpleCallback() {
            @Override
            public void done(Object first, Object second) {
                if((Boolean)first){
                    // object creation went through
                    initializeRequest(point,onSuccess,onConnectivityIssues,onNoTruckers);
                } else{
                    // connectivity issues
                    if (onConnectivityIssues != null)
                        onConnectivityIssues.done(second, null);
                }
            }
        });
    }

    private static void initializeRequest(final ParseGeoPoint point
            , final DispatchActivity.simpleCallback onSuccess
            , final DispatchActivity.simpleCallback onConnectivityIssues, final DispatchActivity.simpleCallback onNoTruckers){
        getClosestTruckDriver(point,new DispatchActivity.simpleCallback() {
            @Override
            public void done(Object first, Object second) {
                if((Boolean)first){
                    // closest truck has been found
                    ParseObject towTrucker = (ParseObject)second;
                    linkRequest(towTrucker,mLocationRequest, new DispatchActivity.simpleCallback() {
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
                    removeRequest(new DispatchActivity.simpleCallback() {
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
            , final DispatchActivity.simpleCallback callback){

        // make sure the right objects are received with the right class names
        if(!truckPost.getClassName().equals(FindATowPost.ACTIVE_TRUCKER_CLASS_NAME)
                || !request.getClassName().equals(LOCATION_REQUEST_CLASS_NAME)){
            Log.e(LOG_TAG, "wrong class names");
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
            , final DispatchActivity.simpleCallback callback){
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
            (ParseGeoPoint point, final DispatchActivity.simpleCallback callback){
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

    public static void trackRequest(final DispatchActivity.simpleCallback onSuccess
            , final DispatchActivity.simpleCallback onConnectivityIssues, final DispatchActivity.simpleCallback onCancel){
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

    private static void checkOnTrucker(final DispatchActivity.simpleCallback onConnectivityIssues,
                                       final DispatchActivity.simpleCallback onCancel){
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

    private static DispatchActivity.simpleCallback retryCallback = new DispatchActivity.simpleCallback() {
        @Override
        public void done(Object first, Object second) {
            ParseGeoPoint point = (ParseGeoPoint)first;
            HashMap<String,DispatchActivity.simpleCallback> callbacks = (HashMap)second;
            DispatchActivity.simpleCallback onConnectivityIssues = callbacks.get(ON_CONNECTIVITY_KEY);
            DispatchActivity.simpleCallback onSuccess = callbacks.get(ON_SUCCESS_KEY);
            DispatchActivity.simpleCallback onNoTruckers = callbacks.get(ON_NO_TRUCKERS);

            initializeRequest(point, onSuccess, onConnectivityIssues, onNoTruckers);
        }
    };

    private static void receivePost(ParseObject truckerPost
            , final DispatchActivity.simpleCallback onConnectivityIssues
            , final DispatchActivity.simpleCallback onSuccess){
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

    public static void removeRequest(final DispatchActivity.simpleCallback callback){
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

    private static void destroyRequest(final DispatchActivity.simpleCallback callback){
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
