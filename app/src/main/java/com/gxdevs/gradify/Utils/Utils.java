package com.gxdevs.gradify.Utils;

import static android.content.Context.MODE_PRIVATE;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.util.Base64;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.transition.AutoTransition;
import androidx.transition.TransitionManager;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.gxdevs.gradify.BuildConfig;
import com.gxdevs.gradify.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Utils {
    static Map<String, String> subjectToType = new HashMap<>();
    static Map<String, Integer> subjectToLectureCount = new HashMap<>();

    public static String getAvatarSeed(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("UserPreferences", MODE_PRIVATE);
        String seed = prefs.getString("avatar_seed", null);
        if (seed == null) {
            seed = String.valueOf(System.currentTimeMillis());
            prefs.edit().putString("avatar_seed", seed).apply();
        }
        return seed;
    }

    public static String decryptUrl(String url) {
        if (url == null) {
            return null;
        }
        if (!url.startsWith("youarenoob.gradify/")) {
            return url;
        }

        try {
            // Remove prefix
            String b64 = url.replace("youarenoob.gradify/", "");

            // Base64 decode (URL_SAFE)
            byte[] encryptedData = Base64.decode(b64, Base64.URL_SAFE);

            if (encryptedData.length < 16) {
                return url;
            }

            // Extract IV (first 16 bytes)
            byte[] iv = new byte[16];
            System.arraycopy(encryptedData, 0, iv, 0, 16);

            // Extract Ciphertext
            int cipherLen = encryptedData.length - 16;
            byte[] ciphertext = new byte[cipherLen];
            System.arraycopy(encryptedData, 16, ciphertext, 0, cipherLen);

            // Get Key
            String keyHex = BuildConfig.SECRET_KEY;

            byte[] keyBytes = new byte[32];
            for (int i = 0; i < keyHex.length(); i += 2) {
                keyBytes[i / 2] = (byte) ((Character.digit(keyHex.charAt(i), 16) << 4)
                        + Character.digit(keyHex.charAt(i + 1), 16));
            }

            SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "AES");
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);

            byte[] decryptedBytes = cipher.doFinal(ciphertext);
            return new String(decryptedBytes, StandardCharsets.UTF_8);

        } catch (Exception e) {
            return url; // Fallback to original (which is broken anyway if encrypted, but avoids crash)
        }
    }

    public static final String INDEX_URL = "https://cdn.jsdelivr.net/gh/gtxPrime/gradify@main/data/index.json";

    public static String githubToJsDelivr(String url) {
        if (url == null || !url.contains("github.com") || !url.contains("/blob/")) {
            return url;
        }
        return url.replace("github.com", "cdn.jsdelivr.net/gh")
                .replace("/blob/", "@");
    }

    private static JSONObject indexCache;
    private static JSONObject formulasCache;

    public static int dpToPx(Context context, int i) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (i * scale);
    }

    public interface DatabaseCallback {
        void onReady(org.json.JSONObject database);

        void onError(String error);
    }

    public static void fetchIndexData(Context context, final DatabaseCallback callback) {
        if (indexCache != null) {
            callback.onReady(indexCache);
            return;
        }

        if (!isNetworkAvailable(context)) {
            String errorMsg = "No internet connection. Please check your network.";
            showToast(context, errorMsg);
            callback.onError(errorMsg);
            return;
        }

        makeApiRequest(context, INDEX_URL, response -> {
            indexCache = response;
            callback.onReady(indexCache);
        }, callback::onError);
    }

    public static void fetchSubjectQuizData(Context context, String subject, final DatabaseCallback callback) {
        fetchIndexData(context, new DatabaseCallback() {
            @Override
            public void onReady(JSONObject index) {
                try {
                    if (!index.has("quizzes")) {
                        callback.onError("Quizzes section not found in index");
                        return;
                    }
                    JSONObject quizzes = index.getJSONObject("quizzes");
                    if (!quizzes.has(subject)) {
                        callback.onError("Subject " + subject + " not found in quizzes index");
                        return;
                    }
                    String url = githubToJsDelivr(quizzes.getString(subject));
                    makeApiRequest(context, url, callback::onReady, callback::onError);
                } catch (JSONException e) {
                    callback.onError("JSON Error: " + e.getMessage());
                }
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public static void fetchLectures(Context context, final DatabaseCallback callback) {
        fetchIndexData(context, new DatabaseCallback() {
            @Override
            public void onReady(JSONObject index) {
                try {
                    if (!index.has("lectures")) {
                        callback.onError("Lectures section not found in index");
                        return;
                    }
                    JSONObject lecturesNested = index.getJSONObject("lectures");
                    JSONObject flattened = new JSONObject();

                    // Flatten Diploma and Foundation subjects for compatibility
                    Iterator<String> levels = lecturesNested.keys();
                    while (levels.hasNext()) {
                        String level = levels.next();
                        Object levelObj = lecturesNested.get(level);
                        if (levelObj instanceof JSONObject) {
                            JSONObject subjects = (JSONObject) levelObj;
                            Iterator<String> subs = subjects.keys();
                            while (subs.hasNext()) {
                                String sub = subs.next();
                                flattened.put(sub, githubToJsDelivr(subjects.getString(sub)));
                            }
                        }
                    }

                    JSONObject result = new JSONObject();
                    result.put("lectures", flattened);
                    callback.onReady(result);

                } catch (JSONException e) {
                    callback.onError("JSON Error: " + e.getMessage());
                }
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public static void fetchFormulas(Context context, final DatabaseCallback callback) {
        if (formulasCache != null) {
            callback.onReady(formulasCache);
            return;
        }
        fetchIndexData(context, new DatabaseCallback() {
            @Override
            public void onReady(JSONObject index) {
                try {
                    if (!index.has("formulas")) {
                        callback.onError("Formulas link not found in index");
                        return;
                    }
                    String url = githubToJsDelivr(index.getString("formulas"));
                    makeApiRequest(context, url, response -> {
                        formulasCache = response;
                        callback.onReady(formulasCache);
                    }, callback::onError);
                } catch (JSONException e) {
                    callback.onError("JSON Error: " + e.getMessage());
                }
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public static void fetchDates(Context context, final DatabaseCallback callback) {
        fetchIndexData(context, new DatabaseCallback() {
            @Override
            public void onReady(JSONObject index) {
                try {
                    if (index.has("dates")) {
                        callback.onReady(index.getJSONObject("dates"));
                    } else {
                        callback.onError("Dates not found in index");
                    }
                } catch (JSONException e) {
                    callback.onError("JSON Error: " + e.getMessage());
                }
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public static Map<String, List<String>> getSubjectsByLevel() {
        Map<String, List<String>> subjectsByLevel = new HashMap<>();

        subjectsByLevel.put("Foundation", Arrays.asList(
                "Maths 1",
                "Stats 1",
                "English 1",
                "CT",
                "Python",
                "Maths 2",
                "Stats 2",
                "English 2"));

        subjectsByLevel.put("Diploma", Arrays.asList(
                "MLF",
                "MLT",
                "MLP",
                "BDM",
                "Business Analytics",
                "TDS",
                "PDSA",
                "DBMS",
                "MAD 1",
                "Java",
                "System Commands",
                "MAD 2"));

        subjectsByLevel.put("Degree", Arrays.asList(
                "Software Testing",
                "Software Engineering",
                "SPG",
                "Deep Learning",
                "A.I.",
                "Introduction to Big Data",
                "Programming in C",
                "Deep Learning for CV",
                "Managerial Economics",
                "ATB",
                "LLM",
                "Speech Technology",
                "DT for App Development",
                "Market Research",
                "Statistical Computing",
                "Advanced Algorithms",
                "Game Theory and Strategy",
                "Computer System Design",
                "Deep Learning Practice",
                "Generative AI",
                "ADS",
                "MLOPS"));
        return subjectsByLevel;
    }

    public static final String[] QUIZ_TYPES = { "Quiz 1", "Quiz 2", "End Term" };

    public static ArrayList<String> getSubjects(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("UserPreferences", MODE_PRIVATE);
        Set<String> subjectSet = prefs.getStringSet("selectedSubjects", null);
        ArrayList<String> subjects = new ArrayList<>();
        if (subjectSet == null || subjectSet.isEmpty()) {
            subjects.add("Select subjects in profile");
        } else {
            subjects.addAll(subjectSet);
        }
        return subjects;
    }

    public static void setPad(View view, String angle, Activity context) {
        WindowCompat.setDecorFitsSystemWindows(context.getWindow(), false);
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());

            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            if (angle.equals("top")) {
                mlp.topMargin = insets.top;
            } else if (angle.equals("bottom")) {
                mlp.bottomMargin = insets.bottom;
            }

            v.setLayoutParams(mlp);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    public interface dataReturn {
        void onSuccess(String[] data);

        void onError(String error);
    }

    public static void fetchData(Context context, final dataReturn callback, String subject, String quizType,
            String year) {
        fetchSubjectQuizData(context, subject, new DatabaseCallback() {
            @Override
            public void onReady(JSONObject database) {
                try {
                    JSONObject papers;
                    if (database.has("papers")) {
                        papers = database.getJSONObject("papers");
                    } else {
                        papers = database;
                    }

                    if (!papers.has(subject)) {
                        if (papers.length() != 1 || !papers.keys().next().equals(subject)) {
                            callback.onError("Subject not found");
                            return;
                        }
                    }
                    JSONObject subjectObj = papers.getJSONObject(subject);

                    if (!subjectObj.has(quizType)) {
                        callback.onError("Quiz type not found");
                        return;
                    }
                    JSONObject quizObj = subjectObj.getJSONObject(quizType);

                    if (year == null) {
                        // Return years
                        JSONArray names = quizObj.names();
                        if (names == null) {
                            callback.onSuccess(new String[0]);
                            return;
                        }
                        List<String> yearsList = new ArrayList<>();
                        for (int i = 0; i < names.length(); i++) {
                            yearsList.add(names.getString(i));
                        }

                        // Custom sorting for formats like "Oct, 2024" or "Feb, 2024"
                        yearsList.sort((s1, s2) -> {
                            try {
                                String[] parts1 = s1.split(", ");
                                String[] parts2 = s2.split(", ");

                                if (parts1.length == 2 && parts2.length == 2) {
                                    int y1 = Integer.parseInt(parts1[1].trim());
                                    int y2 = Integer.parseInt(parts2[1].trim());

                                    if (y1 != y2)
                                        return Integer.compare(y2, y1); // Latest year first

                                    // Same year, compare months
                                    Map<String, Integer> monthMap = new HashMap<>();
                                    String[] months = { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep",
                                            "Oct", "Nov", "Dec" };
                                    for (int i = 0; i < months.length; i++)
                                        monthMap.put(months[i].toLowerCase(), i);

                                    int m1 = monthMap.getOrDefault(parts1[0].trim().toLowerCase(), -1);
                                    int m2 = monthMap.getOrDefault(parts2[0].trim().toLowerCase(), -1);

                                    return Integer.compare(m2, m1); // Latest month first
                                }
                            } catch (Exception e) {
                                // Fallback to string comparison if format is unexpected
                            }
                            return s2.compareTo(s1);
                        });

                        callback.onSuccess(yearsList.toArray(new String[0]));
                    } else {
                        // Return sessions
                        if (!quizObj.has(year)) {
                            callback.onError("Year not found");
                            return;
                        }
                        JSONObject yearObj = quizObj.getJSONObject(year);
                        JSONArray names = yearObj.names();
                        if (names == null) {
                            callback.onSuccess(new String[0]);
                            return;
                        }
                        List<String> sessionsList = new ArrayList<>();
                        for (int i = 0; i < names.length(); i++) {
                            sessionsList.add(names.getString(i));
                        }
                        Collections.sort(sessionsList); // Sort sessions alphabetically
                        callback.onSuccess(sessionsList.toArray(new String[0]));
                    }
                } catch (JSONException e) {
                    callback.onError("JSON Error: " + e.getMessage());
                }
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    static {
        // Math & Statistics
        subjectToType.put("Maths 1", "Maths");
        subjectToType.put("Stats 1", "Maths");
        subjectToType.put("Maths 2", "Maths");
        subjectToType.put("Stats 2", "Maths");
        subjectToType.put("Game Theory and Strategy", "Maths");
        subjectToType.put("Statistical Computing", "Maths");

        // Programming & CS
        subjectToType.put("Python", "CS");
        subjectToType.put("Java", "CS");
        subjectToType.put("Programming in C", "CS");
        subjectToType.put("PDSA", "CS");
        subjectToType.put("TDS", "CS");
        subjectToType.put("Advanced Algorithms", "CS");
        subjectToType.put("CT", "PCS");

        // AI & ML
        subjectToType.put("MLF", "AI");
        subjectToType.put("MLT", "AI");
        subjectToType.put("MLP", "AI");
        subjectToType.put("Deep Learning", "AI");
        subjectToType.put("A.I.", "AI");
        subjectToType.put("Generative AI", "AI");
        subjectToType.put("Deep Learning for CV", "AI");
        subjectToType.put("Deep Learning Practice", "AI");
        subjectToType.put("MLOPS", "AI");

        // Data & Databases
        subjectToType.put("BDM", "DB");
        subjectToType.put("DBMS", "DB");
        subjectToType.put("ADS", "DB");
        subjectToType.put("Introduction to Big Data", "DB");

        // App & System Development
        subjectToType.put("MAD 1", "App");
        subjectToType.put("MAD 2", "App");
        subjectToType.put("DT for App Development", "App");
        subjectToType.put("Software Testing", "App");
        subjectToType.put("Software Engineering", "App");
        subjectToType.put("Computer System Design", "App");

        // Language & Communication
        subjectToType.put("English 1", "Lang");
        subjectToType.put("English 2", "Lang");
        subjectToType.put("Speech Technology", "Lang");

        // Management & Research
        subjectToType.put("Market Research", "Res");
        subjectToType.put("Business Analytics", "Res");
        subjectToType.put("Managerial Economics", "Res");
        subjectToType.put("System Commands", "Res");
        subjectToType.put("SPG", "Res");
        subjectToType.put("LLM", "Res");
        subjectToType.put("ATB", "Res");

        // Subject to Lecture Count
    }

    public static String getSubjectType(String subject) {
        return subjectToType.getOrDefault(subject, "Unknown Type");
    }

    public static int getLectureCount(String subject) {
        return subjectToLectureCount.getOrDefault(subject, 0);
    }

    public static void fetchExamJsonLink(Context context, final ExamLinkCallback callback, String subject,
            String quizType, String year, String session) {
        fetchSubjectQuizData(context, subject, new DatabaseCallback() {
            @Override
            public void onReady(org.json.JSONObject database) {
                try {
                    org.json.JSONObject papers;
                    if (database.has("papers")) {
                        papers = database.getJSONObject("papers");
                    } else {
                        papers = database;
                    }

                    if (!papers.has(subject)) {
                        callback.onError("Paper not found");
                        return;
                    }
                    if (!papers.getJSONObject(subject).has(quizType)) {
                        callback.onError("Paper not found");
                        return;
                    }
                    if (!papers.getJSONObject(subject).getJSONObject(quizType).has(year)) {
                        callback.onError("Paper not found");
                        return;
                    }
                    if (!papers.getJSONObject(subject).getJSONObject(quizType).getJSONObject(year).has(session)) {
                        callback.onError("Paper not found");
                        return;
                    }

                    String link = papers.getJSONObject(subject)
                            .getJSONObject(quizType)
                            .getJSONObject(year)
                            .getString(session);

                    link = decryptUrl(link);
                    link = githubToJsDelivr(link);

                    if (link == null || link.isEmpty()) {
                        callback.onError("Link is empty");
                    } else {
                        callback.onSingleLink(link);
                    }

                } catch (JSONException e) {
                    callback.onError("JSON Error: " + e.getMessage());
                }
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public static void makeApiRequest(Context context, String url, VolleySuccessCallback successCallback,
            VolleyErrorCallback errorCallback) {
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null, successCallback::onSuccess,
                error -> {
                    String message = "Unknown Error";
                    if (error instanceof NetworkError) {
                        message = "No Internet Connection";
                    } else if (error instanceof TimeoutError) {
                        message = "Connection Timed Out";
                    } else if (error instanceof ServerError) {
                        if (error.networkResponse != null) {
                            message = "Server Error: " + error.networkResponse.statusCode;
                        } else {
                            message = "Server Error";
                        }
                    } else if (error instanceof ParseError) {
                        message = "Data Parsing Error";
                    }
                    errorCallback.onError(message);
                });

        request.setRetryPolicy(new DefaultRetryPolicy(
                10000,
                1, // 1 retry
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        Volley.newRequestQueue(context).add(request);
    }

    public interface ExamLinkCallback {
        void onSingleLink(String link);

        void onError(String error);
    }

    public interface VolleySuccessCallback {
        void onSuccess(org.json.JSONObject response);
    }

    public interface VolleyErrorCallback {
        void onError(String errorMessage);
    }

    public static GradientDrawable shadowMaker(Context context) {
        float radiusInPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16,
                context.getResources().getDisplayMetrics());

        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(ContextCompat.getColor(context, R.color.white));
        drawable.setCornerRadius(radiusInPx);
        return drawable;
    }

    public static void setupDropDown(Context context, @NonNull android.widget.AutoCompleteTextView dropdown,
            List<String> data) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, R.layout.dropdown_item, data);
        dropdown.setDropDownBackgroundDrawable(shadowMaker(context));
        dropdown.setAdapter(adapter);
    }

    public static void applyBounceAnimation(View view) {
        view.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                Animation anim = AnimationUtils.loadAnimation(v.getContext(), R.anim.bounce);
                v.startAnimation(anim);
            }
            return false;
        });
    }

    public static void updateTabIndicator(View indicator, View target, ViewGroup container) {
        AutoTransition transition = new AutoTransition();
        transition.setDuration(300);
        transition.setInterpolator(new AccelerateDecelerateInterpolator());
        TransitionManager.beginDelayedTransition(container, transition);

        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) indicator
                .getLayoutParams();
        params.startToStart = target.getId();
        params.endToEnd = target.getId();
        indicator.setLayoutParams(params);
    }

    public static void updateLectureCounts(Context context, List<String> subjects, Runnable onComplete) {
        if (!isNetworkAvailable(context)) {
            showToast(context, "No internet connection. Some data may not load.");
            onComplete.run();
            return;
        }
        fetchLectures(context, new DatabaseCallback() {
            @Override
            public void onReady(JSONObject result) {
                try {
                    JSONObject lecturesMap = result.getJSONObject("lectures");
                    final int[] remaining = { 0 };

                    // Count valid subjects first to manage callbacks
                    List<String> validSubjects = new ArrayList<>();
                    for (String subject : subjects) {
                        if (lecturesMap.has(subject)) {
                            validSubjects.add(subject);
                        }
                    }

                    if (validSubjects.isEmpty()) {
                        onComplete.run();
                        return;
                    }

                    remaining[0] = validSubjects.size();
                    for (String subject : validSubjects) {
                        try {
                            String url = lecturesMap.getString(subject);
                            makeApiRequest(context, url, response -> {
                                try {
                                    int count = 0;
                                    if (response.has("weeks")) {
                                        JSONObject weeks = response.getJSONObject("weeks");
                                        Iterator<String> keys = weeks.keys();
                                        while (keys.hasNext()) {
                                            String weekKey = keys.next();
                                            JSONArray weekLectures = weeks.getJSONArray(weekKey);
                                            count += weekLectures.length();
                                        }
                                    }
                                    subjectToLectureCount.put(subject, count);
                                } catch (JSONException ignored) {
                                } finally {
                                    remaining[0]--;
                                    if (remaining[0] == 0)
                                        onComplete.run();
                                }
                            }, error -> {
                                remaining[0]--;
                                if (remaining[0] == 0)
                                    onComplete.run();
                            });
                        } catch (JSONException e) {
                            remaining[0]--;
                            if (remaining[0] == 0)
                                onComplete.run();
                        }
                    }
                } catch (JSONException e) {
                    onComplete.run();
                }
            }

            @Override
            public void onError(String error) {
                onComplete.run();
            }
        });
    }

    public static String getGreeting() {
        Calendar c = Calendar.getInstance();
        int timeOfDay = c.get(java.util.Calendar.HOUR_OF_DAY);

        if (timeOfDay >= 5 && timeOfDay < 12) {
            return "Good Morning";
        } else if (timeOfDay >= 12 && timeOfDay < 17) {
            return "Good Afternoon";
        } else {
            return "Good Evening";
        }
    }

    public static int getRunningWeek(String startDateStr) {
        if (startDateStr == null || startDateStr.isEmpty())
            return 1;
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss",
                    java.util.Locale.getDefault());
            java.util.Date startDate = sdf.parse(startDateStr);
            if (startDate == null)
                return 1;
            long diff = System.currentTimeMillis() - startDate.getTime();
            long days = diff / (1000 * 60 * 60 * 24);
            int week = (int) (days / 7) + 1;
            return Math.max(1, week);
        } catch (Exception e) {
            return 1;
        }
    }

    public static boolean isNetworkAvailable(Context context) {
        android.net.ConnectivityManager connectivityManager = (android.net.ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null)
            return false;
        android.net.NetworkCapabilities capabilities = connectivityManager
                .getNetworkCapabilities(connectivityManager.getActiveNetwork());
        return capabilities != null && (capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)
                || capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR)
                || capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET));
    }

    public static void showToast(Context context, String message) {
        if (context instanceof Activity) {
            ((Activity) context).runOnUiThread(
                    () -> android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show());
        } else {
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show();
        }
    }
}
