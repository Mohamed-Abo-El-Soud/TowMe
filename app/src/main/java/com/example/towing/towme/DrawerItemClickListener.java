package com.example.towing.towme;


import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;


/**
 * Created by ahmedabdalla on 14-11-26.
 */
public class DrawerItemClickListener
        implements
        ListView.OnItemClickListener
        ,FragmentManager.OnBackStackChangedListener
{

    private static final String LOG_TAG = DrawerItemClickListener.class.getSimpleName();
    private ListView mDrawerList;
    private ActionBarActivity mActivity;
    private String[] mDrawerOptions;
    private DrawerLayout mDrawerLayout;

    private static final String[] fragmentTypes = {
            MapFragment.LOG_TAG
            ,OptionFragment.LOG_TAG
            ,SettingsFragment.LOG_TAG
    };

    private static Fragment[] actviveFragments = {
            new MapFragment()
            ,new OptionFragment()
            ,new SettingsFragment()
    };

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
        mActivity.getSupportFragmentManager().addOnBackStackChangedListener(this);

    }

    @Override
    public void onItemClick(AdapterView parent, View view, int position, long id) {
        selectItem(position);
        mDrawerLayout.closeDrawer(Gravity.START);
    }

    private int getFragmentType(FragmentWithName fragment){
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

//        if (position == ROW_OPTIONS) {
//            Fragment option = new OptionFragment();
//            startFragment(R.id.content_frame, option,TAG);
//        }
//        if (position == ROW_MAP) {
//            Fragment option = new MapFragment();
//            startFragment(R.id.content_frame, option,TAG);
//        }
//        if(position == ROW_SETTINGS){
//            Fragment option = new SettingsFragment();
//            startFragment(R.id.content_frame, option,TAG);
//        }

        startFragment(R.id.content_frame,actviveFragments[position],TAG);

        // Highlight the selected item, update the title, and close the drawer
//        mDrawerList.setItemChecked(position, true);
//        setTitle(mDrawerOptions[position]);
//        mDrawerLayout.closeDrawer(mDrawerList);
    }

    /**
     * starts a given fragment in a given resource id of layout view
     * the startFragment is overloaded to allow non-appCompat
     * fragments to be started as well
     * */
    private void startFragment(int frame,Fragment fragment,String tag){
        FragmentManager fragmentManager = mActivity.getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(frame, fragment,tag)
                .addToBackStack(null)
                .commit();
    }


    public void setTitle(CharSequence title) {
        ActionBar ab = mActivity.getSupportActionBar();
        if(ab !=null)
            ab.setTitle(title);
    }

    @Override
    public void onBackStackChanged() {
        Fragment fragment =
        mActivity.getSupportFragmentManager().findFragmentById(R.id.content_frame);
        try {
            FragmentWithName fragmentWithName = (FragmentWithName)fragment;
            if(fragmentWithName == null)
                throw new Exception("This fragment has not implemented FragmentWithName");

            int fragmentType = getFragmentType(fragmentWithName);
            setTitle(mDrawerOptions[fragmentType]);
            mDrawerList.setItemChecked(fragmentType, true);
            mDrawerLayout.closeDrawer(mDrawerList);

        } catch (Exception e) {
            Log.e(LOG_TAG, "Error: " + e);
            e.printStackTrace();
            return;
        }

    }
}