package com.example.towing.towme;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.example.towing.towme.maps.LocationTrackingService;
import com.example.towing.towme.maps.TowLocationClient;
import com.google.android.gms.common.api.GoogleApiClient;

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

        Button startLocation = (Button)mRootView.findViewById(R.id.start_location_btn);
        Button stopLocation = (Button)mRootView.findViewById(R.id.stop_location_btn);
        Button startService = (Button)mRootView.findViewById(R.id.start_service_btn);
        Button stopService = (Button)mRootView.findViewById(R.id.stop_service_btn);

        startLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TowLocationClient.start();
            }
        });

        stopLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TowLocationClient.stop();
            }
        });

        startService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mContext.startService(new Intent(mContext, LocationTrackingService.class));
            }
        });

        stopService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent service = //MainApplication.myService;
                new Intent(mContext, LocationTrackingService.class);
                mContext.stopService(service);
            }
        });

        mContext = getActivity();

        return mRootView;
    }

    @Override
    public String getName() {
        return LOG_TAG;
    }

}
