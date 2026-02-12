package com.gxdevs.gradify.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.gxdevs.gradify.R;
import com.gxdevs.gradify.Utils.Utils;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences sharedPreferences;

    private EditText apiKeyEditText;
    private TextView apiKeyTutorialLink;
    private TextView editApiKeyText;
    private Slider messageHistorySlider;
    private TextView messageHistoryValue;
    private MaterialCheckBox sendWholeHistoryCheckBox;
    private TextView historyMeaningLink;
    private TextView historyMeaningText;
    private MaterialSwitch strictExamModeSwitch;
    private MaterialSwitch focusModeSwitch;

    public static final String PREFS_NAME = "AppSettingsPrefs";
    public static final String API_KEY = "ApiKey";
    public static final String MESSAGE_HISTORY_COUNT = "MessageHistoryCount";
    public static final String SEND_WHOLE_HISTORY = "SendWholeHistory";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Utils.setPad(findViewById(R.id.settingsHolder), "bottom", this);

        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        ImageView backBtn = findViewById(R.id.backBtn);
        apiKeyEditText = findViewById(R.id.apiKeyEditText);
        apiKeyTutorialLink = findViewById(R.id.apiKeyTutorialLink);
        editApiKeyText = findViewById(R.id.editApiKeyText);
        messageHistorySlider = findViewById(R.id.messageHistorySlider);
        messageHistoryValue = findViewById(R.id.messageHistoryValue);
        sendWholeHistoryCheckBox = findViewById(R.id.sendWholeHistoryCheckBox);
        historyMeaningLink = findViewById(R.id.historyMeaningLink);
        historyMeaningText = findViewById(R.id.historyMeaningText);
        strictExamModeSwitch = findViewById(R.id.strictExamModeSwitch);
        focusModeSwitch = findViewById(R.id.focusModeSwitch);

        setupApiKey();
        setupMessageHistory();

        backBtn.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        strictExamModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Toast.makeText(this, "Coming soon!", Toast.LENGTH_SHORT).show();
            strictExamModeSwitch.setChecked(false);
        });
        focusModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Toast.makeText(this, "Coming soon!", Toast.LENGTH_SHORT).show();
            focusModeSwitch.setChecked(false);
        });
    }

    private void setupApiKey() {
        String apiKey = sharedPreferences.getString(API_KEY, "");
        if (!apiKey.isEmpty()) {
            apiKeyEditText.setText(apiKey);
            apiKeyEditText.setEnabled(false);
            apiKeyTutorialLink.setVisibility(View.GONE);
            editApiKeyText.setVisibility(View.VISIBLE);
        }

        apiKeyEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!apiKeyEditText.isEnabled())
                    return;

                String input = s.toString().trim();
                if (!input.isEmpty()) {
                    sharedPreferences.edit().putString(API_KEY, input).apply();
                    apiKeyEditText.setEnabled(false);
                    apiKeyTutorialLink.setVisibility(View.GONE);
                    editApiKeyText.setVisibility(View.VISIBLE);
                }
            }
        });

        apiKeyTutorialLink.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"));
            startActivity(browserIntent);
        });

        editApiKeyText.setOnClickListener(v -> {
            sharedPreferences.edit().remove(API_KEY).apply();
            apiKeyEditText.setText("");
            apiKeyEditText.setEnabled(true);
            editApiKeyText.setVisibility(View.GONE);
            apiKeyTutorialLink.setVisibility(View.VISIBLE);
            apiKeyEditText.requestFocus();
        });
    }

    private void setupMessageHistory() {
        int historyCount = sharedPreferences.getInt(MESSAGE_HISTORY_COUNT, 3);
        boolean sendWholeHistory = sharedPreferences.getBoolean(SEND_WHOLE_HISTORY, false);

        messageHistorySlider.setValue(historyCount);
        messageHistoryValue.setText(String.valueOf(historyCount));
        sendWholeHistoryCheckBox.setChecked(sendWholeHistory);

        if (sendWholeHistory) {
            messageHistorySlider.setEnabled(false);
            messageHistoryValue.setAlpha(0.5f);
        }

        messageHistorySlider.addOnChangeListener((slider, value, fromUser) -> {
            int count = (int) value;
            messageHistoryValue.setText(String.valueOf(count));
            sharedPreferences.edit().putInt(MESSAGE_HISTORY_COUNT, count).apply();
            if (fromUser) {
                sendWholeHistoryCheckBox.setChecked(false);
            }
        });

        sendWholeHistoryCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean(SEND_WHOLE_HISTORY, isChecked).apply();
            if (isChecked) {
                messageHistorySlider.setEnabled(false);
                messageHistoryValue.setAlpha(0.5f);
            } else {
                messageHistorySlider.setEnabled(true);
                messageHistoryValue.setAlpha(1.0f);
            }
        });

        historyMeaningLink.setOnClickListener(v -> {
            if (historyMeaningText.getVisibility() == View.VISIBLE) {
                historyMeaningText.setVisibility(View.GONE);
            } else {
                historyMeaningText.setVisibility(View.VISIBLE);
            }
        });
    }
}