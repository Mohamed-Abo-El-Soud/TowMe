package com.example.towing.towme;


import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;


/**
 * Created by ahmedabdalla on 14-11-26.
 */
public class DrawerItemClickListener
        implements
        ListView.OnItemClickListener
        ,DrawerLayout.DrawerListener
        ,FragmentManager.OnBackStackChangedListener
{

    // used for troubleshooting
    private static final String LOG_TAG = DrawerItemClickListener.class.getSimpleName();
    // external fields
    private ListView mDrawerList;
    private ActionBarActivity mActivity;
    private String[] mDrawerOptions;
    private DrawerLayout mDrawerLayout;

    /**
     * used to identify the fragments
     * */
    private static final String[] fragmentTypes = {
            MapFragment.LOG_TAG
            ,OptionFragment.LOG_TAG
            ,SettingsFragment.LOG_TAG
    };

    /**
     * a list of all active fragments that are created when the drawer is initialized
     * */
    private static Fragment[] activeFragments = {
            new MapFragment()
            ,new OptionFragment()
            ,new SettingsFragment()
    };

    /**
     * Used as an identifier for fragments
     * */
    public interface FragmentWithName {
        public String getName();
    }

    private String currentTitle = null;

    /**
     * constructor to initialize all required fields
     * */
    public DrawerItemClickListener(ActionBarActivity activity
            ,String[]drawerOptions
            ,DrawerLayout drawerLayout
            ,ListView drawerList){
        mActivity = activity;
        mDrawerOptions = drawerOptions;
        mDrawerLayout = drawerLayout;
        mDrawerList = drawerList;
        mActivity.getSupportFragmentManager().addOnBackStackChangedListener(this);

    }

    /**
     * called when the user clicks on an item in the drawer
     * */
    @Override
    public void onItemClick(AdapterView parent, View view, int position, long id) {
        selectItem(position);
    }


    @Override
    public void onDrawerSlide(View view, float v) {

    }

    @Override
    public void onDrawerOpened(View view) {
        ActionBar ab = mActivity.getSupportActionBar();
        if(ab ==null) return;
        ab.setTitle(mActivity.getString(R.string.app_name));
    }

    @Override
    public void onDrawerClosed(View view) {
        ActionBar ab = mActivity.getSupportActionBar();
        if(ab ==null) return;
        Fragment fragment =
                mActivity.getSupportFragmentManager().findFragmentById(R.id.content_frame);
        if(fragment == null)return;
        FragmentWithName fragmentWithName = (FragmentWithName)fragment;
        if(fragmentWithName == null)return;
        int fragmentType = getFragmentType(fragmentWithName);
        ab.setTitle(mDrawerOptions[fragmentType]);
    }

    @Override
    public void onDrawerStateChanged(int i) {

    }


    /**
     * returns a number that correspond to the index of the activeFragments array
     * @param fragment: fragment in question
     * */
    public static int getFragmentType(FragmentWithName fragment){
        for (int i = 0; i < fragmentTypes.length; i++) {
            if (fragment.getName().equals(fragmentTypes[i]))
                return i;
        }
        return -1;
    }


    /**
     * Swaps fragments in the main content view
     */
    private void selectItem(int position) {
//        mDrawerLayout.closeDrawer(mDrawerList);
        startFragment(R.id.content_frame, activeFragments[position]);
    }

    /**
     * starts a given fragment in a given resource id of layout view
     * */
    private void startFragment(int frame,Fragment fragment){
        FragmentManager fragmentManager = mActivity.getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(frame, fragment)
                .addToBackStack(null)
                .commit();
    }

    /**
     * Changes the title of the action bar to the title of the fragment
     * */
    public void setTitle(CharSequence title) {
        ActionBar ab = mActivity.getSupportActionBar();
        if(ab !=null)
            ab.setTitle(title);
    }

    /**
     * Called when a fragment transaction has occurred or the back button is pressed
     * */
    @Override
    public void onBackStackChanged() {
        Fragment fragment =
        mActivity.getSupportFragmentManager().findFragmentById(R.id.content_frame);
        try {
            FragmentWithName fragmentWithName = (FragmentWithName)fragment;
            if(fragmentWithName == null)
                // make sure the fragment has implemented the FragmentWithName interface
                throw new Exception("This fragment has not implemented FragmentWithName");

            // close the drawer
            if (mDrawerLayout.isDrawerOpen(mDrawerList))
                mDrawerLayout.closeDrawer(mDrawerList)
                ;
            int fragmentType = getFragmentType(fragmentWithName);
            // update the title
            setTitle(mDrawerOptions[fragmentType]);
            // Highlight the selected item
            mDrawerList.setItemChecked(fragmentType, true);

        } catch (Exception e) {
            Log.e(LOG_TAG, "Error: " + e);
            e.printStackTrace();
            return;
        }

    }
}