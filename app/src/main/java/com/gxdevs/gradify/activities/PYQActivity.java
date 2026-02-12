package com.gxdevs.gradify.activities;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;
import com.gxdevs.gradify.R;
import com.gxdevs.gradify.Utils.Utils;

import java.util.Arrays;
import java.util.List;

public class PYQActivity extends AppCompatActivity {

    private MaterialButton startQuizButton, addSubjects;
    MaterialAutoCompleteTextView subjectDrop, quizDrop, yearDrop, sessionDrop;
    String subject, quiz, year, session, examJsonLink;
    ConstraintLayout quizContainer, yearContainer, sessionContainer, modeContainer, modeToggleContainer;
    LinearLayout progressHolder;
    ProgressBar progressBar;
    MaterialSwitch modeToggle;
    TextInputLayout subjectHolder, quizHolder, yearHolder, sessionHolder;
    private View empty_view_pyq, pyqFormContainer;
    boolean examCheck;
    List<String> subjects;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pyq);
        Utils.setPad(findViewById(R.id.containerPYQ), "bottom", this);

        initViews();
        setupListeners();
        subjects = Utils.getSubjects(this);
        if (subjects.get(0).equals("Select subjects in profile")) {
            pyqFormContainer.setVisibility(GONE);
            empty_view_pyq.setVisibility(VISIBLE);
        } else {
            pyqFormContainer.setVisibility(VISIBLE);
            empty_view_pyq.setVisibility(GONE);
            Utils.setupDropDown(this, subjectDrop, subjects);
        }
        Utils.setupDropDown(this, quizDrop, List.of(Utils.QUIZ_TYPES));
        Utils.setupDropDown(this, sessionDrop, List.of()); // Start empty
        findViewById(R.id.backBtn).setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
    }

    private void setupListeners() {
        subjectDrop.setOnItemClickListener(((parent, view, position, id) -> {
            subject = (String) parent.getItemAtPosition(position);
            Log.d("PYQ_DEBUG", "Step 1: Subject selected - " + subject);
            resetAndHideViews(quizDrop, quizContainer);
            resetAndHideViews(yearDrop, yearContainer);
            resetAndHideViews(sessionDrop, sessionContainer);
            resetAndHideViews(sessionDrop, modeContainer);
            resetAndHideViews(sessionDrop, startQuizButton);
            startQuizButton.setVisibility(GONE);

            modeContainer.setVisibility(GONE);
            if (!subject.equals("Select subjects in profile")) {
                Log.d("PYQ_DEBUG", "Step 1: Showing quiz type dropdown");
                quizContainer.setVisibility(VISIBLE);
                if (addSubjects.getVisibility() == VISIBLE) {
                    addSubjects.setVisibility(GONE);
                }
            }
        }));

        quizDrop.setOnItemClickListener(((parent, view, position, id) -> {
            quiz = (String) parent.getItemAtPosition(position);
            Log.d("PYQ_DEBUG", "Step 2: Quiz type selected - " + quiz);
            resetAndHideViews(yearDrop, yearContainer);
            resetAndHideViews(sessionDrop, sessionContainer);
            resetAndHideViews(sessionDrop, modeContainer);
            resetAndHideViews(sessionDrop, startQuizButton);
            Log.d("PYQ_DEBUG", "Step 2: Fetching years for subject=" + subject + ", quiz=" + quiz);
            progressHolder.setVisibility(VISIBLE);
            loadDropdownData(yearDrop, yearContainer, subject, quiz, null);
        }));

        yearDrop.setOnItemClickListener(((parent, view, position, id) -> {
            year = (String) parent.getItemAtPosition(position);
            Log.d("PYQ_DEBUG", "Step 3: Year selected - " + year);
            resetAndHideViews(sessionDrop, sessionContainer);
            resetAndHideViews(sessionDrop, modeContainer);
            resetAndHideViews(sessionDrop, startQuizButton);
            if (!year.equals("No data found")) {
                Log.d("PYQ_DEBUG",
                        "Step 3: Fetching sessions for subject=" + subject + ", quiz=" + quiz + ", year=" + year);
                progressHolder.setVisibility(VISIBLE);
                loadDropdownData(sessionDrop, sessionContainer, subject, quiz, year);
            } else {
                Log.w("PYQ_DEBUG", "Step 3: No data found for this year");
                sessionContainer.setVisibility(GONE);
            }
        }));

        sessionDrop.setOnItemClickListener(((parent, view, position, id) -> {
            session = (String) parent.getItemAtPosition(position);
            Log.d("PYQ_DEBUG", "Step 4: Session selected - " + session);
            Log.d("PYQ_DEBUG", "Step 4: Fetching exam link for subject=" + subject + ", quiz=" + quiz + ", year=" + year
                    + ", session=" + session);
            progressHolder.setVisibility(VISIBLE);
            Utils.fetchExamJsonLink(this, new Utils.ExamLinkCallback() {
                @Override
                public void onSingleLink(String link) {
                    Log.d("PYQ_DEBUG", "Step 5: Exam link received - " + link);
                    if (link.contains("dl.dropboxusercontent.com")) {
                        Log.d("PYQ_DEBUG", "Step 5: Valid Dropbox link, showing start button");
                        startQuizButton.setVisibility(VISIBLE);
                        modeContainer.setVisibility(VISIBLE);
                        progressHolder.setVisibility(GONE);
                        examJsonLink = link;
                    } else {
                        Log.w("PYQ_DEBUG", "Step 5: Link doesn't contain Dropbox URL: " + link);
                    }
                }

                @Override
                public void onError(String error) {
                    Log.e("PYQ_DEBUG", "Step 5: Error fetching exam link - " + error);
                    progressHolder.setVisibility(GONE);
                    Toast.makeText(PYQActivity.this, "No PYQ Found: " + error, Toast.LENGTH_LONG).show();
                }
            }, subject, quiz, year, session);
        }));

        modeToggle.setOnCheckedChangeListener((buttonView, isChecked) -> examCheck = isChecked);

        startQuizButton.setOnClickListener(v -> startQuiz(examJsonLink, examCheck));

        addSubjects.setOnClickListener(v -> startActivity(new Intent(PYQActivity.this, ProfileActivity.class)));
        findViewById(R.id.goToProfileBtnPyq)
                .setOnClickListener(v -> startActivity(new Intent(PYQActivity.this, ProfileActivity.class)));
    }

    private void startQuiz(String examJsonLink, boolean isExamMode) {
        Intent intent = new Intent(this, PYQQuestionActivity.class);
        intent.putExtra("link", examJsonLink);
        intent.putExtra("EXAM_MODE", isExamMode);
        intent.putExtra("SUBJECT", subject);
        startActivity(intent);
    }

    private void initViews() {
        startQuizButton = findViewById(R.id.startQuizButton);
        addSubjects = findViewById(R.id.addSubjects);
        subjectDrop = findViewById(R.id.subjectDrop);
        quizDrop = findViewById(R.id.quizDrop);
        yearDrop = findViewById(R.id.yearDrop);
        sessionDrop = findViewById(R.id.sessionDrop);
        progressBar = findViewById(R.id.progressBar);
        modeToggle = findViewById(R.id.modeToggle);

        quizContainer = findViewById(R.id.quizContainer);
        yearContainer = findViewById(R.id.yearContainer);
        sessionContainer = findViewById(R.id.sessionContainer);
        progressHolder = findViewById(R.id.progressHolder);
        modeContainer = findViewById(R.id.modeContainer);
        modeToggleContainer = findViewById(R.id.modeToggleContainer);

        subjectHolder = findViewById(R.id.subjectHolder);
        quizHolder = findViewById(R.id.quizHolder);
        yearHolder = findViewById(R.id.yearHolder);
        sessionHolder = findViewById(R.id.sessionHolder);

        pyqFormContainer = findViewById(R.id.pyqFormContainer);
        empty_view_pyq = findViewById(R.id.empty_view_pyq);

        quizContainer.setVisibility(GONE);
        yearContainer.setVisibility(GONE);
        sessionContainer.setVisibility(GONE);
        modeContainer.setVisibility(GONE);
        progressHolder.setVisibility(GONE);
        startQuizButton.setVisibility(GONE);
    }

    private void resetAndHideViews(MaterialAutoCompleteTextView dropdown, View container) {
        dropdown.setText("", false); // Reset text without filtering
        if (container != null) {
            container.setVisibility(GONE);
        }
    }

    private void loadDropdownData(MaterialAutoCompleteTextView dropdown, View containerToVisible, String subject,
            String quizType, String year) {
        Log.d("PYQ_DEBUG",
                "loadDropdownData called - Subject: " + subject + ", QuizType: " + quizType + ", Year: " + year);
        Utils.fetchData(this, new Utils.dataReturn() {
            @Override
            public void onSuccess(String[] data) {
                // Convert String[] to List<String>
                List<String> dataList = Arrays.asList(data);

                if (dataList.isEmpty()) {
                    Log.d("PYQ_DEBUG", "Data received is empty.");
                    Toast.makeText(PYQActivity.this, "No PYQ Found", Toast.LENGTH_SHORT).show();
                    if (containerToVisible != null) {
                        containerToVisible.setVisibility(GONE);
                    }
                } else {
                    Log.d("PYQ_DEBUG", "Data received: " + dataList.toString());
                    if (containerToVisible != null) {
                        containerToVisible.setVisibility(VISIBLE);
                    }
                    // Call setupDropDown with the fetched data
                    Utils.setupDropDown(PYQActivity.this, dropdown, dataList);
                }

                if (progressHolder.getVisibility() == VISIBLE) {
                    progressHolder.setVisibility(GONE);
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(PYQActivity.this, "No PYQ Found", Toast.LENGTH_SHORT).show();
                Log.e("PYQ_DEBUG", "Error: " + error);
                if (progressHolder.getVisibility() == VISIBLE) {
                    progressHolder.setVisibility(GONE);
                }
                if (containerToVisible != null) {
                    containerToVisible.setVisibility(GONE);
                }
            }
        }, subject, quizType, year);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}