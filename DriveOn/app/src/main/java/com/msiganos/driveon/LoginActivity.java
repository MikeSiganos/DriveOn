package com.msiganos.driveon;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.msiganos.driveon.helpers.SystemHelper;

import java.util.Objects;

public class LoginActivity extends AppCompatActivity {

    private static final int STORAGE_PERMISSION_REQUEST_CODE = 801;
    private SystemHelper mSystem;
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private TextInputLayout emailTextInputLayout, passwordTextInputLayout;
    private EditText emailEditText, passwordEditText;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        systemInit();
        firebaseInit();
        layoutInit();
        // Get network condition & check for updates
        if (mSystem.getNetworkConnection())
            mSystem.checkForUpdates();
        // If user is already logged in redirect to main activity
        if (mUser != null) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
    }

    private void systemInit() {
        // Set SystemHelper
        mSystem = new SystemHelper(this);
    }

    private void firebaseInit() {
        // Firebase Authentication
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
    }

    private void layoutInit() {
        // TextInputLayout
        emailTextInputLayout = findViewById(R.id.emailLoginTextInputLayout);
        passwordTextInputLayout = findViewById(R.id.passwordLoginTextInputLayout);
        // EditText
        emailEditText = findViewById(R.id.emailLoginEditText);
        passwordEditText = findViewById(R.id.passwordLoginEditText);
        // ProgressBar
        progressBar = findViewById(R.id.loginProgressBar);
    }

    public void login(View view) {
        // Check for empty fields
        if (TextUtils.isEmpty(emailEditText.getText().toString()) ||
                !Patterns.EMAIL_ADDRESS.matcher(emailEditText.getText().toString()).matches()) {
            emailTextInputLayout.setError(getString(R.string.empty_mail));
            emailTextInputLayout.setErrorEnabled(true);
            return;
        } else {
            emailTextInputLayout.setErrorEnabled(false);
        }
        if (TextUtils.isEmpty(passwordEditText.getText().toString())) {
            passwordTextInputLayout.setError(getString(R.string.empty_password));
            passwordTextInputLayout.setErrorEnabled(true);
            return;
        } else {
            passwordTextInputLayout.setErrorEnabled(false);
        }
        // Show progressbar
        progressBar.setVisibility(View.VISIBLE);
        // Get data from UI
        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();
        // Sign in with email & password
        try {
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                // Get user, hide progressbar & open the main page
                                mUser = mAuth.getCurrentUser();
                                if (mUser == null) {
                                    Toast.makeText(LoginActivity.this, getString(R.string.login_failed) + System.lineSeparator() + Objects.requireNonNull(task.getException()).getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                                    Log.w("FirebaseAuth", "Login failed", task.getException());
                                    progressBar.setVisibility(View.INVISIBLE);
                                } else {
                                    // Login
                                    Log.i("FirebaseAuth", "Login succeed (uid: " + mUser.getUid() + ")");
                                    passwordEditText.setText("");
                                    progressBar.setVisibility(View.INVISIBLE);
                                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                }
                            } else {
                                Toast.makeText(LoginActivity.this, getString(R.string.login_failed) + System.lineSeparator() + Objects.requireNonNull(task.getException()).getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                                Log.w("FirebaseAuth", "Login failed", task.getException());
                                progressBar.setVisibility(View.INVISIBLE);
                            }
                        }
                    });
        } catch (Exception e) {
            Log.e("FirebaseAuth", "Login exception", e);
        }
    }

    public void register(View view) {
        // Go to register activity & prefill email
        passwordEditText.setText("");
        Intent intent = new Intent(this, RegisterActivity.class);
        intent.putExtra("emailFromIntent", emailEditText.getText().toString());
        startActivity(intent);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Actions after requesting permissions
        try {
            if (requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
                if (grantResults.length > 0) {
                    boolean accessFineLocationPermission = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean accessCoarseLocationPermission = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (accessFineLocationPermission && accessCoarseLocationPermission) {
                        // Storage permission already given - Check for updates
                        Log.i("Permissions", "Storage permissions already granted");
                        // Update app
                        if (mSystem.getNetworkConnection())
                            mSystem.checkForUpdates();
                    } else {
                        // Ask for storage permission again - Update unavailable until location permission granted
                        Log.w("Permissions", "Storage permissions not granted again");
                        mSystem.getPermissions(STORAGE_PERMISSION_REQUEST_CODE);
                    }
                } else {
                    // No grantResults for storage permissions
                    Log.w("Permissions", "Storage permissions with empty grantResults");
                }
            }
        } catch (Exception e) {
            // If request is cancelled the result arrays are empty
            Log.e("Permissions", "Permissions exception", e);
        }
    }
}