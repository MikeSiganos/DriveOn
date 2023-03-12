package com.msiganos.driveon;

import android.animation.ObjectAnimator;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.msiganos.driveon.helpers.DateTimeHelper;
import com.msiganos.driveon.helpers.SystemHelper;
import com.msiganos.driveon.helpers.UserHelper;
import com.squareup.picasso.Picasso;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private SystemHelper mSystem;
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private DatabaseReference mDatabaseReference;
    private ImageView profileImageView;
    private AlphaAnimation fadeOut, fadeIn;
    private ProgressBar drivingProgressBar;
    private TextView drivingProgressTextView, drivingTextView, statsTextView, myProfileTextView, lastLoginTextView, firstRegisterTextView;
    private int drivingStatus, accelerations, decelerations;
    private double averageSpeed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Full screen content
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        // ActionBar with user logo
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayOptions(actionBar.getDisplayOptions() | ActionBar.DISPLAY_SHOW_CUSTOM);
            profileImageView = new ImageView(actionBar.getThemedContext());
            profileImageView.setScaleType(ImageView.ScaleType.CENTER);
            profileImageView.setImageResource(R.drawable.ic_baseline_car_crash_24);
            ActionBar.LayoutParams layoutParams = new ActionBar.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT, Gravity.END | Gravity.CENTER_VERTICAL);
            layoutParams.rightMargin = 40;
            profileImageView.setLayoutParams(layoutParams);
            actionBar.setCustomView(profileImageView);
        }
        // Main activity
        setContentView(R.layout.activity_main);
        systemInit();
        firebaseInit();
        // Set profile picture
        setProfilePhoto();
        // Set layout
        layoutInit();
        // Update user data
        updateLogin();
        // Get user driver status & Update user statistics
        getDriverStatus();
    }

    private void systemInit() {
        // Set SystemHelper
        mSystem = new SystemHelper(this);
    }

    private void firebaseInit() {
        // Firebase Authentication
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        // Firebase Realtime Database
        FirebaseDatabase mDatabase = FirebaseDatabase.getInstance("https://drive-on-mscsd-default-rtdb.europe-west1.firebasedatabase.app/");
        mDatabaseReference = mDatabase.getReference();
    }

    protected void setProfilePhoto() {
        String profilePhotoUrl = Objects.requireNonNull(mUser.getPhotoUrl()).toString();
        Picasso.get().load(profilePhotoUrl).into(profileImageView);
    }

    private void layoutInit() {
        try {
            // Set Fade in / Fade out effects
            fadeIn = new AlphaAnimation(0.0f, 1.0f);
            fadeOut = new AlphaAnimation(1.0f, 0.0f);
            fadeIn.setDuration(300);
            fadeIn.setFillAfter(true);
            fadeOut.setDuration(300);
            fadeOut.setFillAfter(true);
            fadeIn.setStartOffset(fadeOut.getStartOffset() + 200);
            fadeOut.setStartOffset(fadeIn.getStartOffset() + 200);
            // Set logout extended FAB
            ExtendedFloatingActionButton logoutExtendedFab = findViewById(R.id.logoutExtendedFAB);
            // Margin for full screen
            ViewCompat.setOnApplyWindowInsetsListener(logoutExtendedFab, (v, windowInsets) -> {
                Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                mlp.bottomMargin = insets.bottom + 50;
                mlp.rightMargin = insets.right + 30;
                v.setLayoutParams(mlp);
                return WindowInsetsCompat.CONSUMED;
            });
            // Set progressbar
            drivingProgressBar = findViewById(R.id.drivingProgressBar);
            // Set TextViews
            drivingProgressTextView = findViewById(R.id.drivingProgressTextView);
            drivingTextView = findViewById(R.id.drivingTextView);
            // On portrait change layout
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                ViewCompat.setOnApplyWindowInsetsListener(drivingProgressBar, (v, windowInsets) -> {
                    Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                    drivingProgressBar.setPadding(insets.left + 200, insets.top, insets.right + 200, insets.bottom);
                    return WindowInsetsCompat.CONSUMED;
                });
                ViewCompat.setOnApplyWindowInsetsListener(drivingTextView, (v, windowInsets) -> {
                    Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                    ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                    mlp.bottomMargin = insets.bottom + 185;
                    v.setLayoutParams(mlp);
                    return WindowInsetsCompat.CONSUMED;
                });
            }
            statsTextView = findViewById(R.id.statsDetailsTextView);
            myProfileTextView = findViewById(R.id.myProfileDetailsTextView);
            lastLoginTextView = findViewById(R.id.lastLoginDetailsTextView);
            firstRegisterTextView = findViewById(R.id.firstRegisterDetailsTextView);
            // Initiate progressbar
            setTextProgress("100%", -1);
        } catch (Exception e) {
            Log.e("Layout", "Main activity layout exception", e);
            Toast.makeText(MainActivity.this, getString(R.string.failed), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateLogin() {
        // Update database
        try {
            // Update lastLogin timestamp
            mDatabaseReference.child("Users").child(mUser.getUid()).child("lastLogin").setValue(new DateTimeHelper())
                    .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Log.i("FirebaseDatabase", "Update user's last login on database succeed");
                            } else {
                                Toast.makeText(MainActivity.this, getString(R.string.database_update_failed) + System.lineSeparator() + Objects.requireNonNull(task.getException()).getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                                Log.w("FirebaseDatabase", "Update user's last login on database failed", task.getException());
                            }
                        }
                    });
            // Update device info
            mDatabaseReference.child("Users").child(mUser.getUid()).child("device").setValue(mSystem.getDevice())
                    .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Log.i("FirebaseDatabase", "Update user's device on database succeed");
                            } else {
                                Toast.makeText(MainActivity.this, getString(R.string.database_update_failed) + System.lineSeparator() + Objects.requireNonNull(task.getException()).getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                                Log.w("FirebaseDatabase", "Update user's device on database failed", task.getException());
                            }
                        }
                    });
        } catch (Exception e) {
            Log.e("FirebaseDatabase", "Update login exception", e);
        }
    }

    private void getDriverStatus() {
        // Read driver status from database & Update progressbar
        try {
            mDatabaseReference.child("Users").child(mUser.getUid()).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    // Read user records based on uid
                    UserHelper userHelper = dataSnapshot.getValue(UserHelper.class);
                    if (userHelper != null) {
                        Log.i("FirebaseDatabase", "Get user's driving status succeed");
                        // Get drivingStatus & Update progressbar
                        drivingStatus = userHelper.getDrivingStatus();
                        String progress = drivingStatus + "%";
                        setTextProgress(progress, 0);
                        // Get User data & Update layout
                        averageSpeed = BigDecimal.valueOf(userHelper.getAverageSpeed()).setScale(2, RoundingMode.HALF_UP).doubleValue();
                        accelerations = userHelper.getAccelerations();
                        decelerations = userHelper.getDecelerations();
                        String statsText = getString(R.string.average_speed) + ": " + averageSpeed + System.lineSeparator() + getString(R.string.accelerations) + ": " + accelerations + System.lineSeparator() + getString(R.string.decelerations) + ": " + decelerations;
                        statsTextView.setText(statsText);
                        updateStatistics();
                        int incidents = userHelper.getIncidents();
                        String nickname = userHelper.getNickname();
                        String device = userHelper.getDevice();
                        String myProfileText = nickname + System.lineSeparator() + device + System.lineSeparator() + getString(R.string.incidents) + ": " + incidents;
                        myProfileTextView.setText(myProfileText);
                        String lastLogin = userHelper.getLastLogin().getDate() + System.lineSeparator() + userHelper.getLastLogin().getTime();
                        lastLoginTextView.setText(lastLogin);
                        String firstRegister = userHelper.getFirstRegister().getDate() + System.lineSeparator() + userHelper.getFirstRegister().getTime();
                        firstRegisterTextView.setText(firstRegister);
                    } else {
                        // No data found
                        Log.w("FirebaseDatabase", "No user's driving status data found");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.w("FirebaseDatabase", "Get user's driving status failed", error.toException());
                }
            });
        } catch (Exception e) {
            Log.e("FirebaseDatabase", "Get user's driving status exception", e);
        }
    }

    private void setDrivingProgress(int drivingProgress, int progressInit) {
        // Animate progressbar
        ObjectAnimator.ofInt(drivingProgressBar, "progress", drivingProgress).setDuration(600).start();
        // Text according to driver's status
        String drivingQuality;
        if (drivingProgress >= 90) {
            // Very good driver
            drivingQuality = getString(R.string.excellent_driver);
        } else if (drivingProgress >= 70) {
            // Good driver
            drivingQuality = getString(R.string.good_driver);
        } else if (drivingProgress >= 50) {
            // Moderate driver
            drivingQuality = getString(R.string.moderate_driver);
        } else {
            // Bad driver
            drivingQuality = getString(R.string.bad_driver);
        }
        if (progressInit == 0)
            drivingTextView.setText(drivingQuality);
        drivingTextView.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shaker));
    }

    private void setTextProgress(String progress, int progressInit) {
        // Every time progress value changes update progressbar
        int drivingProgress = Integer.parseInt(progress.replace("%", ""));
        if (progressInit != 0) {
            drivingProgressTextView.setVisibility(View.INVISIBLE);
            drivingTextView.setText("");
            setDrivingProgress(drivingProgress, progressInit);
        } else {
            setDrivingProgress(drivingProgress, progressInit);
            drivingProgressTextView.setVisibility(View.VISIBLE);
            drivingProgressTextView.startAnimation(fadeOut);
            drivingProgressTextView.setText(progress);
            drivingProgressTextView.startAnimation(fadeIn);
        }
    }

    private void updateStatistics() {
        try {
            // Update user statistics
            int status;
            if (averageSpeed >= 100)
                status = 50;
            else if (averageSpeed >= 70)
                status = 70;
            else if (averageSpeed >= 50)
                status = 80;
            else
                status = 60;
            if (accelerations <= 100 && decelerations <= 100)
                status += 20;
            else
                status -= 10;
            mDatabaseReference.child("Users").child(mUser.getUid()).child("drivingStatus").setValue(status)
                    .addOnCompleteListener(MainActivity.this, new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Log.i("FirebaseDatabase", "Update user's driving status on database succeed");
                            } else {
                                Toast.makeText(MainActivity.this, getString(R.string.database_update_failed) + System.lineSeparator() + Objects.requireNonNull(task.getException()).getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                                Log.w("FirebaseDatabase", "Update user's driving status on database failed", task.getException());
                            }
                        }
                    });
            // Update user statistics history
            Date date = new Date();
            DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
            String dateStamp = dateFormat.format(date);
            mDatabaseReference.child("History").child(mUser.getUid()).child("DrivingStatus").child(dateStamp).setValue(status)
                    .addOnCompleteListener(MainActivity.this, new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Log.i("FirebaseDatabase", "Update user's driving status history on database succeed");
                            } else {
                                Toast.makeText(MainActivity.this, getString(R.string.database_update_failed) + System.lineSeparator() + Objects.requireNonNull(task.getException()).getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                                Log.w("FirebaseDatabase", "Update user's driving status history on database failed", task.getException());
                            }
                        }
                    });
            mDatabaseReference.child("History").child(mUser.getUid()).child("AverageSpeed").child(dateStamp).setValue(averageSpeed)
                    .addOnCompleteListener(MainActivity.this, new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Log.i("FirebaseDatabase", "Update user's average speed history on database succeed");
                            } else {
                                Toast.makeText(MainActivity.this, getString(R.string.database_update_failed) + System.lineSeparator() + Objects.requireNonNull(task.getException()).getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                                Log.w("FirebaseDatabase", "Update user's driving speed history on database failed", task.getException());
                            }
                        }
                    });
            mDatabaseReference.child("History").child(mUser.getUid()).child("Accelerations").child(dateStamp).setValue(accelerations)
                    .addOnCompleteListener(MainActivity.this, new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Log.i("FirebaseDatabase", "Update user's acceleration history on database succeed");
                            } else {
                                Toast.makeText(MainActivity.this, getString(R.string.database_update_failed) + System.lineSeparator() + Objects.requireNonNull(task.getException()).getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                                Log.w("FirebaseDatabase", "Update user's acceleration history on database failed", task.getException());
                            }
                        }
                    });
            mDatabaseReference.child("History").child(mUser.getUid()).child("Decelerations").child(dateStamp).setValue(decelerations)
                    .addOnCompleteListener(MainActivity.this, new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Log.i("FirebaseDatabase", "Update user's deceleration history on database succeed");
                            } else {
                                Toast.makeText(MainActivity.this, getString(R.string.database_update_failed) + System.lineSeparator() + Objects.requireNonNull(task.getException()).getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                                Log.w("FirebaseDatabase", "Update user's deceleration history on database failed", task.getException());
                            }
                        }
                    });
        } catch (Exception e) {
            Log.e("FirebaseDatabase", "Updating user's driving status exception", e);
        }
    }

    public void openMainMap(View view) {
        // Go to map intent & upload user's location data
        Intent intent = new Intent(this, MapsActivity.class);
        startActivity(intent);
    }

    public void openSecondaryMap(View view) {
        // Go to mapView intent & view users' data
        Intent intent = new Intent(this, MapsViewActivity.class);
        startActivity(intent);
    }

    public void logout(View view) {
        // Logout and go to login page
        mAuth.signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    public void openUserStats(View view) {
        UserStatsDialog userStatsDialog = new UserStatsDialog(MainActivity.this);
        userStatsDialog.show();
    }

    public void openMyProfile(View view) {
        UserProfileDialog userProfileDialog = new UserProfileDialog(MainActivity.this);
        userProfileDialog.show();
        userProfileDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                setProfilePhoto();
            }
        });

    }
}