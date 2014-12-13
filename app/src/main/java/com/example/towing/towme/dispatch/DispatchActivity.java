package com.example.towing.towme.dispatch;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.towing.towme.LocationPost;
import com.example.towing.towme.MapsActivity;
import com.example.towing.towme.R;
import com.example.towing.towme.Utilites;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.model.GraphUser;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;
import com.parse.FindCallback;
import com.parse.FunctionCallback;
import com.parse.GetCallback;
import com.parse.LogInCallback;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseFacebookUtils;
import com.parse.ParseQuery;
import com.parse.ParseTwitterUtils;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Mohamed on 14-11-29.
 */
public class DispatchActivity extends FragmentActivity
        implements
        ConnectionCallbacks
,
        GoogleApiClient.ConnectionCallbacks
,GoogleApiClient.OnConnectionFailedListener
        , OnConnectionFailedListener
{

    public static final String LOG_TAG = DispatchActivity.class.getSimpleName();
    public static final String IS_ANONYMOUS = "isAnonymous";
    public static final String FIRST_NAME = "firstName";
    public static final String LAST_NAME = "lastName";
    public static final String E_MAIL = "email";
    public static final String PHONE_NUMBER = "phoneNumber";
    public static final String CAR_MODEL = "carModel";
    public static final String CAR_MAKE = "carMake";
    public static final String CAR_YEAR = "carYear";

    public static final String ACCOUNT_INFO_KEY = "DispatchActivity.get.account.info.key";

    private static final String PARSE_CLOUD_GET_ACCESS_TOKEN_FUNCTION = "getUserSessionToken";

    /* Client used to interact with Google APIs. */
    private GoogleApiClient mGoogleApiClient;

    /* A flag indicating that a PendingIntent is in progress and prevents
    * us from starting further intents.
    */
    private boolean mIntentInProgress;
    /* Request code used to invoke sign in user interactions. */
    private static final int RC_SIGN_IN = 0;

    /* Store the connection result from onConnectionFailed callbacks so that we can
     * resolve them when the user clicks sign-in.
     */
    private ConnectionResult mConnectionResult;

    /* Track whether the sign-in button has been clicked so that we know to resolve
     * all issues preventing sign-in without waiting.
     */
    private boolean mSignInClicked;

    /* A simple Call-Back helper interface
    * */
    private interface simpleCallback{
        void done(Object first, Object second);
}


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dispatch);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API)
                .addScope(Plus.SCOPE_PLUS_PROFILE)
                .addScope(Plus.SCOPE_PLUS_LOGIN)
                .build();
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkIfLoggedIn();
        findViewById(R.id.fb_logn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findViewById(R.id.dispatch_starting).setVisibility(View.VISIBLE);
                fbLogin();
            }
        });
        findViewById(R.id.anonymous_login)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        findViewById(R.id.dispatch_starting).setVisibility(View.VISIBLE);
                        userInitialization(new simpleCallback() {
                            @Override
                            public void done(Object first, Object second) {
                                moveOn(null);
                            }
                        });
                    }
                });
        findViewById(R.id.g_plus_sign_in_button)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        findViewById(R.id.dispatch_starting).setVisibility(View.VISIBLE);
                        mGoogleApiClient.connect();
                        if (!mGoogleApiClient.isConnecting()) {
                            mSignInClicked = true;
                            resolveSignInError();
                        }
                    }
                });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    private void fbLogin(){
        ParseFacebookUtils.logIn(Arrays.asList(ParseFacebookUtils.Permissions.User.ABOUT_ME
                , ParseFacebookUtils.Permissions.User.EMAIL
        ),this, new LogInCallback() {
            @Override
            public void done(ParseUser user, ParseException err) {
                if (user == null) {
                    Log.d("MyApp", "Uh oh. The user cancelled the Facebook login.");
                } else if (user.isNew()) {
                    Log.d("MyApp", "User signed up and logged in through Facebook!");
                    Utilites.setUser(user);
                    createNewLocationPost(user);
                    user.put(IS_ANONYMOUS, false);
                    user.saveInBackground();
                    getFBInfo(new simpleCallback() {
                        @Override
                        public void done(Object first, Object second) {
                            if (first != null) {
                                Bundle info = (Bundle) first;
                                moveOn(info);
                            }
                        }
                    });
                } else {
                    Log.d("MyApp", "User logged in through Facebook!");
                    Utilites.setUser(user);
                    getEntries();
                    getFBInfo(new simpleCallback() {
                        @Override
                        public void done(Object first, Object second) {
                            if(first!=null){
                                Bundle info = (Bundle)first;
                                moveOn(info);
                            }
                        }
                    });
                }
            }
        });
    }

