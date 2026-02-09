package com.gxdevs.gradify.services;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.Part;
import com.google.ai.client.generativeai.type.TextPart;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GeminiChatService {
    private static final String TAG = "GeminiChatService";
    // private final GenerativeModelFutures generativeModel; // No longer used for the main call
    private final OkHttpClient httpClient;
    private final Executor executor; // To run callbacks on the correct thread

    // User hardcoded API key
    private static final String MODEL_NAME = "gemini-2.0-flash"; // Using a standard model name
    private static final String GOOGLE_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/" + MODEL_NAME + ":generateContent?key=";
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public interface GeminiChatCallback {
        void onSuccess(String responseText);

        void onError(String errorMessage);
    }

    public GeminiChatService(Executor executor) {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(120, TimeUnit.SECONDS) // 2 minutes
                .readTimeout(120, TimeUnit.SECONDS)    // 2 minutes
                .writeTimeout(120, TimeUnit.SECONDS)   // 2 minutes
                .build();
        this.executor = executor;
    }

    public void generateChatResponse(String API_KEY, List<Content> chatHistory, String currentVideoLink, String userPrompt, GeminiChatCallback callback) {

        JSONArray contentsArray = new JSONArray();
        JSONObject requestBodyJson = new JSONObject();

        try {
            // History processing: Assuming chatHistory from LectureActivity is pre-filtered (e.g., max 3 user, 3 model) and ordered chronologically.
            if (chatHistory != null) {
                for (Content historyContent : chatHistory) {
                    JSONObject historyEntryJson = contentToGoogleApiJson(historyContent);
                    if (historyEntryJson != null) {
                        contentsArray.put(historyEntryJson);
                    }
                }
            }

            // Add current user message with prompt and video link
            JSONObject currentUserContentEntry = buildCurrentUserContentForGoogleApi(currentVideoLink, userPrompt);
            contentsArray.put(currentUserContentEntry);

            // For Google API, the root of the request is an object with a "contents" key
            requestBodyJson.put("contents", contentsArray);
            // The API key is in the URL, not in the root of the JSON body for Google's endpoint.

        } catch (JSONException e) {
            Log.e(TAG, "JSONException while building request for Google API: " + e.getMessage(), e);
            final String errorMsg = "Error building request JSON: " + e.getMessage();
            executor.execute(() -> callback.onError(errorMsg));
            return;
        }

        RequestBody body = RequestBody.create(requestBodyJson.toString(), JSON);
        Request request = new Request.Builder()
                .url(GOOGLE_API_URL + API_KEY) // Use the Google API URL
                .post(body)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "OkHttp call failed: " + e.getMessage(), e);
                final String errorMsg = "API request failed (timeout or network issue): " + e.getMessage();
                executor.execute(() -> callback.onError(errorMsg));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                final String responseBodyString = response.body() != null ? response.body().string() : null;

                if (!response.isSuccessful()) {
                    Log.e(TAG, "API Error: " + response.code() + " - " + responseBodyString);
                    final String errorMsg = "API Error: " + response.code() + " - " + responseBodyString;
                    executor.execute(() -> callback.onError(errorMsg));
                    response.close(); // Ensure response is closed
                    return;
                }

                if (responseBodyString == null || responseBodyString.isEmpty()) {
                    Log.e(TAG, "API returned empty response body.");
                    executor.execute(() -> callback.onError("AI response was empty."));
                    response.close(); // Ensure response is closed
                    return;
                }

                // Parse response for Google API
                try {
                    JSONObject jsonResponse = new JSONObject(responseBodyString);
                    JSONArray candidates = jsonResponse.optJSONArray("candidates");
                    if (candidates != null && candidates.length() > 0) {
                        JSONObject firstCandidate = candidates.getJSONObject(0);
                        JSONObject content = firstCandidate.optJSONObject("content");
                        if (content != null) {
                            JSONArray responseParts = content.optJSONArray("parts");
                            if (responseParts != null && responseParts.length() > 0) {
                                JSONObject firstPart = responseParts.getJSONObject(0);
                                String text = firstPart.optString("text", null);
                                if (!text.isEmpty()) {
                                    final String successText = text;
                                    executor.execute(() -> callback.onSuccess(successText));
                                    response.close();
                                    return;
                                }
                            }
                        }
                    }
                    Log.w(TAG, "Could not extract text from Google API response: " + responseBodyString);
                    executor.execute(() -> callback.onError("AI response format not recognized or text missing."));

                } catch (JSONException e) {
                    Log.e(TAG, "JSONException while parsing Google API response: " + e.getMessage(), e);
                    final String errorMsg = "Error parsing AI response: " + e.getMessage();
                    executor.execute(() -> callback.onError(errorMsg));
                } finally {
                    response.close();
                }
            }
        });
    }

    // Renamed and adapted for Google API structure if needed (current structure is likely fine)
    private JSONObject contentToGoogleApiJson(Content content) {
        try {
            JSONObject contentJson = new JSONObject();
            contentJson.put("role", content.getRole()); // Roles are "user" or "model"
            JSONArray partsArray = new JSONArray();
            for (Part part : content.getParts()) {
                if (part instanceof TextPart) {
                    String text = ((TextPart) part).getText();
                    if (!text.isEmpty()) {
                        JSONObject textPartJson = new JSONObject();
                        textPartJson.put("text", text);
                        partsArray.put(textPartJson);
                    }
                } else {
                    Log.w(TAG, "Skipping non-TextPart in history for Google API: " + part.toString());
                }
            }
            if (partsArray.length() == 0) return null;
            contentJson.put("parts", partsArray);
            return contentJson;
        } catch (JSONException e) {
            Log.e(TAG, "Error converting Content to JSON for Google API: " + e.getMessage());
            return null;
        }
    }

    // Renamed and adapted for Google API structure
    @NonNull
    private JSONObject buildCurrentUserContentForGoogleApi(String currentVideoLink, String userPrompt) throws JSONException {
        JSONObject currentUserTextPart = new JSONObject();
        // Prepending context instruction as before
        currentUserTextPart.put("text", "Answer this using the video context only: " + userPrompt);

        JSONObject currentUserFileUriPart = new JSONObject();
        JSONObject currentUserFileData = new JSONObject();
        currentUserFileData.put("mime_type", "video/youtube"); // Google API needs mime_type for file_uri
        currentUserFileData.put("file_uri", currentVideoLink);
        currentUserFileUriPart.put("file_data", currentUserFileData);

        JSONArray currentUserPartsArray = new JSONArray();
        currentUserPartsArray.put(currentUserTextPart);
        currentUserPartsArray.put(currentUserFileUriPart);

        JSONObject currentUserContentEntry = new JSONObject();
        currentUserContentEntry.put("role", "user");
        currentUserContentEntry.put("parts", currentUserPartsArray);
        return currentUserContentEntry;
    }
}