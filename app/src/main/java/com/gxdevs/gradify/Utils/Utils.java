package com.gxdevs.gradify.Utils;

import static android.content.Context.MODE_PRIVATE;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.util.Base64;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Utils {
    static Map<String, String> subjectToType = new HashMap<>();
    static Map<String, Integer> subjectToLectureCount = new HashMap<>();

    public static String decryptUrl(String url) {
        Log.d("PYQ_DEBUG", "decryptUrl: Attempting to decrypt URL");
        if (url == null) {
            Log.e("PYQ_DEBUG", "decryptUrl: URL is null");
            return null;
        }
        if (!url.startsWith("youarenoob.gradify/")) {
            Log.w("PYQ_DEBUG", "decryptUrl: URL does not have encryption prefix: " + url);
            return url;
        }

        try {
            // Remove prefix
            String b64 = url.replace("youarenoob.gradify/", "");

            // Base64 decode (URL_SAFE)
            byte[] encryptedData = Base64.decode(b64, Base64.URL_SAFE);
            Log.d("PYQ_DEBUG", "decryptUrl: Base64 decoded length: " + encryptedData.length);

            if (encryptedData.length < 16) {
                Log.e("PYQ_DEBUG", "decryptUrl: Invalid encrypted data length (" + encryptedData.length + " < 16)");
                Log.e("Decryption", "Invalid encrypted data length");
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
            String decryptedString = new String(decryptedBytes, StandardCharsets.UTF_8);
            Log.d("PYQ_DEBUG", "decryptUrl: Decryption successful: " + decryptedString);
            return decryptedString;

        } catch (Exception e) {
            Log.e("PYQ_DEBUG", "decryptUrl: Failed to decrypt URL - " + e.getMessage());
            Log.e("Decryption", "Failed to decrypt URL", e);
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

    private static org.json.JSONObject indexCache;
    private static org.json.JSONObject formulasCache;

    public static int dpToPx(Context context, int i) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (i * scale);
    }

    public interface DatabaseCallback {
        void onReady(org.json.JSONObject database);

        void onError(String error);
    }

    public static void fetchIndexData(Context context, final DatabaseCallback callback) {
        Log.d("PYQ_DEBUG", "fetchIndexData: Checking cache");
        if (indexCache != null) {
            Log.d("PYQ_DEBUG", "fetchIndexData: Returning cached index");
            callback.onReady(indexCache);
            return;
        }
        Log.d("PYQ_DEBUG", "fetchIndexData: Fetching from URL: " + INDEX_URL);
        makeApiRequest(context, INDEX_URL, response -> {
            Log.d("PYQ_DEBUG", "fetchIndexData: Successfully fetched index.json");
            indexCache = response;
            callback.onReady(indexCache);
        }, error -> {
            Log.e("PYQ_DEBUG", "fetchIndexData: Error - " + error);
            callback.onError(error);
        });
    }

    public static void fetchSubjectQuizData(Context context, String subject, final DatabaseCallback callback) {
        Log.d("PYQ_DEBUG", "fetchSubjectQuizData: Fetching quiz data for subject - " + subject);
        fetchIndexData(context, new DatabaseCallback() {
            @Override
            public void onReady(org.json.JSONObject index) {
                try {
                    Log.d("PYQ_DEBUG", "fetchSubjectQuizData: Index received, looking for quizzes section");
                    if (!index.has("quizzes")) {
                        Log.e("PYQ_DEBUG", "fetchSubjectQuizData: Quizzes section not found in index");
                        callback.onError("Quizzes section not found in index");
                        return;
                    }
                    org.json.JSONObject quizzes = index.getJSONObject("quizzes");
                    if (!quizzes.has(subject)) {
                        Log.e("PYQ_DEBUG",
                                "fetchSubjectQuizData: Subject '" + subject + "' not found in quizzes index");
                        callback.onError("Subject " + subject + " not found in quizzes index");
                        return;
                    }
                    String url = githubToJsDelivr(quizzes.getString(subject));
                    Log.d("PYQ_DEBUG", "fetchSubjectQuizData: Fetching quiz file from: " + url);
                    makeApiRequest(context, url, response -> {
                        Log.d("PYQ_DEBUG", "fetchSubjectQuizData: Successfully fetched quiz data for " + subject);
                        callback.onReady(response);
                    }, error -> {
                        Log.e("PYQ_DEBUG", "fetchSubjectQuizData: Error fetching quiz file - " + error);
                        callback.onError(error);
                    });
                } catch (JSONException e) {
                    Log.e("PYQ_DEBUG", "fetchSubjectQuizData: JSON Error - " + e.getMessage());
                    callback.onError("JSON Error: " + e.getMessage());
                }
            }

            @Override
            public void onError(String error) {
                Log.e("PYQ_DEBUG", "fetchSubjectQuizData: Error from fetchIndexData - " + error);
                callback.onError(error);
            }
        });
    }

    public static void fetchLectures(Context context, final DatabaseCallback callback) {
        fetchIndexData(context, new DatabaseCallback() {
            @Override
            public void onReady(org.json.JSONObject index) {
                try {
                    if (!index.has("lectures")) {
                        callback.onError("Lectures section not found in index");
                        return;
                    }
                    org.json.JSONObject lecturesNested = index.getJSONObject("lectures");
                    org.json.JSONObject flattened = new org.json.JSONObject();

                    // Flatten Diploma and Foundation subjects for compatibility
                    java.util.Iterator<String> levels = lecturesNested.keys();
                    while (levels.hasNext()) {
                        String level = levels.next();
                        Object levelObj = lecturesNested.get(level);
                        if (levelObj instanceof org.json.JSONObject) {
                            org.json.JSONObject subjects = (org.json.JSONObject) levelObj;
                            java.util.Iterator<String> subs = subjects.keys();
                            while (subs.hasNext()) {
                                String sub = subs.next();
                                flattened.put(sub, githubToJsDelivr(subjects.getString(sub)));
                            }
                        }
                    }

                    org.json.JSONObject result = new org.json.JSONObject();
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
            public void onReady(org.json.JSONObject index) {
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
            public void onReady(org.json.JSONObject index) {
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
        Log.d("PYQ_DEBUG", "fetchData: subject=" + subject + ", quizType=" + quizType + ", year=" + year);
        fetchSubjectQuizData(context, subject, new DatabaseCallback() {
            @Override
            public void onReady(org.json.JSONObject database) {
                try {
                    Log.d("PYQ_DEBUG", "fetchData: Quiz database received");
                    org.json.JSONObject papers;
                    if (database.has("papers")) {
                        papers = database.getJSONObject("papers");
                        Log.d("PYQ_DEBUG", "fetchData: Found 'papers' key");
                    } else {
                        papers = database;
                        Log.d("PYQ_DEBUG", "fetchData: No 'papers' key, using root object");
                    }

                    if (!papers.has(subject)) {
                        if (papers.length() == 1 && papers.keys().next().equals(subject)) {
                            Log.d("PYQ_DEBUG", "fetchData: Subject found as single key");
                        } else {
                            Log.e("PYQ_DEBUG", "fetchData: Subject '" + subject + "' not found in papers");
                            callback.onError("Subject not found");
                            return;
                        }
                    }
                    org.json.JSONObject subjectObj = papers.getJSONObject(subject);
                    Log.d("PYQ_DEBUG", "fetchData: Subject object retrieved");

                    if (!subjectObj.has(quizType)) {
                        Log.e("PYQ_DEBUG",
                                "fetchData: Quiz type '" + quizType + "' not found. Available: " + subjectObj.keys());
                        callback.onError("Quiz type not found");
                        return;
                    }
                    org.json.JSONObject quizObj = subjectObj.getJSONObject(quizType);
                    Log.d("PYQ_DEBUG", "fetchData: Quiz type object retrieved");

                    if (year == null) {
                        // Return years
                        Log.d("PYQ_DEBUG", "fetchData: Returning years list");
                        JSONArray names = quizObj.names();
                        if (names == null) {
                            Log.w("PYQ_DEBUG", "fetchData: No years found");
                            callback.onSuccess(new String[0]);
                            return;
                        }
                        List<String> yearsList = new ArrayList<>();
                        for (int i = 0; i < names.length(); i++) {
                            yearsList.add(names.getString(i));
                        }

                        // Custom sorting for formats like "Oct, 2024" or "Feb, 2024"
                        java.util.Collections.sort(yearsList, (s1, s2) -> {
                            try {
                                String[] parts1 = s1.split(", ");
                                String[] parts2 = s2.split(", ");

                                if (parts1.length == 2 && parts2.length == 2) {
                                    int y1 = Integer.parseInt(parts1[1].trim());
                                    int y2 = Integer.parseInt(parts2[1].trim());

                                    if (y1 != y2)
                                        return Integer.compare(y2, y1); // Latest year first

                                    // Same year, compare months
                                    java.util.Map<String, Integer> monthMap = new java.util.HashMap<>();
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

                        Log.d("PYQ_DEBUG", "fetchData: Found years (chronologically sorted): " + yearsList);
                        callback.onSuccess(yearsList.toArray(new String[0]));
                    } else {
                        // Return sessions
                        Log.d("PYQ_DEBUG", "fetchData: Returning sessions for year: " + year);
                        if (!quizObj.has(year)) {
                            Log.e("PYQ_DEBUG",
                                    "fetchData: Year '" + year + "' not found. Available: " + quizObj.keys());
                            callback.onError("Year not found");
                            return;
                        }
                        org.json.JSONObject yearObj = quizObj.getJSONObject(year);
                        JSONArray names = yearObj.names();
                        if (names == null) {
                            Log.w("PYQ_DEBUG", "fetchData: No sessions found");
                            callback.onSuccess(new String[0]);
                            return;
                        }
                        List<String> sessionsList = new ArrayList<>();
                        for (int i = 0; i < names.length(); i++) {
                            sessionsList.add(names.getString(i));
                        }
                        java.util.Collections.sort(sessionsList); // Sort sessions alphabetically
                        Log.d("PYQ_DEBUG", "fetchData: Found sessions (sorted): " + sessionsList);
                        callback.onSuccess(sessionsList.toArray(new String[0]));
                    }
                } catch (JSONException e) {
                    Log.e("PYQ_DEBUG", "fetchData: JSON Error - " + e.getMessage());
                    callback.onError("JSON Error: " + e.getMessage());
                }
            }

            @Override
            public void onError(String error) {
                Log.e("PYQ_DEBUG", "fetchData: Error from fetchSubjectQuizData - " + error);
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

        // Subject to Lecture Count (dummy data)
        subjectToLectureCount.put("Maths 1", 87);
        subjectToLectureCount.put("Maths 2", 105);
        subjectToLectureCount.put("Stats 1", 135);
        subjectToLectureCount.put("Stats 2", 84);
        subjectToLectureCount.put("CT", 104);
        subjectToLectureCount.put("English 1", 74);
        subjectToLectureCount.put("English 2", 73);
        subjectToLectureCount.put("Python", 85);
        subjectToLectureCount.put("Business Analytics", 58);
        subjectToLectureCount.put("MLF", 85);
        subjectToLectureCount.put("BDM", 88);
        subjectToLectureCount.put("DBMS", 80);
        subjectToLectureCount.put("Java", 57);
        subjectToLectureCount.put("MAD 1", 90);
        subjectToLectureCount.put("MAD 2", 56);
        subjectToLectureCount.put("MLP", 76);
        subjectToLectureCount.put("MLT", 80);
        subjectToLectureCount.put("PDSA", 73);
        subjectToLectureCount.put("System Commands", 33);
        subjectToLectureCount.put("TDS", 0);
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

                    Log.d("PYQ_DEBUG", "fetchExamJsonLink: Encrypted link - " + link);
                    link = decryptUrl(link);
                    Log.d("PYQ_DEBUG", "fetchExamJsonLink: Decrypted link - " + link);

                    if (link == null || link.isEmpty()) {
                        Log.e("PYQ_DEBUG", "fetchExamJsonLink: Link is null or empty after decryption");
                        callback.onError("Link is empty");
                    } else {
                        Log.d("PYQ_DEBUG", "fetchExamJsonLink: Returning link to callback");
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

    public static void makeApiRequest(Context context, String url, VolleySuccessCallback successCallback,
            VolleyErrorCallback errorCallback) {
        Log.d("API Request", "URL: " + url);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null, response -> {
            Log.d("API Response", "Success for URL: " + url);
            successCallback.onSuccess(response);
        }, error -> {
            String message = "Unknown Error";
            if (error instanceof NetworkError) {
                message = "No Internet Connection";
            } else if (error instanceof TimeoutError) {
                message = "Connection Timed Out";
            } else if (error instanceof ServerError) {
                if (error.networkResponse != null) {
                    message = "Server Error: " + error.networkResponse.statusCode;
                    Log.e("API Error", "Response data: " + new String(error.networkResponse.data));
                } else {
                    message = "Server Error";
                }
            } else if (error instanceof ParseError) {
                message = "Data Parsing Error";
            }
            Log.e("API Error", "URL: " + url + " | Error: " + message);
            if (error.getMessage() != null) {
                Log.e("API Error", "Details: " + error.getMessage());
            }
            errorCallback.onError(message);
        });

        request.setRetryPolicy(new DefaultRetryPolicy(
                10000,
                1, // 1 retry
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        Volley.newRequestQueue(context).add(request);
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
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                Animation anim = AnimationUtils.loadAnimation(v.getContext(), R.anim.bounce);
                v.startAnimation(anim);
            }
            return false;
        });
    }

    public static void updateTabIndicator(View indicator, View target, ViewGroup container) {
        androidx.transition.AutoTransition transition = new androidx.transition.AutoTransition();
        transition.setDuration(300);
        transition.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
        androidx.transition.TransitionManager.beginDelayedTransition(container, transition);

        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams params = (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) indicator
                .getLayoutParams();
        params.startToStart = target.getId();
        params.endToEnd = target.getId();
        indicator.setLayoutParams(params);
    }
}
