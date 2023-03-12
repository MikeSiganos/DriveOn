package com.msiganos.driveon;

import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.BlendModeColorFilterCompat;
import androidx.core.graphics.BlendModeCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.picasso.Picasso;

import java.util.Objects;

public class UserProfileDialog extends Dialog {

    private final Context context;
    private final int REQUIRED_LENGTH = 6;
    private final int MAXIMUM_LENGTH = 9;
    private final boolean REQUIRE_SPECIAL_CHARACTERS = true;
    private final boolean REQUIRE_DIGITS = true;
    private final boolean REQUIRE_LOWER_CASE = true;
    private final boolean REQUIRE_UPPER_CASE = true;
    private TextInputLayout nameTextInputLayout, surnameTextInputLayout, nicknameTextInputLayout, profilePhotoUriTextInputLayout, emailTextInputLayout, passwordTextInputLayout, passwordNewTextInputLayout, passwordAgainTextInputLayout;
    private EditText nameEditText, surnameEditText, nicknameEditText, profilePhotoUriEditText, emailEditText, passwordEditText, passwordNewEditText, passwordAgainEditText;
    private ProgressBar passwordSecurityProgressBar;
    private Drawable progressDrawable;
    private AppCompatCheckBox deleteAccountCheckBox, termsCheckBox;
    private FirebaseUser mUser;
    private DatabaseReference mDatabaseReference;
    private int checkboxDefaultTextColor, selectedMenu;

