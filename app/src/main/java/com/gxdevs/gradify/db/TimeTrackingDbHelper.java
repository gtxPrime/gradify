package com.gxdevs.gradify.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

import com.gxdevs.gradify.models.SubjectStatsData;
import com.gxdevs.gradify.models.DailySubjectTotalData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.database.Cursor;

public class TimeTrackingDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "timetracking.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_TIME_ENTRIES = "time_entries";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_SUBJECT_NAME = "subject_name";
    public static final String COLUMN_ACTIVITY_TYPE = "activity_type"; // "lecture" or "pyq"
    public static final String COLUMN_DATE = "date"; // Store as milliseconds since epoch
    public static final String COLUMN_DURATION_MILLIS = "duration_millis";

    private static final String TABLE_CREATE_TIME_ENTRIES =
            "CREATE TABLE " + TABLE_TIME_ENTRIES + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_SUBJECT_NAME + " TEXT, " +
                    COLUMN_ACTIVITY_TYPE + " TEXT, " +
                    COLUMN_DATE + " INTEGER, " +
                    COLUMN_DURATION_MILLIS + " INTEGER);";

    public TimeTrackingDbHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE_TIME_ENTRIES);
        Log.i("TimeTrackingDbHelper", "Database tables created.");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TimeTrackingDbHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TIME_ENTRIES);
        onCreate(db);
    }

    public long addTimeEntry(String subjectName, String activityType, long dateMillis, long durationMillis) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SUBJECT_NAME, subjectName);
        values.put(COLUMN_ACTIVITY_TYPE, activityType);
        values.put(COLUMN_DATE, dateMillis);
        values.put(COLUMN_DURATION_MILLIS, durationMillis);

        long newRowId = -1;
        try {
            newRowId = db.insert(TABLE_TIME_ENTRIES, null, values);
            if (newRowId != -1) {
                Log.d("TimeTrackingDbHelper", "Inserted time entry for " + subjectName + ", type: " + activityType + ", duration: " + durationMillis + "ms, rowId: " + newRowId);
            } else {
                Log.e("TimeTrackingDbHelper", "Failed to insert time entry for " + subjectName);
            }
        } catch (Exception e) {
            Log.e("TimeTrackingDbHelper", "Error inserting time entry", e);
        } finally {
            db.close();
        }
        return newRowId;
    }

    /**
     * Fetches aggregated time spent for each subject, categorized by activity type (lecture/pyq),
     * within a given date range.
     *
     * @param startTimeMillis The start of the period (inclusive).
     * @param endTimeMillis   The end of the period (inclusive).
     * @return A list of SubjectStatsData objects, each representing a subject's aggregated time.
     *         Progress is currently set to 0 as total goal time is not defined yet.
     */
    public List<SubjectStatsData> getAggregatedSubjectStats(long startTimeMillis, long endTimeMillis) {
        Map<String, SubjectStatsData> subjectDataMap = new HashMap<>();

        String[] projection = {
                COLUMN_SUBJECT_NAME,
                COLUMN_ACTIVITY_TYPE,
                "SUM(" + COLUMN_DURATION_MILLIS + ") as total_duration"
        };

        String selection = COLUMN_DATE + " >= ? AND " + COLUMN_DATE + " <= ?";
        String[] selectionArgs = {
                String.valueOf(startTimeMillis),
                String.valueOf(endTimeMillis)
        };

        String groupBy = COLUMN_SUBJECT_NAME + ", " + COLUMN_ACTIVITY_TYPE;

        try (SQLiteDatabase db = this.getReadableDatabase(); Cursor cursor = db.query(
                TABLE_TIME_ENTRIES,
                projection,
                selection,
                selectionArgs,
                groupBy,
                null, // having
                COLUMN_SUBJECT_NAME + " ASC" // orderBy
        )) {
            // having
            // orderBy

            int subjectNameIndex = cursor.getColumnIndex(COLUMN_SUBJECT_NAME);
            int activityTypeIndex = cursor.getColumnIndex(COLUMN_ACTIVITY_TYPE);
            int totalDurationIndex = cursor.getColumnIndex("total_duration");

            while (cursor.moveToNext()) {
                String subjectName = "Unknown Subject";
                if (subjectNameIndex != -1) {
                    subjectName = cursor.getString(subjectNameIndex);
                }

                String activityType = "unknown";
                if (activityTypeIndex != -1) {
                    activityType = cursor.getString(activityTypeIndex);
                }

                long totalDuration = 0;
                if (totalDurationIndex != -1) {
                    totalDuration = cursor.getLong(totalDurationIndex);
                }

                // Get or create SubjectStatsData object from map
                SubjectStatsData statsData = subjectDataMap.get(subjectName);
                if (statsData == null) {
                    // TODO: Define how to calculate actual progress (e.g., against a goal)
                    // For now, setting progress to 0 or a simple calculation if a goal is assumed.
                    // Max progress can be based on a predefined goal or dynamic.
                    statsData = new SubjectStatsData(subjectName, 0, 0, 0, 0);
                    subjectDataMap.put(subjectName, statsData);
                }

                if ("lecture".equalsIgnoreCase(activityType)) {
                    statsData.setLectureTimeMillis(statsData.getLectureTimeMillis() + totalDuration);
                    // statsData.setLectureProgress(...); // Calculate progress later
                } else if ("pyq".equalsIgnoreCase(activityType)) {
                    statsData.setPyqTimeMillis(statsData.getPyqTimeMillis() + totalDuration);
                    // statsData.setPyqProgress(...); // Calculate progress later
                }
            }
        } catch (Exception e) {
            Log.e("TimeTrackingDbHelper", "Error fetching aggregated subject stats", e);
        }
        return new ArrayList<>(subjectDataMap.values());
    }

    /**
     * Fetches total study time aggregated per day for a given date range.
     *
     * @param startTimeMillis The start of the period (inclusive).
     * @param endTimeMillis   The end of the period (inclusive).
     * @return A List of Long values, each representing the total study duration in milliseconds for a day.
     *         The list is ordered by date. Days with no study time are not included.
     */
    public List<Long> getDailyTotalStudyTime(long startTimeMillis, long endTimeMillis) {
        List<Long> dailyTotals = new ArrayList<>();

        String query = "SELECT SUM(" + COLUMN_DURATION_MILLIS + ") as daily_total " +
                       "FROM " + TABLE_TIME_ENTRIES + " " +
                       "WHERE " + COLUMN_DATE + " >= ? AND " + COLUMN_DATE + " <= ? " +
                       "GROUP BY strftime('%Y-%m-%d', " + COLUMN_DATE + " / 1000, 'unixepoch') " +
                       "ORDER BY " + COLUMN_DATE + " ASC";

        String[] selectionArgs = {
                String.valueOf(startTimeMillis),
                String.valueOf(endTimeMillis)
        };

        try (SQLiteDatabase db = this.getReadableDatabase(); Cursor cursor = db.rawQuery(query, selectionArgs)) {
            int dailyTotalIndex = cursor.getColumnIndex("daily_total");
            while (cursor.moveToNext()) {
                if (dailyTotalIndex != -1) {
                    long dailyDuration = cursor.getLong(dailyTotalIndex);
                    if (dailyDuration > 0) { // Only add if there's study time
                        dailyTotals.add(dailyDuration);
                    }
                }
            }
        } catch (Exception e) {
            Log.e("TimeTrackingDbHelper", "Error fetching daily total study time", e);
        }
        return dailyTotals;
    }

    /**
     * Fetches total study time for each subject on a specific day.
     *
     * @param dayStartTimeMillis The start of the day (inclusive).
     * @param dayEndTimeMillis   The end of the day (inclusive).
     * @return A List of DailySubjectTotalData objects, each representing a subject and its total study time for that day.
     */
    public List<DailySubjectTotalData> getDailySubjectTotals(long dayStartTimeMillis, long dayEndTimeMillis) {
        List<DailySubjectTotalData> dailySubjectTotals = new ArrayList<>();

        String[] projection = {
                COLUMN_SUBJECT_NAME,
                "SUM(" + COLUMN_DURATION_MILLIS + ") as total_subject_duration_for_day"
        };

        String selection = COLUMN_DATE + " >= ? AND " + COLUMN_DATE + " <= ?";
        String[] selectionArgs = {
                String.valueOf(dayStartTimeMillis),
                String.valueOf(dayEndTimeMillis)
        };

        try (SQLiteDatabase db = this.getReadableDatabase(); Cursor cursor = db.query(
                TABLE_TIME_ENTRIES,
                projection,
                selection,
                selectionArgs,
                COLUMN_SUBJECT_NAME,
                null, // having
                COLUMN_SUBJECT_NAME + " ASC" // orderBy
        )) {
            // having
            // orderBy

            int subjectNameIndex = cursor.getColumnIndex(COLUMN_SUBJECT_NAME);
            int totalDurationIndex = cursor.getColumnIndex("total_subject_duration_for_day");

            while (cursor.moveToNext()) {
                String subjectName = "Unknown Subject";
                if (subjectNameIndex != -1) {
                    subjectName = cursor.getString(subjectNameIndex);
                }

                long totalDuration = 0;
                if (totalDurationIndex != -1) {
                    totalDuration = cursor.getLong(totalDurationIndex);
                }

                if (totalDuration > 0) { // Only add if there's study time for this subject on this day
                    dailySubjectTotals.add(new DailySubjectTotalData(subjectName, totalDuration));
                }
            }
        } catch (Exception e) {
            Log.e("TimeTrackingDbHelper", "Error fetching daily subject totals", e);
        }
        return dailySubjectTotals;
    }

    // TODO: Add method to get daily time data for the graph
    // public List<DailyGraphData> getDailyGraphData(long startTimeMillis, long endTimeMillis) { ... }

    public void clearAllTimeEntries() {
        try (SQLiteDatabase db = this.getWritableDatabase()) {
            db.delete(TABLE_TIME_ENTRIES, null, null);
            Log.i("TimeTrackingDbHelper", "All time entries deleted.");
        } catch (Exception e) {
            Log.e("TimeTrackingDbHelper", "Error clearing time entries", e);
        }
    }
} 