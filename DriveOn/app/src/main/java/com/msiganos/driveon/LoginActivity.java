package com.msiganos.driveon;

import android.content.Intent;
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
        // If user is already logged in redirect to main activity
        if (mUser != null) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        } else
            // Get network condition & Check for updates
            mSystem.getNetworkConnection();
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
}