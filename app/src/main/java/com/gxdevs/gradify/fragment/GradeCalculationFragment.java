package com.gxdevs.gradify.fragment;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.gxdevs.gradify.R;
import com.gxdevs.gradify.Utils.Utils;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mariuszgromada.math.mxparser.Argument;
import org.mariuszgromada.math.mxparser.Expression;
import org.mariuszgromada.math.mxparser.PrimitiveElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GradeCalculationFragment extends Fragment {

    private MaterialAutoCompleteTextView levelDropdown;
    private MaterialAutoCompleteTextView subjectDropdown;
    private TextInputLayout levelHolder, subjectHolder;
    private LinearLayout dynamicInputContainer;
    private Button calculateButton;
    private TextView resultTextView;
    private View resultCard;

    private Map<String, List<String>> subjectsByLevel;
    private List<SubjectFormula> allFormulas;
    private Map<String, EditText> inputEditTexts = new HashMap<>(); // To store EditTexts for later retrieval

    // Data structure for holding formula details
    private static class SubjectFormula {
        String subjectName;
        String type;
        List<String> inputs;
        String formula;

        SubjectFormula(String subjectName, String type, List<String> inputs, String formula) {
            this.subjectName = subjectName;
            this.type = type;
            this.inputs = inputs;
            this.formula = formula;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_grade, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        levelDropdown = view.findViewById(R.id.levelDropdown);
        subjectDropdown = view.findViewById(R.id.subjectDropdown);
        dynamicInputContainer = view.findViewById(R.id.dynamicInputContainer);
        calculateButton = view.findViewById(R.id.calculateButton);
        resultTextView = view.findViewById(R.id.resultTextView);
        resultCard = view.findViewById(R.id.resultCard);
        levelHolder = view.findViewById(R.id.levelHolder);
        subjectHolder = view.findViewById(R.id.subjectHolder);

        loadFormulas(requireContext());
        subjectsByLevel = Utils.getSubjectsByLevel();

        setupLevelSpinner();

        calculateButton.setOnClickListener(v -> calculateGrade());
    }

    private void loadFormulas(Context context) {
        allFormulas = new ArrayList<>();
        // Show loading state if possible, e.g. disable button
        calculateButton.setEnabled(false);
        resultCard.setVisibility(View.VISIBLE);
        resultTextView.setText("Loading formulas...");

        Utils.fetchFormulas(context, new Utils.DatabaseCallback() {
            @Override
            public void onReady(JSONObject database) {
                try {
                    if (!database.has("formulas")) {
                        resultTextView.setText("Error: No formulas found.");
                        return;
                    }
                    JSONObject formulasObj = database.getJSONObject("formulas");
                    JSONArray names = formulasObj.names();
                    if (names == null)
                        return;

                    for (int i = 0; i < names.length(); i++) {
                        String subjectName = names.getString(i);
                        JSONObject subjectJson = formulasObj.getJSONObject(subjectName);

                        String type = subjectJson.optString("type", "subject");
                        String formulaStr = subjectJson.getString("formula");
                        JSONArray inputsArray = subjectJson.getJSONArray("inputs");
                        List<String> inputs = new ArrayList<>();
                        for (int j = 0; j < inputsArray.length(); j++) {
                            inputs.add(inputsArray.getString(j));
                        }

                        allFormulas.add(new SubjectFormula(subjectName, type, inputs, formulaStr));
                    }

                    // Refresh UI
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            resultCard.setVisibility(View.GONE);
                            resultTextView.setText("");
                            setupLevelSpinner(); // Re-setup to ensure subjects are filtered correctly
                        });
                    }

                } catch (JSONException e) {
                    if (isAdded()) {
                        requireActivity()
                                .runOnUiThread(() -> resultTextView.setText("Error: Could not parse formulas."));
                    }
                }
            }

            @Override
            public void onError(String error) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> resultTextView.setText("Error: " + error));
                }
            }
        });
    }

    private void setupLevelSpinner() {
        if (subjectsByLevel == null) {
            return;
        }
        List<String> levels = new ArrayList<>(subjectsByLevel.keySet());

        setupDropDown(levelDropdown, levels);

        levelDropdown.setOnItemClickListener((parent, view, position, id) -> {
            String selectedLevel = (String) parent.getItemAtPosition(position);
            setupSubjectSpinner(selectedLevel);
            dynamicInputContainer.removeAllViews(); // Clear previous inputs
            resultTextView.setText(""); // Clear previous result
        });
    }

    private void setupSubjectSpinner(String selectedLevel) {
        if (subjectsByLevel == null || allFormulas == null) {
            return;
        }
        List<String> subjectsForLevel = subjectsByLevel.get(selectedLevel);
        if (subjectsForLevel == null) {
            subjectsForLevel = new ArrayList<>();
        }

        // Filter subjects to only those present in the formula sheet for safety
        List<String> availableSubjects = new ArrayList<>();
        for (String subj : subjectsForLevel) {
            if (allFormulas.stream().anyMatch(f -> f.subjectName.equals(subj))) {
                availableSubjects.add(subj);
            }
        }

        setupDropDown(subjectDropdown, availableSubjects);
        subjectDropdown.setText("", false); // Clear previous selection

        subjectDropdown.setOnItemClickListener((parent, view, position, id) -> {
            String selectedSubject = (String) parent.getItemAtPosition(position);
            displayInputFields(selectedSubject);
            resultTextView.setText(""); // Clear previous result
        });
    }

    private void setupDropDown(MaterialAutoCompleteTextView dropdown, List<String> data) {
        Utils.setupDropDown(requireContext(), dropdown, data);
    }

    private void displayInputFields(String subjectName) {
        dynamicInputContainer.removeAllViews();
        inputEditTexts.clear(); // Clear previous EditTexts
        calculateButton.setEnabled(false);
        resultTextView.setText("");

        SubjectFormula selectedFormula = allFormulas.stream()
                .filter(f -> f.subjectName.equals(subjectName))
                .findFirst()
                .orElse(null);

        if (selectedFormula != null) {
            if (selectedFormula.formula.equalsIgnoreCase("Error")) {
                TextView errorMsg = new TextView(requireContext());
                errorMsg.setText("Formula not available for this subject.");
                errorMsg.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                dynamicInputContainer.addView(errorMsg);
                return; // Keep button disabled
            }

            calculateButton.setEnabled(true);
            for (String inputName : selectedFormula.inputs) {
                String originalName = inputName;
                switch (inputName) {
                    case "Quiz1":
                    case "Qz1":
                        inputName = "Quiz 1";
                        break;
                    case "Quiz2":
                    case "Qz2":
                        inputName = "Quiz 2";
                        break;
                    case "F":
                        inputName = "End Term";
                        break;
                }

                TextInputLayout textInputLayout = new TextInputLayout(requireContext(), null);
                textInputLayout.setHint(inputName);
                textInputLayout.setHintEnabled(true);
                textInputLayout.setBoxBackgroundColor(getResources().getColor(R.color.white));
                float radius = (float) convertDpToPx(16);
                textInputLayout.setBoxCornerRadii(radius, radius, radius, radius);
                textInputLayout.setBoxStrokeWidth(0);
                textInputLayout.setBoxStrokeWidthFocused(0);
                int hintColor = getResources().getColor(R.color.unselectedLevelText);
                textInputLayout.setHintTextColor(android.content.res.ColorStateList.valueOf(hintColor));
                textInputLayout.setDefaultHintTextColor(android.content.res.ColorStateList.valueOf(hintColor));

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                params.setMargins(0, convertDpToPx(8), 0, convertDpToPx(8));
                textInputLayout.setLayoutParams(params);

                com.google.android.material.textfield.TextInputEditText editText = new com.google.android.material.textfield.TextInputEditText(
                        textInputLayout.getContext());
                editText.setInputType(
                        android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
                editText.setTextColor(getResources().getColor(R.color.textIcons));
                editText.setTypeface(
                        androidx.core.content.res.ResourcesCompat.getFont(requireContext(), R.font.kanit));
                editText.setPadding(convertDpToPx(16), convertDpToPx(16), convertDpToPx(16), convertDpToPx(16));
                editText.setBackground(null);

                textInputLayout.addView(editText);
                dynamicInputContainer.addView(textInputLayout);
                inputEditTexts.put(originalName, editText); // Use original name for formula mapping
            }
        } else {
            TextView errorMsg = new TextView(requireContext());
            errorMsg.setText("No formula details found for " + subjectName);
            dynamicInputContainer.addView(errorMsg);
        }
    }

    private void calculateGrade() {
        String selectedSubjectName = subjectDropdown.getText().toString();
        if (selectedSubjectName.isEmpty() || selectedSubjectName.equals("Select Subject")) {
            resultCard.setVisibility(View.VISIBLE);
            resultTextView.setText("Please select a subject.");
            return;
        }

        SubjectFormula currentSubjectFormula = allFormulas.stream()
                .filter(f -> f.subjectName.equals(selectedSubjectName))
                .findFirst()
                .orElse(null);

        if (currentSubjectFormula == null || currentSubjectFormula.formula.equalsIgnoreCase("Error")) {
            resultCard.setVisibility(View.VISIBLE);
            resultTextView.setText("Formula not available for calculation.");
            return;
        }

        ArrayList<Argument> arguments = new ArrayList<>();
        boolean allInputsValid = true;

        for (String inputName : currentSubjectFormula.inputs) {
            EditText et = inputEditTexts.get(inputName);
            if (et == null) {
                resultTextView.setText("Internal error: Input field missing for " + inputName);
                allInputsValid = false;
                break;
            }
            String valueStr = et.getText().toString();
            if (valueStr.isEmpty()) {
                et.setError("Field cannot be empty");
                allInputsValid = false;
                continue; // Check all fields
            }
            try {
                double value = Double.parseDouble(valueStr);

                if (value > 100) {
                    et.setError("Value cannot exceed 100");
                    allInputsValid = false;
                    continue; // Check all fields
                }
                if (value < 0) {
                    et.setError("Value cannot be negative");
                    allInputsValid = false;
                    continue; // Check all fields
                }

                // mXparser arguments are case-sensitive, ensure they match formula
                arguments.add(new Argument(inputName.trim(), value));
            } catch (NumberFormatException e) {
                et.setError("Invalid number");
                allInputsValid = false;
            }
        }

        if (!allInputsValid) {
            resultCard.setVisibility(View.VISIBLE);
            resultTextView.setText("Please correct the errors.");
            return;
        }

        String formulaString = getString(currentSubjectFormula, selectedSubjectName);

        Expression expression = new Expression(formulaString, arguments.toArray(new PrimitiveElement[0]));

        if (expression.checkSyntax()) {
            double result = expression.calculate();
            if (Double.isNaN(result)) {
                resultCard.setVisibility(View.VISIBLE);
                resultTextView.setText("Calculation error (NaN).");
            } else {
                String grade = getString(result);
                String score = String.format(java.util.Locale.US, "Result: %.2f", result) + " / Grade: " + grade;
                // Format to 2 decimal places
                resultCard.setVisibility(View.VISIBLE);
                resultTextView.setText(score);
            }
        } else {
            resultCard.setVisibility(View.VISIBLE);
            resultTextView.setText("Error in formula syntax.");
        }
    }

    @NonNull
    private static String getString(SubjectFormula currentSubjectFormula, String selectedSubjectName) {
        String formulaString = currentSubjectFormula.formula;
        // Pre-process formula string for mXparser compatibility
        formulaString = formulaString.replace("Math.max", "max");
        formulaString = formulaString.replace("Math.min", "min");
        formulaString = formulaString.replace("Best(", "max("); // Assuming 'Best' is equivalent to 'max'

        if (selectedSubjectName.equals("Deep Learning Practice")) {
            formulaString = formulaString.replace("Lowest", "min(NPPE1, NPPE2, NPPE3)");
            formulaString = formulaString.replace("Second Best", "SecondBest(NPPE1, NPPE2, NPPE3)");
        }

        if (selectedSubjectName.equals("MLP")) {
            formulaString = formulaString.replace("OPEE1", "OPE1");
            formulaString = formulaString.replace("OPEE2", "OPE2");
        }
        if (selectedSubjectName.equals("Generative AI")) {
            formulaString = formulaString.replace("OPEPE", "OPPE");
        }
        return formulaString;
    }

    @NonNull
    private static String getString(double result) {
        String grade;
        if (result >= 90) {
            grade = "S";
        } else if (90 > result && result >= 80) {
            grade = "A";
        } else if (80 > result && result >= 70) {
            grade = "B";
        } else if (70 > result && result >= 60) {
            grade = "C";
        } else if (60 > result && result >= 50) {
            grade = "D";
        } else if (50 > result && result >= 40) {
            grade = "E";
        } else {
            grade = "U / Fail";
        }
        return grade;
    }

    private int convertDpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}