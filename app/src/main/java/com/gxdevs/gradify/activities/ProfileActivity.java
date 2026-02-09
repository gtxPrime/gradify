package com.gxdevs.gradify.activities;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialCancellationException;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.exceptions.NoCredentialException;

import com.bumptech.glide.Glide;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.gxdevs.gradify.R;
import com.gxdevs.gradify.Utils.Utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";
    private static final String ALLOWED_DOMAIN = "ds.study.iitm.ac.in";
    private static final int MAX_SELECTED_SUBJECTS = 4;

    // UI Components
    private ImageView profileImageView, logOut;
    private TextView userNameTextView, profileMailTextView;
    private ChipGroup subjects_chip_group;
    private NumberPicker levelPicker;
    private MaterialButton signInButton;
    private TextView loginPromptTextView;
    private TextView tvDegreeLevels;
    private TextView tvSubjects;
    private View pickerHolder;

    // Firebase Auth
    private FirebaseAuth auth;

    // Credential Manager
    private CredentialManager credentialManager;

    // SharedPreferences
    private SharedPreferences sharedPreferences;
    private final List<String> levelOptions = new ArrayList<>();

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable levelUpdateRunnable;
    private ImageView mainDecor, greDecor1, greDecor2, greDecor3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        Utils.setPad(findViewById(R.id.containerPro), "bottom", this);

        auth = FirebaseAuth.getInstance();
        credentialManager = CredentialManager.create(this);
        mainDecor = findViewById(R.id.profileHolder);
        greDecor1 = findViewById(R.id.profileHolder1);
        greDecor2 = findViewById(R.id.profileHolder2);
        greDecor3 = findViewById(R.id.profileHolder3);
        View picker_pill = findViewById(R.id.picker_pill);

        picker_pill.setBackground(Utils.setCardColor(this, 8));
        // Initialize UI components
        initializeViews();

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("UserPreferences", MODE_PRIVATE);

        loadCollegeData();
        setupLevelPicker();

        if (auth.getCurrentUser() == null) {
            clearUserPreferences();
        }
        signInButton.setOnClickListener(v -> startGoogleSignIn());
        Utils.buttonTint(this, signInButton);
        logOut.setOnClickListener(v -> {
            if (auth.getCurrentUser() != null) {
                auth.signOut();
                clearUserPreferences();
                updateUI(null);
            }
        });

        // Setup back button
        findViewById(R.id.backBtn).setOnClickListener(v -> finish());

        // Check if user is already signed in
        updateUI(auth.getCurrentUser());

        Utils.setTheme(this, mainDecor, greDecor1, greDecor2, greDecor3);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is signed in and update UI accordingly
        FirebaseUser currentUser = auth.getCurrentUser();
        updateUI(currentUser);
    }

    private void initializeViews() {
        levelPicker = findViewById(R.id.levelPicker);
        profileImageView = findViewById(R.id.profilePic);
        userNameTextView = findViewById(R.id.profileName);
        profileMailTextView = findViewById(R.id.profileMail);
        subjects_chip_group = findViewById(R.id.subjects_chip_group);
        signInButton = findViewById(R.id.signInButton);
        loginPromptTextView = findViewById(R.id.loginPromptTextView);
        logOut = findViewById(R.id.signOutBtn);
        tvDegreeLevels = findViewById(R.id.tv_degree_levels);
        tvSubjects = findViewById(R.id.tv_subjects);
        pickerHolder = findViewById(R.id.pickerHolder);

        userNameTextView.setOnLongClickListener(v -> {
            signInAnonymously();
            return true;
        });
    }

    private void loadCollegeData() {
        Map<String, List<String>> subjectsByLevel = Utils.getSubjectsByLevel();
        // Get actual levels
        List<String> actualLevels = new ArrayList<>(subjectsByLevel.keySet());

        // If empty, add defaults
        if (actualLevels.isEmpty()) {
            actualLevels.add("Foundation");
            actualLevels.add("Diploma");
            actualLevels.add("Degree");

            List<String> defaultSubjects = new ArrayList<>();
            defaultSubjects.add("Default Subject 1");
            defaultSubjects.add("Default Subject 2");

            subjectsByLevel.put("Foundation", defaultSubjects);
            subjectsByLevel.put("Diploma", defaultSubjects);
            subjectsByLevel.put("Degree", defaultSubjects);
        }

        // Now add padding and update levelOptions
        levelOptions.clear();
        levelOptions.addAll(actualLevels);
    }

    private void setupLevelPicker() {
        levelPicker.setMinValue(0);
        levelPicker.setMaxValue(levelOptions.size() - 1);
        levelPicker.setDisplayedValues(levelOptions.toArray(new String[0]));

        levelPicker.setOnValueChangedListener((picker, oldVal, newVal) -> {

            if (auth.getCurrentUser() != null) {
                // Cancel any previously scheduled update
                if (levelUpdateRunnable != null) {
                    handler.removeCallbacks(levelUpdateRunnable);
                }

                // Debounced UI update
                levelUpdateRunnable = () -> {
                    String selectedLevel = levelOptions.get(newVal);
                    saveLevelPreference(selectedLevel);
                    updateSubjectChips(newVal);
                };

                handler.postDelayed(levelUpdateRunnable, 300); // 300ms delay
            }
        });
    }

    private void updateSubjectChips(int levelIndex) {
        // Clear existing chips
        subjects_chip_group.removeAllViews();

        // Add new chips for the selected level
        if (levelIndex >= 0 && levelIndex < levelOptions.size()) {
            // Get subjects for the selected level from Utils
            String selectedLevel = levelOptions.get(levelIndex);
            Map<String, List<String>> subjectsByLevel = Utils.getSubjectsByLevel();
            List<String> subjects = subjectsByLevel.get(selectedLevel);

            if (subjects == null || subjects.isEmpty()) {
                return; // No subjects for this level
            }

            Set<String> savedSubjects = sharedPreferences.getStringSet("selectedSubjects", new HashSet<>());

            for (String subject : subjects) {
                Chip chip = new Chip(this);
                chip.setText(subject);
                chip.setCheckable(true);
                chip.setCheckedIconVisible(true);
                chip.setTextColor(ContextCompat.getColor(this, R.color.white));
                chip.setChipStrokeWidth(2f);
                chip.setChipStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.chip_border)));
                chip.setChipBackgroundColor(ColorStateList.valueOf(Utils.chipUnselected(this)));
                chip.setShapeAppearanceModel(chip.getShapeAppearanceModel().toBuilder().setAllCornerSizes(24f).build());
                chip.setTextSize(16);
                chip.setPadding(20, 10, 20, 10);
                chip.setChipMinHeight(90);
                chip.setTypeface(ResourcesCompat.getFont(this, R.font.inter18regular));

                // Check if this subject was previously selected
                if (savedSubjects.contains(subject)) {
                    chip.setChecked(true);
                    chip.setChipBackgroundColor(ColorStateList.valueOf(Utils.selected(this)));
                    chip.setChipIconTint(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white)));
                }

                // Set listener for this chip
                chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        // User is attempting to check this chip.
                        // Count how many chips are currently checked.
                        int selectedCount = 0;
                        for (int i = 0; i < subjects_chip_group.getChildCount(); i++) {
                            View v = subjects_chip_group.getChildAt(i);
                            if (v instanceof Chip && ((Chip) v).isChecked()) {
                                selectedCount++;
                            }
                        }

                        if (selectedCount > MAX_SELECTED_SUBJECTS) {
                            // The current chip was just checked, making the total > MAX_SELECTED_SUBJECTS.
                            // Revert its state and show a toast.
                            chip.setChecked(false); // This will trigger the listener again with isChecked = false.
                            Toast.makeText(ProfileActivity.this, "You can select max " + MAX_SELECTED_SUBJECTS + " subjects", Toast.LENGTH_SHORT).show();
                            // Do not update style or call updateSelectedSubjects here;
                            // the re-triggered event (for unchecking) will handle it.
                        } else {
                            // Selection is valid (<= MAX_SELECTED_SUBJECTS). Style as selected.
                            chip.setChipBackgroundColor(ColorStateList.valueOf(Utils.selected(this)));
                            chip.setChipIconTint(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white)));
                            updateSelectedSubjects(); // Persist the change
                        }
                    } else {
                        // Chip is being unchecked. Style as unselected.
                        chip.setChipBackgroundColor(ColorStateList.valueOf(Utils.chipUnselected(this)));
                        chip.setTextColor(ContextCompat.getColor(this, R.color.white));
                        updateSelectedSubjects(); // Persist the change
                    }
                });

                subjects_chip_group.addView(chip);
            }
        }
    }

    private void updateSelectedSubjects() {
        Set<String> selectedSubjects = new HashSet<>();

        // Collect all checked chip texts
        for (int i = 0; i < subjects_chip_group.getChildCount(); i++) {
            View view = subjects_chip_group.getChildAt(i);
            if (view instanceof Chip) {
                Chip chip = (Chip) view;
                if (chip.isChecked()) {
                    selectedSubjects.add(chip.getText().toString());
                }
            }
        }

        // Save selected subjects to SharedPreferences
        saveSubjectsPreference(selectedSubjects);
    }

    private void startGoogleSignIn() {
        // Create Google ID token request
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setServerClientId(getString(R.string.web_client_id))
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .setNonce(null) // Optional nonce
                .build();

        // Build the credential request
        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        // Request the credential
        credentialManager.getCredentialAsync(
                this,
                request,
                null,
                Executors.newSingleThreadExecutor(),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse getCredentialResponse) {
                        handleSignInResponse(getCredentialResponse);
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException e) {
                        runOnUiThread(() -> {
                            if (e instanceof GetCredentialCancellationException) {
                                Toast.makeText(ProfileActivity.this, "User canceled the login", Toast.LENGTH_SHORT).show();
                            } else if (e instanceof NoCredentialException) {
                                Toast.makeText(ProfileActivity.this, "No credentials found", Toast.LENGTH_SHORT).show();
                            } else {
                                Log.e("Auth", e.getMessage());
                                Toast.makeText(ProfileActivity.this, "Sign-in failed" + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
        );
    }

    private void handleSignInResponse(GetCredentialResponse response) {
        if (response.getCredential() instanceof CustomCredential) {
            CustomCredential credential = (CustomCredential) response.getCredential();

            if (credential.getType().equals(GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)) {
                try {
                    GoogleIdTokenCredential googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.getData());

                    // Get the Google ID token and email
                    String idToken = googleIdTokenCredential.getIdToken();
                    String email = googleIdTokenCredential.getId();

                    // Check if email domain is allowed
                    if (email.endsWith("@" + ALLOWED_DOMAIN)) {
                        // Authenticate with Firebase
                        firebaseAuthWithGoogle(idToken);
                    } else {
                        runOnUiThread(() -> {
                            showSignInError("Only IIT-M students are allowed");
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing Google ID token credential: " + e.getMessage(), e);
                    runOnUiThread(() -> {
                        showSignInError("Authentication error: " + e.getLocalizedMessage());
                    });
                }
            } else {
                runOnUiThread(() -> {
                    showSignInError("Unexpected credential type");
                });
            }
        } else {
            runOnUiThread(() -> {
                showSignInError("Unexpected credential response");
            });
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success
                        Log.d(TAG, "signInWithCredential:success");
                        FirebaseUser user = auth.getCurrentUser();

                        // Check if email domain is allowed
                        if (user != null && user.getEmail() != null && user.getEmail().endsWith("@" + ALLOWED_DOMAIN)) {
                            updateUI(user);
                        } else {
                            // Sign out if domain not allowed
                            Toast.makeText(ProfileActivity.this,
                                    "Only IIT-M Data Science students are allowed",
                                    Toast.LENGTH_SHORT).show();
                            auth.signOut();
                            updateUI(null);
                        }
                    } else {
                        // Sign in failed
                        Log.w(TAG, "signInWithCredential:failure", task.getException());
                        showSignInError("Authentication Failed");
                        updateUI(null);
                    }
                });
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            // Load preferences only when logged in
            if (!user.isAnonymous()) {
                // User is signed in
                String displayName = user.getDisplayName();

                // Extract actual name by removing roll number (format: "24F1000625 Garvit Sharma")
                if (displayName != null && displayName.contains(" ")) {
                    // Find the first space which separates roll number from name
                    String actualName = displayName.substring(displayName.indexOf(" ")).trim();
                    userNameTextView.setText(actualName);
                } else {
                    // Fallback if format is different
                    userNameTextView.setText(displayName);
                }

                // Format email (showing just domain part or full email as needed)
                String email = user.getEmail();
                if (email != null) {
                    profileMailTextView.setText(email);
                }

                // Load profile image using Glide
                profileImageView.setVisibility(VISIBLE);
                if (user.getPhotoUrl() != null) {
                    String imageUrl = user.getPhotoUrl().toString();
                    String imgX = imageUrl.replace("=s96-c", "=s500");

                    Glide.with(this)
                            .load(imgX)
                            .placeholder(R.drawable.ic_profile_placeholder)
                            .error(R.drawable.ic_profile_placeholder)
                            .into(profileImageView);
                } else {
                    profileImageView.setImageResource(R.drawable.ic_profile_placeholder);
                    profileImageView.setOnLongClickListener(null); // Remove listener if no URL
                }

                // Update sign in button to sign out
            } else {
                userNameTextView.setText("Tester");
                profileMailTextView.setText("tester@gradify.com");
                profileImageView.setVisibility(VISIBLE);
            }
            logOut.setVisibility(VISIBLE);
            enableSelectionUI();
            loginPromptTextView.setVisibility(GONE);
            loadSavedPreferences(); // Load preferences only when logged in
            signInButton.setVisibility(GONE);

        } else {
            // User is signed out
            userNameTextView.setText("Guest User"); // Changed to generic text
            profileMailTextView.setText("Please log in"); // Changed to generic text
            profileImageView.setImageResource(R.drawable.ic_profile_placeholder);
            profileImageView.setVisibility(VISIBLE); // Keep placeholder visible
            profileImageView.setOnLongClickListener(null); // Remove listener when logged out

            // Update sign in button
            signInButton.setVisibility(VISIBLE);
            signInButton.setText(R.string.sign_in_with_google);
            signInButton.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_google));
            logOut.setVisibility(GONE);

            disableSelectionUI();
            showLoginPrompt();
            subjects_chip_group.removeAllViews(); // Clear subject chips
            // clearUserPreferences(); // Moved to logout listener and initial check in onCreate
        }
    }

    private void signInAnonymously() {
        auth.signInAnonymously().addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInAnonymously:success");
                        Toast.makeText(this, "Tester login successful!", Toast.LENGTH_SHORT).show();
                        updateUI(auth.getCurrentUser());
                    } else {
                        Log.w(TAG, "signInAnonymously:failure", task.getException());
                        Toast.makeText(this, "Tester login failed!", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void showSignInError(String errorMessage) {
        Snackbar.make(findViewById(R.id.containerPro), errorMessage, Snackbar.LENGTH_LONG).show();
    }

    private void enableSelectionUI() {
        subjects_chip_group.setEnabled(true);
        levelPicker.setEnabled(true);
        subjects_chip_group.setVisibility(VISIBLE);
        pickerHolder.setVisibility(VISIBLE);
        tvDegreeLevels.setVisibility(VISIBLE);
        tvSubjects.setVisibility(VISIBLE);

        if (loginPromptTextView != null) {
            loginPromptTextView.setVisibility(GONE);
        }
    }

    private void disableSelectionUI() {
        subjects_chip_group.setEnabled(false);
        levelPicker.setEnabled(false);
        subjects_chip_group.setVisibility(GONE);
        pickerHolder.setVisibility(GONE);
        tvDegreeLevels.setVisibility(GONE);
        tvSubjects.setVisibility(GONE);
    }

    private void showLoginPrompt() {
        if (loginPromptTextView != null) {
            loginPromptTextView.setVisibility(View.VISIBLE);
            loginPromptTextView.setText("Please log in to select your study level and subjects");
        }
    }

    private void loadSavedPreferences() {
        // Load selected level
        String savedLevel = sharedPreferences.getString("studyLevel", null);
        if (savedLevel != null) {
            int levelIndex = levelOptions.indexOf(savedLevel);
            if (levelIndex >= 0) {
                levelPicker.setValue(levelIndex);
                updateSubjectChips(levelIndex);
            }
        } else if (!levelOptions.isEmpty()) {
            // If no level is saved, default to first level and update subjects
            updateSubjectChips(0);
        }
    }

    private void saveLevelPreference(String level) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("studyLevel", level);
        editor.apply();
    }

    private void saveSubjectsPreference(Set<String> subjects) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putStringSet("selectedSubjects", subjects);
        editor.apply();
    }

    private void clearUserPreferences() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("studyLevel");
        editor.remove("selectedSubjects");
        editor.apply();
        // Reset level picker to default if options are available
        if (!levelOptions.isEmpty()) {
            levelPicker.setValue(0);
            updateSubjectChips(0); // Also update chips for the default level
        }
    }
}