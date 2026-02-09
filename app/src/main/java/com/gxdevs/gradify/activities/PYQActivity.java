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

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;
import com.gxdevs.gradify.R;
import com.gxdevs.gradify.Utils.Utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PYQActivity extends AppCompatActivity {

    private Button startQuizButton, addSubjects;
    MaterialAutoCompleteTextView subjectDrop, quizDrop, yearDrop, sessionDrop;
    String subject, quiz, year, session, examJsonLink;
    ConstraintLayout quizContainer, yearContainer, sessionContainer, modeContainer, modeToggleContainer;
    LinearLayout progressHolder;
    ProgressBar progressBar;
    MaterialSwitch modeToggle;
    TextInputLayout subjectHolder, quizHolder, yearHolder, sessionHolder;
    boolean examCheck;
    List<String> subjects;
    private ImageView mainDecor, greDecor1, greDecor2, greDecor3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pyq);
        Utils.setPad(findViewById(R.id.containerPYQ), "bottom", this);

        initViews();
        setupListeners();
        subjects = Utils.getSubjects(this);
        if (subjects.get(0).equals("Select subjects in profile")) {
            addSubjects.setVisibility(VISIBLE);
        }

        setupDropDown(subjectDrop, subjects);
        setupDropDown(quizDrop, List.of(Utils.QUIZ_TYPES));
        setupDropDown(sessionDrop, List.of(Utils.QUIZ_TYPES));
        findViewById(R.id.backBtn).setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        Utils.setTheme(this, mainDecor, greDecor1, greDecor2, greDecor3);
        Utils.setDropperColors(this, subjectHolder, R.string.select_subjects);
        Utils.setDropperColors(this, quizHolder, R.string.select_quiz_type);
        Utils.setDropperColors(this, yearHolder, R.string.select_year);
        Utils.setDropperColors(this, sessionHolder, R.string.select_session);
        Utils.buttonTint(this, startQuizButton);
        Utils.switchColors(this, modeToggle);
        modeToggleContainer.setBackground(Utils.setCardColor(this, 10));
    }

    private void setupListeners() {
        subjectDrop.setOnItemClickListener(((parent, view, position, id) -> {
            subject = (String) parent.getItemAtPosition(position);
            resetAndHideViews(quizDrop, quizContainer);
            resetAndHideViews(yearDrop, yearContainer);
            resetAndHideViews(sessionDrop, sessionContainer);
            resetAndHideViews(sessionDrop, modeContainer);
            resetAndHideViews(sessionDrop, startQuizButton);
            startQuizButton.setVisibility(GONE);

            modeContainer.setVisibility(GONE);
            if (!subject.equals("Select subjects in profile")) {
                quizContainer.setVisibility(VISIBLE);
                if (addSubjects.getVisibility() == VISIBLE) {
                    addSubjects.setVisibility(GONE);
                }
            }
        }));

        quizDrop.setOnItemClickListener(((parent, view, position, id) -> {
            quiz = (String) parent.getItemAtPosition(position);
            quiz = quiz.replace(" ", "_");
            resetAndHideViews(yearDrop, yearContainer);
            resetAndHideViews(sessionDrop, sessionContainer);
            resetAndHideViews(sessionDrop, modeContainer);
            resetAndHideViews(sessionDrop, startQuizButton);
            progressHolder.setVisibility(VISIBLE);
            loadDropdownData(yearDrop, subject, quiz, null);
            yearContainer.setVisibility(VISIBLE);
        }));

        yearDrop.setOnItemClickListener(((parent, view, position, id) -> {
            year = (String) parent.getItemAtPosition(position);
            year = year.replace(" ", "_");
            resetAndHideViews(sessionDrop, sessionContainer);
            resetAndHideViews(sessionDrop, modeContainer);
            resetAndHideViews(sessionDrop, startQuizButton);
            if (!year.equals("No_data_found")) {
                progressHolder.setVisibility(VISIBLE);
                loadDropdownData(sessionDrop, subject, quiz, year);
                sessionContainer.setVisibility(VISIBLE);
            }
        }));

        sessionDrop.setOnItemClickListener(((parent, view, position, id) -> {
            session = (String) parent.getItemAtPosition(position);
            progressHolder.setVisibility(VISIBLE);
            Utils.fetchExamJsonLink(this, new Utils.ExamLinkCallback() {
                @Override
                public void onSingleLink(String link) {
                    if (link.contains("dl.dropboxusercontent.com")) {
                        startQuizButton.setVisibility(VISIBLE);
                        modeContainer.setVisibility(VISIBLE);
                        progressHolder.setVisibility(GONE);
                        examJsonLink = link;
                    }
                }

                @Override
                public void onMultipleLinks(String[] links) {
                    progressHolder.setVisibility(GONE);
                    Toast.makeText(PYQActivity.this, "Multiple links found", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(String error) {
                    progressHolder.setVisibility(GONE);
                    Toast.makeText(PYQActivity.this, "No PYQ Found", Toast.LENGTH_SHORT).show();
                }
            }, subject, quiz, year, session);
        }));

        modeToggle.setOnCheckedChangeListener((buttonView, isChecked) -> examCheck = isChecked);

        startQuizButton.setOnClickListener(v -> startQuiz(examJsonLink, examCheck));

        addSubjects.setOnClickListener(v -> startActivity(new Intent(PYQActivity.this, ProfileActivity.class)));
    }

    private void startQuiz(String examJsonLink, boolean isExamMode) {
        Intent intent = new Intent(this, PYQQuestionActivity.class);
        intent.putExtra("link", examJsonLink);
        intent.putExtra("EXAM_MODE", isExamMode);
        intent.putExtra("SUBJECT", subject);
        startActivity(intent);
    }

    private void setupDropDown(MaterialAutoCompleteTextView dropdown, List<String> data) {
        runOnUiThread(() -> {
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, data) {
                @NonNull
                @Override
                public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                    TextView view = (TextView) super.getView(position, convertView, parent);
                    view.setTextColor(Utils.setTextColorBasedOnBackground(PYQActivity.this, "primary"));
                    return view;
                }
            };
            dropdown.setDropDownBackgroundDrawable(Utils.shadowMaker(this));
            dropdown.setDropDownVerticalOffset(5);
            dropdown.setAdapter(adapter);
        });
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

        mainDecor = findViewById(R.id.pyqHolder);
        greDecor1 = findViewById(R.id.pyqDecor1);
        greDecor2 = findViewById(R.id.pyqDecor2);
        greDecor3 = findViewById(R.id.pyqDecor3);

        subjectHolder = findViewById(R.id.subjectHolder);
        quizHolder = findViewById(R.id.quizHolder);
        yearHolder = findViewById(R.id.yearHolder);
        sessionHolder = findViewById(R.id.sessionHolder);

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

    private void loadDropdownData(MaterialAutoCompleteTextView dropdown, String subject, String quizType, String year) {
        Utils.fetchData(this, new Utils.dataReturn() {
            @Override
            public void onSuccess(String[] data) {
                // Convert String[] to List<String>
                List<String> dataList = Arrays.asList(data);
                if (dataList.isEmpty()) {
                    dataList = Collections.singletonList("No data found");
                }
                Log.d("API Response", dataList.toString());
                if (progressHolder.getVisibility() == VISIBLE) {
                    progressHolder.setVisibility(GONE);
                }

                // Call setupDropDown with the fetched data
                setupDropDown(dropdown, dataList);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(PYQActivity.this, "API Error", Toast.LENGTH_SHORT).show();
                Log.e("API Error, try again later", error);
                if (progressHolder.getVisibility() == VISIBLE) {
                    progressHolder.setVisibility(GONE);
                }
            }
        }, subject, quizType, year);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Utils.setTheme(this, mainDecor, greDecor1, greDecor2, greDecor3);
    }
}