package com.example.towing.towme;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

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

        final View rootView = inflater.inflate(R.layout.fragment_option, container, false);

        Button button = (Button)rootView.findViewById(R.id.refresh_button);

//        button.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                ThisApplication thisApplication = (ThisApplication)getActivity().getApplication();
//                User user = thisApplication.getUserInfo();
//                TextView userNameField = (TextView)rootView.findViewById(R.id.user_name_field);
//                TextView passWordField = (TextView)rootView.findViewById(R.id.pass_word_field);
//                userNameField.setText(user.getUserName());
//                passWordField.setText(user.getPassWordByPass());
//            }
//        });

        return rootView;
    }

    @Override
    public String getName() {
        return LOG_TAG;
    }
}
