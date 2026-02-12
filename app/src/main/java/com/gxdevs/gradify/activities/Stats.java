package com.gxdevs.gradify.activities;

import android.annotation.SuppressLint;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.gxdevs.gradify.R;
import com.gxdevs.gradify.Utils.Utils;
import com.gxdevs.gradify.adapters.SubjectStatsAdapter;
import com.gxdevs.gradify.db.TimeTrackingDbHelper;
import com.gxdevs.gradify.models.DailySubjectTotalData;
import com.gxdevs.gradify.models.SubjectStatsData;

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

    private TextView button_daily_view_toggle, button_weekly_view_toggle, button_monthly_view_toggle;
    private PieChart pieChart;

    private TextView textViewSelectedDate;

    private Calendar currentSelectedDate;
    private Calendar currentDisplayMonth;

    private final SimpleDateFormat dailyDateFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
    private final SimpleDateFormat monthlyDateFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());

    // TextViews for center pie chart information
    private TextView textViewCenterPieTitle;
    private TextView textViewCenterPieValue;

    private enum ViewMode {
        DAILY,
        WEEKLY,
        MONTHLY
    }

    private ViewMode currentViewMode = ViewMode.DAILY;

    private static final String TAG = "StatsActivity";

    private String currentlySelectedSubjectName = null;
    private final List<Long> tempEntryIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);
        Utils.setPad(findViewById(R.id.statsContainer), "bottom", this);

        RecyclerView recyclerViewDailySubjectStats = findViewById(R.id.recycler_view_daily_subject_stats);
        button_daily_view_toggle = findViewById(R.id.button_daily_view_toggle);
        button_weekly_view_toggle = findViewById(R.id.button_weekly_view_toggle);
        button_monthly_view_toggle = findViewById(R.id.button_monthly_view_toggle);

        pieChart = findViewById(R.id.custom_pie_chart_stats);
        textViewSelectedDate = findViewById(R.id.selectedDay);

        textViewCenterPieTitle = findViewById(R.id.totalTime);
        textViewCenterPieValue = findViewById(R.id.timeNumbers);

        dbHelper = new TimeTrackingDbHelper(this);

        currentSelectedDate = Calendar.getInstance();
        currentDisplayMonth = Calendar.getInstance();

        recyclerViewDailySubjectStats.setLayoutManager(new LinearLayoutManager(this));
        subjectStatsAdapter = new SubjectStatsAdapter(this, subjectStatsList, this);
        recyclerViewDailySubjectStats.setAdapter(subjectStatsAdapter);

        button_daily_view_toggle.setOnClickListener(v -> switchToDailyView());
        button_weekly_view_toggle.setOnClickListener(v -> switchToWeeklyView());
        button_monthly_view_toggle.setOnClickListener(v -> switchToMonthlyView());

        ImageView buttonPreviousDay = findViewById(R.id.button_previous_day);
        ImageView buttonNextDay = findViewById(R.id.button_next_day);

        buttonPreviousDay.setOnClickListener(v -> navigatePrevious());
        buttonNextDay.setOnClickListener(v -> navigateNext());

        setupPieChart();

        findViewById(R.id.pyqTitle).setOnLongClickListener(v -> {
            addTempData();
            return true;
        });

        switchToDailyView();

        findViewById(R.id.backBtnS).setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
    }

    private void setupPieChart() {
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setExtraOffsets(5, 10, 5, 5);
        pieChart.setDragDecelerationFrictionCoef(0.95f);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.TRANSPARENT);
        pieChart.setTransparentCircleColor(Color.TRANSPARENT);
        pieChart.setTransparentCircleAlpha(0);
        pieChart.setHoleRadius(85f);
        pieChart.setTransparentCircleRadius(0f);
        pieChart.setDrawCenterText(false);
        pieChart.setRotationAngle(140);
        pieChart.setRotationEnabled(true);
        pieChart.setHighlightPerTapEnabled(true);
        pieChart.getLegend().setEnabled(false);
        pieChart.setEntryLabelColor(Color.TRANSPARENT);
    }

    private void switchToDailyView() {
        currentViewMode = ViewMode.DAILY;
        updateTabUI("Daily");
        findViewById(R.id.recycler_view_daily_subject_stats).setVisibility(View.VISIBLE);
        loadDailyModeData();
    }

    private void switchToWeeklyView() {
        currentViewMode = ViewMode.WEEKLY;
        updateTabUI("Weekly");
        findViewById(R.id.recycler_view_daily_subject_stats).setVisibility(View.VISIBLE);
        loadWeeklyModeData();
    }

    private void switchToMonthlyView() {
        currentViewMode = ViewMode.MONTHLY;
        updateTabUI("Monthly");
        findViewById(R.id.recycler_view_daily_subject_stats).setVisibility(View.VISIBLE);
        currentDisplayMonth.setTime(currentSelectedDate.getTime());
        loadMonthlyModeData();
    }

    private void navigatePrevious() {
        if (currentViewMode == ViewMode.DAILY) {
            currentSelectedDate.add(Calendar.DAY_OF_YEAR, -1);
            loadDailyModeData();
        } else if (currentViewMode == ViewMode.WEEKLY) {
            currentSelectedDate.add(Calendar.WEEK_OF_YEAR, -1);
            loadWeeklyModeData();
        } else {
            currentDisplayMonth.add(Calendar.MONTH, -1);
            loadMonthlyModeData();
        }
    }

    private void navigateNext() {
        if (currentViewMode == ViewMode.DAILY) {
            currentSelectedDate.add(Calendar.DAY_OF_YEAR, 1);
            loadDailyModeData();
        } else if (currentViewMode == ViewMode.WEEKLY) {
            currentSelectedDate.add(Calendar.WEEK_OF_YEAR, 1);
            loadWeeklyModeData();
        } else {
            currentDisplayMonth.add(Calendar.MONTH, 1);
            loadMonthlyModeData();
        }
    }

    private void addTempData() {
        long now = System.currentTimeMillis();
        String[] subjects = { "Mathematics", "Science", "History", "Coding" };
        for (int i = 0; i < subjects.length; i++) {
            long id = dbHelper.addTimeEntry(subjects[i], "lecture", now, (i + 1) * 30 * 60 * 1000L);
            tempEntryIds.add(id);
            id = dbHelper.addTimeEntry(subjects[i], "pyq", now, (i + 1) * 20 * 60 * 1000L);
            tempEntryIds.add(id);
        }
        Toast.makeText(this, "Temp data added!", Toast.LENGTH_SHORT).show();
        if (currentViewMode == ViewMode.DAILY)
            loadDailyModeData();
        else if (currentViewMode == ViewMode.WEEKLY)
            loadWeeklyModeData();
        else
            loadMonthlyModeData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!tempEntryIds.isEmpty()) {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            for (Long id : tempEntryIds) {
                db.delete(TimeTrackingDbHelper.TABLE_TIME_ENTRIES, TimeTrackingDbHelper.COLUMN_ID + " = ?",
                        new String[] { String.valueOf(id) });
            }
            db.close();
            Log.d(TAG, "Removed temp test data");
        }
    }

    private void updateTabUI(String mode) {
        View indicator = findViewById(R.id.tab_indicator_stats);
        ViewGroup container = findViewById(R.id.view_selector_container);

        button_daily_view_toggle.setTextColor(ContextCompat.getColor(this, R.color.unselectedLevelText));
        button_weekly_view_toggle.setTextColor(ContextCompat.getColor(this, R.color.unselectedLevelText));
        button_monthly_view_toggle.setTextColor(ContextCompat.getColor(this, R.color.unselectedLevelText));

        if ("Daily".equals(mode)) {
            Utils.updateTabIndicator(indicator, button_daily_view_toggle, container);
            button_daily_view_toggle.setTextColor(Color.WHITE);
        } else if ("Weekly".equals(mode)) {
            Utils.updateTabIndicator(indicator, button_weekly_view_toggle, container);
            button_weekly_view_toggle.setTextColor(Color.WHITE);
        } else {
            Utils.updateTabIndicator(indicator, button_monthly_view_toggle, container);
            button_monthly_view_toggle.setTextColor(Color.WHITE);
        }
    }

    private void updateDateDisplays() {
        if (currentViewMode == ViewMode.DAILY) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());
            textViewSelectedDate.setText(dateFormat.format(currentSelectedDate.getTime()));
        } else if (currentViewMode == ViewMode.WEEKLY) {
            int weekNum = currentSelectedDate.get(Calendar.WEEK_OF_YEAR);
            textViewSelectedDate.setText("Week " + weekNum);
        } else {
            SimpleDateFormat monthOnlyFormat = new SimpleDateFormat("MMMM", Locale.getDefault());
            textViewSelectedDate.setText(monthOnlyFormat.format(currentDisplayMonth.getTime()));
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadDailyModeData() {
        updateDateDisplays();
        Log.d(TAG, "Loading DAILY mode data for: " + dailyDateFormat.format(currentSelectedDate.getTime()));
        currentlySelectedSubjectName = null;

        Calendar dayStart = (Calendar) currentSelectedDate.clone();
        dayStart.set(Calendar.HOUR_OF_DAY, 0);
        dayStart.set(Calendar.MINUTE, 0);
        dayStart.set(Calendar.SECOND, 0);
        dayStart.set(Calendar.MILLISECOND, 0);
        Calendar dayEnd = (Calendar) dayStart.clone();
        dayEnd.set(Calendar.HOUR_OF_DAY, 23);
        dayEnd.set(Calendar.MINUTE, 59);
        dayEnd.set(Calendar.SECOND, 59);
        dayEnd.set(Calendar.MILLISECOND, 999);

        List<SubjectStatsData> fetchedDailyStats = dbHelper.getAggregatedSubjectStats(dayStart.getTimeInMillis(),
                dayEnd.getTimeInMillis());
        List<DailySubjectTotalData> dailyRawData = dbHelper.getDailySubjectTotals(dayStart.getTimeInMillis(),
                dayEnd.getTimeInMillis());

        processAndDisplayStats(fetchedDailyStats, dailyRawData);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadWeeklyModeData() {
        updateDateDisplays();
        Log.d(TAG, "Loading WEEKLY mode data for week: " + currentSelectedDate.get(Calendar.WEEK_OF_YEAR));
        currentlySelectedSubjectName = null;

        Calendar weekStart = (Calendar) currentSelectedDate.clone();
        weekStart.set(Calendar.DAY_OF_WEEK, weekStart.getFirstDayOfWeek());
        weekStart.set(Calendar.HOUR_OF_DAY, 0);
        weekStart.set(Calendar.MINUTE, 0);
        weekStart.set(Calendar.SECOND, 0);
        weekStart.set(Calendar.MILLISECOND, 0);

        Calendar weekEnd = (Calendar) weekStart.clone();
        weekEnd.add(Calendar.DAY_OF_YEAR, 6);
        weekEnd.set(Calendar.HOUR_OF_DAY, 23);
        weekEnd.set(Calendar.MINUTE, 59);
        weekEnd.set(Calendar.SECOND, 59);
        weekEnd.set(Calendar.MILLISECOND, 999);

        List<SubjectStatsData> fetchedWeeklyStats = dbHelper.getAggregatedSubjectStats(weekStart.getTimeInMillis(),
                weekEnd.getTimeInMillis());
        List<DailySubjectTotalData> weeklyRawData = dbHelper.getDailySubjectTotals(weekStart.getTimeInMillis(),
                weekEnd.getTimeInMillis());

        processAndDisplayStats(fetchedWeeklyStats, weeklyRawData);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadMonthlyModeData() {
        updateDateDisplays();
        Log.d(TAG, "Loading MONTHLY mode data for: " + monthlyDateFormat.format(currentDisplayMonth.getTime()));
        currentlySelectedSubjectName = null;

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

        List<SubjectStatsData> fetchedMonthlyStats = dbHelper.getAggregatedSubjectStats(monthStart.getTimeInMillis(),
                monthEnd.getTimeInMillis());
        List<DailySubjectTotalData> monthlyRawData = dbHelper.getDailySubjectTotals(monthStart.getTimeInMillis(),
                monthEnd.getTimeInMillis());

        processAndDisplayStats(fetchedMonthlyStats, monthlyRawData);
    }

    private void updatePieChartData(List<PieEntry> entries, int[] colors) {
        PieDataSet dataSet = new PieDataSet(entries, "Study Stats");
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(15f);

        List<Integer> colorList = new ArrayList<>();
        for (int c : colors)
            colorList.add(c);
        dataSet.setColors(colorList);

        PieData data = new PieData(dataSet);
        data.setValueTextSize(0f);
        pieChart.setData(data);
        pieChart.highlightValues(null);
        pieChart.invalidate();
        pieChart.animateY(1400, Easing.EaseInOutQuart);
    }

    private String formatDailyMinutesToHours(float totalMinutes) {
        float hours = totalMinutes / 60.0f;
        if (hours < 0.1f && totalMinutes > 0)
            return "0.1h";
        if (hours == (int) hours)
            return String.format(Locale.getDefault(), "%dh", (int) hours);
        else
            return String.format(Locale.getDefault(), "%.1fh", hours);
    }

    private void processAndDisplayStats(List<SubjectStatsData> statsList, List<DailySubjectTotalData> rawData) {
        subjectStatsList.clear();
        currentDailyRawData.clear();
        currentDailyRawData.addAll(rawData);

        List<PieEntry> entries = new ArrayList<>();
        float totalMinutes = 0;

        for (DailySubjectTotalData subjectTotal : rawData) {
            float minutes = subjectTotal.getTotalDurationMillis() / (60 * 1000f);
            if (minutes > 0) {
                entries.add(new PieEntry(minutes, subjectTotal.getSubjectName()));
            }
            totalMinutes += minutes;
        }

        int[] pieColors = getResources().getIntArray(R.array.default_pie_chart_colors);
        Map<String, Integer> subjectColorMap = new HashMap<>();
        for (int i = 0; i < entries.size(); i++) {
            String subjectNameFromPieData = entries.get(i).getLabel();
            int colorForPieSubject = pieColors[i % pieColors.length];
            subjectColorMap.put(subjectNameFromPieData, colorForPieSubject);
        }

        for (SubjectStatsData stat : statsList) {
            long totalTimeForSubject = stat.getPyqTimeMillis() + stat.getLectureTimeMillis();
            int pyqProgress = (totalTimeForSubject > 0) ? (int) ((stat.getPyqTimeMillis() * 100) / totalTimeForSubject)
                    : 0;
            int lectureProgress = (totalTimeForSubject > 0)
                    ? (int) ((stat.getLectureTimeMillis() * 100) / totalTimeForSubject)
                    : 0;
            stat.setPyqProgress(pyqProgress);
            stat.setLectureProgress(lectureProgress);

            String subjectNameForStatItem = stat.getSubjectName();
            Integer colorForStat = subjectColorMap.get(subjectNameForStatItem);

            stat.setSubjectColor(colorForStat != null ? colorForStat : Color.GRAY);
            subjectStatsList.add(stat);
        }
        subjectStatsAdapter.notifyDataSetChanged();

        TextView graphPlaceholder = findViewById(R.id.graph_placeholder_text);
        if (entries.isEmpty()) {
            pieChart.setData(null);
            pieChart.invalidate();
            pieChart.setVisibility(View.INVISIBLE);
            textViewCenterPieTitle.setVisibility(View.GONE);
            textViewCenterPieValue.setVisibility(View.GONE);
            if (graphPlaceholder != null)
                graphPlaceholder.setVisibility(View.VISIBLE);
        } else {
            updatePieChartData(entries, pieColors);
            pieChart.setVisibility(View.VISIBLE);
            textViewCenterPieTitle.setVisibility(View.VISIBLE);
            textViewCenterPieValue.setVisibility(View.VISIBLE);
            textViewCenterPieTitle.setText(ContextCompat.getString(this, R.string.studied));
            textViewCenterPieValue.setText(formatDailyMinutesToHours(totalMinutes));
            if (graphPlaceholder != null)
                graphPlaceholder.setVisibility(View.GONE);
        }
    }

    @Override
    public void onItemClick(SubjectStatsData subjectStatsData) {
        String clickedSubjectName = subjectStatsData.getSubjectName();
        long totalMillis = subjectStatsData.getLectureTimeMillis() + subjectStatsData.getPyqTimeMillis();
        float totalMinutes = totalMillis / (60 * 1000f);

        if (clickedSubjectName.equals(currentlySelectedSubjectName)) {
            currentlySelectedSubjectName = null;
            pieChart.highlightValues(null);
            onSelectionClearForCenterText();
        } else {
            currentlySelectedSubjectName = clickedSubjectName;
            PieData data = pieChart.getData();
            if (data != null && data.getDataSet() != null) {
                for (int i = 0; i < data.getDataSet().getEntryCount(); i++) {
                    if (data.getDataSet().getEntryForIndex(i).getLabel().equals(clickedSubjectName)) {
                        pieChart.highlightValue(i, 0);
                        break;
                    }
                }
            }
            textViewCenterPieTitle.setText(ContextCompat.getString(this, R.string.studied));
            textViewCenterPieValue.setText(formatDailyMinutesToHours(totalMinutes));
            textViewCenterPieTitle.setVisibility(View.VISIBLE);
            textViewCenterPieValue.setVisibility(View.VISIBLE);
        }
    }

    private void onSelectionClearForCenterText() {
        float totalMinutes = 0;
        for (DailySubjectTotalData subjectTotal : currentDailyRawData) {
            totalMinutes += subjectTotal.getTotalDurationMillis() / (60 * 1000f);
        }
        if (totalMinutes > 0 || !currentDailyRawData.isEmpty()) {
            textViewCenterPieTitle.setText(ContextCompat.getString(this, R.string.studied));
            textViewCenterPieValue.setText(formatDailyMinutesToHours(totalMinutes));
            textViewCenterPieTitle.setVisibility(View.VISIBLE);
            textViewCenterPieValue.setVisibility(View.VISIBLE);
        } else {
            textViewCenterPieTitle.setVisibility(View.GONE);
            textViewCenterPieValue.setVisibility(View.GONE);
        }
    }
}