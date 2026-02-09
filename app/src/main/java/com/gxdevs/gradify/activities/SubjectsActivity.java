package com.gxdevs.gradify.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gxdevs.gradify.R;
import com.gxdevs.gradify.Utils.Utils;
import com.gxdevs.gradify.adapters.SubjectsAdapter;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import eightbitlab.com.blurview.BlurView;

public class SubjectsActivity extends AppCompatActivity implements SubjectsAdapter.OnSubjectClickListener {

    private RecyclerView recyclerView;
    private SubjectsAdapter adapter;
    private TextView emptyView;
    private final List<String> subjects = new ArrayList<>();

    // Navigation Drawer Fields
    private View customNavDrawer;
    private float navDrawerWidthInPixels;
    private BlurView blurView;
    private ViewGroup rootActivityLayout;
    private ImageView mainDecor, greDecor1, greDecor2, greDecor3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subjects);

        rootActivityLayout = findViewById(R.id.subContainer); // Assuming subContainer is the root for blur
        Utils.setPad(findViewById(R.id.subContainer), "bottom", this);

        // Initialize UI components
        recyclerView = findViewById(R.id.subjects_recycler_view);
        emptyView = findViewById(R.id.empty_view);
        customNavDrawer = findViewById(R.id.customNavDrawerS);
        blurView = findViewById(R.id.blurViewS);
        mainDecor = findViewById(R.id.subjectsDecor);
        greDecor1 = findViewById(R.id.subjectDecor1);
        greDecor2 = findViewById(R.id.subjectDecor2);
        greDecor3 = findViewById(R.id.subjectDecor3);

        View actualDrawerContent = customNavDrawer;

        TextView editSubjects = actualDrawerContent.findViewById(R.id.nav_edit_subjects_header);
        LinearLayout nav_live_lectures = actualDrawerContent.findViewById(R.id.nav_live_lectures);
        LinearLayout nav_helping_lectures = actualDrawerContent.findViewById(R.id.nav_helping_lectures);

        if (editSubjects != null) {
            editSubjects.setOnClickListener(v -> {
                startActivity(new Intent(this, ProfileActivity.class));
                toggleNavDrawer();
            });
        }

        if (nav_live_lectures != null) {
            nav_live_lectures.setOnClickListener(v -> {
                Toast.makeText(this, "Coming Soon", Toast.LENGTH_SHORT).show();
                toggleNavDrawer();
            });
        }

        if (nav_helping_lectures != null) {
            nav_helping_lectures.setOnClickListener(v -> {
                Toast.makeText(this, "Coming Soon", Toast.LENGTH_SHORT).show();
                toggleNavDrawer();
            });
        }

        navDrawerWidthInPixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 280,
                getResources().getDisplayMetrics());

        // Setup RecyclerView
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new SubjectsAdapter(subjects, this);
        recyclerView.setAdapter(adapter);

        findViewById(R.id.backBtnSS).setOnClickListener(v -> onBackPressed());
        findViewById(R.id.menuS).setOnClickListener(v -> toggleNavDrawer());

        if (blurView != null) {
            setupBlurView();
            blurView.setOnClickListener(v -> {
                if (customNavDrawer != null && customNavDrawer.getVisibility() == View.VISIBLE) {
                    toggleNavDrawer();
                }
            });
        }

        // Fetch and display subjects
        loadSubjects();
        Utils.setTheme(this, mainDecor, greDecor1, greDecor2, greDecor3);
    }

    private void setupBlurView() {
        if (blurView == null || rootActivityLayout == null)
            return;
        float radius = 15f;
        Drawable windowBackground = getWindow().getDecorView().getBackground();
        blurView.setupWith(rootActivityLayout)
                .setFrameClearDrawable(windowBackground)
                .setBlurRadius(radius)
                .setBlurAutoUpdate(true);
    }

    private void toggleNavDrawer() {
        if (customNavDrawer == null || blurView == null)
            return;

        if (customNavDrawer.getVisibility() == View.GONE) {
            blurView.setAlpha(0f);
            blurView.setVisibility(View.VISIBLE);
            blurView.animate().alpha(1f).setDuration(300).setListener(null);

            customNavDrawer.setTranslationX(navDrawerWidthInPixels); // Start from right
            customNavDrawer.setVisibility(View.VISIBLE);
            customNavDrawer.animate()
                    .translationX(0f) // Animate to 0
                    .setDuration(300)
                    .setListener(null);
        } else {
            blurView.animate().alpha(0f).setDuration(300).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    blurView.setVisibility(View.GONE);
                }
            });

            customNavDrawer.animate()
                    .translationX(navDrawerWidthInPixels) // Animate to right
                    .setDuration(300)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            customNavDrawer.setVisibility(View.GONE);
                        }
                    });
        }
    }

    @Override
    public void onBackPressed() {
        if (customNavDrawer != null && customNavDrawer.getVisibility() == View.VISIBLE) {
            toggleNavDrawer();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh subjects list when activity resumes
        loadSubjects();
        if (blurView != null) {
            blurView.setBlurAutoUpdate(true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (blurView != null) {
            blurView.setBlurAutoUpdate(false);
        }
    }

    private void loadSubjects() {
        // Get subjects from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("UserPreferences", MODE_PRIVATE);
        Set<String> savedSubjects = sharedPreferences.getStringSet("selectedSubjects", new HashSet<>());

        // Clear and update the list
        subjects.clear();
        if (!savedSubjects.isEmpty()) {
            subjects.addAll(savedSubjects);
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        } else {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        }

        // Notify adapter of changes
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onSubjectClick(String subjectName) {
        if (Utils.getLectureCount(subjectName) == 0) {
            Toast.makeText(this, "No lectures found!", Toast.LENGTH_SHORT).show();
        } else {
            fetchUrlFromDatabase(subjectName);
        }
    }

    private void fetchUrlFromDatabase(String subjectName) {
        Utils.fetchLectures(this, new Utils.DatabaseCallback() {
            @Override
            public void onReady(JSONObject database) {
                try {
                    if (database.has("lectures")) {
                        JSONObject lectures = database.getJSONObject("lectures");
                        if (lectures.has(subjectName)) {
                            String url = lectures.getString(subjectName);
                            if (url != null && !url.isEmpty()) {
                                launchLectureActivity(subjectName, url);
                            } else {
                                Toast.makeText(SubjectsActivity.this, "Lectures coming soon!", Toast.LENGTH_SHORT)
                                        .show();
                            }
                        } else {
                            Toast.makeText(SubjectsActivity.this, "No URL found for subject!", Toast.LENGTH_SHORT)
                                    .show();
                        }
                    } else {
                        Toast.makeText(SubjectsActivity.this, "Error: missing lectures data", Toast.LENGTH_SHORT)
                                .show();
                    }
                } catch (JSONException e) {
                    Toast.makeText(SubjectsActivity.this, "Data Error", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(SubjectsActivity.this, "Error fetching data: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void launchLectureActivity(String subjectName, String jsonUrl) {
        Intent intent = new Intent(this, LectureActivity.class);
        intent.putExtra("json_url", jsonUrl);
        intent.putExtra("subject_name", subjectName);
        startActivity(intent);
    }
}