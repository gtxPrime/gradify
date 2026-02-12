package com.gxdevs.gradify.activities;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

    private static final String ALLOWED_DOMAIN = "ds.study.iitm.ac.in";
    private static final int MAX_SELECTED_SUBJECTS = 4;

    // UI Components
    private ImageView profileImageView, logOut;
    private TextView userNameTextView, profileMailTextView;
    private LinearLayout subjects_container;
    private View level_foundation, level_diploma, level_degree;
    private TextView tv_foundation, tv_diploma, tv_degree;
    private MaterialButton signInButton;
    private TextView loginPromptTextView, subjectCounterText;
    private View selectionTray;
    private TextView selectionCount;
    private LinearLayout selectedChipsContainer;
    private View logged_out_container;
    private FirebaseAuth auth;
    private CredentialManager credentialManager;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        Utils.setPad(findViewById(R.id.containerPro), "bottom", this);

        auth = FirebaseAuth.getInstance();
        credentialManager = CredentialManager.create(this);

        initializeViews();

        findViewById(R.id.top_nav).setPadding(20, 100, 20, 0);

        sharedPreferences = getSharedPreferences("UserPreferences", MODE_PRIVATE);

        setupLevelPicker();

        if (auth.getCurrentUser() == null) {
            clearUserPreferences();
        }
        signInButton.setOnClickListener(v -> startGoogleSignIn());
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
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = auth.getCurrentUser();
        updateUI(currentUser);
    }

    private void initializeViews() {
        profileImageView = findViewById(R.id.profilePic);
        userNameTextView = findViewById(R.id.profileName);
        profileMailTextView = findViewById(R.id.profileMail);
        subjects_container = findViewById(R.id.subjects_container);
        signInButton = findViewById(R.id.signInButton);
        loginPromptTextView = findViewById(R.id.loginPromptTextView);
        logOut = findViewById(R.id.signOutBtn);
        logged_out_container = findViewById(R.id.logged_out_container);

        level_foundation = findViewById(R.id.level_foundation);
        level_diploma = findViewById(R.id.level_diploma);
        level_degree = findViewById(R.id.level_degree);

        tv_foundation = (TextView) level_foundation;
        tv_diploma = (TextView) level_diploma;
        tv_degree = (TextView) level_degree;

        selectionTray = findViewById(R.id.selection_tray);
        selectionCount = findViewById(R.id.selection_count);
        selectedChipsContainer = findViewById(R.id.selected_chips_container);
        subjectCounterText = findViewById(R.id.subject_counter_text);
        MaterialButton saveBtn = findViewById(R.id.save_btn);

        userNameTextView.setOnLongClickListener(v -> {
            signInAnonymously();
            return true;
        });

        saveBtn.setOnClickListener(v -> {
            updateSelectedSubjects();
            Toast.makeText(this, "Preferences Saved!", Toast.LENGTH_SHORT).show();
            hideSelectionTray();
        });
    }

    private void setupLevelPicker() {
        level_foundation.setOnClickListener(v -> selectLevel("Foundation"));
        level_diploma.setOnClickListener(v -> selectLevel("Diploma"));
        level_degree.setOnClickListener(v -> selectLevel("Degree"));
    }

    private void selectLevel(String level) {
        if (auth.getCurrentUser() != null) {
            updateLevelUI(level);
            saveLevelPreference(level);
            updateSubjectChipsByLevel(level);
        }
    }

    private void updateLevelUI(String level) {
        View indicator = findViewById(R.id.tab_indicator_profile);
        ViewGroup container = findViewById(R.id.level_selector_container);

        tv_foundation.setTextColor(ContextCompat.getColor(this, R.color.unselectedLevelText));
        tv_diploma.setTextColor(ContextCompat.getColor(this, R.color.unselectedLevelText));
        tv_degree.setTextColor(ContextCompat.getColor(this, R.color.unselectedLevelText));

        // Set selected
        TextView selectedTV = null;
        if ("Foundation".equals(level))
            selectedTV = tv_foundation;
        else if ("Diploma".equals(level))
            selectedTV = tv_diploma;
        else if ("Degree".equals(level))
            selectedTV = tv_degree;

        if (selectedTV != null) {
            Utils.updateTabIndicator(indicator, selectedTV, container);
            selectedTV.setTextColor(ContextCompat.getColor(this, R.color.white));
        }
    }

    private void updateSubjectChipsByLevel(String level) {
        // Clear existing views
        subjects_container.removeAllViews();

        Map<String, List<String>> subjectsByLevel = Utils.getSubjectsByLevel();
        List<String> subjects = subjectsByLevel.get(level);

        if (subjects == null || subjects.isEmpty())
            return;

        Set<String> savedSubjects = sharedPreferences.getStringSet("selectedSubjects", new HashSet<>());

        // Group subjects by type
        Map<String, List<String>> groupedSubjects = new java.util.LinkedHashMap<>();
        for (String subject : subjects) {
            String type = Utils.getSubjectType(subject);
            if (!groupedSubjects.containsKey(type)) {
                groupedSubjects.put(type, new ArrayList<>());
            }
            groupedSubjects.get(type).add(subject);
        }

        // Create groups
        for (Map.Entry<String, List<String>> entry : groupedSubjects.entrySet()) {
            String type = entry.getKey();
            List<String> typeSubjects = entry.getValue();

            // Add Header
            TextView header = new TextView(this);
            header.setText(type.toUpperCase());
            header.setTextColor(ContextCompat.getColor(this, R.color.unselectedLevelText));
            header.setTextSize(12);
            header.setAllCaps(true);
            header.setLetterSpacing(0.1f);
            header.setPadding(0, 32, 0, 16);
            header.setCompoundDrawablesWithIntrinsicBounds(createDotDrawable(), null, null, null);
            header.setCompoundDrawablePadding(16);
            subjects_container.addView(header);

            // Add ChipGroup
            ChipGroup chipGroup = new ChipGroup(this);
            chipGroup.setChipSpacing(24);

            for (String subject : typeSubjects) {
                Chip chip = new Chip(this);
                chip.setText(subject);
                chip.setCheckable(true);
                chip.setCheckedIconVisible(true);
                chip.setCheckedIcon(ContextCompat.getDrawable(this, R.drawable.ic_check));

                styleChip(chip, savedSubjects.contains(subject));

                chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        if (getSelectedCount() > MAX_SELECTED_SUBJECTS) {
                            chip.setChecked(false);
                            Toast.makeText(this, "Max " + MAX_SELECTED_SUBJECTS + " subjects allowed",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            styleChip(chip, true);
                            updateSelectedSubjects();
                            refreshSelectionTray();
                        }
                    } else {
                        styleChip(chip, false);
                        updateSelectedSubjects();
                        refreshSelectionTray();
                    }
                });

                chipGroup.addView(chip);
            }
            subjects_container.addView(chipGroup);
        }
        refreshSelectionTray();
    }

    private Drawable createDotDrawable() {
        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        dot.setSize(12, 12);
        dot.setColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.heroCards)));
        return dot;
    }

    private void styleChip(Chip chip, boolean isSelected) {
        float density = getResources().getDisplayMetrics().density;
        if (isSelected) {
            chip.setChecked(true);
            chip.setChipBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.selectedChip)));
            chip.setTextColor(ContextCompat.getColor(this, R.color.heroCards));

            // Move tick to the right using closeIcon
            chip.setCloseIcon(ContextCompat.getDrawable(this, R.drawable.ic_check));
            chip.setCloseIconVisible(true);
            chip.setCloseIconTint(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.heroCards)));
            chip.setCheckedIconVisible(false); // Hide the default left tick

            chip.setChipStrokeWidth(0f);
        } else {
            chip.setChecked(false);
            chip.setChipBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.unselectedChip)));
            chip.setTextColor(ContextCompat.getColor(this, R.color.textIcons));
            chip.setChipStrokeColor(ColorStateList.valueOf(Color.parseColor("#E0E0E0")));
            chip.setChipStrokeWidth(density * 1.5f);

            chip.setCloseIconVisible(false);
            chip.setCheckedIconVisible(false);
        }
        chip.setShapeAppearanceModel(
                chip.getShapeAppearanceModel().toBuilder().setAllCornerSizes(density * 16).build());
        chip.setTypeface(ResourcesCompat.getFont(this, R.font.inter18regular));
        chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        chip.setChipMinHeight(density * 48);

        chip.setChipStartPadding(density * 16);
        chip.setChipEndPadding(density * 16);
        chip.setTextStartPadding(density * 4);
        chip.setTextEndPadding(density * 4);

        // Ensure clicking the tick (close icon) also unchecks the chip
        chip.setOnCloseIconClickListener(v -> chip.setChecked(false));
    }

    private int getSelectedCount() {
        int count = 0;
        for (int i = 0; i < subjects_container.getChildCount(); i++) {
            View v = subjects_container.getChildAt(i);
            if (v instanceof ChipGroup) {
                ChipGroup cg = (ChipGroup) v;
                for (int j = 0; j < cg.getChildCount(); j++) {
                    if (((Chip) cg.getChildAt(j)).isChecked())
                        count++;
                }
            }
        }
        return count;
    }

    private void refreshSelectionTray() {
        selectedChipsContainer.removeAllViews();
        int count = 0;

        for (int i = 0; i < subjects_container.getChildCount(); i++) {
            View v = subjects_container.getChildAt(i);
            if (v instanceof ChipGroup) {
                ChipGroup cg = (ChipGroup) v;
                for (int j = 0; j < cg.getChildCount(); j++) {
                    Chip mainChip = (Chip) cg.getChildAt(j);
                    if (mainChip.isChecked()) {
                        count++;
                        addTrayChip(mainChip.getText().toString(), mainChip);
                    }
                }
            }
        }

        selectionCount.setText(count + " / " + MAX_SELECTED_SUBJECTS);
        if (subjectCounterText != null) {
            subjectCounterText.setText(count + " / " + MAX_SELECTED_SUBJECTS + " SUBJECTS SELECTED");
        }

        hideSelectionTray();
    }

    private void addTrayChip(String text, Chip mainChip) {
        View chipView = getLayoutInflater().inflate(R.layout.tray_chip_item, selectedChipsContainer, false);
        TextView tv = chipView.findViewById(R.id.chip_text);
        tv.setText(text);
        chipView.findViewById(R.id.remove_chip).setOnClickListener(v -> mainChip.setChecked(false));
        selectedChipsContainer.addView(chipView);
    }

    private void showSelectionTray() {
        if (selectionTray.getVisibility() != VISIBLE) {
            selectionTray.setVisibility(VISIBLE);
            selectionTray.post(() -> {
                selectionTray.setTranslationY(selectionTray.getHeight());
                selectionTray.animate().translationY(0).setDuration(300).start();
            });
        }
    }

    private void hideSelectionTray() {
        if (selectionTray.getVisibility() == VISIBLE) {
            selectionTray.animate().translationY(selectionTray.getHeight() + 200).setDuration(300)
                    .withEndAction(() -> selectionTray.setVisibility(GONE)).start();
        }
    }

    private void updateSelectedSubjects() {
        Set<String> selectedSubjects = new HashSet<>();

        for (int i = 0; i < subjects_container.getChildCount(); i++) {
            View v = subjects_container.getChildAt(i);
            if (v instanceof ChipGroup) {
                ChipGroup cg = (ChipGroup) v;
                for (int j = 0; j < cg.getChildCount(); j++) {
                    Chip mainChip = (Chip) cg.getChildAt(j);
                    if (mainChip.isChecked()) {
                        selectedSubjects.add(mainChip.getText().toString());
                    }
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
                                Toast.makeText(ProfileActivity.this, "User canceled the login", Toast.LENGTH_SHORT)
                                        .show();
                            } else if (e instanceof NoCredentialException) {
                                Toast.makeText(ProfileActivity.this, "No credentials found", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(ProfileActivity.this, "Sign-in failed" + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
    }

    private void handleSignInResponse(GetCredentialResponse response) {
        if (response.getCredential() instanceof CustomCredential) {
            CustomCredential credential = (CustomCredential) response.getCredential();

            if (credential.getType().equals(GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)) {
                try {
                    GoogleIdTokenCredential googleIdTokenCredential = GoogleIdTokenCredential
                            .createFrom(credential.getData());

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
                        FirebaseUser user = auth.getCurrentUser();

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
                        showSignInError("Authentication Failed");
                        updateUI(null);
                    }
                });
    }

    private void updateUI(FirebaseUser user) {
        String seed = Utils.getAvatarSeed(this);
        if (user != null) {
            String detectedSeed = null;
            if (user.isAnonymous()) {
                detectedSeed = "tester";
            } else if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
                detectedSeed = user.getDisplayName();
            } else if (user.getEmail() != null) {
                detectedSeed = user.getEmail();
            }

            if (detectedSeed != null) {
                seed = detectedSeed;
            }
        }
        String diceBearUrl = "https://api.dicebear.com/7.x/notionists/png?seed=" + seed
                + "&backgroundColor=DDE89D";

        if (user != null) {
            // Load preferences only when logged in
            if (!user.isAnonymous()) {
                // User is signed in
                String displayName = user.getDisplayName();

                if (displayName != null && displayName.contains(" ")) {
                    String actualName = displayName.substring(displayName.indexOf(" ")).trim();
                    userNameTextView.setText(actualName);
                } else {
                    userNameTextView.setText(displayName);
                }

                String email = user.getEmail();
                if (email != null) {
                    profileMailTextView.setText(email);
                }

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
                    Glide.with(this)
                            .load(diceBearUrl)
                            .placeholder(R.drawable.ic_profile_placeholder)
                            .into(profileImageView);
                }

            } else {
                userNameTextView.setText("Tester");
                profileMailTextView.setText("tester@gradify.com");
                profileImageView.setVisibility(VISIBLE);
                Glide.with(this)
                        .load(diceBearUrl)
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .into(profileImageView);
            }
            logOut.setVisibility(VISIBLE);
            enableSelectionUI();
            if (logged_out_container != null) {
                logged_out_container.setVisibility(GONE);
            }
            loadSavedPreferences(); // Load preferences only when logged in
            signInButton.setVisibility(GONE);

        } else {
            // User is signed out
            userNameTextView.setText("Guest User");
            profileMailTextView.setText("Please log in");

            Glide.with(this)
                    .load(diceBearUrl)
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .into(profileImageView);

            profileImageView.setVisibility(VISIBLE);
            profileImageView.setOnLongClickListener(null);

            signInButton.setVisibility(VISIBLE);
            signInButton.setText(R.string.sign_in_with_google);
            signInButton.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_google));
            logOut.setVisibility(GONE);

            disableSelectionUI();
            if (logged_out_container != null) {
                logged_out_container.setVisibility(VISIBLE);
            }
            subjects_container.removeAllViews();
        }
    }

    private void signInAnonymously() {
        auth.signInAnonymously().addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Tester login successful!", Toast.LENGTH_SHORT).show();
                updateUI(auth.getCurrentUser());
            } else {
                Toast.makeText(this, "Tester login failed!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showSignInError(String errorMessage) {
        Snackbar.make(findViewById(R.id.containerPro), errorMessage, Snackbar.LENGTH_LONG).show();
    }

    private void enableSelectionUI() {
        level_foundation.setEnabled(true);
        level_diploma.setEnabled(true);
        level_degree.setEnabled(true);
        subjects_container.setVisibility(VISIBLE);
        findViewById(R.id.level_selector_container).setVisibility(VISIBLE);
        if (subjectCounterText != null)
            subjectCounterText.setVisibility(VISIBLE);

        if (loginPromptTextView != null) {
            loginPromptTextView.setVisibility(GONE);
        }
    }

    private void disableSelectionUI() {
        level_foundation.setEnabled(false);
        level_diploma.setEnabled(false);
        level_degree.setEnabled(false);
        subjects_container.setVisibility(GONE);
        findViewById(R.id.level_selector_container).setVisibility(GONE);
        if (subjectCounterText != null)
            subjectCounterText.setVisibility(GONE);
    }

    private void showLoginPrompt() {
        if (loginPromptTextView != null) {
            loginPromptTextView.setVisibility(View.VISIBLE);
            loginPromptTextView.setText("Please log in to select your study level and subjects");
        }
    }

    private void loadSavedPreferences() {
        Set<String> selectedSubjects = sharedPreferences.getStringSet("selectedSubjects", new HashSet<>());
        String savedLevel = sharedPreferences.getString("studyLevel", "Foundation");

        // If subjects are already selected, find which level they belong to
        if (!selectedSubjects.isEmpty()) {
            String detectedLevel = findLevelForSubjects(selectedSubjects);
            if (detectedLevel != null) {
                savedLevel = detectedLevel;
            }
        }

        updateLevelUI(savedLevel);
        updateSubjectChipsByLevel(savedLevel);
    }

    private String findLevelForSubjects(Set<String> selectedSubjects) {
        Map<String, List<String>> subjectsByLevel = Utils.getSubjectsByLevel();
        for (String subject : selectedSubjects) {
            for (Map.Entry<String, List<String>> entry : subjectsByLevel.entrySet()) {
                if (entry.getValue().contains(subject)) {
                    return entry.getKey();
                }
            }
        }
        return null;
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

        updateLevelUI("Foundation");
        updateSubjectChipsByLevel("Foundation");
    }
}