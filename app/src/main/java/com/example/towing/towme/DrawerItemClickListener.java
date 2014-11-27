package com.example.towing.towme;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

/**
 * Created by ahmedabdalla on 14-11-26.
 */
public class DrawerItemClickListener implements ListView.OnItemClickListener {

    private ListView mDrawerList;
    private ActionBarActivity mActivity;
    private String[] mDrawerOptions;
    private DrawerLayout mDrawerLayout;

    // row positions that correspond to the displayed options
    public static final int ROW_MAP = 0;
    public static final int ROW_OPTIONS = 1;
    public static final int ROW_SETTINGS = 2;

    // tag names for the fragments so we can pull them out later
    public static final String TAG = ".tag";


    public DrawerItemClickListener(ActionBarActivity activity
            ,String[]drawerOptions
            ,DrawerLayout drawerLayout
            ,ListView drawerList){
        mActivity = activity;
        mDrawerOptions = drawerOptions;
        mDrawerLayout = drawerLayout;
        mDrawerList = drawerList;
    }

    @Override
    public void onItemClick(AdapterView parent, View view, int position, long id) {
        selectItem(position);
        mDrawerLayout.closeDrawer(Gravity.START);
    }


    /**
     * Swaps fragments in the main content view
     */
    private void selectItem(int position) {

        if (position == ROW_OPTIONS) {
            Fragment option = new OptionFragment();
            startFragment(R.id.content_frame, option,TAG);
        }
        if (position == ROW_MAP) {
            Fragment option = new MapFragment();
            startFragment(R.id.content_frame, option,TAG);
        }
        if(position == ROW_SETTINGS){
//            mActivity.startActivity(new Intent(mActivity, SettingsActivity.class));
            android.app.Fragment option = (android.app.Fragment)new SettingsFragment();
            startFragment(R.id.content_frame, option,TAG);
        }

        // Highlight the selected item, update the title, and close the drawer
        mDrawerList.setItemChecked(position, true);
        setTitle(mDrawerOptions[position]);
        mDrawerLayout.closeDrawer(mDrawerList);
    }

    /**
     * starts a given fragment in a given resource id of layout view
     * the startFragment is overloaded to allow non-appCompat
     * fragments to be started as well
     * */
    private void startFragment(int frame,Fragment fragment,String tag){
        FragmentManager fragmentManager = mActivity.getSupportFragmentManager();
        removeFragment(tag);
        fragmentManager.beginTransaction()
                .replace(frame, fragment,tag)
                .addToBackStack(null)
                .commit();
    }
    private void startFragment(int frame, android.app.Fragment fragment,String tag){
        removeFragment(tag);
        android.app.FragmentManager fragmentManager = mActivity.getFragmentManager();
        fragmentManager.beginTransaction()
                .replace(frame, fragment, tag)
                .addToBackStack(null)
                .commit();
    }

    private void removeFragment(String tag){
        FragmentManager suppFragmentManager = mActivity.getSupportFragmentManager();
        android.app.FragmentManager fragmentManager = mActivity.getFragmentManager();
        Fragment suppFragment = suppFragmentManager.findFragmentByTag(tag);
        android.app.Fragment fragment = fragmentManager.findFragmentByTag(tag);
        if (suppFragment !=null)
            suppFragmentManager.beginTransaction().remove(suppFragment)
                    .commit();
        if (fragment !=null)
            fragmentManager.beginTransaction().remove(fragment)
                    .commit();
    }


    public void setTitle(CharSequence title) {
        ActionBar ab = mActivity.getSupportActionBar();
        if(ab !=null)
            ab.setTitle(title);
    }
}