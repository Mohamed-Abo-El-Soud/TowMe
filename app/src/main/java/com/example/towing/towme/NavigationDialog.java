package com.example.towing.towme;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.DialogFragment;

/**
 * Created by ahmedabdalla on 14-12-04.
 */
public class NavigationDialog extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.navigation_prompt)
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Parcelable[] locations = getArguments().getParcelableArray(MapsActivity.DIALOG_KEY);
                        Location sAddress = (Location)locations[0];
                        Location dAddress = (Location)locations[1];
                        final Intent intent = new Intent(Intent.ACTION_VIEW
                                , Uri.parse("http://maps.google.com/maps?" + "saddr=" + sAddress.getLatitude()
                                + "," + sAddress.getLongitude() + "&daddr=" + dAddress.getLatitude()
                                + "," + dAddress.getLongitude()));
                        intent.setClassName("com.google.android.apps.maps","com.google.android.maps.MapsActivity");
                        startActivity(intent);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }
}
