package com.example.towing.towme;


import android.app.ActionBar;
import android.content.Intent;
import android.content.res.Configuration;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MapsActivity extends
        ActionBarActivity
//        FragmentActivity
{
    // fields for drawers
    private String[] mDrawerOptions;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // enabling action bar app icon and treating it as a toggle button
        Toolbar tb = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(tb);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        if (savedInstanceState == null) {
            initializeDrawer();
        }

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mToggle.onConfigurationChanged(newConfig);
    }

    /**
     * called to create the drawer and attach the appropriate click listeners
     * */
    private void initializeDrawer(){

        mDrawerOptions = getResources().getStringArray(R.array.drawer_options_array);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        mToggle = new ActionBarDrawerToggle(this, mDrawerLayout
                , R.string.app_name, R.string.app_name){};
        mDrawerLayout.setDrawerListener(mToggle);
        mToggle.setDrawerIndicatorEnabled(true);
        // Set the adapter for the list view
        mDrawerList.setAdapter(new ArrayAdapter<String>(this,
                R.layout.drawer_list_item
                , mDrawerOptions));
        // Set the list's click listener
        DrawerItemClickListener listener = new DrawerItemClickListener(this
                ,mDrawerOptions,mDrawerLayout,mDrawerList);
        mDrawerList.setOnItemClickListener(listener);
        mDrawerLayout.setDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View view, float v) {
                mToggle.onDrawerSlide(view,v);
            }

            @Override
            public void onDrawerOpened(View view) {
                mToggle.onDrawerOpened(view);
                android.support.v7.app.ActionBar ab = getSupportActionBar();
                if(ab ==null) return;
                ab.setTitle(getString(R.string.app_name));
            }

            @Override
            public void onDrawerClosed(View view) {
                mToggle.onDrawerClosed(view);
                android.support.v7.app.ActionBar ab = getSupportActionBar();
                if(ab ==null) return;
                Fragment fragment =
                        getSupportFragmentManager().findFragmentById(R.id.content_frame);
                if(fragment == null)return;
                DrawerItemClickListener.FragmentWithName fragmentWithName
                        = (DrawerItemClickListener.FragmentWithName)fragment;
                if(fragmentWithName == null)return;
                int fragmentType = DrawerItemClickListener.getFragmentType(fragmentWithName);
                ab.setTitle(mDrawerOptions[fragmentType]);
            }

            @Override
            public void onDrawerStateChanged(int i) {
                mToggle.onDrawerStateChanged(i);
            }
        });

        // Select the first item on the list
        mDrawerList.performItemClick(mDrawerList.getAdapter().getView(0, null, null)
                , 0, mDrawerList.getItemIdAtPosition(0));

        getSupportActionBar().setTitle(mDrawerOptions[0]);
    }

    /*
     * Called when the Activity becomes visible.
     */
    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        if (id == R.id.action_options) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, new OptionFragment())
                    .addToBackStack(null)
                    .commit();
            return true;
        }
        // handle the drawer touch listener
        if (mToggle.onOptionsItemSelected(item)) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


}
