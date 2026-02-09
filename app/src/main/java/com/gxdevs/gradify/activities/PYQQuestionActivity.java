package com.gxdevs.gradify.activities;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.appcompat.widget.AppCompatRadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.window.OnBackInvokedDispatcher;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.github.chrisbanes.photoview.PhotoView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.textfield.TextInputEditText;
import com.gxdevs.gradify.R;
import com.gxdevs.gradify.Utils.Utils;
import com.gxdevs.gradify.adapters.QuestionSummaryAdapter;
import com.gxdevs.gradify.db.TimeTrackingDbHelper;
import com.gxdevs.gradify.models.QuestionSummary;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class PYQQuestionActivity extends AppCompatActivity {

    // UI Elements
    private TextView paperNameTextView, timerTextView, progressTextView, questionTextView;
    private TextView questionMarksTextView, questionTypeTextView;
    private TextView resultTitleTextView, correctAnswerTextView, explanationTextView;
    private ImageView questionImageView;
    private RadioGroup singleChoiceGroup;
    private LinearLayout multipleChoiceGroup, textInputGroup;
    private CardView resultCardView, questionCardView, answerCardView;
    private Button previousButton, nextButton, checkButton, submitQuizButton;
    private LinearLayout timerLayout;
    private TextInputEditText answerEditText;
    private View extraInfoView;
    private FloatingActionButton extraInfoFab;
    private LinearLayout abortContainer;

    // Data
    private JSONObject quizData;
    private JSONArray questions;
    private JSONObject paperInfo;
    private int currentQuestionIndex = 0;
    private boolean isExamMode = false;
    private boolean isReviewMode = false;
    private CountDownTimer countDownTimer;
    private long timeRemainingMillis;
    private final Map<Integer, String> userTextAnswers = new HashMap<>();
    private final Map<Integer, List<Integer>> userMcqAnswers = new HashMap<>();
    private final Map<Integer, Integer> userSingleMcqAnswers = new HashMap<>();
    private final Map<Integer, Double> questionScores = new HashMap<>();
    private final Map<Integer, Set<Integer>> extraInfoToQuestionsMap = new HashMap<>(); // Maps extra info indices to question indices
    private final Map<Integer, Integer> questionToExtraInfoMap = new HashMap<>(); // Maps question indices to extra info indices
    private int totalQuestions = 0;
    private int totalDisplayQuestions = 0;
    private double totalMarks = 0;
    private double userScore = 0;
    private final List<QuestionSummary> questionSummaries = new ArrayList<>();
    private long pyqStartTimeMillis; // Added for Time Tracking
    private String subjectNameForTimeTracking; // Added for Time Tracking
    private TimeTrackingDbHelper dbHelper; // Added for DB operations
    private String link;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pyq_question);

        Utils.setPad(findViewById(R.id.containerPYQQuestion), "bottom", this);

        // Get intent data
        Intent intent = getIntent();
        isExamMode = intent.getBooleanExtra("EXAM_MODE", false);
        subjectNameForTimeTracking = intent.getStringExtra("SUBJECT");
        subjectNameForTimeTracking = subjectNameForTimeTracking.replace("_", " ");
        if (subjectNameForTimeTracking.isEmpty()) {
            subjectNameForTimeTracking = "Unknown Subject"; // Default if not provided
            Log.w("TimeTracker", "Subject not passed via intent for PYQQuestionActivity. Using default.");
        }
        link = intent.getStringExtra("link");

        initViews();
        setupListeners();
        dbHelper = new TimeTrackingDbHelper(this); // Initialize DB Helper

        // Configure UI based on mode
        if (!isExamMode) {
            timerLayout.setVisibility(GONE);
            checkButton.setVisibility(VISIBLE);
        }

        // Fetch quiz data
        fetchQuizData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        pyqStartTimeMillis = System.currentTimeMillis();
        Log.d("TimeTracker", "PYQQuestionActivity resumed for subject: " + subjectNameForTimeTracking + " at " + pyqStartTimeMillis);
    }

    private void initViews() {
        paperNameTextView = findViewById(R.id.paperNameTextView);
        timerTextView = findViewById(R.id.timerTextView);
        progressTextView = findViewById(R.id.progressTextView);
        questionTextView = findViewById(R.id.questionTextView);
        questionMarksTextView = findViewById(R.id.questionMarksTextView);
        questionTypeTextView = findViewById(R.id.questionTypeTextView);
        questionImageView = findViewById(R.id.questionImageView);
        singleChoiceGroup = findViewById(R.id.singleChoiceGroup);
        multipleChoiceGroup = findViewById(R.id.multipleChoiceGroup);
        textInputGroup = findViewById(R.id.textInputGroup);
        resultCardView = findViewById(R.id.resultCardView);
        questionCardView = findViewById(R.id.questionCardView);
        previousButton = findViewById(R.id.previousButton);
        nextButton = findViewById(R.id.nextButton);
        checkButton = findViewById(R.id.checkButton);
        submitQuizButton = findViewById(R.id.submitQuizButton);
        timerLayout = findViewById(R.id.timerLayout);
        answerEditText = findViewById(R.id.answerEditText);
        resultTitleTextView = findViewById(R.id.resultTitleTextView);
        correctAnswerTextView = findViewById(R.id.correctAnswerTextView);
        explanationTextView = findViewById(R.id.explanationTextView);
        extraInfoFab = findViewById(R.id.extraInfoFab);
        abortContainer = findViewById(R.id.abortContainer);
        answerCardView = findViewById(R.id.answerCardView);

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    private void setupListeners() {
        previousButton.setOnClickListener(v -> showPreviousQuestion());
        nextButton.setOnClickListener(v -> {
            // Check if we're on the last question
            try {
                boolean isLastQuestion = false;
                int lastQuestionIndex = -1;
                int largestQuestionNumber = 0;

                for (int i = 0; i < totalQuestions; i++) {
                    JSONObject question = questions.getJSONObject(i);

                    if (!isExtraInfoSection(question) && question.has("question_number")) {
                        int questionNumber = question.getInt("question_number");
                        if (questionNumber > largestQuestionNumber) {
                            largestQuestionNumber = questionNumber;
                            lastQuestionIndex = i;
                        }
                    }
                }

                isLastQuestion = (currentQuestionIndex == lastQuestionIndex);

                if (isLastQuestion && !isReviewMode) {
                    // If it's the last question and we're not in review mode, submit the quiz
                    confirmSubmitQuiz();
                } else {
                    // Otherwise, go to the next question
                    showNextQuestion();
                }
            } catch (JSONException e) {
                // If there's an error, just try to go to the next question
                showNextQuestion();
            }
        });
        checkButton.setOnClickListener(v -> checkAnswer());
        submitQuizButton.setOnClickListener(v -> confirmSubmitQuiz());

        // Setup listener for the question image to show full screen
        questionImageView.setOnClickListener(v -> {
            if (questionImageView.getVisibility() == VISIBLE) {
                showImageFullScreen(questionImageView);
            }
        });

        // Setup extra info FAB listener
        extraInfoFab.setOnClickListener(v -> {
            if (questionToExtraInfoMap.containsKey(currentQuestionIndex)) {
                int extraInfoIndex = questionToExtraInfoMap.get(currentQuestionIndex);
                try {
                    showExtraInfoDialog(extraInfoIndex);
                } catch (JSONException e) {
                    Log.e("ExtraInfoError", "Error showing extra info: " + e.getMessage());
                }
            }
        });

        // Setup listener for abort button container
        abortContainer.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Abort Quiz")
                    .setMessage("Are you sure you want to abort the quiz? Your progress will not be saved.")
                    .setPositiveButton("Yes, Abort", (dialog, which) -> {
                        if (countDownTimer != null) {
                            countDownTimer.cancel();
                        }
                        finish(); // Finish activity and go back
                    })
                    .setNegativeButton("No", null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        });
    }

    private void fetchQuizData() {
        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, link, null,
                response -> {
                    quizData = response;
                    try {
                        paperInfo = quizData.getJSONArray("papers").getJSONObject(0);
                        questions = quizData.getJSONArray("questions");
                        totalQuestions = questions.length();

                        // Process extra info sections and map them to questions
                        processExtraInfoSections();

                        // Calculate display question count (excluding extra info sections)
                        calculateDisplayQuestionCount();

                        // Update paper name
                        paperNameTextView.setText(subjectNameForTimeTracking);

                        // Calculate total marks
                        calculateTotalMarks();

                        // Start timer if in exam mode
                        if (isExamMode) {
                            // Assuming "total_time_minutes" in JSON might be a string.
                            String timeStr = paperInfo.getString("total_time_minutes");
                            if (timeStr.equals("4")) {
                                timeStr = "60";
                            }
                            startTimer(Long.parseLong(timeStr) * 60 * 1000);
                        }

                        // Show first question
                        showQuestion(0);
                    } catch (JSONException e) {
                        Toast.makeText(this, "Error parsing data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("JSONError", Objects.requireNonNull(e.getMessage()));
                    }
                },
                error -> {
                    Toast.makeText(this, "Error fetching quiz data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("APIError", error.toString());
                });

        queue.add(jsonObjectRequest);
    }

    private void processExtraInfoSections() throws JSONException {
        // Clear existing mappings
        extraInfoToQuestionsMap.clear();
        questionToExtraInfoMap.clear();

        // Find all extra info sections and create mappings
        for (int i = 0; i < totalQuestions; i++) {
            JSONObject question = questions.getJSONObject(i);

            if (isExtraInfoSection(question)) {
                // Check if it has for_questions field
                if (question.has("for_questions")) {
                    String forQuestionsStr = question.getString("for_questions");

                    // Parse the comma-separated question numbers
                    Set<Integer> relatedQuestions = new HashSet<>();
                    String[] questionNums = forQuestionsStr.split(",");

                    for (String num : questionNums) {
                        try {
                            int questionNumber = Integer.parseInt(num.trim());

                            // Find the question index with this question_number
                            int questionIndex = findQuestionIndexByNumber(questionNumber);
                            if (questionIndex >= 0) {
                                relatedQuestions.add(questionIndex);
                                questionToExtraInfoMap.put(questionIndex, i);
                            }
                        } catch (NumberFormatException e) {
                            Log.e("ExtraInfoError", "Invalid question number: " + num);
                        }
                    }

                    // Store the mapping
                    extraInfoToQuestionsMap.put(i, relatedQuestions);
                }
            }
        }
    }

    // Find a question by its question_number
    private int findQuestionIndexByNumber(int questionNumber) throws JSONException {
        for (int i = 0; i < totalQuestions; i++) {
            JSONObject question = questions.getJSONObject(i);

            // Skip extra info sections
            if (isExtraInfoSection(question)) {
                continue;
            }

            // Check if this question has the desired question_number
            if (question.has("question_number") && question.getInt("question_number") == questionNumber) {
                return i;
            }
        }

        return -1; // Question number not found
    }

    private void calculateDisplayQuestionCount() throws JSONException {
        totalDisplayQuestions = 0;
        for (int i = 0; i < totalQuestions; i++) {
            JSONObject question = questions.getJSONObject(i);
            if (!isExtraInfoSection(question)) {
                totalDisplayQuestions++;
            }
        }

        // Get the maximum question number to ensure we show correct total
        int maxQuestionNumber = 0;
        for (int i = 0; i < totalQuestions; i++) {
            JSONObject question = questions.getJSONObject(i);
            if (!isExtraInfoSection(question) && question.has("question_number")) {
                maxQuestionNumber = Math.max(maxQuestionNumber, question.getInt("question_number"));
            }
        }

        if (maxQuestionNumber > 0) {
            totalDisplayQuestions = maxQuestionNumber;
        }
    }

    private void calculateTotalMarks() throws JSONException {
        totalMarks = 0;
        for (int i = 0; i < totalQuestions; i++) {
            JSONObject question = questions.getJSONObject(i);
            if (!isExtraInfoSection(question)) {
                totalMarks += question.getDouble("marks");
            }
        }
    }

    private boolean isExtraInfoSection(JSONObject question) throws JSONException {
        return "extra_info".equals(question.getString("question_text"));
    }

    private void showQuestion(int index) {
        try {
            currentQuestionIndex = index;
            JSONObject question = questions.getJSONObject(index);

            // Check if this is an extra info section
            if (isExtraInfoSection(question)) {
                showExtraInfoSection(question);
                return;
            }

            // Regular question display
            showRegularQuestion(question);

        } catch (JSONException e) {
            Toast.makeText(this, "Error loading question: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("QuestionError", Objects.requireNonNull(e.getMessage()));
        }
    }

    private void showRegularQuestion(JSONObject question) throws JSONException {
        // Remove any existing extra info view
        if (extraInfoView != null) {
            ViewGroup parent = (ViewGroup) extraInfoView.getParent();
            if (parent != null) {
                parent.removeView(extraInfoView);
            }
            extraInfoView = null;
        }

        // Clear any existing correct answer indicators
        clearCorrectAnswerIndicators();

        // Show the question card
        questionCardView.setVisibility(VISIBLE);

        // Get the question number directly from the JSON
        int questionNumber = question.getInt("question_number");

        // Update progress text
        progressTextView.setText(String.format(Locale.getDefault(), "Question %d/%d", questionNumber, totalDisplayQuestions));

        // Show/hide submit button on last question
        updateNavigationButtons();

        // Update question info
        String questionTxt = question.optString("question_text", "").trim();
        if (!questionTxt.isEmpty()) {
            questionTextView.setText(Html.fromHtml(questionTxt, Html.FROM_HTML_MODE_LEGACY));
            if (questionTextView.getVisibility() == GONE) {
                questionTextView.setVisibility(VISIBLE);
            }
        } else {
            questionTextView.setVisibility(GONE);
        }
        questionMarksTextView.setText(question.getInt("marks") + " Marks");

        // Handle question image
        String questionImageUrl = question.optString("question_image_url", "");
        if (!questionImageUrl.isEmpty()) {
            questionImageView.setVisibility(View.VISIBLE);

            RequestOptions opts = new RequestOptions()
                    // Give me the original dimensions, no downSampling
                    .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                    // Don’t transform or crop the bitmap
                    .dontTransform()
                    // Highest-quality decode (32-bit RGBA)
                    .format(DecodeFormat.PREFER_ARGB_8888);

            Glide.with(this)
                    .load(questionImageUrl)
                    .apply(opts)
                    .into(questionImageView);
        } else {
            questionImageView.setVisibility(View.GONE);
        }

        // Hide all answer containers initially
        singleChoiceGroup.setVisibility(GONE);
        multipleChoiceGroup.setVisibility(GONE);
        textInputGroup.setVisibility(GONE);
        resultCardView.setVisibility(GONE);

        // Get question type and setup appropriate UI
        String questionType = question.getString("question_type");
        if (questionType.equals("mcq")) {
            JSONArray options = question.getJSONArray("options");

            // Count correct answers
            int correctCount = 0;
            for (int i = 0; i < options.length(); i++) {
                if (options.getJSONObject(i).getBoolean("is_correct")) {
                    correctCount++;
                }
            }

            if (answerCardView.getVisibility() == GONE) {
                answerCardView.setVisibility(VISIBLE);
            }
            if (correctCount > 1) {
                setupMultipleChoiceQuestion(options);
                questionTypeTextView.setText(R.string.multiple_answers);
            } else {
                setupSingleChoiceQuestion(options);
                questionTypeTextView.setText(R.string.single_answer);
            }
        } else {
            answerCardView.setVisibility(VISIBLE);
            setupTextInputQuestion(question);
            questionTypeTextView.setText(R.string.text_answer);
        }

        // Restore user's previous answers if available
        restorePreviousAnswers();

        // In review mode, always show the correct answers
        if (isReviewMode) {
            showCorrectAnswers(question);
        }

        // Check if this question has associated extra info and show the FAB
        if (questionToExtraInfoMap.containsKey(currentQuestionIndex)) {
            extraInfoFab.setVisibility(VISIBLE);
        } else {
            extraInfoFab.setVisibility(GONE);
        }
    }

    private void clearCorrectAnswerIndicators() {
        // Clear any indicators in the single choice group parent
        if (singleChoiceGroup.getParent() instanceof ViewGroup) {
            ViewGroup singleChoiceParent = (ViewGroup) singleChoiceGroup.getParent();
            for (int i = singleChoiceParent.getChildCount() - 1; i >= 0; i--) {
                View child = singleChoiceParent.getChildAt(i);
                if (child.getId() == R.id.correctAnswerInfoTextView ||
                        (child instanceof ViewGroup && child.findViewById(R.id.correctAnswerInfoTextView) != null)) {
                    singleChoiceParent.removeViewAt(i);
                }
            }
        }

        // Clear any indicators in the multiple choice group parent
        if (multipleChoiceGroup.getParent() instanceof ViewGroup) {
            ViewGroup multiChoiceParent = (ViewGroup) multipleChoiceGroup.getParent();
            for (int i = multiChoiceParent.getChildCount() - 1; i >= 0; i--) {
                View child = multiChoiceParent.getChildAt(i);
                if (child.getId() == R.id.correctAnswerInfoTextView ||
                        (child instanceof ViewGroup && child.findViewById(R.id.correctAnswerInfoTextView) != null)) {
                    multiChoiceParent.removeViewAt(i);
                }
            }
        }

        // Clear any indicators in the text input group parent
        if (textInputGroup.getParent() instanceof ViewGroup) {
            ViewGroup textInputParent = (ViewGroup) textInputGroup.getParent();
            for (int i = textInputParent.getChildCount() - 1; i >= 0; i--) {
                View child = textInputParent.getChildAt(i);
                if (child.getId() == R.id.correctAnswerInfoTextView ||
                        (child instanceof ViewGroup && child.findViewById(R.id.correctAnswerInfoTextView) != null)) {
                    textInputParent.removeViewAt(i);
                }
            }
        }
    }

    @SuppressLint("InflateParams")
    private void showExtraInfoSection(JSONObject extraInfo) throws JSONException {
        // 1) Remove any existing extra-info view
        if (extraInfoView != null) {
            ViewGroup oldParent = (ViewGroup) extraInfoView.getParent();
            if (oldParent != null) {
                oldParent.removeView(extraInfoView);
            }
        }
        clearCorrectAnswerIndicators();

        // 2) Inflate with the real parent (so match_parent works)
        NestedScrollView scrollView = findViewById(R.id.questionScrollView);
        ViewGroup container = (ViewGroup) scrollView.getChildAt(0);
        LayoutInflater inflater = LayoutInflater.from(this);
        extraInfoView = inflater.inflate(
                R.layout.item_extra_info,
                container,
                false
        );

        // 3) Bind inner views from the newly inflated hierarchy
        TextView extraInfoTextView = extraInfoView.findViewById(R.id.extraInfoTextView);
        ImageView extraInfoImageView = extraInfoView.findViewById(R.id.extraInfoImageView);

        // 4) Populate text
        String extraText = extraInfo.optString("extra_text", "").trim();
        if (!extraText.isEmpty()) {
            extraInfoTextView.setText(
                    Html.fromHtml(extraText, Html.FROM_HTML_MODE_LEGACY)
            );
            extraInfoTextView.setVisibility(View.VISIBLE);
        } else {
            extraInfoTextView.setVisibility(View.GONE);
        }

        // 5) Populate image
        String imageUrl = extraInfo.optString("question_image_url", "");
        if (!imageUrl.isEmpty()) {
            extraInfoImageView.setVisibility(View.VISIBLE);
            Glide.with(this)
                    .load(imageUrl)
                    .into(extraInfoImageView);
            extraInfoImageView.setOnClickListener(v ->
                    showImageFullScreen(extraInfoImageView)
            );
        } else {
            extraInfoImageView.setVisibility(View.GONE);
        }

        // 6) Hide all question UI
        questionCardView.setVisibility(View.GONE);
        resultCardView.setVisibility(View.GONE);
        singleChoiceGroup.setVisibility(View.GONE);
        multipleChoiceGroup.setVisibility(View.GONE);
        textInputGroup.setVisibility(View.GONE);
        answerCardView.setVisibility(View.GONE);

        // 7) Add the extra-info card at the top
        container.addView(extraInfoView, 0);

        // 8) Tidy up
        extraInfoFab.setVisibility(View.GONE);
        updateNavigationButtons();
        progressTextView.setText(R.string.extra_info);
    }

    private void showImageFullScreen(ImageView sourceImageView) {
        // Create dialog without title
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_fullscreen_image);
        dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);

        // Get views
        PhotoView photoView = dialog.findViewById(R.id.fullScreenImageView);
        ImageButton closeButton = dialog.findViewById(R.id.closeImageButton);

        // Load image
        photoView.setImageDrawable(sourceImageView.getDrawable());

        // Set close button listener
        closeButton.setOnClickListener(v -> dialog.dismiss());

        // Show dialog
        dialog.show();
    }

    private void showExtraInfoDialog(int extraInfoIndex) throws JSONException {
        JSONObject extraInfo = questions.getJSONObject(extraInfoIndex);

        // Create dialog
        Dialog dialog = new Dialog(this);
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_extra_info);

        // Set dialog size to almost full screen
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.MATCH_PARENT;
        dialog.getWindow().setAttributes(lp);
        dialog.getWindow().setDimAmount(0.8f); // Darken the background

        // Get views
        TextView titleTextView = dialog.findViewById(R.id.extraInfoTitle);
        TextView contentTextView = dialog.findViewById(R.id.extraInfoText);
        ImageView imageView = dialog.findViewById(R.id.extraInfoImage);
        Button closeButton = dialog.findViewById(R.id.btnCloseExtraInfo);

        // Set content
        String extraText = extraInfo.optString("extra_text", "").trim();
        if (!extraText.isEmpty()) {
            contentTextView.setText(Html.fromHtml(extraText, Html.FROM_HTML_MODE_LEGACY));
            contentTextView.setVisibility(VISIBLE);
        } else {
            contentTextView.setVisibility(GONE);
        }

        // Handle image if present
        String imageUrl = extraInfo.optString("question_image_url", "");
        if (!imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .into(imageView);
            imageView.setVisibility(VISIBLE);

            // Make the image clickable for full-screen viewing
            imageView.setOnClickListener(v -> showImageFullScreen(imageView));
        } else {
            imageView.setVisibility(GONE);
        }

        // Set close button listener
        closeButton.setOnClickListener(v -> dialog.dismiss());

        // Show dialog
        dialog.show();
    }

    private void updateNavigationButtons() {
        try {
            // Show/hide submit button on last question
            boolean isLastQuestion;
            boolean isFirstQuestion;

            // Find the actual first and last displayable questions
            int firstQuestionIndex = -1;
            int lastQuestionIndex = -1;
            int smallestQuestionNumber = Integer.MAX_VALUE;
            int largestQuestionNumber = 0;

            for (int i = 0; i < totalQuestions; i++) {
                JSONObject question = questions.getJSONObject(i);

                if (!isExtraInfoSection(question) && question.has("question_number")) {
                    int questionNumber = question.getInt("question_number");

                    // Track smallest question number (first question)
                    if (questionNumber < smallestQuestionNumber) {
                        smallestQuestionNumber = questionNumber;
                        firstQuestionIndex = i;
                    }

                    // Track largest question number (last question)
                    if (questionNumber > largestQuestionNumber) {
                        largestQuestionNumber = questionNumber;
                        lastQuestionIndex = i;
                    }
                }
            }

            // Check if current question is first or last
            isFirstQuestion = (currentQuestionIndex == firstQuestionIndex);
            isLastQuestion = (currentQuestionIndex == lastQuestionIndex);

            // Always show the Previous button except on first question
            previousButton.setVisibility(isFirstQuestion ? GONE : VISIBLE);

            // Always show the Next button but change its text if it's the last question
            if (isLastQuestion && !isReviewMode) {
                nextButton.setText(R.string.submit);
            } else {
                nextButton.setText(R.string.next);
            }
            nextButton.setVisibility(VISIBLE);

            // Hide the separate submit button completely
            submitQuizButton.setVisibility(GONE);
        } catch (JSONException e) {
            Log.e("NavigationError", Objects.requireNonNull(e.getMessage()));
        }
    }

    private void setupSingleChoiceQuestion(JSONArray options) throws JSONException {
        singleChoiceGroup.removeAllViews();
        singleChoiceGroup.setVisibility(VISIBLE);
        singleChoiceGroup.clearCheck();

        // Get the current selected answer for this question
        Integer selectedOption = userSingleMcqAnswers.get(currentQuestionIndex);
        Log.d("RadioButtonSetup", "Question " + currentQuestionIndex + " selected option: " + selectedOption);

        // Track all radio buttons to implement custom RadioGroup behavior
        List<MaterialRadioButton> allRadioButtons = new ArrayList<>();

        for (int i = 0; i < options.length(); i++) {
            JSONObject option = options.getJSONObject(i);
            MaterialRadioButton radioButton = new MaterialRadioButton(this);
            radioButton.setId(View.generateViewId());
            radioButton.setText(Html.fromHtml(option.getString("text").trim(), Html.FROM_HTML_MODE_LEGACY));
            radioButton.setTextColor(Color.WHITE);
            radioButton.setTextSize(16); // Increased text size
            radioButton.setTag(i);
            Utils.radioColors(this, radioButton);

            // Add to our tracking list
            allRadioButtons.add(radioButton);

            // Check if this option is selected
            if (selectedOption != null && selectedOption == i) {
                radioButton.setChecked(true);
                Log.d("RadioButtonSetup", "Setting option " + i + " checked");
            } else {
                radioButton.setChecked(false);
            }

            // Custom click listener for all radio buttons to implement mutual exclusion
            int finalI = i;
            radioButton.setOnClickListener(v -> {
                // Uncheck all other radio buttons
                for (MaterialRadioButton rb : allRadioButtons) {
                    if (rb != radioButton) {
                        rb.setChecked(false);
                    }
                }

                // Ensure this one is checked
                radioButton.setChecked(true);

                // Save the selection
                userSingleMcqAnswers.put(currentQuestionIndex, finalI);
                Log.d("RadioButton", "Selected option " + finalI + " for question " + currentQuestionIndex);
            });

            // If the option has an image, add it
            String imageUrl = option.getString("image_url");
            if (!imageUrl.isEmpty()) {
                // Create container with full width
                LinearLayout container = new LinearLayout(this);
                container.setOrientation(LinearLayout.VERTICAL);
                container.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
                container.addView(radioButton);
                container.setTag(i); // Also set the tag on container for easy access

                // Create card container for image (with border/shadow)
                CardView cardView = new CardView(this);
                cardView.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
                cardView.setCardElevation(4f);
                cardView.setRadius(8f);
                cardView.setContentPadding(4, 4, 4, 4);
                cardView.setCardBackgroundColor(Color.WHITE);
                cardView.setUseCompatPadding(true);

                ImageView imageView = new ImageView(this);
                imageView.setAdjustViewBounds(true);

                // Make image fill width and set minimum height to ensure visibility
                LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                imageParams.setMargins(0, 0, 0, 0);
                imageView.setLayoutParams(imageParams);
                imageView.setMinimumHeight(500); // Set minimum height explicitly
                imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

                // Add image to card and card to container
                cardView.addView(imageView);
                container.addView(cardView);

                // Load the image
                Glide.with(this)
                        .load(imageUrl)
                        .override(1024, 1024) // Request larger image size
                        .into(imageView);

                // Make the image clickable for full-screen viewing
                imageView.setOnClickListener(v -> {
                    try {
                        String url = options.getJSONObject(finalI).getString("image_url");
                        if (!url.isEmpty()) {
                            showImageFullScreen(imageView);
                        }
                    } catch (JSONException e) {
                        Log.e("ImageError", "Error showing image: " + e.getMessage());
                    }
                });

                // Make the entire container clickable to select the radio button
                container.setOnClickListener(v -> {
                    // Trigger the radio button's click handler
                    radioButton.performClick();
                });

                singleChoiceGroup.addView(container);
            } else {
                singleChoiceGroup.addView(radioButton);
            }

            // In review mode, highlight correct options
            if (isReviewMode && option.getBoolean("is_correct")) {
                radioButton.setTextColor(Color.parseColor("#4CAF50"));
            }
        }

        // We don't need the RadioGroup's listener anymore since we handle selection in the RadioButtons
        singleChoiceGroup.setOnCheckedChangeListener(null);
    }

    private void setupMultipleChoiceQuestion(JSONArray options) throws JSONException {
        multipleChoiceGroup.removeAllViews();
        multipleChoiceGroup.setVisibility(VISIBLE);

        // Get existing selected options for this question
        List<Integer> selectedOptions = userMcqAnswers.getOrDefault(currentQuestionIndex, new ArrayList<>());
        Log.d("CheckboxSetup", "Question " + currentQuestionIndex + " selected options: " + selectedOptions);

        for (int i = 0; i < options.length(); i++) {
            JSONObject option = options.getJSONObject(i);
            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(Html.fromHtml(option.getString("text").trim(), Html.FROM_HTML_MODE_LEGACY));
            checkBox.setTextColor(Color.WHITE);
            checkBox.setTextSize(16); // Increased text size
            checkBox.setTag(i);
            Utils.checkBoxColors(this, checkBox);

            // Set checked state from saved selections
            boolean isChecked = selectedOptions.contains(i);
            checkBox.setChecked(isChecked);
            Log.d("CheckboxSetup", "Option " + i + " checked: " + isChecked);

            // Checkbox click listener (not change listener)
            int finalI = i;
            checkBox.setOnClickListener(v -> {
                boolean newCheckedState = checkBox.isChecked();
                updateMultipleChoiceSelection(finalI, newCheckedState);
                Log.d("Checkbox", "Option " + finalI + " " + (newCheckedState ? "checked" : "unchecked"));
            });

            // If the option has an image, add it
            String imageUrl = option.getString("image_url");
            if (!imageUrl.isEmpty()) {
                // Create container with full width
                LinearLayout container = new LinearLayout(this);
                container.setOrientation(LinearLayout.VERTICAL);
                container.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
                container.addView(checkBox);
                container.setTag(i); // Also set the tag on container for easy access

                // Create card container for image (with border/shadow)
                CardView cardView = new CardView(this);
                cardView.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
                cardView.setCardElevation(4f);
                cardView.setRadius(8f);
                cardView.setContentPadding(4, 4, 4, 4);
                cardView.setCardBackgroundColor(Color.WHITE);
                cardView.setUseCompatPadding(true);

                ImageView imageView = new ImageView(this);
                imageView.setAdjustViewBounds(true);

                // Make image fill width and set minimum height to ensure visibility
                LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                imageParams.setMargins(0, 0, 0, 0);
                imageView.setLayoutParams(imageParams);
                imageView.setMinimumHeight(500); // Set minimum height explicitly
                imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

                // Add image to card and card to container
                cardView.addView(imageView);
                container.addView(cardView);

                // Load the image
                Glide.with(this)
                        .load(imageUrl)
                        .override(1024, 1024) // Request larger image size
                        .into(imageView);

                // Make the image clickable for full-screen viewing
                imageView.setOnClickListener(v -> showImageFullScreen(imageView));

                // Make the container clickable to toggle the checkbox
                container.setOnClickListener(v -> {
                    // Trigger the checkbox's click handler
                    checkBox.performClick();
                });

                multipleChoiceGroup.addView(container);
            } else {
                multipleChoiceGroup.addView(checkBox);
            }

            // In review mode, highlight correct options
            if (isReviewMode && option.getBoolean("is_correct")) {
                checkBox.setTextColor(Color.parseColor("#4CAF50"));
            }
        }
    }

    private void updateMultipleChoiceSelection(int optionIndex, boolean isSelected) {
        // Get or create the list of selected options for this question
        List<Integer> selectedOptions = userMcqAnswers.getOrDefault(currentQuestionIndex, new ArrayList<>());

        // Update the list
        if (isSelected) {
            if (!selectedOptions.contains(optionIndex)) {
                selectedOptions.add(optionIndex);
            }
        } else {
            selectedOptions.remove(Integer.valueOf(optionIndex));
        }

        // Save the updated list
        userMcqAnswers.put(currentQuestionIndex, selectedOptions);

        Log.d("MultipleChoice", "Updated selections for question " + currentQuestionIndex +
                ": option " + optionIndex + " " + (isSelected ? "selected" : "deselected") +
                ", current selections: " + selectedOptions);
    }

    private void setupTextInputQuestion(JSONObject question) throws JSONException {
        textInputGroup.setVisibility(VISIBLE);

        // Clear the EditText first
        answerEditText.setText("");

        // Restore any previously entered text
        String userAnswer = userTextAnswers.get(currentQuestionIndex);
        if (userAnswer != null) {
            answerEditText.setText(userAnswer);
        }

        // Set a listener to save text as user types
        answerEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                saveTextAnswer();
            }
        });

        // In review mode, show the correct answer
        if (isReviewMode) {
            String correctAnswer = question.getString("correct_answer_text");
            LinearLayout container = (LinearLayout) textInputGroup.getParent();

            // Add a view to show the correct answer
            View correctAnswerView = LayoutInflater.from(this).inflate(
                    R.layout.item_correct_answer, container, false);
            TextView correctAnswerInfo = correctAnswerView.findViewById(R.id.correctAnswerInfoTextView);
            String correctAns = "Correct answer: " + correctAnswer;
            correctAnswerInfo.setText(Html.fromHtml(correctAns.trim(), Html.FROM_HTML_MODE_LEGACY));

            // Add to container
            container.addView(correctAnswerView);
        }
    }

    private void saveTextAnswer() {
        // Save the current text input if visible
        if (textInputGroup.getVisibility() == VISIBLE) {
            String text = Objects.requireNonNull(answerEditText.getText()).toString().trim();
            userTextAnswers.put(currentQuestionIndex, text);
        }
    }

    private void showPreviousQuestion() {
        if (currentQuestionIndex > 0) {
            // Save current answers
            saveCurrentAnswers();

            // Find the previous non-extra-info question
            int prevIndex = currentQuestionIndex - 1;
            showQuestion(prevIndex);
        }
    }

    private void showNextQuestion() {
        if (currentQuestionIndex < totalQuestions - 1) {
            // Save current answers
            saveCurrentAnswers();

            // Find the next non-extra-info question
            int nextIndex = currentQuestionIndex + 1;
            showQuestion(nextIndex);
        }
    }

    private void saveCurrentAnswers() {
        try {
            JSONObject question = questions.getJSONObject(currentQuestionIndex);

            // Don't save answers for extra info
            if (isExtraInfoSection(question)) {
                return;
            }

            String questionType = question.getString("question_type");

            if (!questionType.equals("mcq")) {
                // Save text answer
                saveTextAnswer();
            }
            // MCQ answers are saved by their respective listeners
        } catch (JSONException e) {
            Log.e("SaveAnswerError", Objects.requireNonNull(e.getMessage()));
        }
    }

    private void restorePreviousAnswers() {
        try {
            JSONObject question = questions.getJSONObject(currentQuestionIndex);

            // No need to restore answers for extra info
            if (isExtraInfoSection(question)) {
                return;
            }

            String questionType = question.getString("question_type");
            Log.d("RestoreAnswers", "Restoring answers for question " + currentQuestionIndex + " of type " + questionType);

            if (questionType.equals("mcq")) {
                // We don't need to manually restore MCQ answers here anymore
                // The setup methods now handle this when creating the UI elements
                // This avoids conflicts between programmatic changes and listeners
                Log.d("RestoreAnswers", "MCQ answers will be set directly during setup");
            } else {
                // Restore text answer
                String answer = userTextAnswers.get(currentQuestionIndex);
                Log.d("RestoreAnswers", "Text answer: " + (answer != null ? answer : "null"));
                if (answer != null) {
                    answerEditText.setText(answer);
                } else {
                    answerEditText.setText("");
                }
            }
        } catch (JSONException e) {
            Log.e("RestoreAnswerError", Objects.requireNonNull(e.getMessage()));
        }
    }

    private void showCorrectAnswers(JSONObject question) throws JSONException {
        String questionType = question.getString("question_type");
        if (questionType.equals("mcq")) {
            JSONArray options = question.getJSONArray("options");

            // Add a view to display score for this question
            if (questionScores.containsKey(currentQuestionIndex)) {
                double score = questionScores.get(currentQuestionIndex);
                int totalMarks = question.getInt("marks");

                // Count correct answers to determine if it's multiple or single choice
                int correctCount = 0;
                for (int i = 0; i < options.length(); i++) {
                    if (options.getJSONObject(i).getBoolean("is_correct")) {
                        correctCount++;
                    }
                }

                ViewGroup container;
                if (correctCount > 1) {
                    // Multiple choice
                    container = (ViewGroup) multipleChoiceGroup.getParent();
                } else {
                    // Single choice
                    container = (ViewGroup) singleChoiceGroup.getParent();
                }

                // Check if we already added a score view
                boolean scoreViewExists = false;
                for (int i = 0; i < container.getChildCount(); i++) {
                    View child = container.getChildAt(i);
                    if (child.getId() == R.id.correctAnswerInfoTextView) {
                        scoreViewExists = true;
                        break;
                    }
                }

                // Only add the score view if it doesn't exist
                if (!scoreViewExists) {
                    View scoreView = LayoutInflater.from(this).inflate(
                            R.layout.item_correct_answer,
                            container, false);
                    TextView scoreText = scoreView.findViewById(R.id.correctAnswerInfoTextView);

                    String scoreFax;
                    if (score == totalMarks) {
                        scoreFax = "You got all " + totalMarks + " marks for this question!";
                        scoreText.setText(Html.fromHtml(scoreFax.trim(), Html.FROM_HTML_MODE_LEGACY));
                        scoreText.setTextColor(Color.parseColor("#4CAF50"));
                    } else if (score > 0) {
                        scoreFax = "You got " + score + " out of " + totalMarks + " marks for this question.";
                        scoreText.setText(Html.fromHtml(scoreFax.trim(), Html.FROM_HTML_MODE_LEGACY));
                        scoreText.setTextColor(Color.parseColor("#FFC107"));
                    } else {
                        scoreFax = "You got 0 out of " + totalMarks + " marks for this question.";
                        scoreText.setText(Html.fromHtml(scoreFax.trim(), Html.FROM_HTML_MODE_LEGACY));
                        scoreText.setTextColor(Color.parseColor("#F44336"));
                    }

                    // Add to container
                    container.addView(scoreView, container.getChildCount());
                }
            }
        }
        // Text answer - already handled in setupTextInputQuestion
    }

    private void checkAnswer() {
        // Save the current answer first, especially for text inputs
        saveCurrentAnswers();

        try {
            JSONObject question = questions.getJSONObject(currentQuestionIndex);
            String questionType = question.getString("question_type");
            boolean isCorrect = false;
            String correctAnswerStr = "";
            double score = 0;
            double totalMarks = question.getInt("marks");

            if (questionType.equals("mcq")) {
                JSONArray options = question.getJSONArray("options");

                // Count correct answers
                List<Integer> correctOptions = new ArrayList<>();
                for (int i = 0; i < options.length(); i++) {
                    if (options.getJSONObject(i).getBoolean("is_correct")) {
                        correctOptions.add(i);
                    }
                }

                StringBuilder correctOptionsText = new StringBuilder();
                for (int i = 0; i < correctOptions.size(); i++) {
                    int optionIndex = correctOptions.get(i);
                    if (i > 0) {
                        correctOptionsText.append(", ");
                    }
                    correctOptionsText.append(options.getJSONObject(optionIndex).getString("text"));
                }

                if (correctOptions.size() > 1) {
                    // Multiple choice validation
                    List<Integer> selectedOptions = userMcqAnswers.get(currentQuestionIndex);
                    correctAnswerStr = "Correct Answers: " + correctOptionsText.toString();

                    // Calculate partial credit for multi-select
                    if (selectedOptions != null && !selectedOptions.isEmpty()) {
                        double marksPerOption = totalMarks / correctOptions.size();
                        int correctSelectedCount = 0;

                        // Count correct selected answers
                        for (Integer selected : selectedOptions) {
                            if (correctOptions.contains(selected)) {
                                correctSelectedCount++;
                            }
                        }

                        // Only award marks for correct selections
                        if (correctSelectedCount > 0) {
                            score = marksPerOption * correctSelectedCount;

                            // If user selected incorrect options, penalize
                            if (selectedOptions.size() > correctSelectedCount) {
                                score = Math.max(0, score - marksPerOption * (selectedOptions.size() - correctSelectedCount));
                            }
                        }

                        // Check if fully correct
                        if (selectedOptions.size() == correctOptions.size()
                                && new HashSet<>(selectedOptions).containsAll(correctOptions)) {
                            isCorrect = true;
                            score = totalMarks; // Ensure full marks for perfect answer
                        }
                    }
                } else if (correctOptions.size() == 1) {
                    // Single choice validation
                    Integer selectedOption = userSingleMcqAnswers.get(currentQuestionIndex);
                    int correctOption = correctOptions.get(0);
                    correctAnswerStr = "Correct Answer: " + options.getJSONObject(correctOption).getString("text");

                    if (selectedOption != null && selectedOption == correctOption) {
                        isCorrect = true;
                        score = totalMarks;
                    }
                }
            } else {
                // Text input validation
                String userAnswer = userTextAnswers.get(currentQuestionIndex);
                String correctAnswer = question.getString("correct_answer_text").trim();
                correctAnswerStr = "Correct Answer: " + (correctAnswer.isEmpty() ? "[Empty]" : correctAnswer);

                boolean isTextMatch;
                if (correctAnswer.isEmpty()) {
                    isTextMatch = (userAnswer == null || userAnswer.trim().isEmpty());
                } else {
                    // Attempt numeric comparison first
                    try {
                        // Ensure userAnswer is not null before trimming and parsing
                        String userAnswerTrimmed = (userAnswer != null) ? userAnswer.trim() : "";
                        double userDouble = Double.parseDouble(userAnswerTrimmed);
                        double correctDouble = Double.parseDouble(correctAnswer);
                        // Compare with a small tolerance for floating point inaccuracies
                        if (Math.abs(userDouble - correctDouble) < 0.00001) {
                            isTextMatch = true;
                        } else {
                            isTextMatch = false; // Numeric comparison failed
                        }
                    } catch (NumberFormatException e) {
                        // If parsing fails (e.g., userAnswer is null, empty, or non-numeric after trim),
                        // fall back to string comparison
                        isTextMatch = userAnswer != null && userAnswer.trim().equalsIgnoreCase(correctAnswer);
                    }
                }

                if (question.has("range_start")) { // Simplified condition, range_end check comes later
                    boolean inRange = false;
                    String rangeEndStr = question.optString("range_end", ""); // Get range_end, default to empty if not present

                    if (userAnswer != null && !userAnswer.trim().isEmpty()) {
                        try {
                            double rangeStart = question.getDouble("range_start");
                            double userValue = Double.parseDouble(userAnswer.trim());

                            if (rangeEndStr.isEmpty()) {
                                // Case: range_end is empty, check if userAnswer equals range_start
                                if (Math.abs(userValue - rangeStart) < 0.00001) { // Tolerance for double comparison
                                    inRange = true;
                                }
                            } else {
                                // Case: range_end is present, perform standard range check
                                double rangeEnd = Double.parseDouble(rangeEndStr);
                                if (userValue >= rangeStart && userValue <= rangeEnd) {
                                    inRange = true;
                                }
                            }
                        } catch (NumberFormatException nfe) {
                            // Not a number, inRange remains false.
                            Log.w("CheckAnswer", "User answer for ranged question was not a number: " + userAnswer);
                        } catch (JSONException je) {
                            Log.e("CheckAnswerError", "JSON issue with range keys: " + je.getMessage());
                        }
                    }

                    if (inRange) {
                        score = totalMarks;
                    } else if (isTextMatch) { // Fallback to text match if range check fails or not applicable
                        score = totalMarks;
                    }
                } else {
                    // Standard text answer
                    if (isTextMatch) {
                        score = totalMarks;
                    }
                }
            }

            // Update isCorrect based on score
            // Set to false if not full marks, "Partially Correct" handles score > 0
            isCorrect = score == totalMarks;

            // Save the score for this question
            questionScores.put(currentQuestionIndex, score);

            // Show result
            resultCardView.setVisibility(VISIBLE);
            resultTitleTextView.setText(isCorrect ? "Correct!" : (score > 0 ? "Partially Correct" : "Incorrect"));
            resultTitleTextView.setTextColor(isCorrect ?
                    ContextCompat.getColor(this, android.R.color.holo_green_light) :
                    (score > 0 ? ContextCompat.getColor(this, android.R.color.holo_orange_light) :
                            ContextCompat.getColor(this, android.R.color.holo_red_light)));
            correctAnswerTextView.setText(Html.fromHtml(correctAnswerStr.trim(), Html.FROM_HTML_MODE_LEGACY));

            // Show score for this question
            explanationTextView.setVisibility(VISIBLE);
            explanationTextView.setText(Html.fromHtml(String.format(Locale.getDefault(),
                    "You scored %.1f out of %d marks for this question.",
                    score, (int) totalMarks).trim(), Html.FROM_HTML_MODE_LEGACY));

        } catch (JSONException e) {
            Toast.makeText(this, "Error checking answer: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("CheckAnswerError", Objects.requireNonNull(e.getMessage()));
        }
    }

    private void confirmSubmitQuiz() {
        // Save any current answers first
        saveCurrentAnswers();

        //Sexy dialog
        View resultView = LayoutInflater.from(this).inflate(R.layout.confirm_exit, null);
        Button closeButton = resultView.findViewById(R.id.closeButton);
        Button submitExitButton = resultView.findViewById(R.id.submitExitButton);
        TextView dialogHead = resultView.findViewById(R.id.dialogHead);
        TextView dialogDesc = resultView.findViewById(R.id.dialogDesc);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(resultView)
                .setCancelable(false)
                .create();

        dialogHead.setText(R.string.submit_question);
        dialogDesc.setText(R.string.are_you_sure_you_want_to_submit);
        submitExitButton.setText(R.string.submit);

        closeButton.setOnClickListener(vx -> dialog.cancel());
        submitExitButton.setOnClickListener(vx -> {
            submitQuiz();
            dialog.cancel();
        });
        dialog.show();
    }

    private void submitQuiz() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        // Save the current answer first
        saveCurrentAnswers();

        // Log all answers before scoring
        logAllAnswers();

        // Calculate score
        calculateScore();

        // Display results
        showQuizResults();
    }

    // New method to log all saved answers for debugging
    private void logAllAnswers() {
        Log.d("SubmitQuiz", "==== LOGGING ALL ANSWERS ====");

        // Log single choice answers
        for (Map.Entry<Integer, Integer> entry : userSingleMcqAnswers.entrySet()) {
            Log.d("SubmitQuiz", "Single choice - Question " + entry.getKey() + ": Option " + entry.getValue());
        }

        // Log multiple choice answers
        for (Map.Entry<Integer, List<Integer>> entry : userMcqAnswers.entrySet()) {
            Log.d("SubmitQuiz", "Multiple choice - Question " + entry.getKey() + ": Options " + entry.getValue());
        }

        // Log text answers
        for (Map.Entry<Integer, String> entry : userTextAnswers.entrySet()) {
            Log.d("SubmitQuiz", "Text answer - Question " + entry.getKey() + ": \"" + entry.getValue() + "\"");
        }

        Log.d("SubmitQuiz", "==== END ANSWER LOG ====");
    }

    private void calculateScore() {
        userScore = 0;
        questionSummaries.clear();

        try {
            logAllAnswers(); // Log answers again before scoring

            for (int i = 0; i < totalQuestions; i++) {
                JSONObject question = questions.getJSONObject(i);
                boolean isExtraInfo = isExtraInfoSection(question);

                if (isExtraInfo) {
                    // Add extra info to summary but with no score
                    questionSummaries.add(new QuestionSummary(i, 0, 0, 0, true));
                    continue;
                }

                String questionType = question.getString("question_type");
                double questionMarks = question.getDouble("marks");
                double score = 0;
                int questionNumber = question.has("question_number") ? question.getInt("question_number") : 0;

                if (questionType.equals("mcq")) {
                    JSONArray options = question.getJSONArray("options");

                    // Get all correct options
                    List<Integer> correctOptions = new ArrayList<>();
                    for (int j = 0; j < options.length(); j++) {
                        if (options.getJSONObject(j).getBoolean("is_correct")) {
                            correctOptions.add(j);
                        }
                    }

                    Log.d("CalculateScore", "Question " + i + " has " + correctOptions.size() + " correct options: " + correctOptions);

                    if (correctOptions.size() > 1) {
                        // Multiple choice validation
                        List<Integer> selectedOptions = userMcqAnswers.get(i);
                        Log.d("CalculateScore", "User selected: " + (selectedOptions != null ? selectedOptions : "none"));

                        if (selectedOptions != null && !selectedOptions.isEmpty()) {
                            double marksPerOption = questionMarks / correctOptions.size();
                            int correctSelectedCount = 0;

                            // Count correct selected answers
                            for (Integer selected : selectedOptions) {
                                if (correctOptions.contains(selected)) {
                                    correctSelectedCount++;
                                }
                            }

                            Log.d("CalculateScore", "User got " + correctSelectedCount + " correct selections out of " + correctOptions.size());

                            // Award marks for correct selections
                            if (correctSelectedCount > 0) {
                                score = marksPerOption * correctSelectedCount;

                                // If user selected incorrect options, penalize
                                if (selectedOptions.size() > correctSelectedCount) {
                                    score = Math.max(0, score - marksPerOption * (selectedOptions.size() - correctSelectedCount));
                                }
                            }

                            // Full marks for perfect answer
                            if (selectedOptions.size() == correctOptions.size()
                                    && new HashSet<>(selectedOptions).containsAll(correctOptions)) {
                                score = questionMarks;
                            }
                        }
                    } else if (correctOptions.size() == 1) {
                        // Single choice validation
                        Integer selectedOption = userSingleMcqAnswers.get(i);
                        Log.d("CalculateScore", "Single choice - correct: " + correctOptions.get(0) + ", selected: " + selectedOption);
                        if (selectedOption != null && selectedOption.equals(correctOptions.get(0))) {
                            score = questionMarks;
                        }
                    }
                } else {
                    // Text input question
                    String userAnswer = userTextAnswers.get(i);
                    String correctAnswer = question.getString("correct_answer_text").trim();
                    boolean isTextMatch;

                    if (correctAnswer.isEmpty()) {
                        isTextMatch = (userAnswer == null || userAnswer.trim().isEmpty());
                    } else {
                        // Attempt numeric comparison first
                        try {
                            // Ensure userAnswer is not null before trimming and parsing
                            String userAnswerTrimmed = (userAnswer != null) ? userAnswer.trim() : "";
                            double userDouble = Double.parseDouble(userAnswerTrimmed);
                            double correctDouble = Double.parseDouble(correctAnswer);
                            // Compare with a small tolerance for floating point inaccuracies
                            if (Math.abs(userDouble - correctDouble) < 0.00001) {
                                isTextMatch = true;
                            } else {
                                isTextMatch = false; // Numeric comparison failed
                            }
                        } catch (NumberFormatException e) {
                            // If parsing fails (e.g., userAnswer is null, empty, or non-numeric after trim),
                            // fall back to string comparison
                            isTextMatch = userAnswer != null && userAnswer.trim().equalsIgnoreCase(correctAnswer);
                        }
                    }

                    if (question.has("range_start")) { // Simplified condition, range_end check comes later
                        boolean inRange = false;
                        String rangeEndStr = question.optString("range_end", ""); // Get range_end, default to empty if not present

                        if (userAnswer != null && !userAnswer.trim().isEmpty()) {
                            try {
                                double rangeStart = question.getDouble("range_start");
                                double userValue = Double.parseDouble(userAnswer.trim());

                                if (rangeEndStr.isEmpty()) {
                                    // Case: range_end is empty, check if userAnswer equals range_start
                                    if (Math.abs(userValue - rangeStart) < 0.00001) { // Tolerance for double comparison
                                        inRange = true;
                                    }
                                } else {
                                    // Case: range_end is present, perform standard range check
                                    double rangeEnd = Double.parseDouble(rangeEndStr);
                                    if (userValue >= rangeStart && userValue <= rangeEnd) {
                                        inRange = true;
                                    }
                                }
                            } catch (NumberFormatException nfe) {
                                // Not a number, inRange remains false.
                                Log.w("CalculateScore", "User answer for ranged question was not a number: " + userAnswer);
                            } catch (JSONException je) {
                                Log.e("CalculateScore", "JSON issue with range keys: " + je.getMessage());
                            }
                        }

                        if (inRange) {
                            score = questionMarks;
                        } else if (isTextMatch) { // Fallback to text match if range check fails or not applicable
                            score = questionMarks;
                        }
                    } else {
                        // Standard text answer
                        if (isTextMatch) {
                            score = questionMarks;
                        }
                    }
                }
                questionScores.put(i, score);
                userScore += score;
                Log.d("CalculateScore", "Question " + i + " score: " + score + "/" + questionMarks);

                // Add to question summaries
                questionSummaries.add(new QuestionSummary(i, questionNumber, questionMarks, score, false));
            }

            Log.d("CalculateScore", "Final score: " + userScore + "/" + totalMarks);
        } catch (JSONException e) {
            Log.e("ScoreError", Objects.requireNonNull(e.getMessage()));
        }
    }

    private void showQuizResults() {
        View resultView = LayoutInflater.from(this).inflate(R.layout.dialog_quiz_result, null);
        Button closeButton = resultView.findViewById(R.id.closeButton);
        Button reviewButton = resultView.findViewById(R.id.reviewButton);
        TextView scoreTextView = resultView.findViewById(R.id.scoreTextView);
        TextView totalTextView = resultView.findViewById(R.id.totalTextView);
        TextView percentageTextView = resultView.findViewById(R.id.percentageTextView);
        TextView feedbackTextView = resultView.findViewById(R.id.feedbackTextView);
        RecyclerView questionSummaryRecyclerView = resultView.findViewById(R.id.questionSummaryRecyclerView);

        // Set score information
        scoreTextView.setText(String.format(Locale.getDefault(), "%.1f", userScore));
        String totalView = "/" + String.format(Locale.getDefault(), "%.1f", totalMarks);
        totalTextView.setText(totalView);

        // Calculate percentage
        double percentage = (userScore / totalMarks) * 100;
        percentageTextView.setText(String.format(Locale.getDefault(), "%.1f%%", percentage));

        // Set feedback based on score percentage
        if (percentage >= 90) {
            feedbackTextView.setText(R.string.above90);
        } else if (percentage < 90 && percentage >= 80) {
            feedbackTextView.setText(R.string.above80);
        } else if (percentage < 80 && percentage >= 70) {
            feedbackTextView.setText(R.string.above70);
        } else if (percentage < 70 && percentage >= 60) {
            feedbackTextView.setText(R.string.above60);
        } else if (percentage < 60 && percentage >= 50) {
            feedbackTextView.setText(R.string.above50);
        } else if (percentage < 95 && percentage >= 40) {
            feedbackTextView.setText(R.string.above40);
        } else {
            feedbackTextView.setText(R.string.fail_mc);
        }


        // Setup question summary recyclerview
        questionSummaryRecyclerView.setVisibility(VISIBLE);
        questionSummaryRecyclerView.setLayoutManager(new

                LinearLayoutManager(this));

        QuestionSummaryAdapter adapter = new QuestionSummaryAdapter(
                this,
                questionSummaries,
                position -> {
                    // When a question is clicked in the summary, navigate to it
                    isReviewMode = true;
                    showQuestion(position);

                    // Dismiss the dialog
                    AlertDialog dialog = (AlertDialog) resultView.getTag();
                    if (dialog != null) {
                        dialog.dismiss();
                    }
                });

        questionSummaryRecyclerView.setAdapter(adapter);

        // Create and show the dialog
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(resultView)
                .setCancelable(false)
                .create();

        // Store dialog reference for potential dismissal
        reviewButton.setOnClickListener(v ->

        {
            isReviewMode = true;
            isExamMode = false;
            timerLayout.setVisibility(GONE);
            checkButton.setVisibility(GONE);
            showQuestion(0);
            dialog.cancel();
        });
        closeButton.setOnClickListener(v ->

        {
            isReviewMode = true;
            isExamMode = false;
            dialog.cancel();
            onBackPressed();
        });
        resultView.setTag(dialog);
        Objects.requireNonNull(dialog.getWindow()).

                setBackgroundDrawableResource(android.R.color.transparent);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    @Override
    public void onBackPressed() {
        if (isExamMode && !isReviewMode) {
            // Save any current answers first
            saveCurrentAnswers();

            //Sexy dialog
            View resultView = LayoutInflater.from(this).inflate(R.layout.confirm_exit, null);
            Button closeButton = resultView.findViewById(R.id.closeButton);
            Button submitExitButton = resultView.findViewById(R.id.submitExitButton);

            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setView(resultView)
                    .setCancelable(false)
                    .create();


            closeButton.setOnClickListener(vx -> dialog.cancel());
            submitExitButton.setOnClickListener(vx -> {
                submitQuiz();
                dialog.cancel();
            });
            dialog.show();
        } else {
            super.onBackPressed();
        }
    }

    @NonNull
    @Override
    public OnBackInvokedDispatcher getOnBackInvokedDispatcher() {
        return super.getOnBackInvokedDispatcher();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (pyqStartTimeMillis > 0) {
            long endTimeMillis = System.currentTimeMillis();
            long timeSpentMillis = endTimeMillis - pyqStartTimeMillis;
            if (timeSpentMillis > 1000) { // Only record if more than 1 second
                dbHelper.addTimeEntry(subjectNameForTimeTracking, "pyq", new Date().getTime(), timeSpentMillis);
                Log.d("TimeTracker", "PYQQuestionActivity paused. Subject: " + subjectNameForTimeTracking + ". Time spent: " + timeSpentMillis / 1000 + "s. Saved to DB.");
            }
            pyqStartTimeMillis = 0; // Reset start time
        }
    }

    private void startTimer(long durationMillis) {
        timeRemainingMillis = durationMillis;
        countDownTimer = new CountDownTimer(durationMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeRemainingMillis = millisUntilFinished;
                updateTimerText(millisUntilFinished);
            }

            @Override
            public void onFinish() {
                timeRemainingMillis = 0;
                updateTimerText(0);
                // Auto submit when time's up
                Toast.makeText(PYQQuestionActivity.this, "Time's up! Submitting quiz...", Toast.LENGTH_LONG).show();
                submitQuiz();
            }
        }.start();
    }

    private void updateTimerText(long millis) {
        int minutes = (int) (millis / 1000) / 60;
        int seconds = (int) (millis / 1000) % 60;
        timerTextView.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));

        // Change color to red and update background when less than 5 minutes remaining
        if (millis < 5 * 60 * 1000) {
            timerTextView.setTextColor(Color.RED);
            // Add a light background for better visibility
            // Add padding for better appearance
            timerTextView.setPadding(16, 8, 16, 8);
        } else {
            timerTextView.setTextColor(Color.WHITE);
            timerTextView.setBackgroundColor(Color.TRANSPARENT);
            timerTextView.setPadding(0, 0, 0, 0);
        }
    }
}