package com.msiganos.driveon;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.BlendModeColorFilterCompat;
import androidx.core.graphics.BlendModeCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.msiganos.driveon.helpers.SystemHelper;
import com.msiganos.driveon.helpers.UserHelper;

import java.util.Objects;

public class RegisterActivity extends AppCompatActivity {

    private final int REQUIRED_LENGTH = 6;
    private final int MAXIMUM_LENGTH = 9;
    private final boolean REQUIRE_SPECIAL_CHARACTERS = true;
    private final boolean REQUIRE_DIGITS = true;
    private final boolean REQUIRE_LOWER_CASE = true;
    private final boolean REQUIRE_UPPER_CASE = true;
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private DatabaseReference mDatabaseReference;
    private TextInputLayout nameTextInputLayout, surnameTextInputLayout, nicknameTextInputLayout, profilePhotoUriTextInputLayout, emailTextInputLayout, passwordTextInputLayout, passwordAgainTextInputLayout;
    private EditText nameEditText, surnameEditText, nicknameEditText, profilePhotoUriEditText, emailEditText, passwordEditText, passwordAgainEditText;
    private ProgressBar progressBar, passwordSecurityProgressBar;
    private AppCompatCheckBox termsCheckBox;
    private Drawable progressDrawable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        systemInit();
        firebaseInit();
        layoutInit();
    }

    private void systemInit() {
        // Set SystemHelper
        SystemHelper mSystem = new SystemHelper(this);
        // Get network condition
        mSystem.getNetworkConnection();
    }

    private void firebaseInit() {
        // Firebase Authentication
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        // Firebase Realtime Database
        FirebaseDatabase mDatabase = FirebaseDatabase.getInstance("https://drive-on-mscsd-default-rtdb.europe-west1.firebasedatabase.app/");
        mDatabaseReference = mDatabase.getReference();
    }

    private void layoutInit() {
        // TextInputLayout
        nameTextInputLayout = findViewById(R.id.nameRegisterTextInputLayout);
        surnameTextInputLayout = findViewById(R.id.surnameRegisterTextInputLayout);
        nicknameTextInputLayout = findViewById(R.id.nicknameRegisterTextInputLayout);
        profilePhotoUriTextInputLayout = findViewById(R.id.profilePhotoUriRegisterTextInputLayout);
        emailTextInputLayout = findViewById(R.id.emailRegisterTextInputLayout);
        passwordTextInputLayout = findViewById(R.id.passwordRegisterTextInputLayout);
        passwordAgainTextInputLayout = findViewById(R.id.passwordAgainRegisterTextInputLayout);
        // EditText
        nameEditText = findViewById(R.id.nameRegisterEditText);
        surnameEditText = findViewById(R.id.surnameRegisterEditText);
        nicknameEditText = findViewById(R.id.nicknameRegisterEditText);
        profilePhotoUriEditText = findViewById(R.id.profilePhotoUriRegisterEditText);
        emailEditText = findViewById(R.id.emailRegisterEditText);
        passwordEditText = findViewById(R.id.passwordRegisterEditText);
        passwordAgainEditText = findViewById(R.id.passwordAgainRegisterEditText);
        // ProgressBars & Checkbox
        progressBar = findViewById(R.id.registerProgressBar);
        termsCheckBox = findViewById(R.id.agreeWithTerms);
        passwordSecurityProgressBar = findViewById(R.id.passwordSecurityProgressBar);
        progressDrawable = passwordSecurityProgressBar.getProgressDrawable().mutate();
        passwordSecurityProgressBar.setMax(100);
        passwordSecurityProgressBar.setProgress(0);
        progressDrawable.setColorFilter(BlendModeColorFilterCompat.createBlendModeColorFilterCompat(ResourcesCompat.getColor(getResources(), R.color.gray, getTheme()), BlendModeCompat.SRC_IN));
        passwordSecurityProgressBar.setProgressDrawable(progressDrawable);
        // Get username from parent intent & set it in UI
        String emailFromIntent = getIntent().getStringExtra("emailFromIntent");
        emailEditText.setText(emailFromIntent);
        // Set password policy
        passwordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                int currentScore = 0;
                boolean sawUpper = false;
                boolean sawLower = false;
                boolean sawDigit = false;
                boolean sawSpecial = false;
                for (int i = 0; i < charSequence.length(); i++) {
                    char c = charSequence.charAt(i);
                    if (!sawSpecial && !Character.isLetterOrDigit(c)) {
                        currentScore += 1;
                        sawSpecial = true;
                    } else {
                        if (!sawDigit && Character.isDigit(c)) {
                            currentScore += 1;
                            sawDigit = true;
                        } else {
                            if (!sawUpper || !sawLower) {
                                if (Character.isUpperCase(c))
                                    sawUpper = true;
                                else
                                    sawLower = true;
                                if (sawUpper && sawLower)
                                    currentScore += 1;
                            }
                        }
                    }
                }
                if (charSequence.length() > REQUIRED_LENGTH) {
                    if ((REQUIRE_SPECIAL_CHARACTERS && !sawSpecial) || (REQUIRE_UPPER_CASE && !sawUpper) || (REQUIRE_LOWER_CASE && !sawLower) || (REQUIRE_DIGITS && !sawDigit)) {
                        currentScore = 1;
                    } else {
                        currentScore = 2;
                        if (charSequence.length() > MAXIMUM_LENGTH) {
                            currentScore = 3;
                        }
                    }
                } else {
                    currentScore = 0;
                }
                changePasswordSecurityProgressBar(currentScore);
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        // View terms
        Snackbar.make(termsCheckBox.getRootView(), R.string.terms, Snackbar.LENGTH_LONG)
                .setAction(R.string.check_terms, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        viewTerms();
                    }
                })
                .show();
    }

    private void changePasswordSecurityProgressBar(int security) {
        try {
            switch (security) {
                case 0:
                    // Weak password
                    Log.i("PasswordPolicy", "Weak password");
                    ObjectAnimator.ofInt(passwordSecurityProgressBar, "progress", 0).setDuration(300).start();
                    progressDrawable.setColorFilter(BlendModeColorFilterCompat.createBlendModeColorFilterCompat(ResourcesCompat.getColor(getResources(), R.color.gray, getTheme()), BlendModeCompat.SRC_IN));
                    passwordSecurityProgressBar.setProgressDrawable(progressDrawable);
                    passwordTextInputLayout.setHelperText(getString(R.string.weak_password));
                    passwordTextInputLayout.setHelperTextEnabled(true);
                    break;
                case 1:
                    // Moderate password
                    Log.i("PasswordPolicy", "Moderate password");
                    ObjectAnimator.ofInt(passwordSecurityProgressBar, "progress", 33).setDuration(300).start();
                    progressDrawable.setColorFilter(BlendModeColorFilterCompat.createBlendModeColorFilterCompat(ResourcesCompat.getColor(getResources(), R.color.red, getTheme()), BlendModeCompat.SRC_IN));
                    passwordSecurityProgressBar.setProgressDrawable(progressDrawable);
                    passwordTextInputLayout.setHelperText(getString(R.string.moderate_password));
                    passwordTextInputLayout.setHelperTextEnabled(true);
                    break;
                case 2:
                    // Strong password
                    Log.i("PasswordPolicy", "Strong password");
                    ObjectAnimator.ofInt(passwordSecurityProgressBar, "progress", 66).setDuration(300).start();
                    progressDrawable.setColorFilter(BlendModeColorFilterCompat.createBlendModeColorFilterCompat(ResourcesCompat.getColor(getResources(), R.color.yellow, getTheme()), BlendModeCompat.SRC_IN));
                    passwordSecurityProgressBar.setProgressDrawable(progressDrawable);
                    passwordTextInputLayout.setHelperText(getString(R.string.strong_password));
                    passwordTextInputLayout.setHelperTextEnabled(true);
                    break;
                case 3:
                    // Very strong password
                    Log.i("PasswordPolicy", "Very strong password");
                    ObjectAnimator.ofInt(passwordSecurityProgressBar, "progress", 100).setDuration(300).start();
                    progressDrawable.setColorFilter(BlendModeColorFilterCompat.createBlendModeColorFilterCompat(ResourcesCompat.getColor(getResources(), R.color.green, getTheme()), BlendModeCompat.SRC_IN));
                    passwordSecurityProgressBar.setProgressDrawable(progressDrawable);
                    passwordTextInputLayout.setHelperText(getString(R.string.very_strong_password));
                    passwordTextInputLayout.setHelperTextEnabled(true);
                    break;
                default:
                    // Not accepted password
                    Log.i("PasswordPolicy", "Password policy error");
                    ObjectAnimator.ofInt(passwordSecurityProgressBar, "progress", 0).setDuration(300).start();
                    progressDrawable.setColorFilter(BlendModeColorFilterCompat.createBlendModeColorFilterCompat(ResourcesCompat.getColor(getResources(), R.color.gray, getTheme()), BlendModeCompat.SRC_IN));
                    passwordSecurityProgressBar.setProgressDrawable(progressDrawable);
                    passwordTextInputLayout.setHelperText(getString(R.string.required));
                    passwordTextInputLayout.setHelperTextEnabled(false);
            }
        } catch (Exception e) {
            Log.e("PasswordSecurity", "PasswordSecurityProgressBar", e);
        }
    }

    private void viewTerms() {
        String termsURL = getString(R.string.terms_url);
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(termsURL));
        startActivity(browserIntent);
    }

    public void register(View view) {
        // Check for empty fields
        if (TextUtils.isEmpty(nameEditText.getText().toString())) {
            nameTextInputLayout.setError(getString(R.string.empty_name));
            nameTextInputLayout.setErrorEnabled(true);
            return;
        } else {
            nameTextInputLayout.setErrorEnabled(false);
        }
        if (TextUtils.isEmpty(surnameEditText.getText().toString())) {
            surnameTextInputLayout.setError(getString(R.string.empty_surname));
            surnameTextInputLayout.setErrorEnabled(true);
            return;
        } else {
            surnameTextInputLayout.setErrorEnabled(false);
        }
        if (TextUtils.isEmpty(nicknameEditText.getText().toString())) {
            nicknameTextInputLayout.setError(getString(R.string.empty_nickname));
            nicknameTextInputLayout.setErrorEnabled(true);
            return;
        } else {
            nicknameTextInputLayout.setErrorEnabled(false);
        }
        if (TextUtils.isEmpty(profilePhotoUriEditText.getText().toString())) {
            profilePhotoUriTextInputLayout.setError(getString(R.string.empty_profile_photo_uri));
            profilePhotoUriTextInputLayout.setErrorEnabled(false);
        } else {
            profilePhotoUriTextInputLayout.setErrorEnabled(false);
        }
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
        if (TextUtils.isEmpty(passwordAgainEditText.getText().toString())) {
            passwordAgainTextInputLayout.setError(getString(R.string.empty_password_again));
            passwordAgainTextInputLayout.setErrorEnabled(true);
            return;
        } else {
            passwordAgainTextInputLayout.setErrorEnabled(false);
        }
        if (!termsCheckBox.isChecked()) {
            Snackbar.make(termsCheckBox.getRootView(), R.string.terms, Snackbar.LENGTH_SHORT)
                    .setAction(R.string.check_terms, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            viewTerms();
                        }
                    })
                    .show();
            return;
        }
        if (!passwordEditText.getText().toString().equals(passwordAgainEditText.getText().toString())) {
            Snackbar.make(termsCheckBox.getRootView(), R.string.password_mismatch, Snackbar.LENGTH_SHORT).show();
            passwordEditText.setText("");
            passwordAgainEditText.setText("");
            return;
        }
        // Show progressbar
        progressBar.setVisibility(View.VISIBLE);
        // Get data from UI
        String name = nameEditText.getText().toString();
        String surname = surnameEditText.getText().toString();
        String nickname = nicknameEditText.getText().toString();
        String profilePhotoUri = profilePhotoUriEditText.getText().toString();
        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();
        // Register with email & password
        try {
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                // Create user, change their data, hide progressbar & open the login page
                                mUser = mAuth.getCurrentUser();
                                if (mUser == null) {
                                    Toast.makeText(RegisterActivity.this, getString(R.string.registration_failed) + System.lineSeparator() + Objects.requireNonNull(task.getException()).getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                                    Log.w("FirebaseAuth", "Registration failed", task.getException());
                                    progressBar.setVisibility(View.INVISIBLE);
                                } else {
                                    Log.i("FirebaseAuth", "Registration succeed (uid: " + mUser.getUid() + ")");
                                    updateUserProfileData(mUser, name, surname, nickname, profilePhotoUri);
                                    progressBar.setVisibility(View.INVISIBLE);
                                    Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    finish();
                                }
                            } else {
                                Toast.makeText(RegisterActivity.this, getString(R.string.registration_failed) + System.lineSeparator() + Objects.requireNonNull(task.getException()).getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                                Log.w("FirebaseAuth", "Registration failed", task.getException());
                                progressBar.setVisibility(View.INVISIBLE);
                            }
                        }
                    });
        } catch (Exception e) {
            Log.e("FirebaseAuth", "Registration exception", e);
        }
    }

    private void updateUserProfileData(FirebaseUser mUser, String name, String surname, String nickname, String profilePhotoUri) {
        // Edit username & photo
        char firstLetterOfName = name.charAt(0);
        char firstLetterOfSurname = surname.charAt(0);
        String profilePictureUri = "https://eu.ui-avatars.com/api/?size=128&background=random&rounded=true&name=" + firstLetterOfName + "+" + firstLetterOfSurname + "/";
        if (!TextUtils.isEmpty(profilePhotoUri))
            profilePictureUri = profilePhotoUri;
        UserProfileChangeRequest profileChangeRequest = new UserProfileChangeRequest
                .Builder().setDisplayName(nickname).setPhotoUri(Uri.parse(profilePictureUri)).build();
        mUser.updateProfile(profileChangeRequest)
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.i("FirebaseAuth", "Update user profile succeed");
                        } else {
                            Toast.makeText(RegisterActivity.this, getString(R.string.profile_update_failed) + System.lineSeparator() + Objects.requireNonNull(task.getException()).getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                            Log.w("FirebaseAuth", "Update user profile error", task.getException());
                        }
                    }
                });
        // Get device info & country
        String device = Build.MANUFACTURER + " " + Build.DEVICE;
        String locales = getResources().getConfiguration().getLocales().toLanguageTags();
        // Create user object in firebase realtime
        UserHelper newUser = new UserHelper(mUser.getUid(), nickname, device, locales);
        // Write user object to the database
        mDatabaseReference.child("Users").child(mUser.getUid()).setValue(newUser)
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.i("FirebaseDatabase", "Update user profile database succeed", task.getException());
                        } else {
                            Toast.makeText(RegisterActivity.this, getString(R.string.database_update_failed) + System.lineSeparator() + Objects.requireNonNull(task.getException()).getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                            Log.w("FirebaseDatabase", "Update user profile database error", task.getException());
                        }
                    }
                });
    }
}