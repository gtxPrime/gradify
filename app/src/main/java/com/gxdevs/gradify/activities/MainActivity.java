package com.gxdevs.gradify.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.ComponentCaller;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Task;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.appupdate.AppUpdateOptions;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.gxdevs.gradify.R;
import com.gxdevs.gradify.Utils.Utils;
import com.gxdevs.gradify.db.TimeTrackingDbHelper;
import com.gxdevs.gradify.models.DailySubjectTotalData;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import eightbitlab.com.blurview.BlurView;

public class MainActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private TextView greet, totalStudyTimeText;
    private TimeTrackingDbHelper dbHelper;
    private View customNavDrawer;
    private ImageView navProfileImage;
    private TextView navProfileName;
    private float navDrawerWidthInPixels;
    private BlurView blurView;
    private ViewGroup rootActivityLayout;

    // Exam Dates Timer Views
    private LinearLayout examDatesDetailsLayout, examTimersContainer;
    private TextView q1TimerText, q2TimerText, etTimerText;
    private TextView q1DateText, q2DateText, etDateText;
    private TextView upcomingExamName, upcomingExamDays;
    private ImageView navExamDatesArrow;
    private Button enableAutoTimeButton;

    private Handler timerHandler;
    private Runnable timerRunnable;

    private String q1DateStr;
    private String q2DateStr;
    private String etDateStr;
    private String startDateStr;
    private static final String DATE_FORMAT_STR = "yyyy-MM-dd'T'HH:mm:ss";
    private static final TimeZone IST_TIMEZONE = TimeZone.getTimeZone("Asia/Kolkata");
    private static final int REQUEST_CODE_UPDATE = 100;
    private AppUpdateManager appUpdateManager;

    TextView foundationText, pyqSubtitle, lecturesSubtitle, notesSubtitle;
    CardView pyq_card, notes_card, lectures_card, toolCard, supportCard, timeHolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Utils.setPad(findViewById(R.id.root_activity_layout), "bottom", this);
        // Utils.fetchAndCacheApiUrl(this, apiUrl -> Log.d("API_URL", "URL ready: " +
        // apiUrl));
        auth = FirebaseAuth.getInstance();

        appUpdateManager = AppUpdateManagerFactory.create(this);
        checkForMandatoryUpdates();

        rootActivityLayout = findViewById(R.id.root_activity_layout);
        greet = findViewById(R.id.greet);
        blurView = findViewById(R.id.blurView);
        customNavDrawer = findViewById(R.id.custom_nav_drawer_layout);
        navDrawerWidthInPixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 280,
                getResources().getDisplayMetrics());

        dbHelper = new TimeTrackingDbHelper(this);

        View actualDrawerContent = customNavDrawer;
        // Navigation items
        LinearLayout navSettings = actualDrawerContent.findViewById(R.id.nav_settings);
        LinearLayout navLogout = actualDrawerContent.findViewById(R.id.nav_logout);
        LinearLayout navProfileSection = actualDrawerContent.findViewById(R.id.nav_profile_section);
        LinearLayout navExamDates = actualDrawerContent.findViewById(R.id.nav_exam_dates);
        LinearLayout navParadox = actualDrawerContent.findViewById(R.id.nav_paradox);
        LinearLayout navMargazhi = actualDrawerContent.findViewById(R.id.nav_Margazhi);
        LinearLayout navContribute = actualDrawerContent.findViewById(R.id.nav_contribute);
        LinearLayout navDevelopers = actualDrawerContent.findViewById(R.id.nav_developers);
        LinearLayout navDonation = actualDrawerContent.findViewById(R.id.nav_donation);
        navProfileImage = actualDrawerContent.findViewById(R.id.nav_profile_image);
        navProfileName = actualDrawerContent.findViewById(R.id.nav_profile_name);

        // Exam Dates Views
        examDatesDetailsLayout = actualDrawerContent.findViewById(R.id.exam_dates_details_layout);
        examTimersContainer = actualDrawerContent.findViewById(R.id.exam_timers_container);
        q1TimerText = actualDrawerContent.findViewById(R.id.q1_timer_text);
        q2TimerText = actualDrawerContent.findViewById(R.id.q2_timer_text);
        etTimerText = actualDrawerContent.findViewById(R.id.et_timer_text);
        q1DateText = actualDrawerContent.findViewById(R.id.q1_date_text);
        q2DateText = actualDrawerContent.findViewById(R.id.q2_date_text);
        etDateText = actualDrawerContent.findViewById(R.id.et_date_text);
        navExamDatesArrow = actualDrawerContent.findViewById(R.id.nav_exam_dates_arrow);
        enableAutoTimeButton = actualDrawerContent.findViewById(R.id.enable_auto_time_button);

        setupBlurView();
        updateUserInformation();
        setupExamDateStaticTexts();
        updateExamDateSectionUI();

        upcomingExamName = findViewById(R.id.upcoming_exam_name);
        upcomingExamDays = findViewById(R.id.upcoming_exam_days);
        setupExamDateStaticTexts();
        updateExamDateSectionUI();
        setupExamDateStaticTexts();
        updateExamDateSectionUI();

        totalStudyTimeText = findViewById(R.id.total_study_time_text);

        pyq_card = findViewById(R.id.pyq_card);
        notes_card = findViewById(R.id.notes_card);
        lectures_card = findViewById(R.id.lectures_card);
        toolCard = findViewById(R.id.calc_card); // Corrected ID based on XML
        supportCard = findViewById(R.id.support_card);
        timeHolder = findViewById(R.id.focus_mode_card); // Corrected ID based on XML (Focus Card)

        // Initialize views that were previously just found by ID in listeners or not at
        // all
        foundationText = findViewById(R.id.foundation_text);
        pyqSubtitle = findViewById(R.id.pyq_subtitle);
        lecturesSubtitle = findViewById(R.id.lectures_subtitle);
        notesSubtitle = findViewById(R.id.notes_subtitle);

        updateDashboardUI();
        runEntranceAnimations();

        Utils.applyBounceAnimation(timeHolder);
        Utils.applyBounceAnimation(pyq_card);
        Utils.applyBounceAnimation(notes_card);
        Utils.applyBounceAnimation(lectures_card);
        Utils.applyBounceAnimation(toolCard);
        Utils.applyBounceAnimation(supportCard);
        Utils.applyBounceAnimation(findViewById(R.id.profile_image));
        Utils.applyBounceAnimation(findViewById(R.id.menu));
        findViewById(R.id.menu).setOnClickListener(v -> toggleNavDrawer());

        blurView.setOnClickListener(v -> {
            if (customNavDrawer.getVisibility() == View.VISIBLE) {
                toggleNavDrawer();
            }
        });
        customNavDrawer.setOnClickListener(v -> {
        });

        timeHolder.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, Stats.class));
        });

        pyq_card.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, PYQActivity.class));
        });

        notes_card.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, UserNotesActivity.class));
        });

        lectures_card.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SubjectsActivity.class));
        });

        toolCard.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, ToolsActivity.class));
        });

        supportCard.setOnClickListener(v -> showSupportBottomSheet());

        findViewById(R.id.profile_image).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, ProfileActivity.class));
        });

        if (navSettings != null) {
            navSettings.setOnClickListener(v -> {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                toggleNavDrawer();
            });
        }

        if (navExamDates != null) {
            navExamDates.setOnClickListener(v -> {
                toggleExamDatesSection();
            });
        }

        if (navParadox != null) {
            navParadox.setOnClickListener(v -> {
                Toast.makeText(MainActivity.this, "Coming soon", Toast.LENGTH_SHORT).show();
                toggleNavDrawer();
            });
        }

        if (navMargazhi != null) {
            navMargazhi.setOnClickListener(v -> {
                Toast.makeText(MainActivity.this, "Coming soon", Toast.LENGTH_SHORT).show();
                toggleNavDrawer();
            });
        }

        if (navContribute != null) {
            navContribute.setOnClickListener(v -> {
                String url = "https://forms.gle/ch7FinUjHQxDhL4c8";
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            });
        }

        if (navDevelopers != null) {
            navDevelopers.setOnClickListener(v -> {
                startActivity(new Intent(MainActivity.this, DevelopersActivity.class));
                toggleNavDrawer();
            });
        }

        if (navDonation != null) {
            navDonation.setOnClickListener(v -> {
                String url = "https://isthismyportfolio.site/donation.html";
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                toggleNavDrawer();
            });
        }

        if (navProfileSection != null) {
            navProfileSection.setOnClickListener(v -> {
                startActivity(new Intent(MainActivity.this, ProfileActivity.class));
                toggleNavDrawer();
            });
        }

        displayTodaysTotalStudyTime();
        initializeTimer();

        if (enableAutoTimeButton != null) {
            enableAutoTimeButton.setOnClickListener(v -> {
                try {
                    startActivity(new Intent(Settings.ACTION_DATE_SETTINGS));
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Could not open Date & Time settings", Toast.LENGTH_SHORT).show();
                    Log.d("Date and Time", String.valueOf(e.getMessage()));
                }
            });
        }
        // Fetch dates from GitHub
        Utils.fetchDates(this, new Utils.DatabaseCallback() {
            @Override
            public void onReady(org.json.JSONObject dates) {
                try {
                    q1DateStr = dates.getString("quiz_1");
                    q2DateStr = dates.getString("quiz_2");
                    etDateStr = dates.getString("end_term");
                    startDateStr = dates.getString("start_date");

                    runOnUiThread(() -> {
                        setupExamDateStaticTexts();
                        updateExamDateSectionUI();
                        calculateCurrentWeek();
                        updateFocusCardExamInfo();
                    });
                } catch (org.json.JSONException e) {
                    Log.e("MainActivity", "Error parsing dates: " + e.getMessage());
                }
            }

            @Override
            public void onError(String error) {
                Log.e("MainActivity", "Error fetching dates: " + error);
            }
        });
    }

    private void updateFocusCardExamInfo() {
        if (upcomingExamName == null || upcomingExamDays == null)
            return;

        if (q1DateStr == null || q2DateStr == null || etDateStr == null) {
            upcomingExamName.setText("Loading...");
            upcomingExamDays.setText("--");
            return;
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_STR, Locale.getDefault());
            sdf.setTimeZone(IST_TIMEZONE);
            long currentTime = System.currentTimeMillis();

            Date d1 = sdf.parse(q1DateStr);
            Date d2 = sdf.parse(q2DateStr);
            Date d3 = sdf.parse(etDateStr);

            long diff1 = d1.getTime() - currentTime;
            long diff2 = d2.getTime() - currentTime;
            long diff3 = d3.getTime() - currentTime;

            String nextExamName = "None";
            long minDiff = Long.MAX_VALUE;

            if (diff1 > 0 && diff1 < minDiff) {
                minDiff = diff1;
                nextExamName = "Quiz 1";
            }
            if (diff2 > 0 && diff2 < minDiff) {
                minDiff = diff2;
                nextExamName = "Quiz 2";
            }
            if (diff3 > 0 && diff3 < minDiff) {
                minDiff = diff3;
                nextExamName = "End Term";
            }

            if (nextExamName.equals("None")) {
                upcomingExamName.setText("No Upcoming Exams");
                upcomingExamDays.setText("Relax!");
            } else {
                upcomingExamName.setText(nextExamName);
                long days = TimeUnit.MILLISECONDS.toDays(minDiff);
                if (days == 0) {
                    upcomingExamDays.setText("Today");
                } else if (days == 1) {
                    upcomingExamDays.setText("Tomorrow");
                } else {
                    upcomingExamDays.setText(days + " Days Left");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            upcomingExamName.setText("Error");
        }
    }

    private void calculateCurrentWeek() {
        if (startDateStr == null || lecturesSubtitle == null)
            return;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_STR, Locale.getDefault());
            sdf.setTimeZone(IST_TIMEZONE);
            Date startDate = sdf.parse(startDateStr);
            if (startDate != null) {
                long diff = System.currentTimeMillis() - startDate.getTime();
                long days = TimeUnit.MILLISECONDS.toDays(diff);
                long week = (days / 7) + 1;

                String weekText;
                if (week < 1) {
                    weekText = "Starting Soon";
                } else if (week > 12) {
                    weekText = "Week 12+";
                } else {
                    weekText = "Week " + week;
                }
                lecturesSubtitle.setText(weekText);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkForMandatoryUpdates() {
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();
        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                try {
                    appUpdateManager.startUpdateFlowForResult(appUpdateInfo, updateLauncher,
                            AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build());
                } catch (Exception e) {
                    Log.e("Update", "Error starting update flow " + e.getMessage());
                }
            }
        });
    }

    private final ActivityResultLauncher<IntentSenderRequest> updateLauncher = registerForActivityResult(
            new ActivityResultContracts.StartIntentSenderForResult(), result -> {
                if (result.getResultCode() != RESULT_OK) {
                    Toast.makeText(MainActivity.this, "Update failed, try again using PlayStore", Toast.LENGTH_SHORT)
                            .show();
                    checkForMandatoryUpdates();
                }
            });

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data,
            @NonNull ComponentCaller caller) {
        super.onActivityResult(requestCode, resultCode, data, caller);
        if (requestCode == REQUEST_CODE_UPDATE) {
            if (resultCode != RESULT_OK) {
                Toast.makeText(MainActivity.this, "Update is required", Toast.LENGTH_SHORT).show();
                checkForMandatoryUpdates();
            }
        }
    }

    private void setupBlurView() {
        float radius = 15f;

        Drawable windowBackground = getWindow().getDecorView().getBackground();

        blurView.setupWith(rootActivityLayout)
                .setFrameClearDrawable(windowBackground)
                .setBlurRadius(radius)
                .setBlurAutoUpdate(true);
    }

    private String capitalizeProperly(String name) {
        if (name == null || name.isEmpty()) {
            return "";
        }
        String lowerCaseName = name.toLowerCase();
        return lowerCaseName.substring(0, 1).toUpperCase() + lowerCaseName.substring(1);
    }

    private void updateUserInformation() {
        FirebaseUser currentUser = auth.getCurrentUser();
        LinearLayout navLogoutLayout = findViewById(R.id.custom_nav_drawer_layout).findViewById(R.id.nav_logout);
        TextView navLogoutText = navLogoutLayout.findViewById(R.id.nav_logout_text);
        ImageView nav_logout_img = navLogoutLayout.findViewById(R.id.nav_logout_img);

        if (currentUser != null) {
            if (!currentUser.isAnonymous() && currentUser.getDisplayName() != null
                    && !currentUser.getDisplayName().isEmpty()) {
                String fullName = currentUser.getDisplayName();
                String[] words = fullName.split(" ");
                String nameToDisplay;

                if (words.length > 1 && !words[1].isEmpty()) {
                    nameToDisplay = capitalizeProperly(words[1]);
                } else if (words.length > 0 && !words[0].isEmpty()) {
                    nameToDisplay = capitalizeProperly(words[0]);
                } else {
                    nameToDisplay = "User";
                }

                if (currentUser.getPhotoUrl() != null) {
                    String imageUrl = currentUser.getPhotoUrl().toString();
                    String imgX = imageUrl.replace("=s96-c", "=s500");

                    Glide.with(this)
                            .load(imgX)
                            .placeholder(R.drawable.ic_profile_placeholder)
                            .error(R.drawable.ic_profile_placeholder)
                            .into(navProfileImage);
                } else {
                    Glide.with(this)
                            .load(R.drawable.ic_profile_placeholder) // Load placeholder if no photo URL
                            .into(navProfileImage);
                }

                Calendar c = Calendar.getInstance();
                int timeOfDay = c.get(Calendar.HOUR_OF_DAY);
                String greetingTime;
                if (timeOfDay >= 5 && timeOfDay < 12)
                    greetingTime = "Good Morning";
                else if (timeOfDay >= 12 && timeOfDay < 17)
                    greetingTime = "Good Afternoon";
                else
                    greetingTime = "Good Evening";

                String greetText = greetingTime;
                greet.setText(greetText);
                navProfileName.setText(nameToDisplay);

                // User is logged in
                if (navLogoutText != null) {
                    navLogoutText.setText(ContextCompat.getString(this, R.string.logout));
                    navLogoutText.setTextColor(ContextCompat.getColor(this, R.color.red));
                    nav_logout_img.setColorFilter(ContextCompat.getColor(this, R.color.red));
                }
                navLogoutLayout.setOnClickListener(v -> {
                    auth.signOut();
                    Toast.makeText(MainActivity.this, "Logged out", Toast.LENGTH_SHORT).show();
                    updateUserInformation(); // Refresh UI, including navLogout button
                    toggleNavDrawer();
                });
            } else {
                Calendar c = Calendar.getInstance();
                int timeOfDay = c.get(Calendar.HOUR_OF_DAY);
                String greetingTime;
                if (timeOfDay >= 5 && timeOfDay < 12)
                    greetingTime = "Good Morning";
                else if (timeOfDay >= 12 && timeOfDay < 17)
                    greetingTime = "Good Afternoon";
                else
                    greetingTime = "Good Evening";

                String greetText = greetingTime;
                greet.setText(greetText);
                navProfileName.setText("Tester");
            }
        } else {
            Calendar c = Calendar.getInstance();
            int timeOfDay = c.get(Calendar.HOUR_OF_DAY);
            String greetingTime;
            if (timeOfDay >= 5 && timeOfDay < 12)
                greetingTime = "Good Morning";
            else if (timeOfDay >= 12 && timeOfDay < 17)
                greetingTime = "Good Afternoon";
            else
                greetingTime = "Good Evening";

            greet.setText(greetingTime);
            navProfileName.setText(R.string.hello);
            Glide.with(this)
                    .load(R.drawable.ic_profile_placeholder) // Load placeholder if no user
                    .into(navProfileImage);

            // User is not logged in
            if (navLogoutText != null) {
                navLogoutText.setText(ContextCompat.getString(this, R.string.login));
                navLogoutText.setTextColor(ContextCompat.getColor(this, R.color.green));
                nav_logout_img.setColorFilter(ContextCompat.getColor(this, R.color.green));
            }
            navLogoutLayout.setOnClickListener(v -> {
                startActivity(new Intent(MainActivity.this, ProfileActivity.class));
                toggleNavDrawer();
            });
        }
    }

    private void toggleNavDrawer() {
        if (customNavDrawer.getVisibility() == View.GONE) {
            blurView.setAlpha(0f);
            blurView.setVisibility(View.VISIBLE);
            blurView.animate().alpha(1f).setDuration(300).setListener(null);

            customNavDrawer.setTranslationX(-navDrawerWidthInPixels);
            customNavDrawer.setVisibility(View.VISIBLE);
            customNavDrawer.animate()
                    .translationX(0f)
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
                    .translationX(-navDrawerWidthInPixels)
                    .setDuration(300)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            customNavDrawer.setVisibility(View.GONE);
                        }
                    });
        }
    }

    private void toggleExamDatesSection() {
        boolean isCurrentlyVisible = examDatesDetailsLayout.getVisibility() == View.VISIBLE;
        updateExamDateSectionUI();

        if (isCurrentlyVisible) {
            animateLayoutCollapse(examDatesDetailsLayout);
            navExamDatesArrow.animate().rotation(0f).setDuration(300).start();
        } else {
            animateLayoutExpand(examDatesDetailsLayout);
            navExamDatesArrow.animate().rotation(180f).setDuration(300).start();

            if (!isAutomaticDateAndTimeEnabled()) {
                Toast.makeText(this, "Please enable Automatic Date & Time for accurate timers.", Toast.LENGTH_LONG)
                        .show();
            }

            if (isAutomaticDateAndTimeEnabled()) {
                startTimerUpdates();
            }
        }
    }

    private void animateLayoutExpand(final View view) {
        view.measure(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        final int targetHeight = view.getMeasuredHeight();
        view.getLayoutParams().height = 0;
        view.setVisibility(View.VISIBLE);

        ValueAnimator animator = ValueAnimator.ofInt(0, targetHeight);
        animator.addUpdateListener(animation -> {
            view.getLayoutParams().height = (Integer) animation.getAnimatedValue();
            view.requestLayout();
        });
        animator.setDuration(300);
        animator.start();
    }

    private void animateLayoutCollapse(final View view) {
        final int initialHeight = view.getMeasuredHeight();
        ValueAnimator animator = ValueAnimator.ofInt(initialHeight, 0);
        animator.addUpdateListener(animation -> {
            view.getLayoutParams().height = (Integer) animation.getAnimatedValue();
            view.requestLayout();
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                view.setVisibility(View.GONE);
                view.getLayoutParams().height = initialHeight; // Reset for next expand
            }
        });
        animator.setDuration(300);
        animator.start();
    }

    @Override
    public void onBackPressed() {
        if (customNavDrawer.getVisibility() == View.VISIBLE) {
            toggleNavDrawer();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        displayTodaysTotalStudyTime();
        updateUserInformation();
        if (blurView != null) {
            blurView.setBlurAutoUpdate(true);
        }
        updateExamDateSectionUI();
        if (isAutomaticDateAndTimeEnabled() && examDatesDetailsLayout.getVisibility() == View.VISIBLE) {
            startTimerUpdates();
        } else {
            stopTimerUpdates();
        }

        updateDashboardUI();
        runEntranceAnimations();

        appUpdateManager.getAppUpdateInfo().addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                try {
                    appUpdateManager.startUpdateFlowForResult(appUpdateInfo, MainActivity.this,
                            AppUpdateOptions.defaultOptions(AppUpdateType.IMMEDIATE), REQUEST_CODE_UPDATE);
                } catch (IntentSender.SendIntentException e) {
                    Log.e("Update", "Error starting update flow " + e.getMessage());
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (blurView != null) {
            blurView.setBlurAutoUpdate(false);
        }
        stopTimerUpdates();
    }

    private void displayTodaysTotalStudyTime() {
        Calendar todayStart = Calendar.getInstance();
        todayStart.set(Calendar.HOUR_OF_DAY, 0);
        todayStart.set(Calendar.MINUTE, 0);
        todayStart.set(Calendar.SECOND, 0);
        todayStart.set(Calendar.MILLISECOND, 0);

        Calendar todayEnd = Calendar.getInstance();
        todayEnd.set(Calendar.HOUR_OF_DAY, 23);
        todayEnd.set(Calendar.MINUTE, 59);
        todayEnd.set(Calendar.SECOND, 59);
        todayEnd.set(Calendar.MILLISECOND, 999);

        List<DailySubjectTotalData> todaysStats = dbHelper.getDailySubjectTotals(todayStart.getTimeInMillis(),
                todayEnd.getTimeInMillis());

        long totalMillisToday = 0;
        for (DailySubjectTotalData stat : todaysStats) {
            totalMillisToday += stat.getTotalDurationMillis();
        }

        if (totalStudyTimeText != null) {
            totalStudyTimeText.setText(formatMillisToHhMm(totalMillisToday));
        }
    }

    private String formatMillisToHhMm(long millis) {
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        return String.format(Locale.getDefault(), "%dh %02dm", hours, minutes);
    }

    private boolean isAutomaticDateAndTimeEnabled() {
        try {
            return Settings.Global.getInt(getContentResolver(), Settings.Global.AUTO_TIME) == 1;
        } catch (Settings.SettingNotFoundException e) {
            Log.d("Check auto Time", String.valueOf(e.getMessage()));
            return false;
        }
    }

    private void initializeTimer() {
        if (!isAutomaticDateAndTimeEnabled()) {
            stopTimerUpdates();
            return;
        }
        if (timerHandler == null) {
            timerHandler = new Handler(Looper.getMainLooper());
            timerRunnable = new Runnable() {
                @Override
                public void run() {
                    if (isAutomaticDateAndTimeEnabled()) {
                        updateExamTimers();
                        if (timerHandler != null) {
                            timerHandler.postDelayed(this, 1000);
                        }
                    } else {
                        updateExamDateSectionUI();
                        stopTimerUpdates();
                    }
                }
            };
        }
    }

    private void startTimerUpdates() {
        if (isAutomaticDateAndTimeEnabled()) {
            initializeTimer();
            if (timerHandler != null && timerRunnable != null) {
                timerHandler.removeCallbacks(timerRunnable);
                timerHandler.post(timerRunnable);
            }
        } else {
            stopTimerUpdates();
            updateExamDateSectionUI();
        }
    }

    private void stopTimerUpdates() {
        if (timerHandler != null && timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
    }

    private void setupExamDateStaticTexts() {
        if (q1DateStr == null || q2DateStr == null || etDateStr == null) {
            if (q1DateText != null)
                q1DateText.setText("Loading...");
            if (q2DateText != null)
                q2DateText.setText("Loading...");
            if (etDateText != null)
                etDateText.setText("Loading...");
            return;
        }

        SimpleDateFormat displayFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        displayFormat.setTimeZone(IST_TIMEZONE);

        try {
            SimpleDateFormat parseFormat = new SimpleDateFormat(DATE_FORMAT_STR, Locale.getDefault());
            parseFormat.setTimeZone(IST_TIMEZONE);

            Date q1Date = parseFormat.parse(q1DateStr);
            Date q2Date = parseFormat.parse(q2DateStr);
            Date etDate = parseFormat.parse(etDateStr);

            if (q1DateText != null)
                q1DateText.setText("Date: " + displayFormat.format(q1Date));
            if (q2DateText != null)
                q2DateText.setText("Date: " + displayFormat.format(q2Date));
            if (etDateText != null)
                etDateText.setText("Date: " + displayFormat.format(etDate));

        } catch (Exception e) {
            if (q1DateText != null)
                q1DateText.setText(getString(R.string.error));
            if (q2DateText != null)
                q2DateText.setText(getString(R.string.error));
            if (etDateText != null)
                etDateText.setText(getString(R.string.error));
        }
    }

    private void updateExamTimers() {
        if (!isAutomaticDateAndTimeEnabled()) {
            updateExamDateSectionUI();
            stopTimerUpdates();
            return;
        }

        if (q1DateStr == null || q2DateStr == null || etDateStr == null) {
            return;
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_STR, Locale.getDefault());
            sdf.setTimeZone(IST_TIMEZONE);

            long currentTimeMillis = Calendar.getInstance().getTimeInMillis();

            Date targetQ1 = sdf.parse(q1DateStr);
            Date targetQ2 = sdf.parse(q2DateStr);
            Date targetET = sdf.parse(etDateStr);

            updateTimerText(q1TimerText, targetQ1.getTime() - currentTimeMillis);
            updateTimerText(q2TimerText, targetQ2.getTime() - currentTimeMillis);
            updateTimerText(etTimerText, targetET.getTime() - currentTimeMillis);

        } catch (Exception e) {
            e.printStackTrace();
            if (q1TimerText != null)
                q1TimerText.setText(getString(R.string.error));
            if (q2TimerText != null)
                q2TimerText.setText(getString(R.string.error));
            if (etTimerText != null)
                etTimerText.setText(getString(R.string.error));
        }
    }

    private void updateTimerText(TextView textView, long millisRemaining) {
        if (textView == null)
            return;

        if (millisRemaining <= 0) {
            textView.setText("Ho chuka");
            return;
        }

        long days = TimeUnit.MILLISECONDS.toDays(millisRemaining);
        if (days > 0) {
            textView.setText(String.format(Locale.getDefault(), "%d days", days));
        } else {
            long hours = TimeUnit.MILLISECONDS.toHours(millisRemaining) % 24;
            long minutes = TimeUnit.MILLISECONDS.toMinutes(millisRemaining) % 60;
            long seconds = TimeUnit.MILLISECONDS.toSeconds(millisRemaining) % 60;
            textView.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds));
        }
    }

    private void updateExamDateSectionUI() {
        boolean autoTimeEnabled = isAutomaticDateAndTimeEnabled();
        if (autoTimeEnabled) {
            if (examTimersContainer != null)
                examTimersContainer.setVisibility(View.VISIBLE);
            if (enableAutoTimeButton != null)
                enableAutoTimeButton.setVisibility(View.GONE);

            String enableButtonText = getString(R.string.enable_auto_timezone);
            if (q1TimerText != null && (q1TimerText.getText().toString().equals(enableButtonText)
                    || q1TimerText.getText().toString().equals(getString(R.string.error))))
                q1TimerText.setText(getString(R.string.calculating));
            if (q2TimerText != null && (q2TimerText.getText().toString().equals(enableButtonText)
                    || q2TimerText.getText().toString().equals(getString(R.string.error))))
                q2TimerText.setText(getString(R.string.calculating));
            if (etTimerText != null && (etTimerText.getText().toString().equals(enableButtonText)
                    || etTimerText.getText().toString().equals(getString(R.string.error))))
                etTimerText.setText(getString(R.string.calculating));

        } else {
            if (examTimersContainer != null)
                examTimersContainer.setVisibility(View.GONE);
            if (enableAutoTimeButton != null) {
                enableAutoTimeButton.setVisibility(View.VISIBLE);
            }
            stopTimerUpdates();
            stopTimerUpdates();
            updateExamDateSectionUI();
        }
    }

    private void updateDashboardUI() {
        // 1. Greeting
        updateGreeting();

        // 2. Foundation Level & Subjects
        updateLevelAndSubjects();
    }

    private void updateGreeting() {
        Calendar c = Calendar.getInstance();
        int timeOfDay = c.get(Calendar.HOUR_OF_DAY);
        String greetingPre;

        if (timeOfDay >= 5 && timeOfDay < 12) {
            greetingPre = "Good Morning";
        } else if (timeOfDay >= 12 && timeOfDay < 17) {
            greetingPre = "Good Afternoon";
        } else {
            greetingPre = "Good Evening";
        }
    }

    private void updateLevelAndSubjects() {
        SharedPreferences prefs = getSharedPreferences("UserPreferences", MODE_PRIVATE);

        // Level
        String level = prefs.getString("studyLevel", "Foundation");
        if (foundationText != null) {
            foundationText.setText(level.toUpperCase() + " LEVEL");
        }

        // PYQ: Last Practice
        // We assume "last_practice_timestamp" is saved when user exits a PYQ session
        long lastPracticeTime = prefs.getLong("last_practice_timestamp", 0);
        if (pyqSubtitle != null) {
            if (lastPracticeTime == 0) {
                pyqSubtitle.setText("Start Practicing");
            } else {
                pyqSubtitle.setText("Last: " + getRelativeTimeSpanString(lastPracticeTime));
            }
        }

        // Lectures: Current Running Week
        if (lecturesSubtitle != null) {
            int currentWeek = calculateCurrentTermWeek();
            if (currentWeek > 0 && currentWeek <= 12) {
                lecturesSubtitle.setText("Current: Week " + currentWeek);
            } else if (currentWeek > 12) {
                lecturesSubtitle.setText("Term Revision");
            } else {
                lecturesSubtitle.setText("Term Break");
            }
        }

        // Notes: Describes the utility
        if (notesSubtitle != null) {
            notesSubtitle.setText("Smart Revision");
        }
    }

    private String getRelativeTimeSpanString(long timeMillis) {
        long now = System.currentTimeMillis();
        long diff = now - timeMillis;

        if (diff < TimeUnit.MINUTES.toMillis(1)) {
            return "Just now";
        } else if (diff < TimeUnit.HOURS.toMillis(1)) {
            long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
            return minutes + "m ago";
        } else if (diff < TimeUnit.DAYS.toMillis(1)) {
            long hours = TimeUnit.MILLISECONDS.toHours(diff);
            return hours + "h ago";
        } else if (diff < TimeUnit.DAYS.toMillis(7)) {
            long days = TimeUnit.MILLISECONDS.toDays(diff);
            return days + (days == 1 ? " day ago" : " days ago");
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM", Locale.getDefault());
            return sdf.format(new Date(timeMillis));
        }
    }

    private int calculateCurrentTermWeek() {
        Calendar today = Calendar.getInstance();
        int month = today.get(Calendar.MONTH); // 0-based
        int year = today.get(Calendar.YEAR);

        Calendar termStart = getCalendar(year, month);

        if (today.before(termStart)) {
            return 0;
        }

        long diffMillis = today.getTimeInMillis() - termStart.getTimeInMillis();
        long diffDays = TimeUnit.MILLISECONDS.toDays(diffMillis);

        return (int) (diffDays / 7) + 1;
    }

    @NonNull
    private static Calendar getCalendar(int year, int month) {
        Calendar termStart = Calendar.getInstance();
        termStart.set(Calendar.YEAR, year);

        // Approximate IITM Term Starts
        if (month >= Calendar.JANUARY && month < Calendar.MAY) {
            // Jan Term (Starts ~3rd Week of Jan)
            termStart.set(Calendar.MONTH, Calendar.JANUARY);
            termStart.set(Calendar.DAY_OF_MONTH, 19);
        } else if (month >= Calendar.MAY && month < Calendar.SEPTEMBER) {
            // May Term (Starts ~3rd Week of May)
            termStart.set(Calendar.MONTH, Calendar.MAY);
            termStart.set(Calendar.DAY_OF_MONTH, 24);
        } else {
            // Sept Term (Starts ~3rd Week of Sept)
            termStart.set(Calendar.MONTH, Calendar.SEPTEMBER);
            termStart.set(Calendar.DAY_OF_MONTH, 21);
        }
        return termStart;
    }

    private void runEntranceAnimations() {
        // Macro animations: Tile bounce/shine/fade-in
        View[] views = { findViewById(R.id.focus_mode_card), pyq_card, lectures_card, notes_card, toolCard,
                findViewById(R.id.support_card) };

        long delay = 100;
        for (View v : views) {
            if (v == null)
                continue;
            v.setAlpha(0f);
            v.setTranslationY(50f);

            v.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(delay)
                    .setDuration(400)
                    .setInterpolator(new android.view.animation.OvershootInterpolator(1.0f))
                    .start();

            delay += 100;
        }
    }

    private void showSupportBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_support, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        bottomSheetView.findViewById(R.id.close_support_btn).setOnClickListener(v -> bottomSheetDialog.dismiss());

        bottomSheetDialog.show();
    }
}