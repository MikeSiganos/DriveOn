package com.msiganos.driveon;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.BlendModeColorFilterCompat;
import androidx.core.graphics.BlendModeCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.msiganos.driveon.databinding.ActivityMapsBinding;
import com.msiganos.driveon.helpers.LocationHelper;
import com.msiganos.driveon.helpers.SystemHelper;
import com.msiganos.driveon.helpers.TrafficHelper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final int MIN_MILLISECONDS_BETWEEN_GPS_UPDATES = 100;
    private static final int MIN_METERS_FOR_GPS_UPDATES = 0;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 901;
    private static final int CALL_PERMISSION_REQUEST_CODE = 801;
    private static final int SMS_PERMISSION_REQUEST_CODE = 802;
    private GoogleMap gMap;
    private ActivityMapsBinding binding;
    private Marker mPositionMarker;
    private SystemHelper mSystem;
    private FirebaseUser mUser;
    private DatabaseReference mDatabaseReference;
    private LocationManager locationManager;
    private Criteria locationProviderCriteria;
    private String bestLocationProvider;
    private List<String> locationProviders;
    private SensorManager sensorManager;
    private Sensor accelerometerSensor;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor sharedPreferencesEditor;
    private ExtendedFloatingActionButton extendedFloatingActionButton, settingsExtendedFab;
    private FloatingActionButton screenOnFab, uploadDataFab, reportIncidentFab;
    private TextView screenOnActionText, uploadDataActionText, reportIncidentActionText;
    private ProgressBar accelerometerProgressBar;
    private Drawable progressDrawable;
    private final SensorEventListener accelerometerSensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            // Get accelerometer sensor data
            try {
                // Motion detection
                float[] mGravity = event.values.clone();
                float x = mGravity[0];
                float y = mGravity[1];
                float z = mGravity[2];
                // Do the math
                mAccelLast = mAccelCurrent;
                mAccelCurrent = Math.sqrt(x * x + y * y + z * z);
                double delta = mAccelCurrent - mAccelLast;
                mAccel = mAccel * 0.9f + delta;
                // Show custom progressbar
                accelerometerProgressBar.setProgress((int) Math.abs(mAccel));
                if (Math.abs(mAccel) >= 15) {
                    // High
                    if (Math.abs(mAccel) >= 25)
                        sosDialog();
                    progressDrawable.setColorFilter(BlendModeColorFilterCompat.createBlendModeColorFilterCompat(ResourcesCompat.getColor(getResources(), R.color.red, getTheme()), BlendModeCompat.SRC_IN));
                    Log.i("Sensors", "The accelerator sensor has changed: x: " + x + " y: " + y + " z: " + z + " Absolute Acceleration: " + Math.abs(mAccel));
                } else if (Math.abs(mAccel) >= 10) {
                    // Mid
                    progressDrawable.setColorFilter(BlendModeColorFilterCompat.createBlendModeColorFilterCompat(ResourcesCompat.getColor(getResources(), R.color.yellow, getTheme()), BlendModeCompat.SRC_IN));
                } else if (Math.abs(mAccel) >= 5) {
                    // Small
                    progressDrawable.setColorFilter(BlendModeColorFilterCompat.createBlendModeColorFilterCompat(ResourcesCompat.getColor(getResources(), R.color.green, getTheme()), BlendModeCompat.SRC_IN));
                } else {
                    // Nothing
                    progressDrawable.setColorFilter(BlendModeColorFilterCompat.createBlendModeColorFilterCompat(ResourcesCompat.getColor(getResources(), R.color.gray, getTheme()), BlendModeCompat.SRC_IN));
                }
                accelerometerProgressBar.setProgressDrawable(progressDrawable);
            } catch (Exception e) {
                Log.e("Sensors", "Accelerometer exception", e);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Get sensor accuracy
            switch (accuracy) {
                case 0:
                    Log.i("Sensors", "Accelerator sensor accuracy: Unreliable");
                    break;
                case 1:
                    Log.i("Sensors", "Accelerator sensor accuracy: Low accuracy");
                    break;
                case 2:
                    Log.i("Sensors", "Accelerator sensor accuracy: Moderate accuracy");
                    break;
                case 3:
                    Log.i("Sensors", "Accelerator sensor accuracy: high Accuracy");
                    break;
                default:
                    Log.i("Sensors", "The accuracy of the accelerator sensor has changed: " + accuracy);
            }
            // Set current progressbar max value
            try {
                accelerometerProgressBar.setMax((int) accelerometerSensor.getMaximumRange());
            } catch (Exception e) {
                Log.e("Sensors", "Accelerometer exception", e);
            }
        }
    };
    private int statusBarHeight, navigationBarHeight, trafficSecondsTimer;
    private Handler timerHandler;
    private Runnable timerRunnable;
    private ArrayList<Double> speedList;
    private boolean accelerometerAvailable, visibleCustomFab, screenOn, uploadData, useCustomMyLocationIndicator;
    private double HUMAN_SPEED_CONVERTER, currentSpeed, lastSpeed, currentBearing, lastBearing, mAccel, mAccelCurrent, mAccelLast, sumSpeed;
    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(final Location location) {
            // Get user's current location
            try {
                if (location == null) {
                    Log.w("LocationListener", "MapsActivity Location is null");
                    Toast.makeText(MapsActivity.this, getString(R.string.no_location), Toast.LENGTH_SHORT).show();
                } else {
                    // Get location
                    currentLocation = location;
                    // Get last speed
                    lastSpeed = currentSpeed;
                    // Get current speed | kmhSpeed = 3.6 * speed | mileSpeed = 2.23694 * speed
                    currentSpeed = BigDecimal.valueOf(currentLocation.getSpeed() * HUMAN_SPEED_CONVERTER).setScale(2, RoundingMode.HALF_UP).doubleValue();
                    // Get last bearing
                    lastBearing = currentBearing;
                    // Get current bearing
                    currentBearing = currentLocation.getBearing();
                    // View vehicle's speed in KM/H & altitude in M
                    extendedFloatingActionButton.setText(String.format(Locale.getDefault(), "%.2f KM/H | %.2f M", currentSpeed, currentLocation.getAltitude()));
                    // Preparing map camera
                    int zoom = 20;
                    if (currentSpeed >= 50)
                        zoom = 15;
                    if (gMap != null) {
                        if (useCustomMyLocationIndicator) {
                            // Preparing custom myLocation icon
                            if (mPositionMarker == null) {
                                Drawable vectorDrawable = ContextCompat.getDrawable(MapsActivity.this, R.drawable.ic_baseline_car_crash_24);
                                if (vectorDrawable != null) {
                                    vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
                                    vectorDrawable.mutate().setColorFilter(BlendModeColorFilterCompat.createBlendModeColorFilterCompat(ResourcesCompat.getColor(getResources(), R.color.blue_dark, getTheme()), BlendModeCompat.SRC_IN));
                                    Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                                    Canvas canvas = new Canvas(bitmap);
                                    vectorDrawable.draw(canvas);
                                    BitmapDrawable d = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(bitmap, 150, 300, true));
                                    Bitmap bm = d.getBitmap();
                                    mPositionMarker = gMap.addMarker(new MarkerOptions()
                                            .title(mUser.getDisplayName())
                                            .snippet(mSystem.getDevice())
                                            .flat(true)
                                            .icon(BitmapDescriptorFactory.fromBitmap(bm))
                                            .anchor(0.5f, 0.5f)
                                            .position(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude())));
                                }
                            } else
                                animateMarker(mPositionMarker, currentLocation);
                        }
                        // Animate map camera on user's current location
                        gMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                                new CameraPosition.Builder()
                                        .target(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()))
                                        .zoom(zoom)
                                        .bearing(currentLocation.getBearing())
                                        .tilt(80)
                                        .build()
                        ));
                    }
                    // Detect traffic
                    if (currentSpeed < 20) {
                        // Log.i("Traffic", "Traffic detected - Timer: " + trafficSecondsTimer + " - Location: " + currentLocation.getLatitude() + ";" + currentLocation.getLongitude() + " - Speed: " + currentLocation.getSpeed());
                        timerHandler.postDelayed(timerRunnable, 1000);
                        if (trafficSecondsTimer == 1) {
                            Log.i("Traffic", "First detected traffic - Timer: " + trafficSecondsTimer + " - Location: " + currentLocation.getLatitude() + ";" + currentLocation.getLongitude() + " - Speed: " + currentLocation.getSpeed());
                            trafficStart = currentLocation;
                        }
                    } else {
                        // Log.i("Traffic", "No traffic detected - Timer: " + trafficSecondsTimer + " - Location: " + currentLocation.getLatitude() + ";" + currentLocation.getLongitude() + " - Speed: " + currentLocation.getSpeed());
                        timerHandler.removeCallbacks(timerRunnable);
                        trafficEnd = currentLocation;
                        if (trafficSecondsTimer > 100 && trafficStart != null && trafficEnd != null) {
                            Log.i("Traffic", "Traffic reported - Timer: " + trafficSecondsTimer + " - LocationStart: " + trafficStart.getLatitude() + ";" + trafficStart.getLongitude() + " - SpeedStart: " + trafficStart.getSpeed() + " - LocationEnd: " + trafficEnd.getLatitude() + ";" + trafficEnd.getLongitude() + " - SpeedEnd: " + trafficEnd.getSpeed());
                            reportTraffic();
                        }
                        trafficStart = null;
                        trafficEnd = null;
                        trafficSecondsTimer = 0;
                    }
                    // Detect sudden speed changes
                    double acceleration = 0;
                    double deceleration = 0;
                    double a = (currentSpeed - lastSpeed) / 10;
                    double v = BigDecimal.valueOf(a).setScale(2, RoundingMode.HALF_UP).doubleValue();
                    if (a >= 2.5) {
                        // Report acceleration
                        mSystem.setTimestamp();
                        acceleration = v;
                        LocationHelper locationHelper = new LocationHelper(mUser.getUid(), mSystem.getTimestamp(), currentLocation.getLatitude(), currentLocation.getLongitude(), lastSpeed, currentSpeed, acceleration, deceleration, lastBearing, currentBearing, false);
                        uploadDriversBehaviourData(locationHelper);
                    } else if (a <= -2.5) {
                        mSystem.setTimestamp();
                        deceleration = v;
                        LocationHelper locationHelper = new LocationHelper(mUser.getUid(), mSystem.getTimestamp(), currentLocation.getLatitude(), currentLocation.getLongitude(), lastSpeed, currentSpeed, acceleration, deceleration, lastBearing, currentBearing, false);
                        uploadDriversBehaviourData(locationHelper);
                    }
                    // Detect sudden bearing changes while car is moving
                    if (currentSpeed > 20) {
                        if (currentBearing > lastBearing + 90 || currentBearing < lastBearing - 90) {
                            // Report acceleration
                            mSystem.setTimestamp();
                            LocationHelper locationHelper = new LocationHelper(mUser.getUid(), mSystem.getTimestamp(), currentLocation.getLatitude(), currentLocation.getLongitude(), lastSpeed, currentSpeed, acceleration, deceleration, lastBearing, currentBearing, false);
                            uploadDriversBehaviourData(locationHelper);
                        }
                    }
                    // Detect sudden small change of conditions using accelerator
                    if (accelerometerAvailable) {
                        acceleration = 0;
                        deceleration = 0;
                        if (Math.abs(mAccel) >= 15) {
                            if (currentSpeed > lastSpeed) {
                                // Report acceleration
                                mSystem.setTimestamp();
                                acceleration = BigDecimal.valueOf(currentSpeed - lastSpeed).setScale(2, RoundingMode.HALF_UP).doubleValue();
                                LocationHelper locationHelper = new LocationHelper(mUser.getUid(), mSystem.getTimestamp(), currentLocation.getLatitude(), currentLocation.getLongitude(), lastSpeed, currentSpeed, acceleration, deceleration, lastBearing, currentBearing, true);
                                uploadDriversBehaviourData(locationHelper);
                            } else if (currentSpeed < lastSpeed) {
                                // Report deceleration
                                mSystem.setTimestamp();
                                deceleration = BigDecimal.valueOf(lastSpeed - currentSpeed).setScale(2, RoundingMode.HALF_UP).doubleValue();
                                LocationHelper locationHelper = new LocationHelper(mUser.getUid(), mSystem.getTimestamp(), currentLocation.getLatitude(), currentLocation.getLongitude(), lastSpeed, currentSpeed, acceleration, deceleration, lastBearing, currentBearing, true);
                                uploadDriversBehaviourData(locationHelper);
                            }
                        }
                    }
                    // Calculate average speed
                    if (currentSpeed > 0)
                        speedList.add(currentSpeed);
                    for (int i = 0; i < speedList.size(); i++)
                        sumSpeed = speedList.get(i) + sumSpeed;
                    double averageSpeed = BigDecimal.valueOf(sumSpeed / speedList.size()).setScale(2, RoundingMode.HALF_UP).doubleValue();
                    sumSpeed = 0;
                    if (uploadData) {
                        // Update user average speed
                        mDatabaseReference.child("Users").child(mUser.getUid()).child("averageSpeed").setValue(averageSpeed)
                                .addOnCompleteListener(MapsActivity.this, new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            Log.i("FirebaseDatabase", "Update user's average speed on database succeed");
                                        } else {
                                            Toast.makeText(MapsActivity.this, getString(R.string.database_update_failed) + System.lineSeparator() + Objects.requireNonNull(task.getException()).getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                                            Log.w("FirebaseDatabase", "Update user's average speed on database failed", task.getException());
                                        }
                                    }
                                });
                    }
                }
            } catch (Exception e) {
                Log.e("Map", "Location listener exception", e);
            }
        }
    };
    private Location lastKnownLocation, currentLocation, bestLocation, trafficStart, trafficEnd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Full screen content
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        // Maps activity
        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        // Obtain the SupportMapFragment and get notified when the map is ready to be used
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment == null) {
            Log.e("Map", "MapFragment is null");
            Toast.makeText(MapsActivity.this, getString(R.string.failed), Toast.LENGTH_SHORT).show();
            return;
        }
        mapFragment.getMapAsync(this);
        // Initialization
        systemInit();
        firebaseInit();
        layoutInit();
        updateLocation();
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateLocation();
        if (accelerometerAvailable)
            sensorManager.registerListener(accelerometerSensorEventListener, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        locationManager.removeUpdates(locationListener);
        if (accelerometerAvailable)
            sensorManager.unregisterListener(accelerometerSensorEventListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateLocation();
        if (accelerometerAvailable)
            sensorManager.registerListener(accelerometerSensorEventListener, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onStop() {
        super.onStop();
        locationManager.removeUpdates(locationListener);
        if (accelerometerAvailable)
            sensorManager.unregisterListener(accelerometerSensorEventListener);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // Report traffic
        timerHandler.removeCallbacks(timerRunnable);
        trafficEnd = currentLocation;
        if (trafficSecondsTimer > 100 && trafficStart != null && trafficEnd != null) {
            Log.i("Traffic", "Traffic reported - Timer: " + trafficSecondsTimer + " - LocationStart: " + trafficStart.getLatitude() + ";" + trafficStart.getLongitude() + " - SpeedStart: " + trafficStart.getSpeed() + " - LocationEnd: " + trafficEnd.getLatitude() + ";" + trafficEnd.getLongitude() + " - SpeedEnd: " + trafficEnd.getSpeed());
            reportTraffic();
        }
        trafficStart = null;
        trafficEnd = null;
        trafficSecondsTimer = 0;
    }

    private void systemInit() {
        // Set SystemHelper
        mSystem = new SystemHelper(this);
        // Set LocationManager
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        // Set LocationProviders
        locationProviderCriteria = new Criteria();
        locationProviderCriteria.setAccuracy(Criteria.ACCURACY_FINE);
        locationProviderCriteria.setBearingAccuracy(Criteria.ACCURACY_HIGH);
        locationProviderCriteria.setHorizontalAccuracy(Criteria.ACCURACY_HIGH);
        locationProviderCriteria.setSpeedAccuracy(Criteria.ACCURACY_HIGH);
        locationProviderCriteria.setVerticalAccuracy(Criteria.ACCURACY_HIGH);
        locationProviderCriteria.setPowerRequirement(Criteria.POWER_LOW);
        locationProviderCriteria.setSpeedRequired(true);
        locationProviderCriteria.setAltitudeRequired(true);
        locationProviderCriteria.setBearingRequired(true);
        locationProviderCriteria.setCostAllowed(false);
        bestLocationProvider = locationManager.getBestProvider(locationProviderCriteria, true);
        locationProviders = locationManager.getProviders(true);
        Log.i("LocationProviders", "Best provider: " + bestLocationProvider);
        // Set SensorManager
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        // Set Accelerometer & acceleration values
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometerSensor != null) {
            Log.i("Sensors", "Acceleration sensor is available");
            accelerometerAvailable = true;
        } else {
            Log.w("Sensors", "Acceleration sensor is not available");
            Toast.makeText(MapsActivity.this, getString(R.string.no_accelerometer), Toast.LENGTH_SHORT).show();
            accelerometerAvailable = false;
        }
        mAccel = 0.00f;
        mAccelCurrent = SensorManager.GRAVITY_EARTH;
        mAccelLast = SensorManager.GRAVITY_EARTH;
        // Set traffic timer
        trafficSecondsTimer = 0;
        timerHandler = new Handler(Looper.getMainLooper());
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                trafficSecondsTimer++;
            }
        };
        // Set speeds
        HUMAN_SPEED_CONVERTER = 3.6;  // kmhSpeed = 3.6 * speed | mileSpeed = 2.23694 * speed
        currentSpeed = 0;
        lastSpeed = 0;
        currentBearing = 0;
        lastBearing = 0;
        speedList = new ArrayList<>();
        sumSpeed = 0;
        // Set SharedPreferences
        sharedPreferences = getSharedPreferences("DriveOnSharedPreferences", MODE_PRIVATE);
        sharedPreferencesEditor = sharedPreferences.edit();
    }

    private void firebaseInit() {
        // Firebase Authentication
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        // Firebase Realtime Database
        FirebaseDatabase mDatabase = FirebaseDatabase.getInstance("https://drive-on-mscsd-default-rtdb.europe-west1.firebasedatabase.app/");
        mDatabaseReference = mDatabase.getReference();
    }

    private void layoutInit() {
        try {
            // Status & Navigation bars height
            statusBarHeight = 0;
            navigationBarHeight = 0;
            ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, windowInsets) -> {
                Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                statusBarHeight = insets.top;
                navigationBarHeight = insets.bottom;
                return WindowInsetsCompat.CONSUMED;
            });
            // Set location extended FAB
            extendedFloatingActionButton = findViewById(R.id.mapExtendedFAB);
            // Margin for full screen
            ViewCompat.setOnApplyWindowInsetsListener(extendedFloatingActionButton, (v, windowInsets) -> {
                Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                mlp.topMargin = insets.top + 20;
                v.setLayoutParams(mlp);
                return WindowInsetsCompat.CONSUMED;
            });
            extendedFloatingActionButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    updateLocation();
                }
            });
            // Set acceleration progressbar
            accelerometerProgressBar = findViewById(R.id.accelerometerProgressBar);
            progressDrawable = accelerometerProgressBar.getProgressDrawable().mutate();
            // Set custom map settings FAB
            settingsExtendedFab = findViewById(R.id.settingsExtendedFab);
            // Margin for full screen
            ViewCompat.setOnApplyWindowInsetsListener(settingsExtendedFab, (v, windowInsets) -> {
                Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                mlp.bottomMargin = insets.bottom + 10;
                mlp.rightMargin = insets.right + 20;
                v.setLayoutParams(mlp);
                return WindowInsetsCompat.CONSUMED;
            });
            screenOnFab = findViewById(R.id.screenOnFab);
            uploadDataFab = findViewById(R.id.uploadDataFab);
            reportIncidentFab = findViewById(R.id.reportIncidentFab);
            screenOnActionText = findViewById(R.id.screenOnActionText);
            uploadDataActionText = findViewById(R.id.uploadDataActionText);
            reportIncidentActionText = findViewById(R.id.reportIncidentActionText);
            screenOnFab.setVisibility(View.GONE);
            uploadDataFab.setVisibility(View.GONE);
            reportIncidentFab.setVisibility(View.GONE);
            screenOnActionText.setVisibility(View.GONE);
            uploadDataActionText.setVisibility(View.GONE);
            reportIncidentActionText.setVisibility(View.GONE);
            visibleCustomFab = false;
            settingsExtendedFab.shrink();
            settingsExtendedFab.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (!visibleCustomFab) {
                                screenOnFab.show();
                                uploadDataFab.show();
                                reportIncidentFab.show();
                                screenOnActionText.setVisibility(View.VISIBLE);
                                uploadDataActionText.setVisibility(View.VISIBLE);
                                reportIncidentActionText.setVisibility(View.VISIBLE);
                                settingsExtendedFab.extend();
                                visibleCustomFab = true;
                            } else {
                                screenOnFab.hide();
                                uploadDataFab.hide();
                                reportIncidentFab.hide();
                                screenOnActionText.setVisibility(View.GONE);
                                uploadDataActionText.setVisibility(View.GONE);
                                reportIncidentActionText.setVisibility(View.GONE);
                                settingsExtendedFab.shrink();
                                visibleCustomFab = false;
                            }
                        }
                    });
            // Get theme's primary color
            TypedValue typedValue = new TypedValue();
            getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true);
            int primaryColor = typedValue.data;
            // Set screen on FAB and save settings
            screenOn = sharedPreferences.getBoolean("mainMapScreenOn", false);
            if (screenOn) {
                screenOnFab.setBackgroundTintList(ColorStateList.valueOf(primaryColor));
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            } else {
                screenOnFab.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
            screenOnFab.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (screenOnFab.getBackgroundTintList() == ColorStateList.valueOf(Color.GRAY)) {
                                // Keep screen on
                                screenOn = true;
                                screenOnFab.setBackgroundTintList(ColorStateList.valueOf(primaryColor));
                                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                            } else if (screenOnFab.getBackgroundTintList() == ColorStateList.valueOf(primaryColor)) {
                                // Keep screen off
                                screenOn = false;
                                screenOnFab.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
                                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                            }
                            sharedPreferencesEditor.putBoolean("mainMapScreenOn", screenOn);
                            sharedPreferencesEditor.apply();
                        }
                    });
            // Set upload data FAB and save settings
            uploadData = sharedPreferences.getBoolean("mainMapUploadDataOn", true);
            if (uploadData) {
                uploadDataFab.setBackgroundTintList(ColorStateList.valueOf(primaryColor));
            } else {
                uploadDataFab.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
            }
            uploadDataFab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (uploadDataFab.getBackgroundTintList() == ColorStateList.valueOf(Color.GRAY)) {
                        // Upload data on
                        uploadData = true;
                        uploadDataFab.setBackgroundTintList(ColorStateList.valueOf(primaryColor));
                    } else if (uploadDataFab.getBackgroundTintList() == ColorStateList.valueOf(primaryColor)) {
                        // Upload data off
                        uploadData = false;
                        uploadDataFab.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
                    }
                    sharedPreferencesEditor.putBoolean("mainMapUploadDataOn", uploadData);
                    sharedPreferencesEditor.apply();
                }
            });
            // Set report incident FAB
            reportIncidentFab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    updateLocation();
                    if (currentLocation != null) {
                        ReportIncidentDialog reportIncidentDialog = new ReportIncidentDialog(MapsActivity.this, currentLocation);
                        reportIncidentDialog.show();
                    } else if (lastKnownLocation != null) {
                        ReportIncidentDialog reportIncidentDialog = new ReportIncidentDialog(MapsActivity.this, lastKnownLocation);
                        reportIncidentDialog.show();
                    } else if (bestLocation != null) {
                        ReportIncidentDialog reportIncidentDialog = new ReportIncidentDialog(MapsActivity.this, bestLocation);
                        reportIncidentDialog.show();
                    } else {
                        Log.w("Location", "MapsActivity Location is null and Report incident dialog cannot be created");
                        Toast.makeText(MapsActivity.this, getString(R.string.no_location), Toast.LENGTH_SHORT).show();
                    }
                }
            });
            // Get my location default indicator >> True - Custom marker / False - Google
            useCustomMyLocationIndicator = sharedPreferences.getBoolean("useCustomMyLocationIndicator", true);
        } catch (Exception e) {
            Log.e("MapsLayout", "Maps layout exception", e);
            Toast.makeText(MapsActivity.this, getString(R.string.failed), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateLocation() {
        // After checking location permissions Update current location
        try {
            if (mSystem.getPermissions(LOCATION_PERMISSION_REQUEST_CODE)) {
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
                    return;
                }
                if (gMap != null && !useCustomMyLocationIndicator) {
                    Log.i("Map", "MyLocation enabled on map");
                    gMap.setMyLocationEnabled(true);
                }
                bestLocationProvider = locationManager.getBestProvider(locationProviderCriteria, true);
                locationProviders = locationManager.getProviders(true);
                Log.i("LocationProviders", "Best provider: " + bestLocationProvider);
                if (bestLocationProvider != null) {
                    locationManager.requestLocationUpdates(bestLocationProvider, MIN_MILLISECONDS_BETWEEN_GPS_UPDATES, MIN_METERS_FOR_GPS_UPDATES, locationListener);
                    lastKnownLocation = locationManager.getLastKnownLocation(bestLocationProvider);
                    if (lastKnownLocation != null) {
                        if (bestLocation == null || lastKnownLocation.getAccuracy() < bestLocation.getAccuracy())
                            bestLocation = lastKnownLocation;
                    }
                } else if (locationProviders != null) {
                    for (String provider : locationProviders) {
                        Log.i("LocationProviders", "Current provider: " + provider);
                        locationManager.requestLocationUpdates(provider, MIN_MILLISECONDS_BETWEEN_GPS_UPDATES, MIN_METERS_FOR_GPS_UPDATES, locationListener);
                        lastKnownLocation = locationManager.getLastKnownLocation(provider);
                        if (lastKnownLocation != null) {
                            if (bestLocation == null || lastKnownLocation.getAccuracy() < bestLocation.getAccuracy())
                                bestLocation = lastKnownLocation;
                        }
                    }
                } else {
                    Toast.makeText(MapsActivity.this, getString(R.string.no_gps_error), Toast.LENGTH_SHORT).show();
                    Log.w("LocationProviders", "No location providers found");
                }
            }
        } catch (Exception e) {
            Log.e("Permissions", "Update location - permissions exception", e);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Actions after requesting permissions
        try {
            switch (requestCode) {
                case LOCATION_PERMISSION_REQUEST_CODE:
                    // Location Permissions
                    if (grantResults.length > 0) {
                        boolean accessFineLocationPermission = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                        boolean accessCoarseLocationPermission = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                        if (accessFineLocationPermission && accessCoarseLocationPermission) {
                            // Location permission already given - Call LocationHelper - Open Map - Update Location
                            Log.i("Permissions", "Location permissions already granted");
                            // Call location listener
                            updateLocation();
                            // Animate map view on user's last known location
                            if (gMap != null) {
                                if (lastKnownLocation != null) {
                                    gMap.moveCamera(CameraUpdateFactory.newCameraPosition(
                                            new CameraPosition.Builder()
                                                    .target(new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()))
                                                    .zoom(20)
                                                    .bearing(lastKnownLocation.getBearing())
                                                    .tilt(80)
                                                    .build()
                                    ));
                                }
                            }
                        } else {
                            // Ask for location permission again - Map unavailable until location permission granted
                            Log.w("Permissions", "Location permissions not granted again");
                            mSystem.getPermissions(LOCATION_PERMISSION_REQUEST_CODE);
                        }
                    } else {
                        // No grantResults for location permissions
                        Log.w("Permissions", "Location permissions with empty grantResults");
                    }
                    break;
                case CALL_PERMISSION_REQUEST_CODE:
                    // Call Permissions
                    if (grantResults.length > 0) {
                        boolean callPhonePermission = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                        boolean callPrivilegedPermission = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                        if (callPhonePermission && callPrivilegedPermission) {
                            // Call permission already given - Make call
                            Log.i("Permissions", "Call permissions already granted");
                            // Call location listener
                            callHelp();
                        } else {
                            // Ask for call permission again - Call unavailable until call permission granted
                            Log.w("Permissions", "Call permissions not granted again");
                            mSystem.getPermissions(CALL_PERMISSION_REQUEST_CODE);
                        }
                    } else {
                        // No grantResults for call permissions
                        Log.w("Permissions", "Call permissions with empty grantResults");
                    }
                    break;
                case SMS_PERMISSION_REQUEST_CODE:
                    // SMS Permissions
                    if (grantResults.length > 0) {
                        boolean sendSMSPermission = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                        if (sendSMSPermission) {
                            // SMS permission already given - Send SmS
                            Log.i("Permissions", "SMS permissions already granted");
                            // Send SMS
                            sendSOSMessage();
                        } else {
                            // Ask for SMS permission again - SMS unavailable until SMS permission granted
                            Log.w("Permissions", "SMS permissions not granted again");
                            mSystem.getPermissions(SMS_PERMISSION_REQUEST_CODE);
                        }
                    } else {
                        // No grantResults for SMS permissions
                        Log.w("Permissions", "SMS permissions with empty grantResults");
                    }
                    break;
                default:
                    Log.w("Permissions", "Unknown permission request code");
            }
        } catch (Exception e) {
            // If request is cancelled the result arrays are empty
            Log.e("Permissions", "Permissions exception", e);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        gMap = googleMap;
        try {
            gMap.setPadding(0, statusBarHeight, 0, navigationBarHeight);
            boolean success = gMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style));
            if (!success) {
                Log.e("Map", "Style parsing failed");
            }
            gMap.setTrafficEnabled(true);
            gMap.setBuildingsEnabled(true);
        } catch (Exception e) {
            Log.e("Map", "Can't find map style", e);
        }
        // Check user permissions & Get user's last known location
        if (mSystem.getPermissions(LOCATION_PERMISSION_REQUEST_CODE)) {
            Log.i("Location", "Getting current & last known locations since location permissions granted");
            try {
                Snackbar.make(binding.getRoot(), getString(R.string.my_location_indicator_message), Snackbar.LENGTH_SHORT)
                        .setAction(getString(R.string.change), new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Change my location indicator & Reload activity
                                useCustomMyLocationIndicator = !useCustomMyLocationIndicator;
                                sharedPreferencesEditor.putBoolean("useCustomMyLocationIndicator", useCustomMyLocationIndicator);
                                sharedPreferencesEditor.apply();
                                Toast.makeText(MapsActivity.this, getString(R.string.refreshing_content), Toast.LENGTH_SHORT).show();
                                finish();
                                overridePendingTransition(0, 0);
                                startActivity(getIntent());
                                overridePendingTransition(0, 0);
                            }
                        }).show();
                updateLocation();
                if (lastKnownLocation != null) {
                    // Animate map view on user's last known location
                    gMap.moveCamera(CameraUpdateFactory.newCameraPosition(
                            new CameraPosition.Builder()
                                    .target(new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()))
                                    .zoom(20)
                                    .bearing(lastKnownLocation.getBearing())
                                    .tilt(80)
                                    .build()
                    ));
                }
            } catch (Exception e) {
                Log.e("Map", "Map exception", e);
            }
        }
    }

    private void uploadDriversBehaviourData(LocationHelper locationHelper) {
        Log.i("DriversBehaviourData", locationHelper.toString());
        if (uploadData) {
            // Write location data of the current user to the database
            try {
                mDatabaseReference.child("Locations").child(mUser.getUid()).child(String.valueOf(locationHelper.getTimestamp())).setValue(locationHelper).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.i("FirebaseDatabase", "Uploading location data succeed");
                        } else {
                            Toast.makeText(MapsActivity.this, getString(R.string.database_update_failed) + System.lineSeparator() + Objects.requireNonNull(task.getException()).getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                            Log.w("FirebaseDatabase", "Uploading location data failed", task.getException());
                        }
                    }
                });
                if (locationHelper.getAcceleration() != 0) {
                    // Update acceleration data
                    mDatabaseReference.child("Users").child(mUser.getUid()).child("accelerations").setValue(ServerValue.increment(1))
                            .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        Log.i("FirebaseDatabase", "Update user's acceleration data on database succeed");
                                    } else {
                                        Toast.makeText(MapsActivity.this, getString(R.string.database_update_failed) + System.lineSeparator() + Objects.requireNonNull(task.getException()).getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                                        Log.w("FirebaseDatabase", "Update user's acceleration data on database failed", task.getException());
                                    }
                                }
                            });
                } else if (locationHelper.getDeceleration() != 0) {
                    // Update deceleration data
                    mDatabaseReference.child("Users").child(mUser.getUid()).child("decelerations").setValue(ServerValue.increment(1))
                            .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        Log.i("FirebaseDatabase", "Update user's deceleration data on database succeed");
                                    } else {
                                        Toast.makeText(MapsActivity.this, getString(R.string.database_update_failed) + System.lineSeparator() + Objects.requireNonNull(task.getException()).getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                                        Log.w("FirebaseDatabase", "Update user's deceleration data on database failed", task.getException());
                                    }
                                }
                            });
                }
            } catch (Exception e) {
                Log.e("FirebaseDatabase", "Update user data exception", e);
            }
        }
    }

    private void reportTraffic() {
        Log.i("Traffic", "Traffic detected - LocationStart: " + trafficStart.getLatitude() + ";" + trafficStart.getLongitude() + " - SpeedStart: " + trafficStart.getSpeed() + " - LocationEnd: " + trafficEnd.getLatitude() + ";" + trafficEnd.getLongitude() + " - SpeedEnd: " + trafficEnd.getSpeed());
        if (uploadData) {
            // Write location data of the current user to the database
            mSystem.setTimestamp();
            TrafficHelper trafficHelper = new TrafficHelper(mUser.getUid(), mSystem.getTimestamp(), trafficStart, trafficEnd);
            try {
                mDatabaseReference.child("Traffic").child(String.valueOf(trafficHelper.getTimestamp())).setValue(trafficHelper).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.i("FirebaseDatabase", "Uploading traffic data succeed");
                        } else {
                            Toast.makeText(MapsActivity.this, getString(R.string.database_update_failed) + System.lineSeparator() + Objects.requireNonNull(task.getException()).getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                            Log.w("FirebaseDatabase", "Uploading traffic data failed", task.getException());
                        }
                    }
                });
            } catch (Exception e) {
                Log.e("FirebaseDatabase", "Update user data exception", e);
            }
        }
    }

    private void animateMarker(final Marker marker, final Location location) {
        // Map marker animation helper
        final Handler handler = new Handler(Looper.getMainLooper());
        final long start = SystemClock.uptimeMillis();
        final LatLng startLatLng = marker.getPosition();
        final double startRotation = marker.getRotation();
        final long duration = 500;
        final Interpolator interpolator = new LinearInterpolator();
        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed / duration);
                double lng = t * location.getLongitude() + (1 - t) * startLatLng.longitude;
                double lat = t * location.getLatitude() + (1 - t) * startLatLng.latitude;
                float rotation = (float) (t * location.getBearing() + (1 - t) * startRotation);
                marker.setPosition(new LatLng(lat, lng));
                marker.setRotation(rotation);
                if (t < 1.0) {
                    handler.postDelayed(this, 16);
                }
            }
        });
    }

    private void sosDialog() {
        LinearLayout linearLayout = new LinearLayout(MapsActivity.this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        ProgressBar progressBar = new ProgressBar(MapsActivity.this,null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(600);
        progressBar.setMin(0);
        progressBar.setProgress(600);
        progressBar.setKeepScreenOn(true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        progressBar.setLayoutParams(lp);
        linearLayout.addView(progressBar);
        TextView textView = new TextView(MapsActivity.this);
        textView.setText(getString(R.string.sos_dialog_message));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(50, 50, 50, 50);
        textView.setLayoutParams(layoutParams);
        linearLayout.addView(textView);
        AlertDialog dialog = new AlertDialog.Builder(MapsActivity.this)
                .setIcon(android.R.drawable.stat_sys_warning)
                .setTitle(getString(R.string.sos_dialog_title))
                .setView(linearLayout)
                .setPositiveButton(getString(R.string.sos_dialog_ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(getString(R.string.sos_dialog_get_help), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        sendSOSMessage();
                    }
                })
                .create();
        Handler dialogHandler = new Handler(Looper.myLooper());
        Runnable dialogRunnable = new Runnable() {
            public void run() {
                if (progressBar.getProgress() > 0)
                    progressBar.incrementProgressBy(-1);
                else {
                    dialog.dismiss();
                    sendSOSMessage();
                }
                dialogHandler.postDelayed(this, 100);
            }
        };
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                updateLocation();
                dialogHandler.removeCallbacks(dialogRunnable);
            }
        });
        dialog.show();
        locationManager.removeUpdates(locationListener);
        dialogHandler.postDelayed(dialogRunnable, 600);
    }

    private void sendSOSMessage() {
        callHelp();
        messageHelp();
    }

    private void callHelp() {
        // After checking call permissions Call emergency services
        try {
            if (mSystem.getPermissions(CALL_PERMISSION_REQUEST_CODE)) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PRIVILEGED) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.CALL_PHONE, Manifest.permission.CALL_PRIVILEGED}, CALL_PERMISSION_REQUEST_CODE);
                    return;
                }
                Uri callUri = Uri.parse("tel://112");
                Intent callIntent = new Intent(Intent.ACTION_CALL, callUri);
                callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
                startActivity(callIntent);
                Log.i("SOS", "SOS call made");
            }
        } catch (Exception e) {
            Log.e("Permissions", "Make call - permissions exception", e);
        }
    }

    private void messageHelp() {
        // Send alternative message
        try {
            Intent email = new Intent(Intent.ACTION_SEND);
            email.putExtra(Intent.EXTRA_EMAIL, new String[]{"support@projectjet.gr"});
            email.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.sos_need_help));
            email.putExtra(Intent.EXTRA_TEXT, getString(R.string.sos_message));
            email.setType("message/rfc822");
            startActivity(Intent.createChooser(email, getString(R.string.sos_dialog_get_help)));
            Log.i("SOS", "SOS message sent");
        } catch (Exception e) {
            Log.e("Permissions", "Send message exception", e);
        }
        // After checking sms permissions Send SOS SMS
        /*try {
            if (mSystem.getPermissions(SMS_PERMISSION_REQUEST_CODE)) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_REQUEST_CODE);
                    return;
                }
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage("+300000000000", null, getString(R.string.sos_message), null, null);
                Log.i("SOS", "SOS SMS message sent");
            }
        } catch (Exception e) {
            Log.e("Permissions", "Send SMS - permissions exception", e);
        }*/
    }
}