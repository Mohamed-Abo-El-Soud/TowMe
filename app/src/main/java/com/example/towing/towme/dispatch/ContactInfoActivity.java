package com.example.towing.towme.dispatch;

import android.content.Context;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ProgressBar;

import com.example.towing.towme.R;

import java.util.List;

/**
 * Created by ahmedabdalla on 14-12-10.
 */
public class ContactInfoActivity extends FragmentActivity
        implements ScrollFormFragment.LoadingScreen{

    private ProgressBar loading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_info);

    }

    @Override
    protected void onStart() {
        ScrollFormFragment visibleFragment = (ScrollFormFragment)getFirstFragment();
        visibleFragment.setLoading(this);
        loading = (ProgressBar)findViewById(R.id.dispatch_loading);

        super.onStart();
    }

    public Fragment getFirstFragment(){
        FragmentManager fragmentManager = getSupportFragmentManager();
        List<Fragment> fragments = fragmentManager.getFragments();
        for(Fragment fragment : fragments){
            if(fragment != null)
                return fragment;
        }
        return null;
    }

    @Override
    public void showLoading() {
        loading.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideLoading() {
        loading.setVisibility(View.GONE);
    }

}
