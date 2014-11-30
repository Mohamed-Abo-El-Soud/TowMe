package com.example.towing.towme;

        import android.app.Application;
        import android.content.SharedPreferences;
        import android.content.SharedPreferences.Editor;
        import android.preference.PreferenceManager;
        import android.util.Log;
        import android.widget.TextView;
        import android.widget.Toast;

        import com.parse.GetCallback;
        import com.parse.LogInCallback;
        import com.parse.Parse;
        import com.parse.ParseAnonymousUtils;
        import com.parse.ParseException;
        import com.parse.ParseObject;
        import com.parse.ParseQuery;
        import com.parse.ParseUser;

/**
 * Created by ahmedabdalla on 14-11-28.
 */
//public class ThisApplication extends Application {
//
//
//    @Override
//    public void onCreate() {
//        super.onCreate();
//        ParseObject.registerSubclass(LocationPost.class);
//        Parse.initialize(this, getString(R.string.parse_application_id)
//                , getString(R.string.parse_client_key));
//    }
//
//
//
//}
