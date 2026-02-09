package com.gxdevs.gradify.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.gxdevs.gradify.R;
import com.gxdevs.gradify.Utils.Utils;
import com.gxdevs.gradify.adapters.NotesAdapter;
import com.gxdevs.gradify.models.NoteItem;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotesActivity extends AppCompatActivity {

    private static final String TAG = "NotesActivity";

    private TextInputLayout subjectDropdownLayout;
    private MaterialAutoCompleteTextView levelAutocompleteTextView, subjectAutocompleteTextView;
    private LinearLayout selectionLayout;
    private RecyclerView notesRecyclerView;
    private NotesAdapter notesAdapter;
    private List<NoteItem> noteItemList;
    private Map<String, List<String>> subjectsByLevel;
    private String selectedLevel = null;
    private String selectedSubject = null;
    private String responseSubject;
    private TextView pageTitle;

    // New UI elements
    private LinearLayout contributionLayout;
    private Button contributeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);

        ConstraintLayout rootLayout = findViewById(R.id.notes_root_layout);
        Utils.setPad(rootLayout, "bottom", this);

        Button checkButton = findViewById(R.id.check_button);
        subjectDropdownLayout = findViewById(R.id.subject_dropdown_layout);
        TextInputLayout levelDropdownLayout = findViewById(R.id.level_dropdown_layout);
        levelAutocompleteTextView = findViewById(R.id.level_autocomplete_textview);
        subjectAutocompleteTextView = findViewById(R.id.subject_autocomplete_textview);
        selectionLayout = findViewById(R.id.selection_layout);
        notesRecyclerView = findViewById(R.id.notes_recycler_view);
        pageTitle = findViewById(R.id.pageTitle);
        ImageView mainDecor = findViewById(R.id.notesDecor);
        ImageView greDecor1 = findViewById(R.id.notesDecor1);
        ImageView greDecor2 = findViewById(R.id.notesDecor2);
        ImageView greDecor3 = findViewById(R.id.notesDecor3);

        // Initialize new UI elements
        contributionLayout = findViewById(R.id.contribution_layout);
        contributeButton = findViewById(R.id.contribute_button);
        // contributionTextView = findViewById(R.id.contribution_textview); // Not
        // strictly needed if text is static

        noteItemList = new ArrayList<>();

        // Fetch subjects and levels dynamically from index.json
        Utils.fetchIndexData(this, new Utils.DatabaseCallback() {
            @Override
            public void onReady(JSONObject index) {
                try {
                    if (index.has("lectures")) {
                        JSONObject lectures = index.getJSONObject("lectures");
                        subjectsByLevel = new HashMap<>();
                        java.util.Iterator<String> keys = lectures.keys();
                        while (keys.hasNext()) {
                            String level = keys.next();
                            JSONObject subs = lectures.getJSONObject(level);
                            List<String> subList = new ArrayList<>();
                            java.util.Iterator<String> subKeys = subs.keys();
                            while (subKeys.hasNext()) {
                                subList.add(subKeys.next());
                            }
                            subjectsByLevel.put(level, subList);
                        }
                        runOnUiThread(() -> setupLevelSelector());
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing index for subjects: " + e.getMessage());
                    // Fallback to hardcoded
                    subjectsByLevel = Utils.getSubjectsByLevel();
                    setupLevelSelector();
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to fetch index for subjects: " + error);
                subjectsByLevel = Utils.getSubjectsByLevel();
                setupLevelSelector();
            }
        });

        setupRecyclerView();

        checkButton.setOnClickListener(v -> {
            if (selectedLevel == null) {
                Toast.makeText(NotesActivity.this, "Please select a level", Toast.LENGTH_SHORT).show();
                return;
            }
            if (selectedSubject == null) {
                Toast.makeText(NotesActivity.this, "Please select a subject", Toast.LENGTH_SHORT).show();
                return;
            }
            fetchNotesData(selectedSubject);
        });

        findViewById(R.id.backBtnN).setOnClickListener(v -> onBackPressed());
        findViewById(R.id.contiNotes).setOnClickListener(v -> sendContribute());

        Utils.setTheme(this, mainDecor, greDecor1, greDecor2, greDecor3);
        Utils.buttonTint(NotesActivity.this, checkButton);
        Utils.setDropperColors(NotesActivity.this, levelDropdownLayout, R.string.select_level);
        Utils.setDropperColors(NotesActivity.this, subjectDropdownLayout, R.string.select_subjects);
    }

    private void setupLevelSelector() {
        if (subjectsByLevel == null)
            return;
        List<String> levels = new ArrayList<>(subjectsByLevel.keySet());
        Collections.sort(levels);
        ArrayAdapter<String> levelAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line,
                levels) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setTextColor(Utils.setTextColorBasedOnBackground(NotesActivity.this, "primary"));
                return view;
            }
        };
        levelAutocompleteTextView.setAdapter(levelAdapter);
        levelAutocompleteTextView.setDropDownBackgroundDrawable(Utils.shadowMaker(NotesActivity.this));

        levelAutocompleteTextView.setOnItemClickListener((parent, view, position, id) -> {
            selectedLevel = (String) parent.getItemAtPosition(position);
            subjectAutocompleteTextView.setText(""); // Clear previous selection
            selectedSubject = null;
            Utils.setDropperColors(NotesActivity.this, subjectDropdownLayout, R.string.select_subjects);
            setupSubjectSelector(selectedLevel);
            subjectDropdownLayout.setEnabled(true);
        });
    }

    private void setupSubjectSelector(String level) {
        List<String> subjects = subjectsByLevel.getOrDefault(level, new ArrayList<>());
        Collections.sort(subjects);

        ArrayAdapter<String> subjectAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_dropdown_item_1line, subjects) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setTextColor(Utils.setTextColorBasedOnBackground(NotesActivity.this, "primary"));
                return view;
            }
        };
        subjectAutocompleteTextView.setAdapter(subjectAdapter);
        subjectAutocompleteTextView.setDropDownBackgroundDrawable(Utils.shadowMaker(NotesActivity.this));
        subjectAutocompleteTextView.setOnItemClickListener(
                (parent, view, position, id) -> selectedSubject = (String) parent.getItemAtPosition(position));

        if (subjects.isEmpty()) {
            subjectDropdownLayout.setEnabled(false);
            Toast.makeText(this, "No subjects available for this level.", Toast.LENGTH_SHORT).show();
        } else {
            subjectDropdownLayout.setEnabled(true);
        }
    }

    private void setupRecyclerView() {
        notesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        notesAdapter = new NotesAdapter(this, noteItemList);
        notesRecyclerView.setAdapter(notesAdapter);
    }

    private void fetchNotesData(String subject) {
        Log.d(TAG, "Fetching notes link for subject: " + subject);

        // Fetch from index.json
        Utils.fetchIndexData(this, new Utils.DatabaseCallback() {
            @Override
            public void onReady(JSONObject index) {
                try {
                    String noteLink = null;
                    if (index.has("notes")) {
                        JSONObject notes = index.getJSONObject("notes");
                        if (notes.has(subject)) {
                            noteLink = notes.getString(subject);
                        }
                    }

                    // Fallback to quizzes map if notes not found, maybe they are in the same file
                    if (noteLink == null && index.has("quizzes")) {
                        JSONObject quizzes = index.getJSONObject("quizzes");
                        if (quizzes.has(subject)) {
                            noteLink = quizzes.getString(subject);
                        }
                    }

                    if (noteLink != null && !noteLink.isEmpty()) {
                        fetchNotesFromJson(Utils.githubToJsDelivr(noteLink));
                    } else {
                        showContributionLayout();
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "JSON Error: " + e.getMessage());
                    showContributionLayout();
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Index fetch error: " + error);
                Toast.makeText(NotesActivity.this, "Failed to load index", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showContributionLayout() {
        selectionLayout.setVisibility(View.GONE);
        notesRecyclerView.setVisibility(View.GONE);
        contributionLayout.setVisibility(View.VISIBLE);
        pageTitle.setText(R.string.notes);
        contributeButton.setOnClickListener(v -> sendContribute());
    }

    private void fetchNotesFromJson(String jsonUrl) {
        Log.d(TAG, "Fetching notes JSON from: " + jsonUrl);

        JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.GET, jsonUrl, null,
                response -> {
                    try {
                        Log.d(TAG, "Notes JSON response: " + response.toString());

                        responseSubject = response.optString("subject", ""); // optional
                        JSONArray contentsArray = response.getJSONArray("contents");
                        noteItemList.clear();

                        for (int i = 0; i < contentsArray.length(); i++) {
                            JSONObject item = contentsArray.getJSONObject(i);
                            String link = Utils.decryptUrl(item.getString("link"));
                            String helper = item.getString("helper");
                            String week = item.getString("week");

                            noteItemList.add(new NoteItem(link, helper, week));
                        }

                        notesAdapter.updateData(noteItemList);
                        animateToRecyclerView();
                        contributionLayout.setVisibility(View.GONE);

                    } catch (JSONException e) {
                        Log.e(TAG, "JSON Parsing error: " + e.getMessage());
                        Toast.makeText(NotesActivity.this, "Error parsing notes.", Toast.LENGTH_LONG).show();
                    }
                },
                error -> {
                    Log.e(TAG, "Error loading notes JSON: " + error.getMessage());
                    Toast.makeText(NotesActivity.this, "Failed to load notes file.", Toast.LENGTH_LONG).show();
                });

        // Use the same retry policy for the second request as well
        jsonRequest.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
                10000,
                1,
                com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        Volley.newRequestQueue(this).add(jsonRequest);
    }

    private void sendContribute() {
        String url = "https://forms.gle/4UjRRCU6gRJnMeg97";
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }

    private void animateToRecyclerView() {
        Animation slideOutLeft = AnimationUtils.loadAnimation(this, R.anim.slide_out_left);
        Animation slideInRight = AnimationUtils.loadAnimation(this, R.anim.slide_in_right);

        slideOutLeft.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                selectionLayout.setVisibility(View.GONE);
                notesRecyclerView.setVisibility(View.VISIBLE);
                pageTitle.setText(responseSubject);
                notesRecyclerView.startAnimation(slideInRight);
                contributionLayout.setVisibility(View.GONE); // Hide contribution layout
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        selectionLayout.startAnimation(slideOutLeft);
    }

    @Override
    public void onBackPressed() {
        if (notesRecyclerView.getVisibility() == View.VISIBLE) {
            animateToSelectionLayout();
        } else {
            super.onBackPressed();
        }
    }

    public void animateToSelectionLayout() {
        Animation slideOutLeft = AnimationUtils.loadAnimation(this, R.anim.slide_out_left);
        Animation slideInRight = AnimationUtils.loadAnimation(this, R.anim.slide_in_right);

        slideOutLeft.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                selectionLayout.setVisibility(View.VISIBLE);
                notesRecyclerView.setVisibility(View.GONE);
                pageTitle.setText(R.string.notes);
                selectionLayout.startAnimation(slideInRight);
                contributionLayout.setVisibility(View.GONE); // Hide contribution layout
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        notesRecyclerView.startAnimation(slideOutLeft);
    }
}
