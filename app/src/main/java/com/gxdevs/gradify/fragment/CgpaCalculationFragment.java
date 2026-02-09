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
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cgpa, container, false);

        subjectsContainer = view.findViewById(R.id.subjectsContainer);
        addSubjectButton = view.findViewById(R.id.addSubjectButton);
        calculateCgpaButton = view.findViewById(R.id.calculateCgpaButton);
        cgpaResultTextView = view.findViewById(R.id.cgpaResultTextView);

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

        Utils.buttonTint(requireContext(), calculateCgpaButton);
        Utils.buttonTint(requireContext(), addSubjectButton);
        Utils.setDropperColors(requireContext(), creditsHolder, R.string.enter_credits);
        Utils.setDropperColors(requireContext(), gradeHolder, R.string.enter_grade);

        return view;
    }

    private void addSubjectField() {
        subjectCount++;
        LayoutInflater inflater = LayoutInflater.from(getContext());
        LinearLayout subjectEntryLayout = (LinearLayout) inflater.inflate(R.layout.subject_input_item, subjectsContainer, false);

        TextView subjectLabel = subjectEntryLayout.findViewById(R.id.subjectLabel);
        TextInputLayout creditsHolder = subjectEntryLayout.findViewById(R.id.creditsHolder);
        TextInputEditText creditsEditText = subjectEntryLayout.findViewById(R.id.creditsEditText);
        TextInputLayout gradeHolder = subjectEntryLayout.findViewById(R.id.gradeInputLayout);
        TextInputEditText gradeEditText = subjectEntryLayout.findViewById(R.id.gradeEditText);

        subjectLabel.setText(String.format(Locale.getDefault(), "Subject %d", subjectCount));

        Utils.setDropperColors(requireContext(), creditsHolder, R.string.enter_credits);
        Utils.setDropperColors(requireContext(), gradeHolder, R.string.enter_grade);

        subjectsContainer.addView(subjectEntryLayout);
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
                Toast.makeText(getContext(), "Invalid grade '" + grade + "' for Subject " + subjectIndex + ". Valid grades: S, A, B, C, D, E, U, W, I.", Toast.LENGTH_LONG).show();
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
                Toast.makeText(getContext(), "Please fill all fields for Subject " + (i + 1), Toast.LENGTH_SHORT).show();
                validInput = false;
                break;
            }

            try {
                double credits = Double.parseDouble(creditsStr);
                
                if (credits <= 0) {
                    Toast.makeText(getContext(), "Credits must be positive for Subject " + (i + 1), Toast.LENGTH_SHORT).show();
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
                Toast.makeText(getContext(), "Invalid number format for credits for Subject " + (i + 1), Toast.LENGTH_SHORT).show();
                validInput = false;
                break;
            } catch (IllegalArgumentException e) {
                validInput = false;
                break;
            }
        }

        if (validInput) {
            if (actualSubjectsConsidered == 0) {
                 if (subjectEntries.stream().allMatch(entry -> entry.gradeEditText.getText().toString().trim().equalsIgnoreCase("I"))) {
                    cgpaResultTextView.setText("CGPA: N/A (All subjects ignored)");
                 } else if (totalCredits == 0 && subjectEntries.size() > 0 && !subjectEntries.stream().allMatch(entry -> entry.gradeEditText.getText().toString().trim().isEmpty() && entry.creditsEditText.getText().toString().trim().isEmpty())) {
                    cgpaResultTextView.setText("CGPA: N/A (No valid subjects for calculation)");
                 }
                 else if (totalCredits == 0) {
                     cgpaResultTextView.setText("CGPA: N/A (No credits entered or all subjects ignored)");
                 }
                 else {
                    double cgpa = totalWeightedPoints / totalCredits;
                    cgpaResultTextView.setText(String.format(Locale.getDefault(), "CGPA: %.2f", cgpa));
                 }
            } else if (totalCredits == 0) {
                 cgpaResultTextView.setText("CGPA: N/A (Total credits are zero or all subjects ignored)");
            }
            else {
                double cgpa = totalWeightedPoints / totalCredits;
                cgpaResultTextView.setText(String.format(Locale.getDefault(), "CGPA: %.2f", cgpa));
            }
        } else {
            cgpaResultTextView.setText("CGPA: Error");
        }
    }
} 