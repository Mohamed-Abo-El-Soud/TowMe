package com.example.towing.towme;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.parse.ParseUser;

/**
 * Created by Mohamed on 14-11-29.
 */
public class Utilites{

    private static final String FIRST_TIME_KEY = ".first_time_key_";
    private static ParseUser mUser = null;
    private static LocationPost mPost = null;

    public static boolean checkIfFirstTime(Context context){
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(context);
        Boolean key = sharedPreferences.getBoolean(FIRST_TIME_KEY, true);
        if(key){
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(FIRST_TIME_KEY,false);
            editor.apply();
            return true;
        }
        else
            return false;
    }
    public static boolean storeString (Context context, String key, String value){
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(context);
        if(!sharedPreferences.getString(key,"none").equals("none"))
            return false;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key,value);
        return editor.commit();
    }

    public static String retrieveString (Context context, String key){
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(context);
        String string = sharedPreferences.getString(key,"none");
        if(string.equals("none"))
            return null;
        return string;
    }



    public static ParseUser getUser() {
        return mUser;
    }


    public static LocationPost getLocationPost() {
        return mPost;
    }

    public static void setUser(ParseUser user) {
        mUser = user;
    }


    public static void setLocationPost(LocationPost locationPost) {
        mPost = locationPost;
    }
}
