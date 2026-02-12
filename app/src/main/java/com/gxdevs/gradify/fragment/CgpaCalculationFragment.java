package com.gxdevs.gradify.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.gxdevs.gradify.R;
import com.gxdevs.gradify.Utils.Utils;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CgpaCalculationFragment extends Fragment {

    private LinearLayout subjectsContainer;
    private Button addSubjectButton;
    private Button calculateCgpaButton;
    private TextView cgpaResultTextView;
    private View resultCard;
    private int subjectCount = 1;

    private static class SubjectEntry {
        TextInputEditText creditsEditText;
        TextInputEditText gradeEditText;

        SubjectEntry(TextInputEditText creditsEditText, TextInputEditText gradeEditText) {
            this.creditsEditText = creditsEditText;
            this.gradeEditText = gradeEditText;
        }
    }

    private List<SubjectEntry> subjectEntries = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cgpa, container, false);

        subjectsContainer = view.findViewById(R.id.subjectsContainer);
        addSubjectButton = view.findViewById(R.id.addSubjectButton);
        calculateCgpaButton = view.findViewById(R.id.calculateCgpaButton);
        cgpaResultTextView = view.findViewById(R.id.cgpaResultTextView);
        resultCard = view.findViewById(R.id.resultCardCgpa);

        // Add the first subject entry
        TextInputLayout creditsHolder = view.findViewById(R.id.creditsHolder1);
        TextInputLayout gradeHolder = view.findViewById(R.id.gradeHolder1);

        TextInputEditText initialCreditsEditText = view.findViewById(R.id.creditsEditText1);
        TextInputEditText initialGradeEditText = view.findViewById(R.id.gradeEditText1);
        if (initialCreditsEditText != null && initialGradeEditText != null) {
            subjectEntries.add(new SubjectEntry(initialCreditsEditText, initialGradeEditText));
        }

        addSubjectButton.setOnClickListener(v -> addSubjectField());
        calculateCgpaButton.setOnClickListener(v -> calculateCgpa());

        return view;
    }

    private void addSubjectField() {
        subjectCount++;
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View subjectEntryLayout = inflater.inflate(R.layout.subject_input_item,
                subjectsContainer, false);

        TextView subjectLabel = subjectEntryLayout.findViewById(R.id.subjectLabel);
        TextInputLayout creditsHolder = subjectEntryLayout.findViewById(R.id.creditsHolder);
        TextInputEditText creditsEditText = subjectEntryLayout.findViewById(R.id.creditsEditText);
        TextInputLayout gradeHolder = subjectEntryLayout.findViewById(R.id.gradeInputLayout);
        TextInputEditText gradeEditText = subjectEntryLayout.findViewById(R.id.gradeEditText);

        subjectLabel.setText(String.format(Locale.getDefault(), "Subject %d", subjectCount));

        if (subjectsContainer != null) {
            subjectsContainer.addView(subjectEntryLayout);
        }
        subjectEntries.add(new SubjectEntry(creditsEditText, gradeEditText));
    }

    private double getPointsFromGrade(String grade, int subjectIndex) {
        grade = grade.toUpperCase(Locale.ROOT);
        switch (grade) {
            case "S":
                return 10;
            case "A":
                return 9;
            case "B":
                return 8;
            case "C":
                return 7;
            case "D":
                return 6;
            case "E":
                return 4;
            case "U":
            case "W":
                return 0;
            case "I":
                return -1;
            default:
                Toast.makeText(getContext(), "Invalid grade '" + grade + "' for Subject " + subjectIndex
                        + ". Valid grades: S, A, B, C, D, E, U, W, I.", Toast.LENGTH_LONG).show();
                throw new IllegalArgumentException("Invalid grade");
        }
    }

    private void calculateCgpa() {
        double totalCredits = 0;
        double totalWeightedPoints = 0;
        boolean validInput = true;
        int actualSubjectsConsidered = 0;

        for (int i = 0; i < subjectEntries.size(); i++) {
            SubjectEntry entry = subjectEntries.get(i);
            String creditsStr = entry.creditsEditText.getText().toString();
            String gradeStr = entry.gradeEditText.getText().toString().trim();

            if (creditsStr.isEmpty() || gradeStr.isEmpty()) {
                resultCard.setVisibility(View.VISIBLE);
                cgpaResultTextView.setText("Please fill all fields.");
                validInput = false;
                break;
            }

            try {
                double credits = Double.parseDouble(creditsStr);

                if (credits <= 0) {
                    resultCard.setVisibility(View.VISIBLE);
                    cgpaResultTextView.setText("Credits must be positive.");
                    validInput = false;
                    break;
                }

                double points = getPointsFromGrade(gradeStr, i + 1);

                if (points == -1) {
                    continue;
                }

                totalCredits += credits;
                totalWeightedPoints += (credits * points);
                actualSubjectsConsidered++;

            } catch (NumberFormatException e) {
                resultCard.setVisibility(View.VISIBLE);
                cgpaResultTextView.setText("Invalid number format.");
                validInput = false;
                break;
            } catch (IllegalArgumentException e) {
                resultCard.setVisibility(View.VISIBLE);
                cgpaResultTextView.setText("Invalid grade.");
                validInput = false;
                break;
            }
        }

        if (validInput) {
            resultCard.setVisibility(View.VISIBLE);
            if (actualSubjectsConsidered == 0) {
                if (subjectEntries.stream()
                        .allMatch(entry -> entry.gradeEditText.getText().toString().trim().equalsIgnoreCase("I"))) {
                    cgpaResultTextView.setText("CGPA: N/A");
                } else {
                    cgpaResultTextView.setText("CGPA: N/A");
                }
            } else if (totalCredits == 0) {
                cgpaResultTextView.setText("CGPA: N/A");
            } else {
                double cgpa = totalWeightedPoints / totalCredits;
                cgpaResultTextView.setText(String.format(Locale.getDefault(), "CGPA: %.2f", cgpa));
            }
        } else {
            resultCard.setVisibility(View.VISIBLE);
            // Error text already set
        }
    }
}