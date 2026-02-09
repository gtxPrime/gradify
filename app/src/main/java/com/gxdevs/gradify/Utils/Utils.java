package com.gxdevs.gradify.Utils;

import static android.content.Context.MODE_PRIVATE;
import static android.graphics.drawable.GradientDrawable.LINEAR_GRADIENT;
import static com.gxdevs.gradify.activities.SettingsActivity.GRADIENT_1_COLOR_KEY;
import static com.gxdevs.gradify.activities.SettingsActivity.PRIMARY_COLOR_KEY;
import static com.gxdevs.gradify.activities.SettingsActivity.SECONDARY_COLOR_KEY;
import static com.gxdevs.gradify.activities.SettingsActivity.THEME_STYLE_KEY;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.gxdevs.gradify.R;
import com.gxdevs.gradify.activities.SettingsActivity;
import com.gxdevs.gradify.BuildConfig;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Utils {
    static Map<String, String> subjectToType = new HashMap<>();
    static Map<String, Integer> subjectToLectureCount = new HashMap<>();

    public static String decryptUrl(String url) {
        if (url == null)
            return null;
        if (!url.startsWith("youarenoob.gradify/")) {
            return url;
        }

        try {
            // Remove prefix
            String b64 = url.replace("youarenoob.gradify/", "");

            // Base64 decode (URL_SAFE)
            byte[] encryptedData = Base64.decode(b64, Base64.URL_SAFE);

            if (encryptedData.length < 16) {
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
            if (keyHex == null || keyHex.isEmpty()) {
                Log.e("Decryption", "Secret Key not found in BuildConfig");
                return url;
            }

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
            return new String(decryptedBytes, "UTF-8");

        } catch (Exception e) {
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

    public interface DatabaseCallback {
        void onReady(org.json.JSONObject database);

        void onError(String error);
    }

    public static void fetchIndexData(Context context, final DatabaseCallback callback) {
        if (indexCache != null) {
            callback.onReady(indexCache);
            return;
        }
        makeApiRequest(context, INDEX_URL, response -> {
            indexCache = response;
            callback.onReady(indexCache);
        }, callback::onError);
    }

    public static void fetchQuizData(Context context, final DatabaseCallback callback) {
        fetchIndexData(context, callback);
    }

    public static void fetchSubjectQuizData(Context context, String subject, final DatabaseCallback callback) {
        fetchIndexData(context, new DatabaseCallback() {
            @Override
            public void onReady(org.json.JSONObject index) {
                try {
                    if (!index.has("quizzes")) {
                        callback.onError("Quizzes section not found in index");
                        return;
                    }
                    org.json.JSONObject quizzes = index.getJSONObject("quizzes");
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
            public void onReady(org.json.JSONObject database) {
                try {
                    org.json.JSONObject papers;
                    if (database.has("papers")) {
                        papers = database.getJSONObject("papers");
                    } else {
                        // In the new split structure, the subject object might be at the root
                        // OR inside 'papers'
                        papers = database;
                    }

                    if (!papers.has(subject)) {
                        // Some split files might only contain the subject's data directly
                        // If it's the BDM file, and it's root is { "papers": { "BDM": ... } }
                        // Then we already handle it. If it's just { "BDM": ... } then:
                        if (papers.length() == 1 && papers.keys().next().equals(subject)) {
                            // Already correct
                        } else {
                            // Check if the current object IS the subject data
                            // (Based on looking at quiz_BDM.json, it HAS 'papers' key)
                            callback.onError("Subject not found");
                            return;
                        }
                    }
                    org.json.JSONObject subjectObj = papers.getJSONObject(subject);

                    if (!subjectObj.has(quizType)) {
                        callback.onError("Quiz type not found");
                        return;
                    }
                    org.json.JSONObject quizObj = subjectObj.getJSONObject(quizType);

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
                        // Use a simple sort logic or keep as is? The names() order is not guaranteed.
                        // Let's just return as is for now.
                        callback.onSuccess(yearsList.toArray(new String[0]));
                    } else {
                        // Return sessions
                        if (!quizObj.has(year)) {
                            callback.onError("Year not found");
                            return;
                        }
                        org.json.JSONObject yearObj = quizObj.getJSONObject(year);
                        JSONArray names = yearObj.names();
                        if (names == null) {
                            callback.onSuccess(new String[0]);
                            return;
                        }
                        List<String> sessionsList = new ArrayList<>();
                        for (int i = 0; i < names.length(); i++) {
                            sessionsList.add(names.getString(i));
                        }
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

                    link = decryptUrl(link);

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

    public interface ExamLinkCallback {
        void onSingleLink(String link);

        void onMultipleLinks(String[] links);

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
            Log.d("API Response", response.toString());
            successCallback.onSuccess(response);
        }, error -> {
            String message = "Unknown Error";
            if (error instanceof com.android.volley.NoConnectionError
                    || error instanceof com.android.volley.NetworkError) {
                message = "No Internet Connection";
            } else if (error instanceof com.android.volley.TimeoutError) {
                message = "Connection Timed Out";
            } else if (error instanceof com.android.volley.ServerError) {
                if (error.networkResponse != null) {
                    message = "Server Error: " + error.networkResponse.statusCode;
                } else {
                    message = "Server Error";
                }
            } else if (error instanceof com.android.volley.ParseError) {
                message = "Data Parsing Error";
            }
            Log.e("API Error", message);
            errorCallback.onError(message);
        });

        request.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
                10000,
                1, // 1 retry
                com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        Volley.newRequestQueue(context).add(request);
    }

    public static int setTextColorBasedOnBackground(Context context, String whichColor) {
        // Calculate the luminance of the background color
        SharedPreferences preferences = context.getSharedPreferences(SettingsActivity.PREFS_NAME, MODE_PRIVATE);
        int backgroundColor;
        int textColor;
        if (whichColor.equals("primary")) {
            backgroundColor = preferences.getInt(PRIMARY_COLOR_KEY,
                    ContextCompat.getColor(context, R.color.primaryColor));
        } else {
            backgroundColor = preferences.getInt(SECONDARY_COLOR_KEY,
                    ContextCompat.getColor(context, R.color.secondaryColor));
        }

        double luminance = (0.299 * Color.red(backgroundColor) +
                0.587 * Color.green(backgroundColor) +
                0.114 * Color.blue(backgroundColor)) / 255;

        if (luminance > 0.5) {
            textColor = Color.BLACK;
        } else {
            textColor = Color.WHITE;
        }
        return textColor;
    }

    public static int dropperTextColor(int bgColor) {
        int textColor;
        double luminance = (0.299 * Color.red(bgColor) +
                0.587 * Color.green(bgColor) +
                0.114 * Color.blue(bgColor)) / 255;

        if (luminance > 0.5) {
            textColor = Color.BLACK;
        } else {
            textColor = Color.WHITE;
        }
        return textColor;
    }

    private static GradientDrawable createRadialGradient(Context context, String colorKey,
            int defaultColorResId) {
        SharedPreferences prefs = context.getSharedPreferences(SettingsActivity.PREFS_NAME, MODE_PRIVATE);
        int startColor = prefs.getInt(colorKey, ContextCompat.getColor(context, defaultColorResId));

        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setShape(GradientDrawable.RECTANGLE); // Will be used with oval shape on ImageView if needed
        gradientDrawable.setCornerRadius(100f); // Match your oval_gradient.xml, or make it a parameter

        gradientDrawable.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        gradientDrawable.setGradientCenter(0.5f, 0.5f); // Center of the shape
        // Adjust gradientRadius as needed, this is an example.
        // It might be better to calculate this based on the view's dimensions if they
        // are known.
        gradientDrawable.setGradientRadius(210 * context.getResources().getDisplayMetrics().density); // Example radius
                                                                                                      // in dp
        gradientDrawable.setColors(new int[] { startColor, Color.TRANSPARENT }); // Radial from startColor to
                                                                                 // transparent

        return gradientDrawable;
    }

    public static void applyGradient1(Context context, ImageView imageView) {
        GradientDrawable gradient = createRadialGradient(context, GRADIENT_1_COLOR_KEY, R.color.ga1);
        imageView.setImageDrawable(gradient);
    }

    public static void applyGradient2(Context context, ImageView imageView) {
        // Replace R.color.gradient_mid_color with your actual default color resource ID
        GradientDrawable gradient = createRadialGradient(context, SettingsActivity.GRADIENT_2_COLOR_KEY, R.color.ga2); // Ensure
                                                                                                                       // ga2
                                                                                                                       // exists
        imageView.setImageDrawable(gradient);
    }

    public static void applyGradient3(Context context, ImageView imageView) {
        // Replace R.color.gradient_end_color with your actual default color resource ID
        GradientDrawable gradient = createRadialGradient(context, SettingsActivity.GRADIENT_3_COLOR_KEY, R.color.ga3); // Ensure
                                                                                                                       // ga3
                                                                                                                       // exists
        imageView.setImageDrawable(gradient);
    }

    public static void bgGrayGenerate(Context context, ImageView imageView) {
        SharedPreferences preferences = context.getSharedPreferences(SettingsActivity.PREFS_NAME, MODE_PRIVATE);
        int startColor = preferences.getInt(GRADIENT_1_COLOR_KEY, ContextCompat.getColor(context, R.color.primary));
        int endColor = ContextCompat.getColor(context, R.color.background);

        GradientDrawable gradientDrawable = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[] { startColor, endColor, endColor });

        imageView.setImageDrawable(gradientDrawable);
    }

    public static void setTheme(Context context, ImageView skyLight, ImageView decor1, ImageView decor2,
            ImageView decor3) {
        SharedPreferences preferences = context.getSharedPreferences(SettingsActivity.PREFS_NAME, MODE_PRIVATE);
        String currentStyle = preferences.getString(THEME_STYLE_KEY, "EdgeFlare");

        if ("SkyLight".equals(currentStyle)) {
            skyLight.setVisibility(View.VISIBLE);
            Utils.bgGrayGenerate(context, skyLight);

            decor1.setVisibility(View.GONE);
            decor2.setVisibility(View.GONE);
            decor3.setVisibility(View.GONE);

        } else {
            skyLight.setVisibility(View.GONE);

            decor1.setVisibility(View.VISIBLE);
            decor2.setVisibility(View.VISIBLE);
            decor3.setVisibility(View.VISIBLE);

            Utils.applyGradient1(context, decor1);
            Utils.applyGradient2(context, decor2);
            Utils.applyGradient3(context, decor3);
        }
    }

    public static GradientDrawable setCardColor(Context context, int amount) {
        SharedPreferences preferences = context.getSharedPreferences(SettingsActivity.PREFS_NAME, MODE_PRIVATE);
        int startColor;
        if (amount == 10) {
            startColor = preferences.getInt(PRIMARY_COLOR_KEY, ContextCompat.getColor(context, R.color.primaryColor));
        } else {
            startColor = preferences.getInt(SECONDARY_COLOR_KEY,
                    ContextCompat.getColor(context, R.color.secondaryColor));
        }
        float radiusInPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, amount,
                context.getResources().getDisplayMetrics());

        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(startColor);
        drawable.setCornerRadius(radiusInPx);
        return drawable;
    }

    public static void setDropperColors(Context context, TextInputLayout textInputLayout, int hint) {
        SharedPreferences preferences = context.getSharedPreferences(SettingsActivity.PREFS_NAME, MODE_PRIVATE);
        int startColor = preferences.getInt(PRIMARY_COLOR_KEY, ContextCompat.getColor(context, R.color.primaryColor));
        textInputLayout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_FILLED);
        textInputLayout.setBoxBackgroundColor(startColor);
        textInputLayout.setHint(ContextCompat.getString(context, hint));
        textInputLayout.setDefaultHintTextColor(ColorStateList.valueOf(dropperTextColor(startColor)));
    }

    public static GradientDrawable shadowMaker(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(SettingsActivity.PREFS_NAME, MODE_PRIVATE);
        int startColor = preferences.getInt(PRIMARY_COLOR_KEY, ContextCompat.getColor(context, R.color.primaryColor));
        float radiusInPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10,
                context.getResources().getDisplayMetrics());
        int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5,
                context.getResources().getDisplayMetrics());
        int width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 200,
                context.getResources().getDisplayMetrics());

        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(startColor);
        drawable.setCornerRadius(radiusInPx);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            drawable.setPadding(padding, padding, padding, padding);
        }
        drawable.setSize(width, 0);
        return drawable;
    }

    public static void buttonTint(Context context, Button button) {
        SharedPreferences preferences = context.getSharedPreferences(SettingsActivity.PREFS_NAME, MODE_PRIVATE);
        int startColor = preferences.getInt(PRIMARY_COLOR_KEY, ContextCompat.getColor(context, R.color.primaryColor));
        button.setBackgroundTintList(ColorStateList.valueOf(startColor));
    }

    public static GradientDrawable drawerMaker(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(SettingsActivity.PREFS_NAME, MODE_PRIVATE);
        int startColor = preferences.getInt(PRIMARY_COLOR_KEY, ContextCompat.getColor(context, R.color.primaryColor));

        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[] { startColor, ContextCompat.getColor(context, R.color.background),
                        ContextCompat.getColor(context, R.color.background) });
        drawable.setGradientType(LINEAR_GRADIENT);
        drawable.setCornerRadius(20f);
        return drawable;
    }

    public static void switchColors(Context context, MaterialSwitch materialSwitch) {
        SharedPreferences preferences = context.getSharedPreferences(SettingsActivity.PREFS_NAME, MODE_PRIVATE);
        int color = preferences.getInt(PRIMARY_COLOR_KEY, ContextCompat.getColor(context, R.color.primaryColor));
        materialSwitch.setThumbTintList(ColorStateList.valueOf(color));
    }

    public static void radioColors(Context context, MaterialRadioButton radioButton) {
        SharedPreferences preferences = context.getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE);
        int checkColor = preferences.getInt(GRADIENT_1_COLOR_KEY, ContextCompat.getColor(context, R.color.glowColor));
        ColorStateList colorStateList = new ColorStateList(
                new int[][] {
                        new int[] { android.R.attr.state_checked },
                        new int[] { -android.R.attr.state_checked }
                },
                new int[] {
                        checkColor,
                        ContextCompat.getColor(context, R.color.gray)
                });
        radioButton.setButtonTintList(colorStateList);
    }

    public static void checkBoxColors(Context context, CheckBox checkBox) {
        SharedPreferences preferences = context.getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE);
        int checkColor = preferences.getInt(GRADIENT_1_COLOR_KEY, ContextCompat.getColor(context, R.color.glowColor));
        ColorStateList colorStateList = new ColorStateList(
                new int[][] {
                        new int[] { android.R.attr.state_checked },
                        new int[] { -android.R.attr.state_checked }
                },
                new int[] {
                        checkColor,
                        ContextCompat.getColor(context, R.color.gray)
                });
        checkBox.setButtonTintList(colorStateList);
    }

    public static int chipUnselected(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(SettingsActivity.PREFS_NAME, MODE_PRIVATE);
        return preferences.getInt(SECONDARY_COLOR_KEY, ContextCompat.getColor(context, R.color.secondaryColor));
    }

    public static int selected(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(SettingsActivity.PREFS_NAME, MODE_PRIVATE);
        return preferences.getInt(PRIMARY_COLOR_KEY, ContextCompat.getColor(context, R.color.primaryColor));
    }
}
