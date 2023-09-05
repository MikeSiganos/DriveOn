package com.msiganos.driveon;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class UserStatsDialog extends Dialog {

    private final Context context;
    private FirebaseUser mUser;
    private DatabaseReference mDatabaseReference;
    private LineGraphSeries<DataPoint> drivingStatusSeries, averageSpeedSeries, accelerationSeries, decelerationSeries;

    public UserStatsDialog(@NonNull Context context) {
        super(context);
        this.context = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            // Dialog interface
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            Objects.requireNonNull(getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            setContentView(R.layout.dialog_user_stats);
            setCancelable(true);
            // Dialog items
            TextView drivingStatusTextView = findViewById(R.id.drivingStatusGraphTextView);
            drivingStatusTextView.setText(context.getString(R.string.driving_behaviour_status));
            drivingStatusTextView.setTextColor(Color.GREEN);
            TextView averageSpeedTextView = findViewById(R.id.averageSpeedGraphTextView);
            averageSpeedTextView.setText(context.getString(R.string.average_speed));
            averageSpeedTextView.setTextColor(Color.RED);
            TextView accelerationsTextView = findViewById(R.id.accelerationsGraphTextView);
            accelerationsTextView.setText(context.getString(R.string.accelerations));
            accelerationsTextView.setTextColor(Color.CYAN);
            TextView decelerationsTextView = findViewById(R.id.decelerationsGraphTextView);
            decelerationsTextView.setText(context.getString(R.string.decelerations));
            decelerationsTextView.setTextColor(Color.MAGENTA);
            GraphView graphView = findViewById(R.id.statsGraphView);
            // Set graph labels formatter
            graphView.getGridLabelRenderer().setHumanRounding(false);
            graphView.getGridLabelRenderer().setNumHorizontalLabels(14);
            graphView.getGridLabelRenderer().setHorizontalLabelsAngle(100);
            graphView.getGridLabelRenderer().setNumVerticalLabels(11);
            DateFormat dateFormat = new SimpleDateFormat("MM/yyyy", Locale.getDefault());
            graphView.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
                @Override
                public String formatLabel(double value, boolean isValueX) {
                    super.formatLabel(value, isValueX);
                    if (isValueX) {
                        // X values - Show only values for current year on big screen
                        Date date = new Date(Math.round(value * 1000L));
                        String xLabel = dateFormat.format(date);
                        if (!xLabel.equals("12/" + (Year.now().getValue() - 1)) && !xLabel.equals("01/" + (Year.now().getValue() + 1)))
                            return xLabel;
                        else
                            return "";
                    } else {
                        // Y values - Show only integer values from 0 to 250 on big screen
                        return String.valueOf(Math.round(value));
                    }
                }
            });
            graphView.getGridLabelRenderer().reloadStyles();
            // Set graph x bounds for the current year
            LocalDate localStartDate = LocalDate.of(Year.now().getValue() - 1, 12, 31);
            long epochStart = localStartDate.atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
            graphView.getViewport().setMinX(epochStart);
            LocalDate localEndDate = LocalDate.of(Year.now().getValue() + 1, 1, 1);
            long epochEnd = localEndDate.atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
            graphView.getViewport().setMaxX(epochEnd);
            graphView.getViewport().setXAxisBoundsManual(true);
            graphView.getViewport().setMinY(0);
            graphView.getViewport().setMaxY(250);
            graphView.getViewport().setYAxisBoundsManual(true);
            // Enable horizontal & vertical graph zooming & scrolling
            graphView.getViewport().setScalable(true);
            graphView.getViewport().setScrollable(true);
            graphView.getViewport().setScalableY(true);
            graphView.getViewport().setScrollableY(true);
            // Dialog buttons
            Button closeButton = findViewById(R.id.closeStatisticsButton);
            Button resetButton = findViewById(R.id.resetStatisticsButton);
            // Buttons listeners
            closeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Close view user statistics
                    dismiss();
                }
            });
            resetButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Reset user stats
                    resetUserStats();
                }
            });
            // Firebase instance
            firebaseInit();
            // Download & View user stats
            downloadData();
            // Fill driving behaviour graph
            drivingStatusSeries = new LineGraphSeries<>();
            drivingStatusSeries.setTitle(context.getString(R.string.driving_behaviour_status));
            drivingStatusSeries.setColor(Color.GREEN);
            drivingStatusSeries.setDrawDataPoints(true);
            drivingStatusSeries.setDataPointsRadius(10);
            drivingStatusSeries.setThickness(8);
            graphView.addSeries(drivingStatusSeries);
            // Fill average speed graph
            averageSpeedSeries = new LineGraphSeries<>();
            averageSpeedSeries.setTitle(context.getString(R.string.average_speed));
            averageSpeedSeries.setColor(Color.RED);
            averageSpeedSeries.setDrawDataPoints(true);
            averageSpeedSeries.setDataPointsRadius(10);
            averageSpeedSeries.setThickness(8);
            graphView.addSeries(averageSpeedSeries);
            // Fill accelerations graph
            accelerationSeries = new LineGraphSeries<>();
            accelerationSeries.setTitle(context.getString(R.string.accelerations));
            accelerationSeries.setColor(Color.CYAN);
            accelerationSeries.setDrawDataPoints(true);
            accelerationSeries.setDataPointsRadius(10);
            accelerationSeries.setThickness(8);
            graphView.addSeries(accelerationSeries);
            // Fill decelerations graph
            decelerationSeries = new LineGraphSeries<>();
            decelerationSeries.setTitle(context.getString(R.string.decelerations));
            decelerationSeries.setColor(Color.MAGENTA);
            decelerationSeries.setDrawDataPoints(true);
            decelerationSeries.setDataPointsRadius(10);
            decelerationSeries.setThickness(8);
            graphView.addSeries(decelerationSeries);
        } catch (Exception e) {
            Log.e("UserStatsLayout", "Creating user stats layout exception", e);
            Toast.makeText(context, context.getString(R.string.failed), Toast.LENGTH_SHORT).show();
        }
    }

    private void firebaseInit() {
        // Firebase Authentication
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        // Firebase Realtime Database
        FirebaseDatabase mDatabase = FirebaseDatabase.getInstance("https://drive-on-mscsd-default-rtdb.europe-west1.firebasedatabase.app/");
        mDatabaseReference = mDatabase.getReference();
    }

    private void downloadData() {
        // Download and view user stats from database
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd", Locale.getDefault());
        try {
            mDatabaseReference.child("History").child(mUser.getUid()).child("DrivingStatus").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    // Read user driving status records based on uid
                    DataPoint[] dataPoints = new DataPoint[(int) dataSnapshot.getChildrenCount()];
                    int index = 0;
                    for (DataSnapshot dateDataSnapshot : dataSnapshot.getChildren()) {
                        String date = dateDataSnapshot.getKey();
                        if (date != null) {
                            LocalDate localDate = LocalDate.parse(date, dateFormatter);
                            long epoch = localDate.atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
                            Integer drivingStatus = dateDataSnapshot.getValue(Integer.class);
                            if (drivingStatus != null) {
                                dataPoints[index] = new DataPoint(epoch, drivingStatus);
                                index++;
                            } else {
                                // No data found
                                Log.w("FirebaseDatabase", "No user's driving status data found");
                            }
                        } else {
                            // No data found
                            Log.w("FirebaseDatabase", "No user's driving status data found");
                        }
                    }
                    drivingStatusSeries.resetData(dataPoints);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.w("FirebaseDatabase", "Get user's driving status data failed", error.toException());
                }
            });
            mDatabaseReference.child("History").child(mUser.getUid()).child("AverageSpeed").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    // Read user average speed records based on uid
                    DataPoint[] dataPoints = new DataPoint[(int) dataSnapshot.getChildrenCount()];
                    int index = 0;
                    for (DataSnapshot dateDataSnapshot : dataSnapshot.getChildren()) {
                        String date = dateDataSnapshot.getKey();
                        if (date != null) {
                            LocalDate localDate = LocalDate.parse(date, dateFormatter);
                            long epoch = localDate.atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
                            Double averageSpeed = dateDataSnapshot.getValue(Double.class);
                            if (averageSpeed != null) {
                                averageSpeed = BigDecimal.valueOf(averageSpeed).setScale(2, RoundingMode.HALF_UP).doubleValue();
                                dataPoints[index] = new DataPoint(epoch, averageSpeed);
                                index++;
                            } else {
                                // No data found
                                Log.w("FirebaseDatabase", "No user's average speed data found");
                            }
                        } else {
                            // No data found
                            Log.w("FirebaseDatabase", "No user's average speed data found");
                        }
                    }
                    averageSpeedSeries.resetData(dataPoints);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.w("FirebaseDatabase", "Get user's average speed data failed", error.toException());
                }
            });
            mDatabaseReference.child("History").child(mUser.getUid()).child("Accelerations").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    // Read user accelerations records based on uid
                    DataPoint[] dataPoints = new DataPoint[(int) dataSnapshot.getChildrenCount()];
                    int index = 0;
                    for (DataSnapshot dateDataSnapshot : dataSnapshot.getChildren()) {
                        String date = dateDataSnapshot.getKey();
                        if (date != null) {
                            LocalDate localDate = LocalDate.parse(date, dateFormatter);
                            long epoch = localDate.atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
                            Integer accelerations = dateDataSnapshot.getValue(Integer.class);
                            if (accelerations != null) {
                                dataPoints[index] = new DataPoint(epoch, accelerations);
                                index++;
                            } else {
                                // No data found
                                Log.w("FirebaseDatabase", "No user's acceleration data found");
                            }
                        } else {
                            // No data found
                            Log.w("FirebaseDatabase", "No user's acceleration data found");
                        }
                    }
                    accelerationSeries.resetData(dataPoints);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.w("FirebaseDatabase", "Get user's acceleration data failed", error.toException());
                }
            });
            mDatabaseReference.child("History").child(mUser.getUid()).child("Decelerations").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    // Read user deceleration records based on uid
                    DataPoint[] dataPoints = new DataPoint[(int) dataSnapshot.getChildrenCount()];
                    int index = 0;
                    for (DataSnapshot dateDataSnapshot : dataSnapshot.getChildren()) {
                        String date = dateDataSnapshot.getKey();
                        if (date != null) {
                            LocalDate localDate = LocalDate.parse(date, dateFormatter);
                            long epoch = localDate.atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
                            Integer decelerations = dateDataSnapshot.getValue(Integer.class);
                            if (decelerations != null) {
                                dataPoints[index] = new DataPoint(epoch, decelerations);
                                index++;
                            } else {
                                // No data found
                                Log.w("FirebaseDatabase", "No user's deceleration data found");
                            }
                        } else {
                            // No data found
                            Log.w("FirebaseDatabase", "No user's deceleration data found");
                        }
                    }
                    decelerationSeries.resetData(dataPoints);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.w("FirebaseDatabase", "Get user's deceleration data failed", error.toException());
                }
            });
        } catch (Exception e) {
            Log.e("FirebaseDatabase", "Get user's stats exception", e);
        }
    }

    private void resetUserStats() {
        // Reset user stats
        try {
            mDatabaseReference.child("Users").child(mUser.getUid()).child("accelerations").setValue(0)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Log.i("FirebaseDatabase", "Update user stats on database succeed", task.getException());
                            } else {
                                Toast.makeText(context, context.getString(R.string.database_update_failed) + System.lineSeparator() + Objects.requireNonNull(task.getException()).getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                                Log.w("FirebaseDatabase", "Update user stats on database error", task.getException());
                            }
                        }
                    });
            mDatabaseReference.child("Users").child(mUser.getUid()).child("decelerations").setValue(0)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Log.i("FirebaseDatabase", "Update user stats on database succeed", task.getException());
                            } else {
                                Toast.makeText(context, context.getString(R.string.database_update_failed) + System.lineSeparator() + Objects.requireNonNull(task.getException()).getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                                Log.w("FirebaseDatabase", "Update user stats on database error", task.getException());
                            }
                        }
                    });
            mDatabaseReference.child("Users").child(mUser.getUid()).child("averageSpeed").setValue(0)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Log.i("FirebaseDatabase", "Update user stats on database succeed", task.getException());
                            } else {
                                Toast.makeText(context, context.getString(R.string.database_update_failed) + System.lineSeparator() + Objects.requireNonNull(task.getException()).getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                                Log.w("FirebaseDatabase", "Update user stats on database error", task.getException());
                            }
                        }
                    });
            mDatabaseReference.child("Users").child(mUser.getUid()).child("drivingStatus").setValue(0)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Log.i("FirebaseDatabase", "Update user stats on database succeed", task.getException());
                            } else {
                                Toast.makeText(context, context.getString(R.string.database_update_failed) + System.lineSeparator() + Objects.requireNonNull(task.getException()).getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                                Log.w("FirebaseDatabase", "Update user stats on database error", task.getException());
                            }
                        }
                    });
        } catch (Exception e) {
            Log.e("FirebaseDatabase", "Reset user stats exception", e);
        }
    }
}