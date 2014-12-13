package com.example.towing.towme;


import android.content.res.Configuration;
import android.location.Location;
import android.support.v4.app.Fragment;
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

import com.example.towing.towme.maps.Placer;


public class MapsActivity extends
        ActionBarActivity
    implements Placer.dialogListener
{
    // fields for drawers
    private String[] mDrawerOptions;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mToggle;

    private NavigationDialog mDialog;
    public static final String DIALOG_KEY = ".dialog_key.";


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
            mDialog = new NavigationDialog();
        }

    }

    @Override
    public void showDialog(Location source,Location destination) {
        if (mDialog==null) return;
        Bundle bundle = new Bundle();
        Location[] locations = {source,destination};
        bundle.putParcelableArray(DIALOG_KEY,locations);
        mDialog.setArguments(bundle);
        mDialog.show(getSupportFragmentManager(), null);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if(mToggle != null)
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

    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() == 0) {
            this.finish();
        } else {
            getFragmentManager().popBackStack();
        }
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
//        MenuItem searchItem = menu.findItem(R.id.action_search);
//        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
//
//        // Get the SearchView and set the searchable configuration
//        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
//        // Assumes current activity is the searchable activity
//        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
//        searchView.setIconifiedByDefault(false); // Do not iconify the widget; expand it by default

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle the drawer touch listener
        if (mToggle.onOptionsItemSelected(item)) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


}
