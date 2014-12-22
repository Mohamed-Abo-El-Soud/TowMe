package com.example.towing.towme.dispatch;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.towing.towme.MapsActivity;
import com.example.towing.towme.R;
import com.example.towing.towme.Utilites;
import com.example.towing.towme.dispatch.DispatchActivity.simpleCallback;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

/**
 * Created by ahmedabdalla on 14-12-10.
 */
public class ScrollFormFragment extends Fragment {
    private View mRootView;
    private Context mContext;

    private EditText firstNameField;
    private EditText lastNameField;
    private EditText phoneNumberField;
    private EditText emailField;
    private Spinner carModelField;
    private Spinner carMakeField;
    private Spinner carYearField;
    private Button submitButton;

    private static final int STARTING_YEAR = 1970;
    private static final int CURRENT_YEAR = Calendar.getInstance().get(Calendar.YEAR);

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container
            , @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_scroll_form, container, false);

        mContext = getActivity();

        firstNameField = (EditText)mRootView.findViewById(R.id.first_name);
        lastNameField = (EditText)mRootView.findViewById(R.id.last_name);
        phoneNumberField = (EditText)mRootView.findViewById(R.id.phone_number);
        emailField = (EditText)mRootView.findViewById(R.id.e_mail);
        carModelField = (Spinner)mRootView.findViewById(R.id.car_model);
        carMakeField = (Spinner)mRootView.findViewById(R.id.car_make);
        carYearField = (Spinner)mRootView.findViewById(R.id.car_year);
        submitButton = (Button)mRootView.findViewById(R.id.submit_button);

        carModelField.setEnabled(false);

        fillInCarYears();
        fillInCarMakes();

        carMakeField.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String model = (String) ((TextView) view).getText();
                fillInCarModels(model);

                carModelField.setEnabled(true);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                carModelField.setEnabled(false);
            }
        });

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(validateInput()) {
                    sendInfo(new simpleCallback() {
                        @Override
                        public void done(Object first, Object second) {
                            startActivity(new Intent(getActivity(), MapsActivity.class));
                        }
                    });
                }
            }
        });

        Intent intent = getActivity().getIntent();
        if(intent!=null)
            fillInAccountInfo(intent.getBundleExtra(DispatchActivity.ACCOUNT_INFO_KEY));
        return mRootView;
    }

    private void fillInAccountInfo(Bundle accountInfo){
        if(accountInfo==null) return;
        String email = accountInfo.getString(DispatchActivity.E_MAIL);
        String firstName = accountInfo.getString(DispatchActivity.FIRST_NAME);
        String lastName = accountInfo.getString(DispatchActivity.LAST_NAME);
        String phoneNumber = accountInfo.getString(DispatchActivity.PHONE_NUMBER);
        if(email!=null)
            emailField.setText(email);
        if(firstName!=null)
            firstNameField.setText(firstName);
        if(lastName!=null)
            lastNameField.setText(lastName);
        if(phoneNumber!=null)
            phoneNumberField.setText(phoneNumber);
    }

    private void sendInfo(final simpleCallback callback){
        ParseUser user = Utilites.getUser();
        String email = emailField.getText().toString();
        String firstName = firstNameField.getText().toString();
        String lastName = lastNameField.getText().toString();
        String phoneNumber = phoneNumberField.getText().toString();
        String carYear = carYearField.getSelectedItem().toString();
        String carMake = carMakeField.getSelectedItem().toString();
        String carModel = carModelField.getSelectedItem().toString();
        user.put(DispatchActivity.E_MAIL,email);
        user.put(DispatchActivity.FIRST_NAME,firstName);
        user.put(DispatchActivity.LAST_NAME,lastName);
        user.put(DispatchActivity.PHONE_NUMBER,phoneNumber);
        user.put(DispatchActivity.CAR_YEAR,carYear);
        user.put(DispatchActivity.CAR_MAKE,carMake);
        user.put(DispatchActivity.CAR_MODEL,carModel);
        user.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if(e==null){
                    if(callback!=null)
                        callback.done(null,null);
                } else {
                    Toast.makeText(mContext, "make sure you have an internet " +
                            "connection to use this app"
                            , Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void fillInCarYears(){

        List<String> list = new ArrayList<String>();
        for (int i = STARTING_YEAR; i <= CURRENT_YEAR; i++) {
            list.add(i+"");
        }

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>
                (mContext, android.R.layout.simple_spinner_item,list);

        dataAdapter.setDropDownViewResource
                (android.R.layout.simple_spinner_dropdown_item);

        carYearField.setAdapter(dataAdapter);
    }

    private void fillInCarMakes(){

        List<String> list = new ArrayList<String>();

        list.add("Honda");
        list.add("Toyota");
        list.add("Hyundai");



        ArrayAdapter<CharSequence> dataAdapter;
//        = new ArrayAdapter<String>
//                (mContext, android.R.layout.simple_spinner_item,list);

        dataAdapter = ArrayAdapter.createFromResource(
                mContext,R.array.car_makes,android.R.layout.simple_spinner_item);

        dataAdapter.setDropDownViewResource
                (android.R.layout.simple_spinner_dropdown_item);

        carMakeField.setAdapter(dataAdapter);
    }

    private void fillInCarModels(String model){

        List<String> list = new ArrayList<String>();

        String[] models;

        String[] HondaModels = {"Accord","Civic","Oddessy","CR-V","Pilot"};
        String[] ToyotaModels = {"Camry","Corrolla","Sienna","Rava","Highlander"};
        String[] HyundaiModels = {"Elantra","Sonata","Genesis","Tuscon","Santa-Fe"};

        Integer resource;
        switch (model){
            case "Honda":
                models = HondaModels;
                resource = R.array.honda_car_models;
                break;
            case "Toyota":
                models = ToyotaModels;
                resource = R.array.toyota_car_models;
                break;
            case "Hyundai":
                models = HyundaiModels;
                resource = R.array.hyundai_car_models;
                break;
            default:
                models = null;
                resource = null;
                break;
        }
        if(models == null) return;

        list.addAll(Arrays.asList(models));

        ArrayAdapter<CharSequence> dataAdapter;
//        = new ArrayAdapter<String>
//                (mContext, android.R.layout.simple_spinner_item,list);


        dataAdapter = ArrayAdapter.createFromResource(
                mContext,resource,android.R.layout.simple_spinner_item);

        dataAdapter.setDropDownViewResource
                (android.R.layout.simple_spinner_dropdown_item);

        carModelField.setAdapter(dataAdapter);
    }

    private Boolean validateInput(){
        String initialText = "Enter a valid ";
        if(emailField.getText().toString().equals("")) {
            Toast.makeText(getActivity(),initialText+"email",Toast.LENGTH_SHORT).show();
            return false;
        }
        if(firstNameField.getText().toString().equals("")){
            Toast.makeText(getActivity(),initialText+"first name",Toast.LENGTH_SHORT).show();
            return false;
        }
        if(lastNameField.getText().toString().equals("")){
            Toast.makeText(getActivity(),initialText+"last name",Toast.LENGTH_SHORT).show();
            return false;
        }
        if(phoneNumberField.getText().toString().equals("")) {
            Toast.makeText(getActivity(), initialText + "phone number", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

}
