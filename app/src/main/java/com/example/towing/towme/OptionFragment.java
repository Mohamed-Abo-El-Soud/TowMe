package com.example.towing.towme;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.plus.Plus;

/**
 * Created by ahmedabdalla on 14-11-26.
 */
public class OptionFragment extends Fragment implements
        DrawerItemClickListener.FragmentWithName
{

    public static final String LOG_TAG = OptionFragment.class.getSimpleName();
    GoogleApiClient mGoogleApiClient;  // initialized in onCreate
    private View mRootView;
    private Context mContext;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        mRootView = inflater.inflate(R.layout.fragment_option, container, false);

//        mRootView.findViewById(R.id.sign_in_button).setOnClickListener(this);

        mContext = getActivity();

        return mRootView;
    }

    @Override
    public String getName() {
        return LOG_TAG;
    }

}
