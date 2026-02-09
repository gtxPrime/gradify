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
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.gxdevs.gradify.R;
import com.gxdevs.gradify.Utils.Utils;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;
import com.skydoves.colorpickerview.ColorPickerDialog;
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences sharedPreferences;

    private MaterialButtonToggleGroup themeStyleToggleGroup;
    private ConstraintLayout primaryColorLayout;
    private ImageView primaryColorPreview;
    private ImageView skyLightColorPreview;
    private ConstraintLayout secondaryColorLayout;
    private ImageView secondaryColorPreview;
    private LinearLayout skyLightColorPickerLayout;
    private LinearLayout edgeFlareColorPickersLayout;
    private LinearLayout gradientColorsLayout;
    private RelativeLayout gradient1ColorLayout;
    private RelativeLayout skyLightColorLayout;
    private ImageView gradient1ColorPreview;
    private RelativeLayout gradient2ColorLayout;
    private ImageView gradient2ColorPreview;
    private RelativeLayout gradient3ColorLayout;
    private ImageView gradient3ColorPreview;
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
    private ImageView settingsDecor1, settingsDecor2, settingsDecor3;
    private ImageView bgImageView;
    private TextView resetToDefaultColorsText;

    public static final String PREFS_NAME = "AppSettingsPrefs";
    public static final String API_KEY = "ApiKey";
    public static final String MESSAGE_HISTORY_COUNT = "MessageHistoryCount";
    public static final String SEND_WHOLE_HISTORY = "SendWholeHistory";
    public static final String THEME_STYLE_KEY = "ThemeStyle";
    public static final String PRIMARY_COLOR_KEY = "PrimaryColor";
    public static final String SECONDARY_COLOR_KEY = "SecondaryColor";
    public static final String GRADIENT_1_COLOR_KEY = "Gradient1Color";
    public static final String GRADIENT_2_COLOR_KEY = "Gradient2Color";
    public static final String GRADIENT_3_COLOR_KEY = "Gradient3Color";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Utils.setPad(findViewById(R.id.settingsHolder), "bottom", this);

        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        ImageView backBtn = findViewById(R.id.backBtn);
        themeStyleToggleGroup = findViewById(R.id.themeToggleGroup);
        primaryColorLayout = findViewById(R.id.primaryColorLayout);
        primaryColorPreview = findViewById(R.id.primaryColorPreview);
        skyLightColorPreview = findViewById(R.id.skyLightColorPreview);
        secondaryColorLayout = findViewById(R.id.secondaryColorLayout);
        secondaryColorPreview = findViewById(R.id.secondaryColorPreview);
        skyLightColorPickerLayout = findViewById(R.id.skyLightGradientPickerLayout);
        edgeFlareColorPickersLayout = findViewById(R.id.gradientColorsLayout);
        gradient1ColorLayout = findViewById(R.id.gradient1ColorLayout);
        skyLightColorLayout = findViewById(R.id.skyLightColorLayout);
        gradient1ColorPreview = findViewById(R.id.gradient1ColorPreview);
        gradient2ColorLayout = findViewById(R.id.gradient2ColorLayout);
        gradient2ColorPreview = findViewById(R.id.gradient2ColorPreview);
        gradient3ColorLayout = findViewById(R.id.gradient3ColorLayout);
        gradient3ColorPreview = findViewById(R.id.gradient3ColorPreview);
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
        settingsDecor1 = findViewById(R.id.settingsDecor1);
        settingsDecor2 = findViewById(R.id.settingsDecor2);
        settingsDecor3 = findViewById(R.id.settingsDecor3);
        bgImageView = findViewById(R.id.bgImageView);
        resetToDefaultColorsText = findViewById(R.id.resetToDefaultColors);

        setupThemeSelection();
        setupApiKey();
        setupMessageHistory();
        loadColorPreviews();
        applyCurrentThemeStyle();

        backBtn.setOnClickListener(v -> onBackPressed());

        strictExamModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Toast.makeText(this, "Coming soon!", Toast.LENGTH_SHORT).show();
            strictExamModeSwitch.setChecked(false);
        });
        focusModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Toast.makeText(this, "Coming soon!", Toast.LENGTH_SHORT).show();
            focusModeSwitch.setChecked(false);
        });
    }

    private void setupThemeSelection() {
        themeStyleToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                if (checkedId == R.id.skyLightThemeButton) {
                    editor.putString(THEME_STYLE_KEY, "SkyLight");
                } else if (checkedId == R.id.edgeFlareThemeButton) {
                    editor.putString(THEME_STYLE_KEY, "EdgeFlare");
                }
                editor.apply();
                applyCurrentThemeStyle();
            }
        });

        setupColorPickers();
        setupResetToDefaultButton();
    }

    private void applyCurrentThemeStyle() {
        String currentStyle = sharedPreferences.getString(THEME_STYLE_KEY, "EdgeFlare");

        if ("SkyLight".equals(currentStyle)) {
            themeStyleToggleGroup.check(R.id.skyLightThemeButton);
            bgImageView.setVisibility(View.VISIBLE);
            Utils.bgGrayGenerate(this, bgImageView);

            settingsDecor1.setVisibility(View.GONE);
            settingsDecor2.setVisibility(View.GONE);
            settingsDecor3.setVisibility(View.GONE);

            if (skyLightColorPickerLayout != null)
                skyLightColorPickerLayout.setVisibility(View.VISIBLE);
            if (edgeFlareColorPickersLayout != null)
                edgeFlareColorPickersLayout.setVisibility(View.GONE);

        } else {
            themeStyleToggleGroup.check(R.id.edgeFlareThemeButton);
            bgImageView.setVisibility(View.GONE);

            settingsDecor1.setVisibility(View.VISIBLE);
            settingsDecor2.setVisibility(View.VISIBLE);
            settingsDecor3.setVisibility(View.VISIBLE);
            updateEdgeFlareDecor();

            if (skyLightColorPickerLayout != null)
                skyLightColorPickerLayout.setVisibility(View.GONE);
            if (edgeFlareColorPickersLayout != null)
                edgeFlareColorPickersLayout.setVisibility(View.VISIBLE);
        }
    }

    private void setupColorPickers() {
        if (skyLightColorPickerLayout != null && skyLightColorPickerLayout.findViewById(R.id.skyLightColorLayout) != null) {
            (skyLightColorPickerLayout.findViewById(R.id.skyLightColorLayout)).setOnClickListener(v -> showColorPickerDialog("Select Sky Color", GRADIENT_1_COLOR_KEY, gradient1ColorPreview));
        } else if (skyLightColorLayout != null) {
            skyLightColorLayout.setOnClickListener(v -> showColorPickerDialog("Select Main Color", GRADIENT_1_COLOR_KEY, gradient1ColorPreview));
        }

        if (edgeFlareColorPickersLayout != null && edgeFlareColorPickersLayout.findViewById(R.id.gradient1ColorLayout) != null) {
            (edgeFlareColorPickersLayout.findViewById(R.id.gradient1ColorLayout)).setOnClickListener(v -> showColorPickerDialog("Select Gradient 1 Color", GRADIENT_1_COLOR_KEY, gradient1ColorPreview));
        } else if (gradient1ColorLayout != null && !"SkyLight".equals(sharedPreferences.getString(THEME_STYLE_KEY, ""))) {
            gradient1ColorLayout.setOnClickListener(v -> showColorPickerDialog("Select Gradient 1 Color", GRADIENT_1_COLOR_KEY, gradient1ColorPreview));
        }

        if (gradient2ColorLayout != null)
            gradient2ColorLayout.setOnClickListener(v -> showColorPickerDialog("Select Gradient 2 Color", GRADIENT_2_COLOR_KEY, gradient2ColorPreview));
        if (gradient3ColorLayout != null)
            gradient3ColorLayout.setOnClickListener(v -> showColorPickerDialog("Select Gradient 3 Color", GRADIENT_3_COLOR_KEY, gradient3ColorPreview));

        if (primaryColorLayout != null)
            primaryColorLayout.setOnClickListener(v -> showColorPickerDialog("Select Primary Color", PRIMARY_COLOR_KEY, primaryColorPreview));
        if (secondaryColorLayout != null)
            secondaryColorLayout.setOnClickListener(v -> showColorPickerDialog("Select Secondary Color", SECONDARY_COLOR_KEY, secondaryColorPreview));
    }

    private void showColorPickerDialog(String title, String key, ImageView colorPreview) {
        new ColorPickerDialog.Builder(this)
                .setTitle(title)
                .setPreferenceName(key)
                .setPositiveButton(getString(android.R.string.ok), (ColorEnvelopeListener) (envelope, fromUser) -> {
                    sharedPreferences.edit().putInt(key, envelope.getColor()).apply();
                    colorPreview.setBackgroundColor(envelope.getColor());

                    String currentStyle = sharedPreferences.getString(THEME_STYLE_KEY, "EdgeFlare");
                    if ("SkyLight".equals(currentStyle) && key.equals(GRADIENT_1_COLOR_KEY)) {
                        Utils.bgGrayGenerate(this, bgImageView);
                    } else if ("EdgeFlare".equals(currentStyle) &&
                            (key.equals(GRADIENT_1_COLOR_KEY) || key.equals(GRADIENT_2_COLOR_KEY) || key.equals(GRADIENT_3_COLOR_KEY))) {
                        updateEdgeFlareDecor();
                    }
                })
                .setNegativeButton(getString(android.R.string.cancel), (dialogInterface, i) -> dialogInterface.dismiss())
                .attachAlphaSlideBar(true)
                .attachBrightnessSlideBar(true)
                .setBottomSpace(12)
                .show();
    }

    private void updateEdgeFlareDecor() {
        Utils.applyGradient1(this, settingsDecor1);
        Utils.applyGradient2(this, settingsDecor2);
        Utils.applyGradient3(this, settingsDecor3);
    }

    private void loadColorPreviews() {
        primaryColorPreview.setBackgroundColor(sharedPreferences.getInt(PRIMARY_COLOR_KEY, ContextCompat.getColor(this, R.color.primaryColor)));
        secondaryColorPreview.setBackgroundColor(sharedPreferences.getInt(SECONDARY_COLOR_KEY, ContextCompat.getColor(this, R.color.secondaryColor)));
        gradient1ColorPreview.setBackgroundColor(sharedPreferences.getInt(GRADIENT_1_COLOR_KEY, ContextCompat.getColor(this, R.color.ga1)));
        skyLightColorPreview.setBackgroundColor(sharedPreferences.getInt(GRADIENT_1_COLOR_KEY, ContextCompat.getColor(this, R.color.ga1)));
        gradient2ColorPreview.setBackgroundColor(sharedPreferences.getInt(GRADIENT_2_COLOR_KEY, ContextCompat.getColor(this, R.color.ga2)));
        gradient3ColorPreview.setBackgroundColor(sharedPreferences.getInt(GRADIENT_3_COLOR_KEY, ContextCompat.getColor(this, R.color.ga3)));
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
                if (s.toString().isEmpty()) {
                    apiKeyEditText.setEnabled(true);
                    editApiKeyText.setVisibility(View.GONE);
                    apiKeyTutorialLink.setVisibility(View.VISIBLE);
                } else {
                    sharedPreferences.edit().putString(API_KEY, s.toString()).apply();
                    apiKeyEditText.setEnabled(false);
                    apiKeyTutorialLink.setVisibility(View.GONE);
                    editApiKeyText.setVisibility(View.VISIBLE);
                }
            }
        });

        apiKeyTutorialLink.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"));
            Toast.makeText(this, "Google ya YouTube kar le!", Toast.LENGTH_SHORT).show();
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

    private void setupResetToDefaultButton() {
        if (resetToDefaultColorsText != null) {
            resetToDefaultColorsText.setOnClickListener(v -> {
                SharedPreferences.Editor editor = sharedPreferences.edit();

                // Reset to default colors
                editor.putInt(PRIMARY_COLOR_KEY, ContextCompat.getColor(this, R.color.primaryColor));
                editor.putInt(SECONDARY_COLOR_KEY, ContextCompat.getColor(this, R.color.secondaryColor));
                editor.putInt(GRADIENT_1_COLOR_KEY, ContextCompat.getColor(this, R.color.ga1));
                editor.putInt(GRADIENT_2_COLOR_KEY, ContextCompat.getColor(this, R.color.ga2));
                editor.putInt(GRADIENT_3_COLOR_KEY, ContextCompat.getColor(this, R.color.ga3));

                // Optionally, reset theme style to a default (e.g., EdgeFlare)
                // editor.putString(THEME_STYLE_KEY, "EdgeFlare");

                editor.apply();

                // Reload UI elements to reflect changes
                loadColorPreviews();
                applyCurrentThemeStyle(); // Re-apply style to update decors if needed

                // You might want to restart the activity or notify other parts of the app
                // for a full theme refresh if necessary.
                // For now, just updating previews and current activity's theme elements.
                // recreate(); // This is a more forceful way to apply changes if needed.
            });
        }
    }
} 