    public UserProfileDialog(@NonNull Context context) {
        super(context);
        this.context = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            // Dialog interface
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            setContentView(R.layout.dialog_user_profile);
            setCancelable(true);
            // ImageView
            ImageView profileImageView = findViewById(R.id.updateProfileImageView);
            // TextInputLayout
            nameTextInputLayout = findViewById(R.id.nameUpdateProfileTextInputLayout);
            nameTextInputLayout.setVisibility(View.GONE);
            surnameTextInputLayout = findViewById(R.id.surnameUpdateProfileTextInputLayout);
            surnameTextInputLayout.setVisibility(View.GONE);
            nicknameTextInputLayout = findViewById(R.id.nicknameUpdateProfileTextInputLayout);
            nicknameTextInputLayout.setVisibility(View.GONE);
            profilePhotoUriTextInputLayout = findViewById(R.id.profilePhotoUriUpdateProfileTextInputLayout);
            profilePhotoUriTextInputLayout.setVisibility(View.GONE);
            emailTextInputLayout = findViewById(R.id.emailUpdateProfileTextInputLayout);
            emailTextInputLayout.setVisibility(View.GONE);
            passwordTextInputLayout = findViewById(R.id.passwordUpdateProfileTextInputLayout);
            passwordTextInputLayout.setVisibility(View.GONE);
            passwordNewTextInputLayout = findViewById(R.id.passwordNewUpdateProfileTextInputLayout);
            passwordNewTextInputLayout.setVisibility(View.GONE);
            passwordAgainTextInputLayout = findViewById(R.id.passwordAgainUpdateProfileTextInputLayout);
            passwordAgainTextInputLayout.setVisibility(View.GONE);
            // EditText
            nameEditText = findViewById(R.id.nameUpdateProfileEditText);
            surnameEditText = findViewById(R.id.surnameUpdateProfileEditText);
            nicknameEditText = findViewById(R.id.nicknameUpdateProfileEditText);
            profilePhotoUriEditText = findViewById(R.id.profilePhotoUriUpdateProfileEditText);
            emailEditText = findViewById(R.id.emailUpdateProfileEditText);
            passwordEditText = findViewById(R.id.passwordUpdateProfileEditText);
            passwordNewEditText = findViewById(R.id.passwordNewUpdateProfileEditText);
            passwordAgainEditText = findViewById(R.id.passwordAgainUpdateProfileEditText);
            // ProgressBars & Checkboxes
            deleteAccountCheckBox = findViewById(R.id.deleteProfileCheckBox);
            deleteAccountCheckBox.setVisibility(View.GONE);
            termsCheckBox = findViewById(R.id.agreeWithTermsUpdateProfile);
            termsCheckBox.setChecked(true);
            checkboxDefaultTextColor = termsCheckBox.getCurrentTextColor();
            passwordSecurityProgressBar = findViewById(R.id.passwordSecurityUpdateProfileProgressBar);
            progressDrawable = passwordSecurityProgressBar.getProgressDrawable().mutate();
            passwordSecurityProgressBar.setMax(100);
            passwordSecurityProgressBar.setProgress(0);
            progressDrawable.setColorFilter(BlendModeColorFilterCompat.createBlendModeColorFilterCompat(ResourcesCompat.getColor(context.getResources(), R.color.gray, context.getTheme()), BlendModeCompat.SRC_IN));
            passwordSecurityProgressBar.setProgressDrawable(progressDrawable);
            passwordSecurityProgressBar.setVisibility(View.GONE);
            // Set password policy
            passwordNewEditText.addTextChangedListener(new TextWatcher() {
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
            // Set selected menu
            selectedMenu = 0;
            // Dialog buttons
            Button updateNicknameButton = findViewById(R.id.nicknameUpdateProfileButton);
            Button updatePhotoButton = findViewById(R.id.photoUpdateProfileButton);
            Button updateMailButton = findViewById(R.id.mailUpdateProfileButton);
            Button updatePasswordButton = findViewById(R.id.passwordUpdateProfileButton);
            Button deleteAccountButton = findViewById(R.id.deleteProfileButton);
            Button saveButton = findViewById(R.id.saveUpdateProfileButton);
            Button clearButton = findViewById(R.id.clearUpdateProfileButton);
            Button cancelButton = findViewById(R.id.cancelUpdateProfileButton);
            // Buttons listeners
            updateNicknameButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Set selected menu
                    selectedMenu = 1;
                    // Prepare layout
                    clearFields();
                    nameTextInputLayout.setVisibility(View.GONE);
                    surnameTextInputLayout.setVisibility(View.GONE);
                    nicknameTextInputLayout.setVisibility(View.VISIBLE);
                    profilePhotoUriTextInputLayout.setVisibility(View.GONE);
                    emailTextInputLayout.setVisibility(View.GONE);
                    passwordTextInputLayout.setVisibility(View.GONE);
                    passwordNewTextInputLayout.setVisibility(View.GONE);
                    passwordSecurityProgressBar.setVisibility(View.GONE);
                    passwordAgainTextInputLayout.setVisibility(View.GONE);
                    deleteAccountCheckBox.setVisibility(View.GONE);
                    termsCheckBox.setVisibility(View.GONE);
                    // Fill saved nickname
                    nicknameEditText.setText(mUser.getDisplayName());
                }
            });
            updatePhotoButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Set selected menu
                    selectedMenu = 2;
                    // Prepare layout
                    clearFields();
                    nameTextInputLayout.setVisibility(View.VISIBLE);
                    surnameTextInputLayout.setVisibility(View.VISIBLE);
                    nicknameTextInputLayout.setVisibility(View.GONE);
                    profilePhotoUriTextInputLayout.setVisibility(View.VISIBLE);
                    emailTextInputLayout.setVisibility(View.GONE);
                    passwordTextInputLayout.setVisibility(View.GONE);
                    passwordNewTextInputLayout.setVisibility(View.GONE);
                    passwordSecurityProgressBar.setVisibility(View.GONE);
                    passwordAgainTextInputLayout.setVisibility(View.GONE);
                    deleteAccountCheckBox.setVisibility(View.GONE);
                    termsCheckBox.setVisibility(View.GONE);
                    // Fill saved profile photo URL
                    profilePhotoUriEditText.setText(Objects.requireNonNull(mUser.getPhotoUrl()).toString());
                }
            });
            updateMailButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Set selected menu
                    selectedMenu = 3;
                    // Prepare layout
                    clearFields();
                    nameTextInputLayout.setVisibility(View.GONE);
                    surnameTextInputLayout.setVisibility(View.GONE);
                    nicknameTextInputLayout.setVisibility(View.GONE);
                    profilePhotoUriTextInputLayout.setVisibility(View.GONE);
                    emailTextInputLayout.setVisibility(View.VISIBLE);
                    passwordTextInputLayout.setVisibility(View.VISIBLE);
                    passwordNewTextInputLayout.setVisibility(View.GONE);
                    passwordSecurityProgressBar.setVisibility(View.GONE);
                    passwordAgainTextInputLayout.setVisibility(View.GONE);
                    deleteAccountCheckBox.setVisibility(View.GONE);
                    termsCheckBox.setVisibility(View.GONE);
                    // Fill saved email address
                    emailEditText.setText(mUser.getEmail());
                }
            });
            updatePasswordButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Set selected menu
                    selectedMenu = 4;
                    // Prepare layout
                    clearFields();
                    nameTextInputLayout.setVisibility(View.GONE);
                    surnameTextInputLayout.setVisibility(View.GONE);
                    nicknameTextInputLayout.setVisibility(View.GONE);
                    profilePhotoUriTextInputLayout.setVisibility(View.GONE);
                    emailTextInputLayout.setVisibility(View.GONE);
                    passwordTextInputLayout.setVisibility(View.VISIBLE);
                    passwordNewTextInputLayout.setVisibility(View.VISIBLE);
                    passwordSecurityProgressBar.setVisibility(View.VISIBLE);
                    passwordAgainTextInputLayout.setVisibility(View.VISIBLE);
                    deleteAccountCheckBox.setVisibility(View.GONE);
                    termsCheckBox.setVisibility(View.GONE);
                }
            });
            deleteAccountButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Set selected menu
                    selectedMenu = 5;
                    // Prepare layout
                    clearFields();
                    nameTextInputLayout.setVisibility(View.GONE);
                    surnameTextInputLayout.setVisibility(View.GONE);
                    nicknameTextInputLayout.setVisibility(View.GONE);
                    profilePhotoUriTextInputLayout.setVisibility(View.GONE);
                    emailTextInputLayout.setVisibility(View.VISIBLE);
                    passwordTextInputLayout.setVisibility(View.VISIBLE);
                    passwordNewTextInputLayout.setVisibility(View.GONE);
                    passwordSecurityProgressBar.setVisibility(View.GONE);
                    passwordAgainTextInputLayout.setVisibility(View.GONE);
                    deleteAccountCheckBox.setVisibility(View.VISIBLE);
                    termsCheckBox.setVisibility(View.GONE);
                }
            });
            saveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Save profile changes
                    uploadData();
                }
            });
            clearButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Set selected menu & clear fields
                    selectedMenu = 0;
                    uploadData();
                }
            });
            cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Set selected menu & cancel update profile
                    selectedMenu = 0;
                    cancel();
                }
            });
            // Checkbox listeners
            deleteAccountCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                    if (checked) {
                        deleteAccountCheckBox.setTextColor(checkboxDefaultTextColor);
                        Snackbar.make(deleteAccountCheckBox.getRootView(), R.string.close_account, Snackbar.LENGTH_SHORT).show();
                    }
                }
            });
            termsCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                    if (checked) {
                        termsCheckBox.setTextColor(checkboxDefaultTextColor);
                    }
                    viewTerms();
                }
            });
            // Firebase instance
            firebaseInit();
            // View terms
            viewTerms();
            // Set profile picture
            String profilePhotoUrl = Objects.requireNonNull(mUser.getPhotoUrl()).toString();
            Picasso.get().load(profilePhotoUrl).into(profileImageView);
        } catch (Exception e) {
            Log.e("DialogLayout", "Preparing dialog layout & Get user's profile data exception", e);
        }
    }

    private void changePasswordSecurityProgressBar(int security) {
        try {
            switch (security) {
                case 0:
                    // Weak password
                    Log.i("PasswordPolicy", "Weak password");
                    ObjectAnimator.ofInt(passwordSecurityProgressBar, "progress", 0).setDuration(300).start();
                    progressDrawable.setColorFilter(BlendModeColorFilterCompat.createBlendModeColorFilterCompat(ResourcesCompat.getColor(context.getResources(), R.color.gray, context.getTheme()), BlendModeCompat.SRC_IN));
                    passwordSecurityProgressBar.setProgressDrawable(progressDrawable);
                    passwordNewTextInputLayout.setHelperText(context.getString(R.string.weak_password));
                    passwordNewTextInputLayout.setHelperTextEnabled(true);
                    passwordAgainTextInputLayout.setErrorEnabled(false);
                    passwordAgainTextInputLayout.setHelperText(context.getString(R.string.required));
                    passwordAgainTextInputLayout.setHelperTextEnabled(true);
                    break;
                case 1:
                    // Moderate password
                    Log.i("PasswordPolicy", "Moderate password");
                    ObjectAnimator.ofInt(passwordSecurityProgressBar, "progress", 33).setDuration(300).start();
                    progressDrawable.setColorFilter(BlendModeColorFilterCompat.createBlendModeColorFilterCompat(ResourcesCompat.getColor(context.getResources(), R.color.red, context.getTheme()), BlendModeCompat.SRC_IN));
                    passwordSecurityProgressBar.setProgressDrawable(progressDrawable);
                    passwordNewTextInputLayout.setHelperText(context.getString(R.string.moderate_password));
                    passwordNewTextInputLayout.setHelperTextEnabled(true);
                    passwordAgainTextInputLayout.setErrorEnabled(false);
                    passwordAgainTextInputLayout.setHelperText(context.getString(R.string.required));
                    passwordAgainTextInputLayout.setHelperTextEnabled(true);
                    break;
                case 2:
                    // Strong password
                    Log.i("PasswordPolicy", "Strong password");
                    ObjectAnimator.ofInt(passwordSecurityProgressBar, "progress", 66).setDuration(300).start();
                    progressDrawable.setColorFilter(BlendModeColorFilterCompat.createBlendModeColorFilterCompat(ResourcesCompat.getColor(context.getResources(), R.color.yellow, context.getTheme()), BlendModeCompat.SRC_IN));
                    passwordSecurityProgressBar.setProgressDrawable(progressDrawable);
                    passwordNewTextInputLayout.setHelperText(context.getString(R.string.strong_password));
                    passwordNewTextInputLayout.setHelperTextEnabled(true);
                    passwordAgainTextInputLayout.setErrorEnabled(false);
                    passwordAgainTextInputLayout.setHelperText(context.getString(R.string.required));
                    passwordAgainTextInputLayout.setHelperTextEnabled(true);
                    break;
                case 3:
                    // Very strong password
                    Log.i("PasswordPolicy", "Very strong password");
                    ObjectAnimator.ofInt(passwordSecurityProgressBar, "progress", 100).setDuration(300).start();
                    progressDrawable.setColorFilter(BlendModeColorFilterCompat.createBlendModeColorFilterCompat(ResourcesCompat.getColor(context.getResources(), R.color.green, context.getTheme()), BlendModeCompat.SRC_IN));
                    passwordSecurityProgressBar.setProgressDrawable(progressDrawable);
                    passwordNewTextInputLayout.setHelperText(context.getString(R.string.very_strong_password));
                    passwordNewTextInputLayout.setHelperTextEnabled(true);
                    passwordAgainTextInputLayout.setErrorEnabled(false);
                    passwordAgainTextInputLayout.setHelperText(context.getString(R.string.required));
                    passwordAgainTextInputLayout.setHelperTextEnabled(true);
                    break;
                default:
                    // Not accepted password
                    Log.i("PasswordPolicy", "Password policy error");
                    ObjectAnimator.ofInt(passwordSecurityProgressBar, "progress", 0).setDuration(300).start();
                    progressDrawable.setColorFilter(BlendModeColorFilterCompat.createBlendModeColorFilterCompat(ResourcesCompat.getColor(context.getResources(), R.color.gray, context.getTheme()), BlendModeCompat.SRC_IN));
                    passwordSecurityProgressBar.setProgressDrawable(progressDrawable);
                    passwordNewTextInputLayout.setHelperText(context.getString(R.string.required));
                    passwordNewTextInputLayout.setHelperTextEnabled(false);
                    passwordAgainTextInputLayout.setErrorEnabled(false);
                    passwordAgainTextInputLayout.setHelperText(context.getString(R.string.required));
                    passwordAgainTextInputLayout.setHelperTextEnabled(true);
            }
        } catch (Exception e) {
            Log.e("PasswordSecurity", "PasswordSecurityProgressBar", e);
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

    private void viewTerms() {
        String termsURL = context.getString(R.string.terms_url);
        Snackbar.make(termsCheckBox.getRootView(), R.string.terms, Snackbar.LENGTH_SHORT)
                .setAction(R.string.check_terms, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(termsURL));
                        context.startActivity(browserIntent);
                    }
                }).show();
    }

    private void updateProfileNickname(String nickname) {
        // Edit user nickname on Firebase Auth & Realtime Database
        if (TextUtils.isEmpty(nickname)) {
            nicknameTextInputLayout.setError(context.getString(R.string.empty_nickname));
            nicknameTextInputLayout.setErrorEnabled(true);
            return;
        } else {
            nicknameTextInputLayout.setErrorEnabled(false);
        }
        if (nickname.equals(mUser.getDisplayName())) {
            nicknameTextInputLayout.setError(context.getString(R.string.equal_nickname));
            nicknameTextInputLayout.setErrorEnabled(true);
            return;
        } else {
            nicknameTextInputLayout.setErrorEnabled(false);
        }
        UserProfileChangeRequest profileChangeRequest = new UserProfileChangeRequest
                .Builder().setDisplayName(nickname).build();
        mUser.updateProfile(profileChangeRequest)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.i("FirebaseAuth", "Update user nickname succeed");
                            mDatabaseReference.child("Users").child(mUser.getUid()).child("nickname").setValue(nickname)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                Log.i("FirebaseDatabase", "Update user nickname on database succeed", task.getException());
                                                clearFields();
                                                dismiss();
                                            } else {
                                                Toast.makeText(context, context.getString(R.string.database_update_failed) + System.lineSeparator() + Objects.requireNonNull(task.getException()).getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                                                Log.w("FirebaseDatabase", "Update user nickname on database error", task.getException());
                                            }
                                        }
                                    });
                        } else {
                            Toast.makeText(context, context.getString(R.string.profile_update_failed) + System.lineSeparator() + Objects.requireNonNull(task.getException()).getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                            Log.w("FirebaseAuth", "Update user nickname error", task.getException());
                        }
                    }
                });
    }

    private void updateProfilePhotoWithUrl(String profilePhotoUri) {
        // Edit user profile photo on Firebase Auth using photo uri
        if (TextUtils.isEmpty(profilePhotoUri)) {
            profilePhotoUriTextInputLayout.setError(context.getString(R.string.empty_profile_photo_uri));
            profilePhotoUriTextInputLayout.setErrorEnabled(true);
            return;
        } else {
            profilePhotoUriTextInputLayout.setErrorEnabled(false);
        }
        if (profilePhotoUri.equals(Objects.requireNonNull(mUser.getPhotoUrl()).toString())) {
            profilePhotoUriTextInputLayout.setError(context.getString(R.string.equal_profile_photo_uri));
            profilePhotoUriTextInputLayout.setErrorEnabled(true);
            return;
        } else {
            profilePhotoUriTextInputLayout.setErrorEnabled(false);
        }
        UserProfileChangeRequest profileChangeRequest = new UserProfileChangeRequest
                .Builder().setPhotoUri(Uri.parse(profilePhotoUri)).build();
        mUser.updateProfile(profileChangeRequest)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.i("FirebaseAuth", "Update user profile photo succeed");
                            clearFields();
                            dismiss();
                        } else {
                            Toast.makeText(context, context.getString(R.string.profile_update_failed) + System.lineSeparator() + Objects.requireNonNull(task.getException()).getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                            Log.w("FirebaseAuth", "Update user profile photo error", task.getException());
                        }
                    }
                });
    }

    private void updateProfilePhotoWithFullName(String name, String surname) {
        // Edit user profile photo on Firebase Auth using name and surname
        if (TextUtils.isEmpty(name)) {
            nameTextInputLayout.setError(context.getString(R.string.empty_name));
            nameTextInputLayout.setErrorEnabled(true);
            return;
        } else {
            nameTextInputLayout.setErrorEnabled(false);
        }
        if (TextUtils.isEmpty(surname)) {
            surnameTextInputLayout.setError(context.getString(R.string.empty_surname));
            surnameTextInputLayout.setErrorEnabled(true);
            return;
        } else {
            surnameTextInputLayout.setErrorEnabled(false);
        }
        char firstLetterOfName = name.charAt(0);
        char firstLetterOfSurname = surname.charAt(0);
        String profilePhotoUri = "https://eu.ui-avatars.com/api/?size=128&background=random&rounded=true&name=" + firstLetterOfName + "+" + firstLetterOfSurname + "/";
        if (profilePhotoUri.equals(Objects.requireNonNull(mUser.getPhotoUrl()).toString())) {
            profilePhotoUriTextInputLayout.setError(context.getString(R.string.equal_profile_photo_uri));
            profilePhotoUriTextInputLayout.setErrorEnabled(true);
            return;
        } else {
            profilePhotoUriTextInputLayout.setErrorEnabled(false);
        }
        UserProfileChangeRequest profileChangeRequest = new UserProfileChangeRequest
                .Builder().setPhotoUri(Uri.parse(profilePhotoUri)).build();
        mUser.updateProfile(profileChangeRequest)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.i("FirebaseAuth", "Update user profile photo succeed");
                            clearFields();
                            dismiss();
                        } else {
                            Toast.makeText(context, context.getString(R.string.profile_update_failed) + System.lineSeparator() + Objects.requireNonNull(task.getException()).getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                            Log.w("FirebaseAuth", "Update user profile photo error", task.getException());
                        }
                    }
                });
    }

    private void updateProfileMail(String email, String password) {
        // Re-authenticate user & Edit user profile email on Firebase Auth
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailTextInputLayout.setError(context.getString(R.string.empty_mail));
            emailTextInputLayout.setErrorEnabled(true);
            return;
        } else {
            emailTextInputLayout.setErrorEnabled(false);
        }
        if (email.equals(mUser.getEmail())) {
            emailTextInputLayout.setError(context.getString(R.string.equal_mail));
            emailTextInputLayout.setErrorEnabled(true);
            return;
        } else {
            emailTextInputLayout.setErrorEnabled(false);
        }
        if (TextUtils.isEmpty(password)) {
            passwordTextInputLayout.setError(context.getString(R.string.empty_password));
            passwordTextInputLayout.setErrorEnabled(true);
            return;
        } else {
            passwordTextInputLayout.setErrorEnabled(false);
        }
        AuthCredential credential = EmailAuthProvider.getCredential(Objects.requireNonNull(mUser.getEmail()), password);
        mUser.reauthenticate(credential)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.i("FirebaseAuth", "Update user re-authentication succeed");
                            mUser.updateEmail(email)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                Log.i("FirebaseAuth", "Update user profile email succeed");
                                                clearFields();
                                                dismiss();
                                            } else {
                                                Toast.makeText(context, context.getString(R.string.profile_update_failed) + System.lineSeparator() + Objects.requireNonNull(task.getException()).getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                                                Log.w("FirebaseAuth", "Update user profile email error", task.getException());
                                            }
                                        }
                                    });
                        } else {
                            Toast.makeText(context, context.getString(R.string.profile_update_failed) + System.lineSeparator() + Objects.requireNonNull(task.getException()).getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                            Log.w("FirebaseAuth", "Update user re-authentication error", task.getException());
                        }
                    }
                });
    }

    private void updateProfilePassword(String password, String newPassword, String newPasswordAgain) {
        // Re-authenticate user & Edit user profile password on Firebase Auth
        if (TextUtils.isEmpty(password)) {
            passwordTextInputLayout.setError(context.getString(R.string.empty_old_password));
            passwordTextInputLayout.setErrorEnabled(true);
            return;
        } else {
            passwordTextInputLayout.setErrorEnabled(false);
        }
        if (TextUtils.isEmpty(newPassword)) {
            passwordNewTextInputLayout.setError(context.getString(R.string.empty_new_password));
            passwordNewTextInputLayout.setErrorEnabled(true);
            return;
        } else {
            passwordNewTextInputLayout.setErrorEnabled(false);
        }
        if (password.equals(newPassword)) {
            passwordTextInputLayout.setError(context.getString(R.string.equal_password));
            passwordTextInputLayout.setErrorEnabled(true);
            passwordNewTextInputLayout.setError(context.getString(R.string.equal_password));
            passwordNewTextInputLayout.setErrorEnabled(true);
            return;
        } else {
            passwordTextInputLayout.setErrorEnabled(false);
            passwordNewTextInputLayout.setErrorEnabled(false);
        }
        if (TextUtils.isEmpty(newPasswordAgain)) {
            passwordAgainTextInputLayout.setError(context.getString(R.string.empty_password_again));
            passwordAgainTextInputLayout.setErrorEnabled(true);
            return;
        } else {
            passwordAgainTextInputLayout.setErrorEnabled(false);
        }
        if (!newPassword.equals(newPasswordAgain)) {
            passwordNewTextInputLayout.setHelperTextEnabled(false);
            passwordNewTextInputLayout.setError(context.getString(R.string.password_mismatch));
            passwordNewTextInputLayout.setErrorEnabled(true);
            passwordAgainTextInputLayout.setHelperTextEnabled(false);
            passwordAgainTextInputLayout.setError(context.getString(R.string.password_mismatch));
            passwordAgainTextInputLayout.setErrorEnabled(true);
            passwordAgainEditText.setText("");
            return;
        } else {
            passwordNewTextInputLayout.setErrorEnabled(false);
            passwordNewTextInputLayout.setHelperTextEnabled(true);
            passwordAgainTextInputLayout.setErrorEnabled(false);
            passwordAgainTextInputLayout.setHelperTextEnabled(true);
        }
        AuthCredential credential = EmailAuthProvider.getCredential(Objects.requireNonNull(mUser.getEmail()), password);
        mUser.reauthenticate(credential)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.i("FirebaseAuth", "Update user re-authentication succeed");
                            mUser.updatePassword(newPassword)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                Log.i("FirebaseAuth", "Update user profile password succeed");
                                                clearFields();
                                                dismiss();
                                            } else {
                                                Toast.makeText(context, context.getString(R.string.profile_update_failed) + System.lineSeparator() + Objects.requireNonNull(task.getException()).getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                                                Log.w("FirebaseAuth", "Update user profile password error", task.getException());
                                            }
                                        }
                                    });
                        } else {
                            Toast.makeText(context, context.getString(R.string.profile_update_failed) + System.lineSeparator() + Objects.requireNonNull(task.getException()).getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                            Log.w("FirebaseAuth", "Update user re-authentication error", task.getException());
                        }
                    }
                });
    }

    private void deleteProfile(String email, String password) {
        // Re-authenticate user & Edit user profile email on Firebase Auth
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailTextInputLayout.setError(context.getString(R.string.empty_mail));
            emailTextInputLayout.setErrorEnabled(true);
            return;
        } else {
            emailTextInputLayout.setErrorEnabled(false);
        }
        if (TextUtils.isEmpty(password)) {
            passwordTextInputLayout.setError(context.getString(R.string.empty_password));
            passwordTextInputLayout.setErrorEnabled(true);
            return;
        } else {
            passwordTextInputLayout.setErrorEnabled(false);
        }
        if (!deleteAccountCheckBox.isChecked()) {
            deleteAccountCheckBox.setTextColor(ResourcesCompat.getColor(context.getResources(), R.color.red, context.getTheme()));
            return;
        } else {
            termsCheckBox.setTextColor(checkboxDefaultTextColor);
        }
        AuthCredential credential = EmailAuthProvider.getCredential(Objects.requireNonNull(email), password);
        mUser.reauthenticate(credential)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.i("FirebaseAuth", "Update user re-authentication succeed");
                            // Delete data from firebase realtime database
                            mDatabaseReference.child("Users").child(mUser.getUid()).removeValue()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                Toast.makeText(context, context.getString(R.string.delete_profile_data_succeed), Toast.LENGTH_SHORT).show();
                                                Log.i("FirebaseDatabase", "Delete user profile data succeed");
                                            } else {
                                                Toast.makeText(context, context.getString(R.string.profile_update_failed) + System.lineSeparator() + Objects.requireNonNull(task.getException()).getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                                                Log.w("FirebaseDatabase", "Delete user profile data error", task.getException());
                                            }
                                        }
                                    });
                            mDatabaseReference.child("Locations").child(mUser.getUid()).removeValue()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                Toast.makeText(context, context.getString(R.string.delete_location_data_succeed), Toast.LENGTH_SHORT).show();
                                                Log.i("FirebaseDatabase", "Delete user location data succeed");
                                            } else {
                                                Toast.makeText(context, context.getString(R.string.profile_update_failed) + System.lineSeparator() + Objects.requireNonNull(task.getException()).getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                                                Log.w("FirebaseDatabase", "Delete user location data error", task.getException());
                                            }
                                        }
                                    });
                            mDatabaseReference.child("History").child(mUser.getUid()).removeValue()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                Toast.makeText(context, context.getString(R.string.delete_history_data_succeed), Toast.LENGTH_SHORT).show();
                                                Log.i("FirebaseDatabase", "Delete user history data succeed");
                                            } else {
                                                Toast.makeText(context, context.getString(R.string.profile_update_failed) + System.lineSeparator() + Objects.requireNonNull(task.getException()).getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                                                Log.w("FirebaseDatabase", "Delete user history data error", task.getException());
                                            }
                                        }
                                    });
                            // Delete data from firebase authentication
                            mUser.delete()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                Toast.makeText(context, context.getString(R.string.delete_account_succeed), Toast.LENGTH_SHORT).show();
                                                Log.i("FirebaseAuth", "Delete user account succeed");
                                                clearFields();
                                                dismiss();
                                                Intent intent = new Intent(context, LoginActivity.class);
                                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                context.startActivity(intent);
                                            } else {
                                                Toast.makeText(context, context.getString(R.string.profile_update_failed) + System.lineSeparator() + Objects.requireNonNull(task.getException()).getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                                                Log.w("FirebaseAuth", "Delete user account error", task.getException());
                                            }
                                        }
                                    });
                        } else {
                            Toast.makeText(context, context.getString(R.string.profile_update_failed) + System.lineSeparator() + Objects.requireNonNull(task.getException()).getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                            Log.w("FirebaseAuth", "Update user re-authentication error", task.getException());
                        }
                    }
                });
    }

    private void uploadData() {
        // Write updated profile data of the current user to the database
        try {
            switch (selectedMenu) {
                case 1:
                    updateProfileNickname(nicknameEditText.getText().toString());
                    break;
                case 2:
                    if (!TextUtils.isEmpty(nameEditText.getText().toString()) || !TextUtils.isEmpty(surnameEditText.getText().toString())) {
                        updateProfilePhotoWithFullName(nameEditText.getText().toString(), surnameEditText.getText().toString());
                    } else {
                        updateProfilePhotoWithUrl(profilePhotoUriEditText.getText().toString());
                    }
                    break;
                case 3:
                    updateProfileMail(emailEditText.getText().toString(), passwordEditText.getText().toString());
                    break;
                case 4:
                    updateProfilePassword(passwordEditText.getText().toString(), passwordNewEditText.getText().toString(), passwordAgainEditText.getText().toString());
                    break;
                case 5:
                    deleteProfile(emailEditText.getText().toString(), passwordEditText.getText().toString());
                    break;
                default:
                    clearFields();
                    nameTextInputLayout.setVisibility(View.GONE);
                    surnameTextInputLayout.setVisibility(View.GONE);
                    nicknameTextInputLayout.setVisibility(View.GONE);
                    profilePhotoUriTextInputLayout.setVisibility(View.GONE);
                    emailTextInputLayout.setVisibility(View.GONE);
                    passwordTextInputLayout.setVisibility(View.GONE);
                    passwordNewTextInputLayout.setVisibility(View.GONE);
                    passwordSecurityProgressBar.setVisibility(View.GONE);
                    passwordAgainTextInputLayout.setVisibility(View.GONE);
                    deleteAccountCheckBox.setVisibility(View.GONE);
                    termsCheckBox.setVisibility(View.VISIBLE);
                    if (!termsCheckBox.isChecked()) {
                        termsCheckBox.setTextColor(ResourcesCompat.getColor(context.getResources(), R.color.red, context.getTheme()));
                        viewTerms();
                        return;
                    } else {
                        termsCheckBox.setTextColor(checkboxDefaultTextColor);
                    }
                    dismiss();
            }
        } catch (Exception e) {
            Log.e("FirebaseAuth", "Update user data exception", e);
        }
    }

    private void clearFields() {
        // Clear all fields and set them to defaults
        nameEditText.setText("");
        surnameEditText.setText("");
        nicknameEditText.setText("");
        profilePhotoUriEditText.setText("");
        emailEditText.setText("");
        passwordEditText.setText("");
        passwordNewEditText.setText("");
        passwordAgainEditText.setText("");
        deleteAccountCheckBox.setChecked(false);
        deleteAccountCheckBox.setTextColor(checkboxDefaultTextColor);
        termsCheckBox.setTextColor(checkboxDefaultTextColor);
    }
}