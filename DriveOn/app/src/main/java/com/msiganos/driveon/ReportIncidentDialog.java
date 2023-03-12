package com.msiganos.driveon;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.msiganos.driveon.helpers.IncidentHelper;

import java.util.Objects;

public class ReportIncidentDialog extends Dialog {

    private final Context context;
    private final Location location;
    private CheckBox accidentCheckbox, closedRoadCheckbox, trafficCheckbox, otherIncidentCheckbox;
    private EditText reportIncidentEditText;
    private Button reportButton, clearButton;
    private FirebaseUser mUser;
    private DatabaseReference mDatabaseReference;

    public ReportIncidentDialog(@NonNull Context context, Location location) {
        super(context);
        this.context = context;
        this.location = location;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Dialog interface
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        setContentView(R.layout.dialog_report_incident);
        setCancelable(true);
        // Dialog items
        accidentCheckbox = findViewById(R.id.reportAccidentCheckbox);
        closedRoadCheckbox = findViewById(R.id.reportClosedRoadCheckbox);
        trafficCheckbox = findViewById(R.id.reportTrafficCheckbox);
        otherIncidentCheckbox = findViewById(R.id.reportOtherCheckbox);
        reportIncidentEditText = findViewById(R.id.reportIncidentEditText);
        // Dialog buttons
        reportButton = findViewById(R.id.reportIncidentButton);
        reportButton.setEnabled(false);
        clearButton = findViewById(R.id.clearIncidentButton);
        clearButton.setEnabled(false);
        Button cancelButton = findViewById(R.id.cancelIncidentButton);
        // Checkboxes listeners
        accidentCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                anyDetailsEntered();
            }
        });
        closedRoadCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                anyDetailsEntered();
            }
        });
        trafficCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                anyDetailsEntered();
            }
        });
        otherIncidentCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                anyDetailsEntered();
            }
        });
        // EditText listeners
        reportIncidentEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                anyDetailsEntered();
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        // Buttons listeners
        reportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Report incident with text
                if (reportIncidentEditText.getText() != null && !reportIncidentEditText.getText().toString().equals("")) {
                    uploadData();
                    dismiss();
                } else {
                    Snackbar.make(view, R.string.empty_incident_text, Snackbar.LENGTH_SHORT).show();
                }
            }
        });
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Clear fields
                accidentCheckbox.setChecked(false);
                closedRoadCheckbox.setChecked(false);
                trafficCheckbox.setChecked(false);
                otherIncidentCheckbox.setChecked(false);
                reportIncidentEditText.setText("");
                reportIncidentEditText.clearFocus();
                reportButton.setEnabled(false);
                clearButton.setEnabled(false);
            }
        });
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Cancel reporting incident
                cancel();
            }
        });
        // Firebase instance
        firebaseInit();
    }

    private void anyDetailsEntered() {
        // Check if any details entered and unlock report and clear buttons
        boolean accident = accidentCheckbox.isChecked();
        boolean closedRoad = closedRoadCheckbox.isChecked();
        boolean traffic = trafficCheckbox.isChecked();
        boolean other = otherIncidentCheckbox.isChecked();
        boolean text = !reportIncidentEditText.getText().toString().equals("");
        reportButton.setEnabled((accident || closedRoad || traffic || other) && text);
        clearButton.setEnabled(accident || closedRoad || traffic || other || text);
    }

    private void firebaseInit() {
        // Firebase Authentication
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        // Firebase Realtime Database
        FirebaseDatabase mDatabase = FirebaseDatabase.getInstance("https://drive-on-mscsd-default-rtdb.europe-west1.firebasedatabase.app/");
        mDatabaseReference = mDatabase.getReference();
    }

    private void uploadData() {
        // Write location data of the current user to the database
        try {
            String details = reportIncidentEditText.getText().toString();
            IncidentHelper incidentHelper = new IncidentHelper(mUser.getUid(), details, location.getLatitude(), location.getLongitude(), location.getSpeed(), location.getAltitude(), location.getBearing());
            if (accidentCheckbox.isChecked()) {
                mDatabaseReference.child("Reports").child("Accidents").child(String.valueOf(incidentHelper.getTimestamp())).setValue(incidentHelper).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.i("FirebaseDatabase", location.getTime() + " Uploading location data succeed");
                        } else {
                            Toast.makeText(context, context.getString(R.string.database_update_failed), Toast.LENGTH_SHORT).show();
                            Log.w("FirebaseDatabase", location.getTime() + " Uploading location data failed", task.getException());
                        }
                    }
                });
            }
            if (closedRoadCheckbox.isChecked()) {
                mDatabaseReference.child("Reports").child("ClosedRoads").child(String.valueOf(incidentHelper.getTimestamp())).setValue(incidentHelper).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.i("FirebaseDatabase", location.getTime() + " Uploading location data succeed");
                        } else {
                            Toast.makeText(context, context.getString(R.string.database_update_failed), Toast.LENGTH_SHORT).show();
                            Log.w("FirebaseDatabase", location.getTime() + " Uploading location data failed", task.getException());
                        }
                    }
                });
            }
            if (trafficCheckbox.isChecked()) {
                mDatabaseReference.child("Reports").child("Traffic").child(String.valueOf(incidentHelper.getTimestamp())).setValue(incidentHelper).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.i("FirebaseDatabase", location.getTime() + " Uploading location data succeed");
                        } else {
                            Toast.makeText(context, context.getString(R.string.database_update_failed), Toast.LENGTH_SHORT).show();
                            Log.w("FirebaseDatabase", location.getTime() + " Uploading location data failed", task.getException());
                        }
                    }
                });
            }
            if (otherIncidentCheckbox.isChecked()) {
                mDatabaseReference.child("Reports").child("Other").child(String.valueOf(incidentHelper.getTimestamp())).setValue(incidentHelper).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            // Toast.makeText(context, context.getString(R.string.success), Toast.LENGTH_SHORT).show();
                            Log.i("FirebaseDatabase", location.getTime() + " Uploading location data succeed");
                        } else {
                            Toast.makeText(context, context.getString(R.string.database_update_failed), Toast.LENGTH_SHORT).show();
                            Log.w("FirebaseDatabase", location.getTime() + " Uploading location data failed", task.getException());
                        }
                    }
                });
            }
            // Update user incidents data
            mDatabaseReference.child("Users").child(mUser.getUid()).child("incidents").setValue(ServerValue.increment(1))
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Log.i("FirebaseDatabase", "Update user's incidents data on database succeed");
                            } else {
                                Toast.makeText(context, context.getString(R.string.database_update_failed) + System.lineSeparator() + Objects.requireNonNull(task.getException()).getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                                Log.w("FirebaseDatabase", "Update user's acceleration data on database failed", task.getException());
                            }
                        }
                    });
        } catch (Exception e) {
            Log.e("FirebaseDatabase", "Update user data exception", e);
        }
    }
}