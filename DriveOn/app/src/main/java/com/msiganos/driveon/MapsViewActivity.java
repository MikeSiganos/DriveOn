package com.msiganos.driveon;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.util.Pair;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.collections.MarkerManager;
import com.msiganos.driveon.databinding.ActivityMapsViewBinding;
import com.msiganos.driveon.helpers.IncidentHelper;
import com.msiganos.driveon.helpers.LocationHelper;
import com.msiganos.driveon.helpers.MarkerClusterInfoWindowAdapter;
import com.msiganos.driveon.helpers.MarkerClusterItemHelper;
import com.msiganos.driveon.helpers.MarkerClusterRenderer;
import com.msiganos.driveon.helpers.SystemHelper;
import com.msiganos.driveon.helpers.TrafficHelper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class MapsViewActivity extends FragmentActivity implements OnMapReadyCallback {

    private static GoogleMap gMap;
    private ActivityMapsViewBinding binding;
    private DatabaseReference mDatabaseReference;
    private LocationManager locationManager;
    private ExtendedFloatingActionButton extendedFloatingActionButton, settingsMapViewExtendedFab;
    private FloatingActionButton incidentsFab, trafficFab, accelerationsFab, decelerationsFab;
    private TextView incidentsActionText, trafficActionText, accelerationsActionText, decelerationsActionText;
    private int statusBarHeight, navigationBarHeight;
    private boolean visibleCustomFab, showIncidentsFab, showTrafficFab, showAccelerationsFab, showDecelerationsFab;
    private DateFormat timestampFormat;
    private DateTimeFormatter dateTimeFormatter, dateFormatter;
    private Date fromDate, toDate;
    private ClusterManager<MarkerClusterItemHelper> incidentsAccidentsClusterManager, incidentsClosedRoadsClusterManager, incidentsTrafficClusterManager, incidentsOtherClusterManager, trafficStartClusterManager, trafficEndClusterManager, accelerationsClusterManager, decelerationsClusterManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Full screen content
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        // MapsView activity
        binding = ActivityMapsViewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        // Obtain the SupportMapFragment and get notified when the map is ready to be used
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapView);
        if (mapFragment == null) {
            Log.e("MapView", "MapFragment is null");
            Toast.makeText(MapsViewActivity.this, getString(R.string.failed), Toast.LENGTH_SHORT).show();
            return;
        }
        mapFragment.getMapAsync(this);
        // Initialization
        systemInit();
        firebaseInit();
        layoutInit();
    }

    @SuppressWarnings("SpellCheckingInspection")
    private void systemInit() {
        // Set SystemHelper
        SystemHelper mSystem = new SystemHelper(this);
        // Get network condition
        mSystem.getNetworkConnection();
        // Set LocationManager
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        String stampFormat = "yyyyMMddHHmmss";
        timestampFormat = new SimpleDateFormat(stampFormat, Locale.getDefault());
        dateTimeFormatter = DateTimeFormatter.ofPattern(stampFormat, Locale.getDefault());
        dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault());
        // Date filter
        fromDate = new Date();
        toDate = fromDate;
    }

    private void firebaseInit() {
        // Firebase Authentication
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser mUser = mAuth.getCurrentUser();
        if (mUser != null)
            Log.i("Firebase", "MapsViewActivity Firebase user: " + mUser.getDisplayName());
        else
            Log.w("Firebase", "MapsViewActivity Unknown Firebase user");
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
            extendedFloatingActionButton = findViewById(R.id.mapViewExtendedFAB);
            // Margin for full screen
            ViewCompat.setOnApplyWindowInsetsListener(extendedFloatingActionButton, (v, windowInsets) -> {
                Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                mlp.topMargin = insets.top + 20;
                v.setLayoutParams(mlp);
                return WindowInsetsCompat.CONSUMED;
            });
            DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            String dateRange = dateFormat.format(fromDate) + "-" + dateFormat.format(toDate);
            extendedFloatingActionButton.setText(dateRange);
            extendedFloatingActionButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    pickDates();
                }
            });
            // Set custom map view settings FAB
            settingsMapViewExtendedFab = findViewById(R.id.settingsMapViewExtendedFab);
            // Margin for full screen
            ViewCompat.setOnApplyWindowInsetsListener(settingsMapViewExtendedFab, (v, windowInsets) -> {
                Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                mlp.bottomMargin = insets.bottom + 10;
                mlp.rightMargin = insets.right + 20;
                v.setLayoutParams(mlp);
                return WindowInsetsCompat.CONSUMED;
            });
            incidentsFab = findViewById(R.id.incidentsFab);
            trafficFab = findViewById(R.id.trafficFab);
            accelerationsFab = findViewById(R.id.accelerationsFab);
            decelerationsFab = findViewById(R.id.decelerationsFab);
            incidentsActionText = findViewById(R.id.incidentsActionText);
            trafficActionText = findViewById(R.id.trafficActionText);
            accelerationsActionText = findViewById(R.id.accelerationsActionText);
            decelerationsActionText = findViewById(R.id.decelerationsActionText);
            incidentsFab.setVisibility(View.GONE);
            trafficFab.setVisibility(View.GONE);
            accelerationsFab.setVisibility(View.GONE);
            decelerationsFab.setVisibility(View.GONE);
            incidentsActionText.setVisibility(View.GONE);
            trafficActionText.setVisibility(View.GONE);
            accelerationsActionText.setVisibility(View.GONE);
            decelerationsActionText.setVisibility(View.GONE);
            visibleCustomFab = false;
            settingsMapViewExtendedFab.shrink();
            settingsMapViewExtendedFab.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (!visibleCustomFab) {
                                incidentsFab.show();
                                trafficFab.show();
                                accelerationsFab.show();
                                decelerationsFab.show();
                                incidentsActionText.setVisibility(View.VISIBLE);
                                trafficActionText.setVisibility(View.VISIBLE);
                                accelerationsActionText.setVisibility(View.VISIBLE);
                                decelerationsActionText.setVisibility(View.VISIBLE);
                                settingsMapViewExtendedFab.extend();
                                visibleCustomFab = true;
                            } else {
                                incidentsFab.hide();
                                trafficFab.hide();
                                accelerationsFab.hide();
                                decelerationsFab.hide();
                                incidentsActionText.setVisibility(View.GONE);
                                trafficActionText.setVisibility(View.GONE);
                                accelerationsActionText.setVisibility(View.GONE);
                                decelerationsActionText.setVisibility(View.GONE);
                                settingsMapViewExtendedFab.shrink();
                                visibleCustomFab = false;
                            }
                        }
                    });
            // Get theme's primary color
            TypedValue typedValue = new TypedValue();
            getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true);
            int primaryColor = typedValue.data;
            // Set incidents FAB
            incidentsFab.setBackgroundTintList(ColorStateList.valueOf(primaryColor));
            incidentsFab.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (incidentsFab.getBackgroundTintList() == ColorStateList.valueOf(Color.GRAY)) {
                                // Show incidents data
                                incidentsFab.setBackgroundTintList(ColorStateList.valueOf(primaryColor));
                                showIncidents();
                                showIncidentsFab = true;
                            } else if (incidentsFab.getBackgroundTintList() == ColorStateList.valueOf(primaryColor)) {
                                // Hide incidents data
                                incidentsFab.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
                                hideIncidents();
                                showIncidentsFab = false;
                            }
                        }
                    });
            // Set traffic FAB
            trafficFab.setBackgroundTintList(ColorStateList.valueOf(primaryColor));
            trafficFab.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (trafficFab.getBackgroundTintList() == ColorStateList.valueOf(Color.GRAY)) {
                                // Show traffic data
                                trafficFab.setBackgroundTintList(ColorStateList.valueOf(primaryColor));
                                showTraffic();
                                showTrafficFab = true;
                            } else if (trafficFab.getBackgroundTintList() == ColorStateList.valueOf(primaryColor)) {
                                // Hide traffic data
                                trafficFab.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
                                hideTraffic();
                                showTrafficFab = false;
                            }
                        }
                    });
            // Set accelerations FAB
            accelerationsFab.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
            accelerationsFab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (accelerationsFab.getBackgroundTintList() == ColorStateList.valueOf(Color.GRAY)) {
                        // Show accelerations data
                        accelerationsFab.setBackgroundTintList(ColorStateList.valueOf(primaryColor));
                        showAccelerations();
                        showAccelerationsFab = true;
                    } else if (accelerationsFab.getBackgroundTintList() == ColorStateList.valueOf(primaryColor)) {
                        // Hide accelerations data
                        accelerationsFab.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
                        hideAccelerations();
                        showAccelerationsFab = false;
                    }
                }
            });
            // Set decelerations FAB
            decelerationsFab.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
            decelerationsFab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (decelerationsFab.getBackgroundTintList() == ColorStateList.valueOf(Color.GRAY)) {
                        // Show decelerations data
                        decelerationsFab.setBackgroundTintList(ColorStateList.valueOf(primaryColor));
                        showDecelerations();
                        showDecelerationsFab = true;
                    } else if (decelerationsFab.getBackgroundTintList() == ColorStateList.valueOf(primaryColor)) {
                        // Hide decelerations data
                        decelerationsFab.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
                        hideDecelerations();
                        showDecelerationsFab = false;
                    }
                }
            });
            // Show incidents
            showIncidentsFab = true;
            // Show traffic
            showTrafficFab = true;
            // Hide accelerations
            showAccelerationsFab = false;
            // Hide decelerations
            showDecelerationsFab = false;
        } catch (Exception e) {
            Log.e("MapsViewLayout", "Maps view layout exception", e);
            Toast.makeText(MapsViewActivity.this, getString(R.string.failed), Toast.LENGTH_SHORT).show();
        }
    }

    private void pickDates() {
        MaterialDatePicker.Builder<Pair<Long, Long>> materialDateBuilder = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText(getString(R.string.extended_fab_date_label))
                .setTheme(R.style.CorrectMaterialCalendar)
                .setSelection(
                        new Pair<>(MaterialDatePicker.thisMonthInUtcMilliseconds(), MaterialDatePicker.todayInUtcMilliseconds())
                );
        MaterialDatePicker<Pair<Long, Long>> materialDatePicker = materialDateBuilder.build();
        materialDatePicker.addOnPositiveButtonClickListener(
                new MaterialPickerOnPositiveButtonClickListener<Pair<Long, Long>>() {
                    @Override
                    public void onPositiveButtonClick(Pair<Long, Long> selection) {
                        fromDate = new Date(selection.first);
                        toDate = new Date(selection.second + 86400000);  // Plus one day to fix search issue
                        DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                        String dateRange = dateFormat.format(fromDate) + "-" + dateFormat.format(new Date(selection.second));  // Show correct selected date range
                        extendedFloatingActionButton.setText(dateRange);
                        if (showIncidentsFab)
                            showIncidents();
                        else
                            hideIncidents();
                        if (showTrafficFab)
                            showTraffic();
                        else
                            hideTraffic();
                        if (showAccelerationsFab)
                            showAccelerations();
                        else
                            hideAccelerations();
                        if (showDecelerationsFab)
                            showDecelerations();
                        else
                            hideDecelerations();
                    }
                });
        materialDatePicker.addOnNegativeButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
        materialDatePicker.addOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {

            }
        });
        materialDatePicker.addOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {

            }
        });
        materialDatePicker.show(getSupportFragmentManager(), materialDatePicker.toString());
    }

    private void showIncidents() {
        try {
            // Date filter
            String fromTimestamp = timestampFormat.format(fromDate);
            String toTimestamp = timestampFormat.format(toDate);
            // Accidents
            Query accidentsQuery = mDatabaseReference.child("Reports").child("Accidents").orderByChild("timestamp").startAt(fromTimestamp).endAt(toTimestamp);
            accidentsQuery.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    // Reset accidents data
                    incidentsAccidentsClusterManager.clearItems();
                    // Cluster counter result
                    int counter = 0;
                    // Read location records
                    for (DataSnapshot reportsDataSnapshot : dataSnapshot.getChildren()) {
                        IncidentHelper incidentHelper = reportsDataSnapshot.getValue(IncidentHelper.class);
                        if (incidentHelper != null) {
                            // Found data
                            LocalDate date = LocalDate.parse(incidentHelper.getTimestamp(), dateTimeFormatter);
                            if (gMap != null) {
                                LatLng latLng = new LatLng(incidentHelper.getLatitude(), incidentHelper.getLongitude());
                                BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW);
                                String title = getString(R.string.accident) + " (" + dateFormatter.format(date) + ")";
                                String snippet = incidentHelper.getIncident() + System.lineSeparator() + getString(R.string.user_id) + ": " + incidentHelper.getUid();
                                MarkerClusterItemHelper incidentsAccidentsClusterItem = new MarkerClusterItemHelper(latLng, bitmapDescriptor, title, snippet, Color.YELLOW, incidentHelper);
                                incidentsAccidentsClusterManager.addItem(incidentsAccidentsClusterItem);
                                counter++;
                            }
                        } else {
                            // No data found
                            Log.w("FirebaseDatabase", "No location data found");
                        }
                    }
                    incidentsAccidentsClusterManager.cluster();
                    refreshMap();
                    Log.i("Clusters", "Incidents accidents clusters loaded (" + counter + ")");
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(getApplicationContext(), getString(R.string.failed) + System.lineSeparator() + error.toException().getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }
            });
            // ClosedRoads
            Query closedRoadsQuery = mDatabaseReference.child("Reports").child("ClosedRoads").orderByChild("timestamp").startAt(fromTimestamp).endAt(toTimestamp);
            closedRoadsQuery.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    // Reset closedRoads data
                    incidentsClosedRoadsClusterManager.clearItems();
                    // Cluster counter result
                    int counter = 0;
                    // Read location records
                    for (DataSnapshot reportsDataSnapshot : dataSnapshot.getChildren()) {
                        IncidentHelper incidentHelper = reportsDataSnapshot.getValue(IncidentHelper.class);
                        if (incidentHelper != null) {
                            // Found data
                            LocalDate date = LocalDate.parse(incidentHelper.getTimestamp(), dateTimeFormatter);
                            if (gMap != null) {
                                LatLng latLng = new LatLng(incidentHelper.getLatitude(), incidentHelper.getLongitude());
                                BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED);
                                String title = getString(R.string.closed_road) + " (" + dateFormatter.format(date) + ")";
                                String snippet = incidentHelper.getIncident() + System.lineSeparator() + getString(R.string.user_id) + ": " + incidentHelper.getUid();
                                MarkerClusterItemHelper incidentsClosedRoadsClusterItem = new MarkerClusterItemHelper(latLng, bitmapDescriptor, title, snippet, Color.RED, incidentHelper);
                                incidentsClosedRoadsClusterManager.addItem(incidentsClosedRoadsClusterItem);
                                counter++;
                            }
                        } else {
                            // No data found
                            Log.w("FirebaseDatabase", "No location data found");
                        }
                    }
                    incidentsClosedRoadsClusterManager.cluster();
                    refreshMap();
                    Log.i("Clusters", "Incidents closed roads clusters loaded (" + counter + ")");
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(getApplicationContext(), getString(R.string.failed) + System.lineSeparator() + error.toException().getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }
            });
            // Traffic
            Query trafficQuery = mDatabaseReference.child("Reports").child("Traffic").orderByChild("timestamp").startAt(fromTimestamp).endAt(toTimestamp);
            trafficQuery.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    // Reset traffic data
                    incidentsTrafficClusterManager.clearItems();
                    // Cluster counter result
                    int counter = 0;
                    // Read location records
                    for (DataSnapshot reportsDataSnapshot : dataSnapshot.getChildren()) {
                        IncidentHelper incidentHelper = reportsDataSnapshot.getValue(IncidentHelper.class);
                        if (incidentHelper != null) {
                            // Found data
                            LocalDate date = LocalDate.parse(incidentHelper.getTimestamp(), dateTimeFormatter);
                            if (gMap != null) {
                                LatLng latLng = new LatLng(incidentHelper.getLatitude(), incidentHelper.getLongitude());
                                BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN);
                                String title = getString(R.string.traffic) + " (" + dateFormatter.format(date) + ")";
                                String snippet = incidentHelper.getIncident() + System.lineSeparator() + getString(R.string.user_id) + ": " + incidentHelper.getUid();
                                MarkerClusterItemHelper incidentsTrafficClusterItem = new MarkerClusterItemHelper(latLng, bitmapDescriptor, title, snippet, Color.GREEN, incidentHelper);
                                incidentsTrafficClusterManager.addItem(incidentsTrafficClusterItem);
                                counter++;
                            }
                        } else {
                            // No data found
                            Log.w("FirebaseDatabase", "No location data found");
                        }
                    }
                    incidentsTrafficClusterManager.cluster();
                    refreshMap();
                    Log.i("Clusters", "Incidents traffic clusters loaded (" + counter + ")");
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(getApplicationContext(), getString(R.string.failed) + System.lineSeparator() + error.toException().getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }
            });
            // Other
            Query otherQuery = mDatabaseReference.child("Reports").child("Other").orderByChild("timestamp").startAt(fromTimestamp).endAt(toTimestamp);
            otherQuery.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    // Reset other incident data
                    incidentsOtherClusterManager.clearItems();
                    // Cluster counter result
                    int counter = 0;
                    // Read location records
                    for (DataSnapshot reportsDataSnapshot : dataSnapshot.getChildren()) {
                        IncidentHelper incidentHelper = reportsDataSnapshot.getValue(IncidentHelper.class);
                        if (incidentHelper != null) {
                            // Found data
                            LocalDate date = LocalDate.parse(incidentHelper.getTimestamp(), dateTimeFormatter);
                            if (gMap != null) {
                                LatLng latLng = new LatLng(incidentHelper.getLatitude(), incidentHelper.getLongitude());
                                BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE);
                                String title = getString(R.string.other) + " (" + dateFormatter.format(date) + ")";
                                String snippet = incidentHelper.getIncident() + System.lineSeparator() + getString(R.string.user_id) + ": " + incidentHelper.getUid();
                                MarkerClusterItemHelper incidentsOtherClusterItem = new MarkerClusterItemHelper(latLng, bitmapDescriptor, title, snippet, Color.BLUE, incidentHelper);
                                incidentsOtherClusterManager.addItem(incidentsOtherClusterItem);
                                counter++;
                            }
                        } else {
                            // No data found
                            Log.w("FirebaseDatabase", "No location data found");
                        }
                    }
                    incidentsOtherClusterManager.cluster();
                    refreshMap();
                    Log.i("Clusters", "Incidents other clusters loaded (" + counter + ")");
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(getApplicationContext(), getString(R.string.failed) + System.lineSeparator() + error.toException().getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            Log.e("MapsViewData", "Map data load exception", e);
            Toast.makeText(MapsViewActivity.this, getString(R.string.failed), Toast.LENGTH_SHORT).show();
        }
    }

    private void hideIncidents() {
        // Hide incidents
        incidentsAccidentsClusterManager.clearItems();
        incidentsClosedRoadsClusterManager.clearItems();
        incidentsTrafficClusterManager.clearItems();
        incidentsOtherClusterManager.clearItems();
        incidentsAccidentsClusterManager.cluster();
        incidentsClosedRoadsClusterManager.cluster();
        incidentsTrafficClusterManager.cluster();
        incidentsOtherClusterManager.cluster();
        refreshMap();
    }

    private void showTraffic() {
        try {
            // Date filter
            String fromTimestamp = timestampFormat.format(fromDate);
            String toTimestamp = timestampFormat.format(toDate);
            // Get traffic data
            Query trafficQuery = mDatabaseReference.child("Traffic").orderByChild("timestamp").startAt(fromTimestamp).endAt(toTimestamp);
            trafficQuery.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    // Reset traffic data
                    trafficStartClusterManager.clearItems();
                    trafficEndClusterManager.clearItems();
                    // Cluster counter result
                    int counter = 0;
                    // Read location records
                    for (DataSnapshot trafficDataSnapshot : dataSnapshot.getChildren()) {
                        TrafficHelper trafficHelper = trafficDataSnapshot.getValue(TrafficHelper.class);
                        if (trafficHelper != null) {
                            // Found data
                            LocalDate date = LocalDate.parse(trafficHelper.getTimestamp(), dateTimeFormatter);
                            if (gMap != null) {
                                LatLng latLngStart = new LatLng(trafficHelper.getLatitudeStart(), trafficHelper.getLongitudeStart());
                                LatLng latLngEnd = new LatLng(trafficHelper.getLatitudeEnd(), trafficHelper.getLongitudeEnd());
                                BitmapDescriptor bitmapStartDescriptor = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED);
                                BitmapDescriptor bitmapEndDescriptor = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN);
                                String titleStart = getString(R.string.traffic_start) + " (" + dateFormatter.format(date) + ")";
                                String snippetStart = getString(R.string.traffic_start) + ": " + BigDecimal.valueOf(trafficHelper.getSpeedStart()).setScale(2, RoundingMode.HALF_UP).doubleValue() + "KM/H" + System.lineSeparator() + getString(R.string.user_id) + ": " + trafficHelper.getUid();
                                String titleEnd = getString(R.string.traffic_end) + " (" + dateFormatter.format(date) + ")";
                                String snippetEnd = getString(R.string.traffic_end) + ": " + BigDecimal.valueOf(trafficHelper.getSpeedEnd()).setScale(2, RoundingMode.HALF_UP).doubleValue() + "KM/H" + System.lineSeparator() + getString(R.string.user_id) + ": " + trafficHelper.getUid();
                                MarkerClusterItemHelper trafficStartClusterItem = new MarkerClusterItemHelper(latLngStart, bitmapStartDescriptor, titleStart, snippetStart, Color.RED, trafficHelper);
                                trafficStartClusterManager.addItem(trafficStartClusterItem);
                                MarkerClusterItemHelper trafficEndClusterItem = new MarkerClusterItemHelper(latLngEnd, bitmapEndDescriptor, titleEnd, snippetEnd, Color.GREEN, trafficHelper);
                                trafficEndClusterManager.addItem(trafficEndClusterItem);
                                counter++;
                            }
                        } else {
                            // No data found
                            Log.w("FirebaseDatabase", "No location data found");
                        }
                    }
                    trafficStartClusterManager.cluster();
                    trafficEndClusterManager.cluster();
                    refreshMap();
                    Log.i("Clusters", "Traffic clusters loaded (" + counter + ")");
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(getApplicationContext(), getString(R.string.failed) + System.lineSeparator() + error.toException().getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            Log.e("MapsViewData", "Map data load exception", e);
            Toast.makeText(MapsViewActivity.this, getString(R.string.failed), Toast.LENGTH_SHORT).show();
        }
    }

    private void hideTraffic() {
        // Hide traffic
        trafficStartClusterManager.clearItems();
        trafficEndClusterManager.clearItems();
        trafficStartClusterManager.cluster();
        trafficEndClusterManager.cluster();
        refreshMap();
    }

    @SuppressWarnings("unchecked")
    private void showAccelerations() {
        try {
            // Date filter
            String fromTimestamp = timestampFormat.format(fromDate);
            String toTimestamp = timestampFormat.format(toDate);
            // Get accelerations data
            Query accelerationsQuery = mDatabaseReference.child("Locations");
            accelerationsQuery.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    // Reset accelerations data
                    accelerationsClusterManager.clearItems();
                    // Cluster counter result
                    int counter = 0;
                    // Read location records
                    for (DataSnapshot accelerationDataSnapshot : dataSnapshot.getChildren()) {
                        Map<String, LocationHelper> map = (Map<String, LocationHelper>) accelerationDataSnapshot.getValue();
                        if (map != null) {
                            for (Map.Entry<String, LocationHelper> entry : map.entrySet()) {
                                String key_timestamp = entry.getKey();
                                if (Long.parseLong(key_timestamp) >= Long.parseLong(fromTimestamp) && Long.parseLong(key_timestamp) <= Long.parseLong(toTimestamp)) {
                                    Gson gson = new Gson();
                                    JsonElement jsonElement = gson.toJsonTree(entry.getValue());
                                    LocationHelper locationHelper = gson.fromJson(jsonElement, LocationHelper.class);
                                    if (locationHelper != null) {
                                        // Found data
                                        if (locationHelper.getAcceleration() != 0) {
                                            LocalDate date = LocalDate.parse(locationHelper.getTimestamp(), dateTimeFormatter);
                                            if (gMap != null) {
                                                LatLng latLng = new LatLng(locationHelper.getLatitude(), locationHelper.getLongitude());
                                                BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN);
                                                String title = getString(R.string.accelerations) + " (" + dateFormatter.format(date) + ")";
                                                String snippet = getString(R.string.accelerations) + ": " + BigDecimal.valueOf(locationHelper.getLastSpeed()).setScale(2, RoundingMode.HALF_UP).doubleValue() + "KM/H --> " + BigDecimal.valueOf(locationHelper.getSpeed()).setScale(2, RoundingMode.HALF_UP).doubleValue() + "KM/H" + System.lineSeparator() + getString(R.string.user_id) + ": " + locationHelper.getUid();
                                                MarkerClusterItemHelper accelerationClusterItem = new MarkerClusterItemHelper(latLng, bitmapDescriptor, title, snippet, Color.GREEN, locationHelper);
                                                accelerationsClusterManager.addItem(accelerationClusterItem);
                                                counter++;
                                            }
                                        }
                                    } else {
                                        // No data found
                                        Log.w("FirebaseDatabase", "No location data found");
                                    }
                                }
                            }
                        } else {
                            // No data found
                            Log.w("FirebaseDatabase", "No location data found");
                        }
                    }
                    accelerationsClusterManager.cluster();
                    refreshMap();
                    Log.i("Clusters", "Accelerations clusters loaded (" + counter + ")");
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(getApplicationContext(), getString(R.string.failed) + System.lineSeparator() + error.toException().getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            Log.e("MapsViewData", "Map data load exception", e);
            Toast.makeText(MapsViewActivity.this, getString(R.string.failed), Toast.LENGTH_SHORT).show();
        }
    }

    private void hideAccelerations() {
        // Hide accelerations
        accelerationsClusterManager.clearItems();
        accelerationsClusterManager.cluster();
        refreshMap();
    }

    @SuppressWarnings("unchecked")
    private void showDecelerations() {
        try {
            // Date filter
            String fromTimestamp = timestampFormat.format(fromDate);
            String toTimestamp = timestampFormat.format(toDate);
            // Get decelerations data
            Query decelerationsQuery = mDatabaseReference.child("Locations");
            decelerationsQuery.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    // Reset decelerations data
                    decelerationsClusterManager.clearItems();
                    // Cluster counter result
                    int counter = 0;
                    // Read location records
                    for (DataSnapshot decelerationDataSnapshot : dataSnapshot.getChildren()) {
                        Map<String, LocationHelper> map = (Map<String, LocationHelper>) decelerationDataSnapshot.getValue();
                        if (map != null) {
                            for (Map.Entry<String, LocationHelper> entry : map.entrySet()) {
                                String key_timestamp = entry.getKey();
                                if (Long.parseLong(key_timestamp) >= Long.parseLong(fromTimestamp) && Long.parseLong(key_timestamp) <= Long.parseLong(toTimestamp)) {
                                    Gson gson = new Gson();
                                    JsonElement jsonElement = gson.toJsonTree(entry.getValue());
                                    LocationHelper locationHelper = gson.fromJson(jsonElement, LocationHelper.class);
                                    if (locationHelper != null) {
                                        // Found data
                                        if (locationHelper.getDeceleration() != 0) {
                                            LocalDate date = LocalDate.parse(locationHelper.getTimestamp(), dateTimeFormatter);
                                            if (gMap != null) {
                                                LatLng latLng = new LatLng(locationHelper.getLatitude(), locationHelper.getLongitude());
                                                BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED);
                                                String title = getString(R.string.decelerations) + " (" + dateFormatter.format(date) + ")";
                                                String snippet = getString(R.string.decelerations) + ": " + BigDecimal.valueOf(locationHelper.getLastSpeed()).setScale(2, RoundingMode.HALF_UP).doubleValue() + "KM/H --> " + BigDecimal.valueOf(locationHelper.getSpeed()).setScale(2, RoundingMode.HALF_UP).doubleValue() + "KM/H" + System.lineSeparator() + getString(R.string.user_id) + ": " + locationHelper.getUid();
                                                MarkerClusterItemHelper decelerationClusterItem = new MarkerClusterItemHelper(latLng, bitmapDescriptor, title, snippet, Color.RED, locationHelper);
                                                decelerationsClusterManager.addItem(decelerationClusterItem);
                                                counter++;
                                            }
                                        }
                                    } else {
                                        // No data found
                                        Log.w("FirebaseDatabase", "No location data found");
                                    }
                                }
                            }
                        } else {
                            // No data found
                            Log.w("FirebaseDatabase", "No location data found");
                        }
                    }
                    decelerationsClusterManager.cluster();
                    refreshMap();
                    Log.i("Clusters", "Decelerations clusters loaded (" + counter + ")");
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(getApplicationContext(), getString(R.string.failed) + System.lineSeparator() + error.toException().getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            Log.e("MapsViewData", "Map data load exception", e);
            Toast.makeText(MapsViewActivity.this, getString(R.string.failed), Toast.LENGTH_SHORT).show();
        }
    }

    private void hideDecelerations() {
        // Hide decelerations
        decelerationsClusterManager.clearItems();
        decelerationsClusterManager.cluster();
        refreshMap();
    }

    private void refreshMap() {
        if (gMap != null) {
            gMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                    new CameraPosition.Builder()
                            .target(gMap.getCameraPosition().target)
                            .zoom(gMap.getCameraPosition().zoom)
                            .bearing(gMap.getCameraPosition().bearing)
                            .tilt(gMap.getCameraPosition().tilt)
                            .build()
            ));
        }
    }

    @SuppressLint("PotentialBehaviorOverride")
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
            // Custom map pin text
            if (gMap != null) {
                // Setup map markers & clusters
                MarkerManager markerManager = new MarkerManager(gMap);
                incidentsAccidentsClusterManager = new ClusterManager<>(this, gMap, markerManager);
                incidentsClosedRoadsClusterManager = new ClusterManager<>(this, gMap, markerManager);
                incidentsTrafficClusterManager = new ClusterManager<>(this, gMap, markerManager);
                incidentsOtherClusterManager = new ClusterManager<>(this, gMap, markerManager);
                trafficStartClusterManager = new ClusterManager<>(this, gMap, markerManager);
                trafficEndClusterManager = new ClusterManager<>(this, gMap, markerManager);
                accelerationsClusterManager = new ClusterManager<>(this, gMap, markerManager);
                decelerationsClusterManager = new ClusterManager<>(this, gMap, markerManager);
                gMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
                    @Override
                    public void onCameraIdle() {
                        incidentsAccidentsClusterManager.cluster(); // .onCameraIdle();
                        incidentsClosedRoadsClusterManager.cluster();
                        incidentsTrafficClusterManager.cluster();
                        incidentsOtherClusterManager.cluster();
                        trafficStartClusterManager.cluster();
                        trafficEndClusterManager.cluster();
                        accelerationsClusterManager.cluster();
                        decelerationsClusterManager.cluster();
                        refreshMap();
                    }
                });
                gMap.setOnMarkerClickListener(markerManager);
                gMap.setInfoWindowAdapter(markerManager);
                gMap.setOnInfoWindowClickListener(markerManager);
                incidentsAccidentsClusterManager.getMarkerCollection().setInfoWindowAdapter(new MarkerClusterInfoWindowAdapter(this));
                incidentsClosedRoadsClusterManager.getMarkerCollection().setInfoWindowAdapter(new MarkerClusterInfoWindowAdapter(this));
                incidentsTrafficClusterManager.getMarkerCollection().setInfoWindowAdapter(new MarkerClusterInfoWindowAdapter(this));
                incidentsOtherClusterManager.getMarkerCollection().setInfoWindowAdapter(new MarkerClusterInfoWindowAdapter(this));
                trafficStartClusterManager.getMarkerCollection().setInfoWindowAdapter(new MarkerClusterInfoWindowAdapter(this));
                trafficEndClusterManager.getMarkerCollection().setInfoWindowAdapter(new MarkerClusterInfoWindowAdapter(this));
                accelerationsClusterManager.getMarkerCollection().setInfoWindowAdapter(new MarkerClusterInfoWindowAdapter(this));
                decelerationsClusterManager.getMarkerCollection().setInfoWindowAdapter(new MarkerClusterInfoWindowAdapter(this));
                incidentsAccidentsClusterManager.setRenderer(new MarkerClusterRenderer(this, gMap, incidentsAccidentsClusterManager));
                incidentsClosedRoadsClusterManager.setRenderer(new MarkerClusterRenderer(this, gMap, incidentsClosedRoadsClusterManager));
                incidentsTrafficClusterManager.setRenderer(new MarkerClusterRenderer(this, gMap, incidentsTrafficClusterManager));
                incidentsOtherClusterManager.setRenderer(new MarkerClusterRenderer(this, gMap, incidentsOtherClusterManager));
                trafficStartClusterManager.setRenderer(new MarkerClusterRenderer(this, gMap, trafficStartClusterManager));
                trafficEndClusterManager.setRenderer(new MarkerClusterRenderer(this, gMap, trafficEndClusterManager));
                accelerationsClusterManager.setRenderer(new MarkerClusterRenderer(this, gMap, accelerationsClusterManager));
                decelerationsClusterManager.setRenderer(new MarkerClusterRenderer(this, gMap, decelerationsClusterManager));
            }
            // Show incidents
            showIncidents();
            // Show traffic
            showTraffic();
            // Hide accelerations
            hideAccelerations();
            // Hide decelerations
            hideDecelerations();
        } catch (Exception e) {
            Log.e("Map", "Can't find map style", e);
        }
        // Check user permissions & get user's last known location
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            final int LOCATION_PERMISSION_REQUEST_CODE = 901;
            ActivityCompat.requestPermissions(MapsViewActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            googleMap.setMyLocationEnabled(true);
            Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastKnownLocation != null) {
                LatLng lastKnownLatLng = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                // Animate map view on user's last known location
                gMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                        new CameraPosition.Builder()
                                .target(lastKnownLatLng)
                                .zoom(20)
                                .bearing(lastKnownLocation.getBearing())
                                .tilt(80)
                                .build()
                ));
            }
        }
    }
}