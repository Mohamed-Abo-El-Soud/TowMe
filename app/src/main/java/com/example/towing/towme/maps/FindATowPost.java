package com.example.towing.towme.maps;

import android.os.Bundle;
import android.support.annotation.NonNull;

import com.example.towing.towme.dispatch.DispatchActivity;
import com.parse.DeleteCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import org.json.JSONObject;

/**
 * TRUCKER - RELATED CLASS
 *  Submits a post indicating the Tow-Trucker is active and ready to receive requests for tows.
 *  the post is regularly updated with the most recent location of the user.
 * */
public class FindATowPost{
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
            ,final DispatchActivity.simpleCallback onConnectivityIssues
            ,final DispatchActivity.simpleCallback onSuccess){
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
            , final DispatchActivity.simpleCallback onSuccess, final DispatchActivity.simpleCallback onConnectivityIssues
            , final DispatchActivity.simpleCallback onCancel){
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
            , final DispatchActivity.simpleCallback onConnectivityIssues
            , final DispatchActivity.simpleCallback onSuccess
            , final DispatchActivity.simpleCallback onCancel){
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

    private static void checkRequest(ParseObject postObject, final DispatchActivity.simpleCallback onCancel
            ,final DispatchActivity.simpleCallback onConnectivityIssues, final DispatchActivity.simpleCallback onSuccess){
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

    public static void removePost(final DispatchActivity.simpleCallback onConnectivityIssues){
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
            , final DispatchActivity.simpleCallback onConnectivityIssues, final DispatchActivity.simpleCallback onSuccess){
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
            , final DispatchActivity.simpleCallback onConnectivityIssues
            , final DispatchActivity.simpleCallback onSuccess){
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

    public static void rejectRequest(final DispatchActivity.simpleCallback onConnectivityIssues){
        if(mTruckerPost==null) return;
        mTruckerPost.put(STATE_COLUMN, STATE_IDLE);
        mTruckerPost.put(REQUESTS_COLUMN, JSONObject.NULL);
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

    public static void acceptRequest(final DispatchActivity.simpleCallback onConnectivityIssues
            ,final DispatchActivity.simpleCallback onSuccess){
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