//    private void getFBInfo(){
//        Request request = Request.newMeRequest(ParseFacebookUtils.getSession()
//                ,new Request.GraphUserCallback() {
//            @Override
//            public void onCompleted(GraphUser user, Response response) {
//                if (user != null) {
//                    ParseUser activeUser = Utilites.getUser();
//                    activeUser.put(IS_ANONYMOUS,false);
//                    activeUser.put(FIRST_NAME,user.getFirstName());
//                    String endName = "";
//                    if(user.getMiddleName() !=null)
//                        endName += user.getMiddleName();
//                    if(user.getLastName() !=null) {
//                        // add a space between the middle and last name
//                        if (user.getMiddleName() != null)
//                            endName += " ";
//                        endName += user.getLastName();
//                    }
//                    if(endName.length()>1)
//                        activeUser.put(LAST_NAME,endName);
//                    if(user.getProperty(ParseFacebookUtils.Permissions.User.EMAIL)!=null){
//                        activeUser.put(E_MAIL,user.getProperty(ParseFacebookUtils
//                                .Permissions.User.EMAIL));
//                    }
//                    activeUser.saveInBackground(new SaveCallback() {
//                        @Override
//                        public void done(ParseException e) {
//                            if(e!=null){
//                                Log.e(LOG_TAG,"Error: " + e);
//                                e.printStackTrace();
//                            }
//                            else {
//                                Log.d(LOG_TAG,"info saved successfully!");
//                            }
//                        }
//                    });
//                } else if (response.getError() != null) {
//                    // handle error
//                }
//            }
//        });
//        request.executeAsync();
//    }

    private void getFBInfo(final simpleCallback callback){
        Request request = Request.newMeRequest(ParseFacebookUtils.getSession()
                ,new Request.GraphUserCallback() {
            @Override
            public void onCompleted(GraphUser user, Response response) {
                if (user != null) {
                    Bundle result = new Bundle();
                    result.putString(FIRST_NAME,user.getFirstName());
                    String endName = "";
                    if(user.getMiddleName() !=null)
                        endName += user.getMiddleName();
                    if(user.getLastName() !=null) {
                        // add a space between the middle and last name
                        if (user.getMiddleName() != null)
                            endName += " ";
                        endName += user.getLastName();
                    }
                    if(endName.length()>1)
                        result.putString(LAST_NAME,endName);
                    if(user.getProperty(ParseFacebookUtils.Permissions.User.EMAIL)!=null){
                        result.putString(E_MAIL,(String)user.getProperty(ParseFacebookUtils
                                .Permissions.User.EMAIL));
                    }
                    if(callback!=null)
                        callback.done(result,null);
//                    activeUser.saveInBackground(new SaveCallback() {
//                        @Override
//                        public void done(ParseException e) {
//                            if(e!=null){
//                                Log.e(LOG_TAG,"Error: " + e);
//                                e.printStackTrace();
//                            }
//                            else {
//                                Log.d(LOG_TAG,"info saved successfully!");
//                            }
//                        }
//                    });
                } else if (response.getError() != null) {
                    // handle error
                    if(callback!=null)
                        callback.done(null,null);
                }
            }
        });
        request.executeAsync();
    }

    private void getPhoneInfo(){
        TelephonyManager tMgr = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        String mPhoneNumber = tMgr.getLine1Number();
        ParseUser activeUser = Utilites.getUser();
        activeUser.put(PHONE_NUMBER,mPhoneNumber);
        activeUser.saveInBackground();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ParseFacebookUtils.finishAuthentication(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {

            if (resultCode != RESULT_OK) {
                mSignInClicked = false;
            }

            mIntentInProgress = false;

            if (!mGoogleApiClient.isConnecting()) {
                mGoogleApiClient.connect();
            }
        }
    }

    private void checkIfLoggedIn(){
        final ParseUser currentUser = ParseUser.getCurrentUser();
        final Activity that = this;
        // put the loader on the screen to prevent the user from using the buttons
        findViewById(R.id.dispatch_starting).setVisibility(View.VISIBLE);
        if (currentUser != null) {
            currentUser.fetchInBackground(new GetCallback<ParseUser>() {
                @Override
                public void done(ParseUser parseUser, ParseException e) {
                    if (parseUser == null) {
                        // user has been deleted
                        // do nothing
                        if (findViewById(R.id.dispatch_starting).getVisibility() != View.GONE)
                            findViewById(R.id.dispatch_starting).setVisibility(View.GONE);
                    } else {
                        // user is already logged in
                        // skip the setup and go to the actual app
                        Utilites.setUser(currentUser);
                        getEntries();
                        startActivity(new Intent(that,MapsActivity.class));
                        if (findViewById(R.id.dispatch_starting).getVisibility() != View.GONE)
                            findViewById(R.id.dispatch_starting).setVisibility(View.GONE);
                    }
                }
            });
        } else{
            // there is no user, give the user options to add a new user
            if (findViewById(R.id.dispatch_starting).getVisibility() != View.GONE)
                findViewById(R.id.dispatch_starting).setVisibility(View.GONE);
        }
    }

    private void userInitialization(final simpleCallback callback){
        final ParseUser currentUser = ParseUser.getCurrentUser();
        if (currentUser != null) {
            currentUser.fetchInBackground(new GetCallback<ParseUser>() {
                @Override
                public void done(ParseUser parseUser, ParseException e) {
                    if(parseUser==null) {
                        // user has been deleted
                        createAnonymousUser(callback);
                    }
                    else{
                        // user is already logged in
                        Utilites.setUser(currentUser);
                        getEntries();
                        if(callback!=null)
                            callback.done(parseUser,null);
//                        moveOn(null);
                    }
                }
            });
        } else {
            // there's no user online, create a new user
            createAnonymousUser(callback);
        }
    }

    private void createAnonymousUser(final simpleCallback callback){
        ParseUser.logOut();
        ParseAnonymousUtils.logIn(new LogInCallback() {
            @Override
            public void done(ParseUser user, ParseException e) {
                if (e != null) {
                    Log.d(LOG_TAG, "Anonymous login failed.");
                } else {
                    Log.d(LOG_TAG, "Anonymous user logged in.");
                    user.put(IS_ANONYMOUS, true);
                    user.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            if (e != null) {
                                Log.e(LOG_TAG, "Error: " + e);
                                e.printStackTrace();
                            }
                        }
                    });
                    Utilites.setUser(user);
                    createNewLocationPost(user);
                    if (callback != null){
                        callback.done(user, null);
                    }
                }
            }
        });
    }


    private void getEntries(){
        ParseQuery<LocationPost> query = LocationPost.getQuery();
        query.whereEqualTo("user", Utilites.getUser());
        query.findInBackground(new FindCallback<LocationPost>() {
            public void done(List<LocationPost> scoreList, ParseException e) {
                if (e == null) {
                    Log.d(LOG_TAG, "Retrieved " + scoreList.size() + " posts");
                    if (scoreList.size() >= 1) {
                        clearMostEntries(scoreList);
                        Utilites.setLocationPost(scoreList.get(0));
                    }
                    else {
                        createNewLocationPost(Utilites.getUser());
                    }
                } else {
                    Log.d(LOG_TAG, "Error: " + e.getMessage());
                    createNewLocationPost(Utilites.getUser());
                }
            }
        });
    }

    private void clearMostEntries(List<LocationPost> list){
        for (int i = 1; i < list.size(); i++) {
            list.get(i).deleteInBackground();
        }
    }

    private void createNewLocationPost(ParseUser user){
        LocationPost locationPost = new LocationPost();
        locationPost.setUser(user);
        locationPost.saveInBackground();
        Utilites.setLocationPost(locationPost);
    }

    private void moveOn(Bundle extraStuff){
//        findViewById(R.id.dispatch_starting).setVisibility(View.GONE);
//        Intent intent = new Intent(this,MapsActivity.class);
        Intent intent = new Intent(this,ContactInfoActivity.class);
        if(extraStuff!=null)
            intent.putExtra(ACCOUNT_INFO_KEY,extraStuff);
        startActivity(intent);
        if(findViewById(R.id.dispatch_starting).getVisibility()!=View.GONE)
            findViewById(R.id.dispatch_starting).setVisibility(View.GONE);
    }


    @Override
    public void onConnected(Bundle bundle) {
        mSignInClicked = false;
        // We've resolved any connection errors.  mGoogleApiClient can be used to
        // access Google APIs on behalf of the user.

        Toast.makeText(this,"g+ has been connected!",Toast.LENGTH_LONG).show();
        Person person = Plus.PeopleApi.getCurrentPerson(mGoogleApiClient);
        String email = Plus.AccountApi.getAccountName(mGoogleApiClient);
        Person.Name personName = person.getName();
        String lastName = personName.getFamilyName();
        String middleName = personName.getMiddleName();
        String firstName = personName.getGivenName();
//        getAccessToken(email,firstName,middleName,lastName);
        becomeUser(email);
    }

    private void becomeUser(final String emailField){
        HashMap<String,String> emailInput = new HashMap<>();
        emailInput.put("email", emailField);
        ParseCloud.callFunctionInBackground(PARSE_CLOUD_GET_ACCESS_TOKEN_FUNCTION
                ,emailInput,new FunctionCallback<Object>() {
            @Override
            public void done(Object item, ParseException e) {
                if (e==null && !item.equals("null")) {
                    ParseUser.becomeInBackground((String)item,new LogInCallback() {
                        @Override
                        public void done(ParseUser user, ParseException e) {
                            if(user!=null) {
                                // current user has been found
                                Utilites.setUser(user);
                                getEntries();
                                getGPlusInfo(new simpleCallback() {
                                    @Override
                                    public void done(Object first, Object second) {
                                        if(first!=null){
                                            Bundle info = (Bundle)first;
                                            moveOn(info);
                                        }
                                    }
                                });
                            }
                            else{
                                // couldn't find the user for some reason
                                Log.e(LOG_TAG,"Error: "+e);
                                e.printStackTrace();
                            }
                        }
                    });
                }
                else{
                    if(e!=null){
                        Log.e(LOG_TAG,"Error: "+e);
                        e.printStackTrace();
                    }
                    // user didn't exist before, create a new one
                    createAnonymousUser(new simpleCallback() {
                        @Override
                        public void done(Object first, Object second) {
                            getGPlusInfo(new simpleCallback() {
                                @Override
                                public void done(Object first, Object second) {
                                    if(first!=null){
                                        Utilites.getUser().put(IS_ANONYMOUS, false);
                                        Utilites.getUser().saveInBackground();
                                        Bundle info = (Bundle)first;
                                        moveOn(info);
                                    }
                                }
                            });
                        }
                    });
                    // user doesn't exist, create new user
//                    createAnonymousUser(new simpleCallback() {
//                        @Override
//                        public void done(Object first, Object second) {
//                            ParseUser activeUser = (ParseUser)first;
//                            Person person = Plus.PeopleApi.getCurrentPerson(mGoogleApiClient);
//                            Person.Name personName = person.getName();
//                            String lastName = personName.getFamilyName();
//                            String middleName = personName.getMiddleName();
//                            String firstName = personName.getGivenName();
//                            if(firstName !=null)
//                                activeUser.put(FIRST_NAME,firstName);
//                            String endName = "";
//                            if(middleName !=null)
//                                endName += middleName;
//                            if(lastName !=null){
//                                // add a space between the middle and last name
//                                if(middleName !=null)
//                                    endName+= " ";
//                                endName += lastName;}
//                            if(endName.length()>1)
//                                activeUser.put(LAST_NAME,endName);
//                            if(emailField!=null)
//                                activeUser.put(E_MAIL,emailField);
//
//                            activeUser.saveInBackground(new SaveCallback() {
//                                @Override
//                                public void done(ParseException e) {
//                                    if(e!=null){
//                                        Log.e(LOG_TAG,"Error: " + e);
//                                        e.printStackTrace();
//                                    }
//                                    else {
//                                        Log.d(LOG_TAG,"info saved successfully!");
//                                    }
//                                }
//                            });
//
//                        }
//                    });



                }
            }
        });
    }

    private void getGPlusInfo(simpleCallback callback){
        Bundle result = new Bundle();
        Person person = Plus.PeopleApi.getCurrentPerson(mGoogleApiClient);
        Person.Name personName = person.getName();
        String email = Plus.AccountApi.getAccountName(mGoogleApiClient);
        String lastName = personName.getFamilyName();
        String middleName = personName.getMiddleName();
        String firstName = personName.getGivenName();
        if(firstName !=null)
            result.putString(FIRST_NAME, firstName);
        String endName = "";
        if(middleName !=null)
            endName += middleName;
        if(lastName !=null){
            // add a space between the middle and last name
            if(middleName !=null)
                endName+= " ";
            endName += lastName;
        }
        if(endName.length()>1)
            result.putString(LAST_NAME, endName);
        if(email!=null)
            result.putString(E_MAIL, email);

        if(callback !=null)
            callback.done(result,null);
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onDisconnected() {

    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        if (!mIntentInProgress) {
            // Store the ConnectionResult so that we can use it later when the user clicks
            // 'sign-in'.
            mConnectionResult = result;

            if (mSignInClicked) {
                // The user has already clicked 'sign-in' so we attempt to resolve all
                // errors until the user is signed in, or they cancel.
                resolveSignInError();
            }
        }
    }

    /* A helper method to resolve the current ConnectionResult error. */
    private void resolveSignInError() {
        if (mConnectionResult.hasResolution()) {
            try {
                mIntentInProgress = true;
                startIntentSenderForResult(mConnectionResult.getResolution().getIntentSender(),
                        RC_SIGN_IN, null, 0, 0, 0);
            } catch (IntentSender.SendIntentException e) {
                // The intent was canceled before it was sent.  Return to the default
                // state and attempt to connect to get an updated ConnectionResult.
                mIntentInProgress = false;
                mGoogleApiClient.connect();
            }
        }
    }

}
