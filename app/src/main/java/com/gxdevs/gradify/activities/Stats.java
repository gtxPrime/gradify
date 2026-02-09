package com.gxdevs.gradify.activities;

import static com.gxdevs.gradify.activities.SettingsActivity.PRIMARY_COLOR_KEY;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gxdevs.gradify.Components.CustomPieChart;
import com.gxdevs.gradify.R;
import com.gxdevs.gradify.Utils.Utils;
import com.gxdevs.gradify.adapters.SubjectStatsAdapter;
import com.gxdevs.gradify.db.TimeTrackingDbHelper;
import com.gxdevs.gradify.models.DailySubjectTotalData;
import com.gxdevs.gradify.models.SubjectStatsData;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Stats extends AppCompatActivity implements SubjectStatsAdapter.OnItemClickListener {

    private SubjectStatsAdapter subjectStatsAdapter;
    private TimeTrackingDbHelper dbHelper;
    private final List<SubjectStatsData> subjectStatsList = new ArrayList<>();
    private final List<DailySubjectTotalData> currentDailyRawData = new ArrayList<>();
    private final List<SubjectStatsData> currentMonthlyRawStats = new ArrayList<>();
    private CustomPieChart customPieChart;

    private TextView textViewSelectedDate, textViewSelectedMonth;
    private ImageButton buttonNextDay;
    private ImageButton buttonNextMonth;
    private LinearLayout layoutDailyNavigation, layoutMonthlyNavigation;

    private Calendar currentSelectedDate;
    private Calendar currentDisplayMonth;

    private final SimpleDateFormat dailyDateFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
    private final SimpleDateFormat monthlyDateFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());

    // TextViews for center pie chart information
    private TextView textViewCenterPieTitle; // Assuming ID: totalTime
    private TextView textViewCenterPieValue; // Assuming ID: timeNumbers
    private MaterialCardView pieChartHolder;

    private enum ViewMode {
        DAILY,
        MONTHLY
    }

    private ViewMode currentViewMode = ViewMode.DAILY;

    private static final String TAG = "StatsActivity";

    private String currentlySelectedSubjectName = null;
    private ImageView statsHolder, statsHolder1, statsHolder2, statsHolder3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);
        Utils.setPad(findViewById(R.id.statsContainer), "bottom", this);

        RecyclerView recyclerViewDailySubjectStats = findViewById(R.id.recycler_view_daily_subject_stats);
        ImageButton buttonPreviousDay = findViewById(R.id.button_previous_day);
        ImageButton buttonPreviousMonth = findViewById(R.id.button_previous_month);
        MaterialButtonToggleGroup mainViewToggleGroup = findViewById(R.id.viewToggle);
        SharedPreferences preferences = getSharedPreferences(SettingsActivity.PREFS_NAME, MODE_PRIVATE);
        int startColor = preferences.getInt(PRIMARY_COLOR_KEY, ContextCompat.getColor(this, R.color.primaryColor));

        textViewSelectedMonth = findViewById(R.id.text_view_selected_month);
        textViewSelectedMonth = findViewById(R.id.text_view_selected_month);
        layoutDailyNavigation = findViewById(R.id.layout_daily_navigation);
        layoutMonthlyNavigation = findViewById(R.id.layout_monthly_navigation);
        customPieChart = findViewById(R.id.custom_pie_chart_stats);
        buttonNextMonth = findViewById(R.id.button_next_month);
        textViewSelectedDate = findViewById(R.id.selectedDay);
        buttonNextDay = findViewById(R.id.button_next_day);
        pieChartHolder = findViewById(R.id.pieChartHolder);
        statsHolder = findViewById(R.id.statsHolder);
        statsHolder1 = findViewById(R.id.statsHolder1);
        statsHolder2 = findViewById(R.id.statsHolder2);
        statsHolder3 = findViewById(R.id.statsHolder3);

        // Initialize center TextViews from XML
        textViewCenterPieTitle = findViewById(R.id.totalTime);
        textViewCenterPieValue = findViewById(R.id.timeNumbers);

        dbHelper = new TimeTrackingDbHelper(this);
        //addDemoDataIfNeeded();

        currentSelectedDate = Calendar.getInstance();
        currentDisplayMonth = Calendar.getInstance();

        recyclerViewDailySubjectStats.setLayoutManager(new LinearLayoutManager(this));
        subjectStatsAdapter = new SubjectStatsAdapter(this, subjectStatsList, this);
        recyclerViewDailySubjectStats.setAdapter(subjectStatsAdapter);

        mainViewToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.button_daily_view_toggle) {
                    switchToDailyView();
                } else if (checkedId == R.id.button_monthly_view_toggle) {
                    switchToMonthlyView();
                }
            }
        });

        buttonPreviousDay.setOnClickListener(v -> {
            currentSelectedDate.add(Calendar.DAY_OF_YEAR, -1);
            if (currentViewMode == ViewMode.DAILY) loadDailyModeData();
        });
        buttonNextDay.setOnClickListener(v -> {
            currentSelectedDate.add(Calendar.DAY_OF_YEAR, 1);
            if (currentViewMode == ViewMode.DAILY) loadDailyModeData();
        });

        buttonPreviousMonth.setOnClickListener(v -> {
            currentDisplayMonth.add(Calendar.MONTH, -1);
            if (currentViewMode == ViewMode.MONTHLY) {
                currentSelectedDate.set(currentDisplayMonth.get(Calendar.YEAR), currentDisplayMonth.get(Calendar.MONTH), 1);
                loadMonthlyModeData();
            }
        });
        buttonNextMonth.setOnClickListener(v -> {
            currentDisplayMonth.add(Calendar.MONTH, 1);
            if (currentViewMode == ViewMode.MONTHLY) {
                currentSelectedDate.set(currentDisplayMonth.get(Calendar.YEAR), currentDisplayMonth.get(Calendar.MONTH), 1);
                loadMonthlyModeData();
            }
        });

        mainViewToggleGroup.check(R.id.button_daily_view_toggle);
        switchToDailyView();

        if (customPieChart == null) {
            Log.e(TAG, "CustomPieChart not found in layout!");
        }
        findViewById(R.id.backBtnS).setOnClickListener(v -> onBackPressed());

        pieChartHolder.setCardBackgroundColor(startColor);

        Utils.setTheme(this, statsHolder, statsHolder1, statsHolder2, statsHolder3);
    }

    private void switchToDailyView() {
        currentViewMode = ViewMode.DAILY;
        findViewById(R.id.recycler_view_daily_subject_stats).setVisibility(View.VISIBLE);
        layoutDailyNavigation.setVisibility(View.VISIBLE);
        layoutMonthlyNavigation.setVisibility(View.GONE);
        loadDailyModeData();
    }

    private void switchToMonthlyView() {
        currentViewMode = ViewMode.MONTHLY;
        findViewById(R.id.recycler_view_daily_subject_stats).setVisibility(View.VISIBLE);
        layoutDailyNavigation.setVisibility(View.GONE);
        layoutMonthlyNavigation.setVisibility(View.VISIBLE);
        currentDisplayMonth.set(Calendar.YEAR, currentSelectedDate.get(Calendar.YEAR));
        currentDisplayMonth.set(Calendar.MONTH, currentSelectedDate.get(Calendar.MONTH));
        loadMonthlyModeData();
    }

    private void updateDateDisplays() {
        textViewSelectedDate.setText(dailyDateFormat.format(currentSelectedDate.getTime()));
        textViewSelectedMonth.setText(monthlyDateFormat.format(currentDisplayMonth.getTime()));

        // Update visibility or content of center pie texts based on view mode if needed
        // This will be largely handled by loadXModeData and listener callbacks

        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        Calendar normalizedCurrentSelectedDate = (Calendar) currentSelectedDate.clone();
        normalizedCurrentSelectedDate.set(Calendar.HOUR_OF_DAY, 0);
        normalizedCurrentSelectedDate.set(Calendar.MINUTE, 0);
        normalizedCurrentSelectedDate.set(Calendar.SECOND, 0);
        normalizedCurrentSelectedDate.set(Calendar.MILLISECOND, 0);
        buttonNextDay.setEnabled(normalizedCurrentSelectedDate.before(today));

        Calendar currentMonthCal = Calendar.getInstance();
        buttonNextMonth.setEnabled(currentDisplayMonth.get(Calendar.YEAR) < currentMonthCal.get(Calendar.YEAR) ||
                (currentDisplayMonth.get(Calendar.YEAR) == currentMonthCal.get(Calendar.YEAR) &&
                        currentDisplayMonth.get(Calendar.MONTH) < currentMonthCal.get(Calendar.MONTH)));
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadDailyModeData() {
        updateDateDisplays();
        Log.d(TAG, "Loading DAILY mode data for: " + dailyDateFormat.format(currentSelectedDate.getTime()));
        currentlySelectedSubjectName = null;
        customPieChart.highlightSegmentByName(null);

        subjectStatsList.clear();
        currentDailyRawData.clear();

        Calendar dayStart = (Calendar) currentSelectedDate.clone();
        dayStart.set(Calendar.HOUR_OF_DAY, 0);
        dayStart.set(Calendar.MINUTE, 0);
        dayStart.set(Calendar.SECOND, 0);
        dayStart.set(Calendar.MILLISECOND, 0);
        Calendar dayEnd = (Calendar) currentSelectedDate.clone();
        dayEnd.set(Calendar.HOUR_OF_DAY, 23);
        dayEnd.set(Calendar.MINUTE, 59);
        dayEnd.set(Calendar.SECOND, 59);
        dayEnd.set(Calendar.MILLISECOND, 999);
        List<SubjectStatsData> fetchedDailySubjectBreakdown = dbHelper.getAggregatedSubjectStats(dayStart.getTimeInMillis(), dayEnd.getTimeInMillis());

        currentDailyRawData.addAll(dbHelper.getDailySubjectTotals(dayStart.getTimeInMillis(), dayEnd.getTimeInMillis()));

        List<Integer> pieDataMinutes = new ArrayList<>();
        List<String> pieSubjectNames = new ArrayList<>();
        int totalDailyMinutes = 0;

        for (DailySubjectTotalData subjectTotal : currentDailyRawData) {
            int minutes = (int) (subjectTotal.getTotalDurationMillis() / (60 * 1000L));
            if (minutes > 0) {
                pieDataMinutes.add(minutes);
                pieSubjectNames.add(subjectTotal.getSubjectName());
                // Log for pie chart data preparation
                Log.d(TAG, "PieDataPrep (Daily): Added subject '" + subjectTotal.getSubjectName() + "' with " + minutes + " minutes for pie chart.");
            }
            totalDailyMinutes += minutes;
        }

        int[] pieColors = getResources().getIntArray(R.array.default_pie_chart_colors);
        Map<String, Integer> subjectColorMap = new HashMap<>();
        Log.d(TAG, "--- Daily Subject Color Map Population ---");
        for (int i = 0; i < pieSubjectNames.size(); i++) {
            String subjectNameFromPieData = pieSubjectNames.get(i);
            int colorForPieSubject = pieColors[i % pieColors.length];
            subjectColorMap.put(subjectNameFromPieData, colorForPieSubject);
            Log.d(TAG, "ColorMap (Daily): Mapped pie subject '" + subjectNameFromPieData + "' to color #" + Integer.toHexString(colorForPieSubject));
        }
        Log.d(TAG, "--- End Daily Subject Color Map ---");

        Log.d(TAG, "--- Daily RecyclerView Color Assignment ---");
        for (SubjectStatsData stat : fetchedDailySubjectBreakdown) {
            long totalTimeForSubject = stat.getPyqTimeMillis() + stat.getLectureTimeMillis();
            int pyqProgress = (totalTimeForSubject > 0) ? (int) ((stat.getPyqTimeMillis() * 100) / totalTimeForSubject) : 0;
            int lectureProgress = (totalTimeForSubject > 0) ? (int) ((stat.getLectureTimeMillis() * 100) / totalTimeForSubject) : 0;
            stat.setPyqProgress(pyqProgress);
            stat.setLectureProgress(lectureProgress);

            String subjectNameForStatItem = stat.getSubjectName();
            Log.d(TAG, "ColorLookup (Daily): For RecyclerView item, attempting to get color for subject '" + subjectNameForStatItem + "'");
            Integer colorForStat = subjectColorMap.get(subjectNameForStatItem);

            if (colorForStat == null) {
                Log.w(TAG, "ColorLookup (Daily): Color NOT FOUND in map for '" + subjectNameForStatItem + "'. Using default (Gray).");
                stat.setSubjectColor(Color.GRAY); // Default color
            } else {
                Log.d(TAG, "ColorLookup (Daily): Color FOUND for '" + subjectNameForStatItem + "'. Applying color #" + Integer.toHexString(colorForStat));
                stat.setSubjectColor(colorForStat);
            }
            subjectStatsList.add(stat);
        }
        Log.d(TAG, "--- End Daily RecyclerView Color Assignment ---");
        subjectStatsAdapter.notifyDataSetChanged();
        Log.d(TAG, "RecyclerView updated with " + subjectStatsList.size() + " subjects for the day.");

        TextView graphPlaceholder = findViewById(R.id.graph_placeholder_text);
        if (pieDataMinutes.isEmpty()) {
            customPieChart.setData(new ArrayList<>(), new ArrayList<>(), new int[]{});
            customPieChart.setVisibility(View.INVISIBLE);
            textViewCenterPieTitle.setVisibility(View.GONE);
            textViewCenterPieValue.setVisibility(View.GONE);
            if (graphPlaceholder != null) graphPlaceholder.setVisibility(View.VISIBLE);
            Log.d(TAG, "PieChart: No subject data (in minutes > 0) for the selected day.");
        } else {
            customPieChart.setData(pieDataMinutes, pieSubjectNames, pieColors);
            customPieChart.setVisibility(View.VISIBLE);
            textViewCenterPieTitle.setVisibility(View.VISIBLE);
            textViewCenterPieValue.setVisibility(View.VISIBLE);
            textViewCenterPieTitle.setText(R.string.time_studied);
            textViewCenterPieValue.setText(formatMinutesToHhMm(totalDailyMinutes));
            if (graphPlaceholder != null) graphPlaceholder.setVisibility(View.GONE);
            Log.d(TAG, "PieChart updated for daily subject distribution with " + pieDataMinutes.size() + " subjects.");
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadMonthlyModeData() {
        updateDateDisplays();
        Log.d(TAG, "Loading MONTHLY mode data for: " + monthlyDateFormat.format(currentDisplayMonth.getTime()));
        currentlySelectedSubjectName = null;
        customPieChart.highlightSegmentByName(null);

        subjectStatsList.clear();
        currentMonthlyRawStats.clear();

        Calendar monthStart = (Calendar) currentDisplayMonth.clone();
        monthStart.set(Calendar.DAY_OF_MONTH, 1);
        monthStart.set(Calendar.HOUR_OF_DAY, 0);
        monthStart.set(Calendar.MINUTE, 0);
        monthStart.set(Calendar.SECOND, 0);
        monthStart.set(Calendar.MILLISECOND, 0);
        Calendar monthEnd = (Calendar) currentDisplayMonth.clone();
        monthEnd.set(Calendar.DAY_OF_MONTH, monthEnd.getActualMaximum(Calendar.DAY_OF_MONTH));
        monthEnd.set(Calendar.HOUR_OF_DAY, 23);
        monthEnd.set(Calendar.MINUTE, 59);
        monthEnd.set(Calendar.SECOND, 59);
        monthEnd.set(Calendar.MILLISECOND, 999);

        currentMonthlyRawStats.addAll(dbHelper.getAggregatedSubjectStats(monthStart.getTimeInMillis(), monthEnd.getTimeInMillis()));

        List<Integer> pieDataMinutesMonthly = new ArrayList<>();
        List<String> pieSubjectNamesMonthly = new ArrayList<>();
        int totalMonthlyMinutes = 0;

        for (SubjectStatsData subjectData : currentMonthlyRawStats) {
            long totalTimeForSubjectThisMonthMillis = subjectData.getLectureTimeMillis() + subjectData.getPyqTimeMillis();
            int minutes = (int) (totalTimeForSubjectThisMonthMillis / (60 * 1000L));
            if (minutes > 0) {
                pieDataMinutesMonthly.add(minutes);
                pieSubjectNamesMonthly.add(subjectData.getSubjectName());
                // Log for pie chart data preparation
                Log.d(TAG, "PieDataPrep (Monthly): Added subject '" + subjectData.getSubjectName() + "' with " + minutes + " minutes for pie chart.");
            }
            totalMonthlyMinutes += minutes;
        }

        int[] pieColors = getResources().getIntArray(R.array.default_pie_chart_colors);
        Map<String, Integer> subjectColorMapMonthly = new HashMap<>();
        Log.d(TAG, "--- Monthly Subject Color Map Population ---");
        for (int i = 0; i < pieSubjectNamesMonthly.size(); i++) {
            String subjectNameFromPieDataMonthly = pieSubjectNamesMonthly.get(i);
            int colorForPieSubjectMonthly = pieColors[i % pieColors.length];
            subjectColorMapMonthly.put(subjectNameFromPieDataMonthly, colorForPieSubjectMonthly);
            Log.d(TAG, "ColorMap (Monthly): Mapped pie subject '" + subjectNameFromPieDataMonthly + "' to color #" + Integer.toHexString(colorForPieSubjectMonthly));
        }
        Log.d(TAG, "--- End Monthly Subject Color Map ---");

        Log.d(TAG, "--- Monthly RecyclerView Color Assignment ---");
        for (SubjectStatsData stat : currentMonthlyRawStats) { // Iterate over currentMonthlyRawStats as it was used for pieSubjectNamesMonthly
            long totalTimeForSubject = stat.getPyqTimeMillis() + stat.getLectureTimeMillis();
            // Only add to subjectStatsList (for RecyclerView) if it has time, similar to pie chart logic maybe?
            // Or ensure that all items from currentMonthlyRawStats are processed for the list view, even if their pie segment was 0.
            // For now, assuming we process all for the list and try to color them.
            if (totalTimeForSubject > 0) { // This condition was already here for adding to subjectStatsList
                int pyqProgress = (int) ((stat.getPyqTimeMillis() * 100) / totalTimeForSubject);
                int lectureProgress = (int) ((stat.getLectureTimeMillis() * 100) / totalTimeForSubject);
                if (pyqProgress + lectureProgress != 100 && (pyqProgress > 0 || lectureProgress > 0)) {
                    if (lectureProgress > pyqProgress) {
                        lectureProgress = 100 - pyqProgress;
                    } else {
                        pyqProgress = 100 - lectureProgress;
                    }
                }
                stat.setPyqProgress(pyqProgress);
                stat.setLectureProgress(lectureProgress);

                String subjectNameForStatItemMonthly = stat.getSubjectName();
                Log.d(TAG, "ColorLookup (Monthly): For RecyclerView item, attempting to get color for subject '" + subjectNameForStatItemMonthly + "'");
                Integer colorForStatMonthly = subjectColorMapMonthly.get(subjectNameForStatItemMonthly);

                if (colorForStatMonthly == null) {
                    Log.w(TAG, "ColorLookup (Monthly): Color NOT FOUND in map for '" + subjectNameForStatItemMonthly + "'. Using default (Gray).");
                    stat.setSubjectColor(Color.GRAY);
                } else {
                    Log.d(TAG, "ColorLookup (Monthly): Color FOUND for '" + subjectNameForStatItemMonthly + "'. Applying color #" + Integer.toHexString(colorForStatMonthly));
                    stat.setSubjectColor(colorForStatMonthly);
                }
                subjectStatsList.add(stat);
            } else {
                stat.setSubjectColor(R.color.primaryColor);
            }
        }
        Log.d(TAG, "--- End Monthly RecyclerView Color Assignment ---");
        subjectStatsAdapter.notifyDataSetChanged();
        Log.d(TAG, "Monthly RecyclerView updated with " + subjectStatsList.size() + " subjects for the month.");

        TextView graphPlaceholder = findViewById(R.id.graph_placeholder_text);
        if (pieDataMinutesMonthly.isEmpty()) {
            customPieChart.setData(new ArrayList<>(), new ArrayList<>(), new int[]{});
            customPieChart.setVisibility(View.INVISIBLE);
            textViewCenterPieTitle.setVisibility(View.GONE);
            textViewCenterPieValue.setVisibility(View.GONE);
            if (graphPlaceholder != null) graphPlaceholder.setVisibility(View.VISIBLE);
            Log.d(TAG, "PieChart: No subject data (in minutes > 0) for the selected month.");
        } else {
            customPieChart.setData(pieDataMinutesMonthly, pieSubjectNamesMonthly, pieColors);
            customPieChart.setVisibility(View.VISIBLE);
            textViewCenterPieTitle.setVisibility(View.VISIBLE);
            textViewCenterPieValue.setVisibility(View.VISIBLE);
            textViewCenterPieTitle.setText(R.string.time_studied);
            textViewCenterPieValue.setText(formatMinutesToHhMm(totalMonthlyMinutes));
            if (graphPlaceholder != null) graphPlaceholder.setVisibility(View.GONE);
            Log.d(TAG, "PieChart (Monthly) updated for subject distribution with " + pieDataMinutesMonthly.size() + " subjects.");
        }
    }

    // Implementation of SubjectStatsAdapter.OnItemClickListener
    @Override
    public void onItemClick(SubjectStatsData subjectStatsData) {
        String clickedSubjectName = subjectStatsData.getSubjectName();
        long totalMillis = subjectStatsData.getLectureTimeMillis() + subjectStatsData.getPyqTimeMillis();
        int totalMinutes = (int) (totalMillis / (60 * 1000L)); // Convert total millis to minutes

        if (clickedSubjectName.equals(currentlySelectedSubjectName)) {
            // Clicked the same item again, deselect
            currentlySelectedSubjectName = null;
            customPieChart.highlightSegmentByName(null);
            // Revert center text to total view
            onSelectionClearForCenterText();
        } else {
            // New item selected
            currentlySelectedSubjectName = clickedSubjectName;
            customPieChart.highlightSegmentByName(clickedSubjectName);
            textViewCenterPieTitle.setText(clickedSubjectName);
            textViewCenterPieValue.setText(formatMinutesToHhMm(totalMinutes));
            textViewCenterPieTitle.setVisibility(View.VISIBLE);
            textViewCenterPieValue.setVisibility(View.VISIBLE);
        }
    }

    // Renamed from onSelectionClear to avoid interface name clash and specify its purpose
    private void onSelectionClearForCenterText() {
        if (currentViewMode == ViewMode.DAILY) {
            int totalDailyMinutes = 0;
            for (DailySubjectTotalData subjectTotal : currentDailyRawData) {
                totalDailyMinutes += (int) (subjectTotal.getTotalDurationMillis() / (60 * 1000L));
            }
            if (totalDailyMinutes > 0 || !currentDailyRawData.isEmpty()) {
                textViewCenterPieTitle.setText(R.string.time_studied);
                textViewCenterPieValue.setText(formatMinutesToHhMm(totalDailyMinutes));
                textViewCenterPieTitle.setVisibility(View.VISIBLE);
                textViewCenterPieValue.setVisibility(View.VISIBLE);
            } else {
                textViewCenterPieTitle.setVisibility(View.GONE);
                textViewCenterPieValue.setVisibility(View.GONE);
            }
        } else if (currentViewMode == ViewMode.MONTHLY) {
            int totalMonthlyMinutes = 0;
            for (SubjectStatsData subjectData : currentMonthlyRawStats) {
                totalMonthlyMinutes += (int) ((subjectData.getLectureTimeMillis() + subjectData.getPyqTimeMillis()) / (60 * 1000L));
            }
            if (totalMonthlyMinutes > 0 || !currentMonthlyRawStats.isEmpty()) {
                textViewCenterPieTitle.setText(R.string.time_studied);
                textViewCenterPieValue.setText(formatMinutesToHhMm(totalMonthlyMinutes));
                textViewCenterPieTitle.setVisibility(View.VISIBLE);
                textViewCenterPieValue.setVisibility(View.VISIBLE);
            } else {
                textViewCenterPieTitle.setVisibility(View.GONE);
                textViewCenterPieValue.setVisibility(View.GONE);
            }
        }
    }

    // Helper to format minutes (from pie chart data) to Hh Mm string
    private String formatMinutesToHhMm(int totalMinutes) {
        if (totalMinutes < 0) totalMinutes = 0;
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;
        if (hours > 0) {
            return String.format(Locale.getDefault(), "%dh %dm", hours, minutes);
        } else if (totalMinutes == 0) { // Handle 0 minutes explicitly
            return "0m";
        } else {
            return String.format(Locale.getDefault(), "%dm", minutes);
        }
    }
}