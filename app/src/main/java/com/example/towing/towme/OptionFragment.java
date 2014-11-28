package com.example.towing.towme;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by ahmedabdalla on 14-11-26.
 */
public class OptionFragment extends Fragment implements
        DrawerItemClickListener.FragmentWithName {

    public static final String ARG_OPTION_NAME = ".option_name";
    public static final String LOG_TAG = OptionFragment.class.getSimpleName();

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
        return inflater.inflate(R.layout.fragment_option, container, false);
    }

    @Override
    public String getName() {
        return LOG_TAG;
    }
}
