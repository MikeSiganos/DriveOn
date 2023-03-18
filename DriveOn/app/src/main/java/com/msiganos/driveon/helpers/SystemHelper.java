package com.msiganos.driveon.helpers;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.msiganos.driveon.R;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SystemHelper {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 901;
    private final Context context;
    private final Activity activity;
    private final LocationManager locationManager;
    private String device, timestamp;

    public SystemHelper(Activity activity) {
        this.activity = activity;
        this.context = activity.getApplicationContext();
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        setDevice();
        setTimestamp();
    }

    public String getDevice() {
        return device;
    }

    public void setDevice() {
        this.device = Build.MANUFACTURER + " " + Build.DEVICE;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp() {
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
        Date date = new Date();
        this.timestamp = dateFormat.format(date);
    }

    protected boolean getNetworkConnection() {
        boolean connected = false;
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            Network network = connectivityManager.getActiveNetwork();
            if (network != null) {
                // The device is connected to a network
                NetworkCapabilities activeNetwork = connectivityManager.getNetworkCapabilities(network);
                if (activeNetwork != null) {
                    // The device is connected to an active network
                    if (activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        // Connected to wifi & check internet connection
                        Log.i("Network", "The device is connected to a wifi active network");
                        connected = getInternetConnection();
                    } else if (activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                        // Connected to mobile data & check internet connection
                        Log.i("Network", "The device is connected to a mobile data active network");
                        connected = getInternetConnection();
                    } else if (activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                        // Connected to ethernet & check internet connection
                        Log.i("Network", "The device is connected to an ethernet active network");
                        connected = getInternetConnection();
                    } else if (activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)) {
                        // Connected to bluetooth & check internet connection
                        Log.i("Network", "The device is connected to a bluetooth active network");
                        connected = getInternetConnection();
                    }
                } else {
                    // The device is not connected to an active network
                    Log.w("Network", "The device is not connected to an active network");
                    showNoConnectionAlertDialog();
                }
            } else {
                // The device is not connected to a network
                Log.w("Network", "The device is not connected to a network");
                showNoConnectionAlertDialog();
            }
        } catch (Exception e) {
            Log.e("Network", "Network exception", e);
        }
        return connected;
    }

    protected boolean getInternetConnection() {
        // Try ping google.com and if it's successful we have internet
        String command = "ping -c 1 google.com";
        boolean connection = false;
        try {
            if (Runtime.getRuntime().exec(command).waitFor() == 0) {
                Log.i("Network", "The device is connected to the internet");
                connection = true;
            } else {
                Log.w("Network", "The device is not connected to the internet");
                showNoConnectionAlertDialog();
            }
        } catch (Exception e) {
            Log.e("Network", "Network exception", e);
        }
        return connection;
    }

    protected void showNoConnectionAlertDialog() {
        // Make custom alert dialog for no connection error
        AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                .setTitle(context.getString(R.string.no_network_connection))
                .setMessage(context.getString(R.string.no_network_error))
                .setCancelable(true)
                .setPositiveButton(context.getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setNeutralButton(context.getString(R.string.retry), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if (getNetworkConnection())
                            getFirebaseConnection();
                    }
                })
                .setNegativeButton(context.getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        // activity.onBackPressed();
                    }
                });
        builder.show();
    }

    protected void getFirebaseConnection() {
        // Check firebase realtime database connection
        DatabaseReference connectedRef = FirebaseDatabase.getInstance("https://drive-on-mscsd-default-rtdb.europe-west1.firebasedatabase.app/").getReference(".info/connected");
        try {
            connectedRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (Boolean.TRUE.equals(snapshot.getValue(Boolean.class))) {
                        Log.i("FirebaseDatabase", "Firebase connection succeed");
                    } else {
                        Log.w("FirebaseDatabase", "Firebase connection failed");
                        Toast.makeText(context, context.getString(R.string.no_connection), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.w("FirebaseDatabase", "Firebase connection error", error.toException());
                }
            });
        } catch (Exception e) {
            Log.e("FirebaseDatabase", "Firebase connection exception", e);
        }
    }

    protected boolean getGServices() {
        // Check google play services
        switch (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)) {
            case ConnectionResult.SUCCESS:
                Log.i("GoogleServices", "Google Play Services Ok");
                Toast.makeText(context, context.getString(R.string.google_services_ok), Toast.LENGTH_SHORT).show();
                return true;
            case ConnectionResult.SERVICE_MISSING:
                Log.w("GoogleServices", "Google Play Services Missing");
                Toast.makeText(context, context.getString(R.string.google_services_missing), Toast.LENGTH_SHORT).show();
                return false;
            case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
                Log.w("GoogleServices", "Update Google Play Services");
                Toast.makeText(context, context.getString(R.string.google_services_update), Toast.LENGTH_SHORT).show();
                return false;
            default:
                Log.w("GoogleServices", "GServices availability: " + GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context));
                return false;
        }
    }

    protected boolean getGPSConnection() {
        boolean gpsEnabled = false;
        final boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (isGPSEnabled) {
            // GPS enabled
            Log.i("Sensors", "GPS enabled");
            gpsEnabled = true;
        } else {
            // If GPS is not connected prompt user to enable it
            Log.w("Sensors", "GPS disabled");
            AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                    .setTitle(context.getString(R.string.no_gps_connection))
                    .setMessage(context.getString(R.string.no_gps_error))
                    .setCancelable(true)
                    .setPositiveButton(context.getString(R.string.ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            activity.startActivity(intent);
                        }
                    })
                    .setNeutralButton(context.getString(R.string.retry), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            getGPSConnection();
                        }
                    })
                    .setNegativeButton(context.getString(R.string.cancel), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                            // activity.onBackPressed();
                        }
                    });
            builder.show();
        }
        return gpsEnabled;
    }

    public boolean getPermissions(int requestCode) {
        // Check for permissions based on requestCode
        boolean permissionGranted = false;
        try {
            switch (requestCode) {
                case LOCATION_PERMISSION_REQUEST_CODE:
                    // Check user's location permissions
                    if (getGPSConnection()) {
                        // GPS enabled
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            // Permissions granted
                            Log.i("Permissions", "Location permissions granted");
                            permissionGranted = true;
                        } else if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION) &&
                                ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                            // Permissions not granted but explain user
                            Log.w("Permissions", "Location permissions still not granted");
                            showPermissionsAlertDialog(context.getString(R.string.no_gps_connection), context.getString(R.string.location_permissions), LOCATION_PERMISSION_REQUEST_CODE);
                        } else {
                            // Ask for location permissions
                            Log.w("Permissions", "Location permissions not granted");
                            ActivityCompat.requestPermissions(activity,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
                        }
                    }
                    break;
                default:
                    Log.w("Permissions", "Unknown permission request code");
            }
        } catch (Exception e) {
            Log.e("Permissions", "Permissions exception", e);
        }
        return permissionGranted;
    }

    private void showPermissionsAlertDialog(String title, String message, int requestCode) {
        // AlertDialog Builder for custom permissions error
        AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(true)
                .setPositiveButton(context.getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Ok Open app setting to manually grant permissions
                        dialog.dismiss();
                        try {
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            intent.setData(Uri.parse("package:" + context.getPackageName()));
                            activity.startActivity(intent);
                        } catch (Exception e) {
                            Log.e("AppSettings", "Start custom intent exception", e);
                            Toast.makeText(context, context.getString(R.string.settings_intent_error), Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNeutralButton(context.getString(R.string.retry), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Retry Re-Check for specific permissions
                        dialog.dismiss();
                        getPermissions(requestCode);
                    }
                })
                .setNegativeButton(context.getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Cancel
                        dialog.cancel();
                    }
                });
        builder.show();
    }
}