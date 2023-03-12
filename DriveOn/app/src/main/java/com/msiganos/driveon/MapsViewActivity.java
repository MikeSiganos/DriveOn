package com.msiganos.driveon;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
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
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.msiganos.driveon.databinding.ActivityMapsViewBinding;
import com.msiganos.driveon.helpers.LocationHelper;

public class MapsViewActivity extends FragmentActivity implements OnMapReadyCallback {

    private final int LOCATION_PERMISSION_REQUEST_CODE = 901;
    private GoogleMap gMap;
    private ActivityMapsViewBinding binding;
    private FirebaseUser mUser;
    private DatabaseReference mDatabaseReference;
    private LocationManager locationManager;
    private int statusBarHeight, navigationBarHeight;

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

    private void systemInit() {
        // Set LocationManager
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
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
        } catch (Exception e) {
            Log.e("MapsViewLayout", "Maps view layout exception", e);
            Toast.makeText(MapsViewActivity.this, getString(R.string.failed), Toast.LENGTH_SHORT).show();
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
        // Check user permissions & get user's last known location
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
        downloadData();
    }

    private void downloadData() {
        // Read location data of the current user from the database
        mDatabaseReference.child("Locations").child(mUser.getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Read every location record based on uid
                for (DataSnapshot locationDataSnapshot : dataSnapshot.getChildren()) {
                    LocationHelper locationHelper = locationDataSnapshot.getValue(LocationHelper.class);
                    if (locationHelper != null) {
                        // Set & add markers
                        if (gMap != null) {
                            LatLng latLng = new LatLng(locationHelper.getLatitude(), locationHelper.getLongitude());
                            BitmapDescriptor bitmapDescriptor;
                            String title, snippet;
                            if (locationHelper.getAcceleration() > 0) {
                                bitmapDescriptor = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN);
                                title = mUser.getDisplayName() + " (" + locationHelper.getTimestamp() + ")";
                                snippet = "Acceleration: " + locationHelper.getAcceleration();
                            } else if (locationHelper.getDeceleration() < 0) {
                                bitmapDescriptor = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA);
                                title = mUser.getDisplayName() + " (" + locationHelper.getTimestamp() + ")";
                                snippet = "Deceleration: " + locationHelper.getDeceleration();
                            } else {
                                bitmapDescriptor = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE);
                                title = mUser.getDisplayName() + " (" + locationHelper.getTimestamp() + ")";
                                snippet = "Other incident: (Using accelerometer: " + locationHelper.isUsingAccelerometer() + ") - Acceleration: " + locationHelper.getAcceleration() + " - Deceleration: " + locationHelper.getDeceleration() + " - Last Speed: " + locationHelper.getLastSpeed() + " - Speed: " + locationHelper.getSpeed() + " - Last Bearing: " + locationHelper.getLastBearing() + " - Bearing: " + locationHelper.getBearing();
                            }
                            gMap.addMarker(new MarkerOptions().position(latLng).icon(bitmapDescriptor).title(title).snippet(snippet));
                            gMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                        }
                    } else {
                        // No data found
                        Toast.makeText(getApplicationContext(), getString(R.string.no_data), Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getApplicationContext(), getString(R.string.failed) + System.lineSeparator() + error.toException().getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}