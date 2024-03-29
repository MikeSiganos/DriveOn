package com.msiganos.driveon.helpers;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
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
import android.os.Environment;
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
import com.msiganos.driveon.BuildConfig;
import com.msiganos.driveon.R;

import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import javax.net.ssl.HttpsURLConnection;

public class SystemHelper {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 901;
    private final Context context;
    private final Activity activity;
    private final LocationManager locationManager;
    private DatabaseReference mDatabaseReference;
    private String device, timestamp, updateUrl, currentAppVersionName, news;
    private int currentAppVersionCode = -1;
    private boolean firebaseConnection = false;
    private boolean internet = false;

    public SystemHelper(Activity activity) {
        this.activity = activity;
        this.context = activity.getApplicationContext();
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        initFirebase();
        setDevice();
        setTimestamp();
    }

    private void initFirebase() {
        // Firebase Realtime Database
        FirebaseDatabase mDatabase = FirebaseDatabase.getInstance("https://drive-on-mscsd-default-rtdb.europe-west1.firebasedatabase.app/");
        mDatabaseReference = mDatabase.getReference();
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

    @SuppressWarnings("SpellCheckingInspection")
    public void setTimestamp() {
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
        Date date = new Date();
        this.timestamp = dateFormat.format(date);
    }

    public boolean getNetworkConnection() {
        // Check active network
        boolean connected = false;
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
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
        // Check internet connection
        internet = false;
        try {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        HttpsURLConnection urlConnection = (HttpsURLConnection) new URL("https://clients3.google.com/generate_204").openConnection();
                        urlConnection.setRequestProperty("User-Agent", "Android");
                        urlConnection.setRequestProperty("Connection", "close");
                        urlConnection.setConnectTimeout(5000);
                        urlConnection.connect();
                        internet = urlConnection.getResponseCode() == 204 && urlConnection.getContentLength() == 0;
                        Log.i("Network", "The device is connected to the internet");
                        // Check for updates
                        checkForUpdates();
                        // Check firebase realtime database connection
                        getFirebaseConnection();
                        if (firebaseConnection)
                            Log.i("FirebaseDatabase", "There is an active access to the firebase database");
                        else
                            Log.w("FirebaseDatabase", "There is not an active access to the firebase database");
                    } catch (Exception e) {
                        showNoConnectionAlertDialog();
                        Log.w("Network", "The device is not connected to the internet");
                        Log.e("Network", "Network exception ", e);
                    }
                }
            }).start();
        } catch (Exception e) {
            showNoConnectionAlertDialog();
            Log.w("Network", "The device is not connected to the internet");
            Log.e("Network", "Network exception ", e);
        }
        return internet;
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
                        getNetworkConnection();
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
        firebaseConnection = false;
        try {
            mDatabaseReference.child(".info/connected").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (Boolean.TRUE.equals(snapshot.getValue(Boolean.class))) {
                        Log.i("FirebaseDatabase", "Firebase connection succeed");
                        firebaseConnection = true;
                    } else {
                        Log.w("FirebaseDatabase", "Firebase connection failed");
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
        // Check Google Play Services
        if (!getGServices()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                    .setTitle(context.getString(R.string.app_name))
                    .setMessage(context.getString(R.string.google_services_check))
                    .setNeutralButton(context.getString(R.string.ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            builder.show();
        }
    }

    protected boolean getGServices() {
        // Check google play services
        switch (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)) {
            case ConnectionResult.SUCCESS:
                Log.i("GoogleServices", context.getString(R.string.google_services_ok));
                // Toast.makeText(context, context.getString(R.string.google_services_ok), Toast.LENGTH_SHORT).show();
                return true;
            case ConnectionResult.SERVICE_MISSING:
                Log.w("GoogleServices", context.getString(R.string.google_services_missing));
                Toast.makeText(context, context.getString(R.string.google_services_missing), Toast.LENGTH_SHORT).show();
                return false;
            case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
                Log.w("GoogleServices", context.getString(R.string.google_services_update));
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
                case -1:
                    Log.d("Permissions", "Permission request code -1: Other permission option");
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

    public void checkForUpdates() {
        try {
            Log.i("Update", "Checking for updates...");
            int appVersionCode = BuildConfig.VERSION_CODE;
            String appVersionName = BuildConfig.VERSION_NAME;
            // Read user records from firebase
            mDatabaseReference.child("Application").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    // Get update data from firebase
                    currentAppVersionCode = Integer.parseInt(Objects.requireNonNull(dataSnapshot.child("version").getValue()).toString());
                    updateUrl = Objects.requireNonNull(dataSnapshot.child("update").getValue()).toString();
                    currentAppVersionName = Objects.requireNonNull(dataSnapshot.child("number").getValue()).toString();
                    news = Objects.requireNonNull(dataSnapshot.child("news").getValue()).toString();
                    Log.i("Update", "v" + appVersionName + " --> v" + currentAppVersionName);
                    if (currentAppVersionCode > appVersionCode) {
                        // Download & Install update
                        // Toast.makeText(context, context.getString(R.string.message_new_update), Toast.LENGTH_SHORT).show();
                        Log.i("Update", "We found some updates! Let's download & install the new app...");
                        AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                                .setTitle("\uD83C\uDD95 " + context.getString(R.string.app_name))
                                .setMessage(context.getString(R.string.message_new_update) + System.lineSeparator() + news + System.lineSeparator() + "v" + appVersionName + " ➠ v" + currentAppVersionName)
                                .setPositiveButton(context.getString(R.string.ok), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();
                                        updateApp(updateUrl);
                                    }
                                })
                                .setNegativeButton(context.getString(R.string.update_later), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();
                                    }
                                });
                        builder.show();
                    } else {
                        // No updates found
                        Log.i("Update", "We didn't find any updates for this app yet...");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(context, context.getString(R.string.failed) + System.lineSeparator() + error.toException().getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("Update", error.toException().toString());
                }
            });
        } catch (Exception e) {
            Log.e("Update", "Update exception", e);
        }
    }

    protected void updateApp(String updateUrl) {
        try {
            // Set DownloadManager
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(updateUrl));
            request.setTitle(context.getString(R.string.app_name));
            request.setDescription(context.getString(R.string.update));
            // Set notification visibility
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            // Set file type
            request.setMimeType("application/vnd.android.package-archive");
            // Set destination
            //request.setDestinationUri(uri);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "DriveOn.apk");
            // Get download service and enqueue file
            final DownloadManager manager = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);  // context.
            manager.enqueue(request);
        } catch (Exception e) {
            Log.e("Update", "Update exception ", e);
            Toast.makeText(context, context.getString(R.string.failed), Toast.LENGTH_SHORT).show();
        }
    }
}