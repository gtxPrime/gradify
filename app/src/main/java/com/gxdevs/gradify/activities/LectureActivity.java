package com.gxdevs.gradify.activities;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.gxdevs.gradify.activities.SettingsActivity.PRIMARY_COLOR_KEY;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewStub;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gxdevs.gradify.R;
import com.gxdevs.gradify.Utils.Utils;
import com.gxdevs.gradify.adapters.ChatAdapter;
import com.gxdevs.gradify.adapters.LectureRecyclerAdapter;
import com.gxdevs.gradify.db.AppDatabase;
import com.gxdevs.gradify.db.TimeTrackingDbHelper;
import com.gxdevs.gradify.db.dao.ChatDao;
import com.gxdevs.gradify.db.entities.ChatMessageEntity;
import com.gxdevs.gradify.models.ChatMessage;
import com.gxdevs.gradify.models.LectureData;
import com.gxdevs.gradify.models.VideoItem;
import com.gxdevs.gradify.services.GeminiChatService;
import com.google.ai.client.generativeai.type.Content;
import com.google.android.material.chip.Chip;
import com.google.android.material.textview.MaterialTextView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.FullscreenListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eightbitlab.com.blurview.BlurView;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class LectureActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "LecturePrefs";
    private static final String KEY_WEEK = "weekIndex";
    private static final String KEY_VIDEO = "videoIndex";
    private static final OkHttpClient httpClient = new OkHttpClient();

    private String ytLink = "Error!";
    private RecyclerView recyclerView;
    private FrameLayout fullscreenContainer;
    private MaterialTextView videoTitleTextView;
    private LectureRecyclerAdapter adapter;
    private YouTubePlayerView youTubePlayerView;
    private YouTubePlayer youTubePlayer;
    private LectureData lectureData;
    private ConstraintLayout contentAreaBelowPlayer; // The new group
    private ConstraintLayout chatLayout; // This is the root of chat_layout.xml, inflated by ViewStub
    private RecyclerView chatMessagesRecyclerView;
    private EditText editTextChatMessage;

    private List<ChatMessage> chatMessageList;
    private ChatAdapter chatAdapter;
    private GeminiChatService geminiChatService;
    private Handler animasiHandler;
    private Runnable animasiRunnable;
    private int currentAnimasiStep = 0;
    private final String[] animasiMessages = {
            "Waking up the AI.",
            "Waking up the AI..",
            "Waking up the AI...",
            "Watching the video.",
            "Watching the video..",
            "Watching the video...",
            "Analyzing deeply.",
            "Analyzing deeply..",
            "Analyzing deeply...",
            "Almost there.",
            "Almost there..",
            "Almost there..."
    };

    private int currentWeekIndex;
    private int currentVideoIndex;
    private float resumePosition; // Needs to be updated during playback
    private String subjectName;
    private long lectureStartTimeMillis;
    private boolean isAiResponding = false; // Flag to track AI response state

    private SharedPreferences lecturePrefs;
    private SharedPreferences settingsPrefs; // For app-wide settings
    private TimeTrackingDbHelper dbHelper;
    private ChatDao chatDao;
    private ImageView lectDecor, lectDecor1, lectDecor2, lectDecor3;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lecture);

        recyclerView = findViewById(R.id.recyclerView_lectures);
        fullscreenContainer = findViewById(R.id.fullscreen_container);
        videoTitleTextView = findViewById(R.id.textView_video_title);
        youTubePlayerView = findViewById(R.id.youtube_player_view);
        CardView reviewCard = findViewById(R.id.reviewCard);
        CardView aiCard = findViewById(R.id.aiCard);
        contentAreaBelowPlayer = findViewById(R.id.content_area_below_player);
        lectDecor = findViewById(R.id.lectDecor);
        lectDecor1 = findViewById(R.id.lectDecor1);
        lectDecor2 = findViewById(R.id.lectDecor2);
        lectDecor3 = findViewById(R.id.lectDecor3);

        SharedPreferences preferences = getSharedPreferences(SettingsActivity.PREFS_NAME, MODE_PRIVATE);
        int startColor = preferences.getInt(PRIMARY_COLOR_KEY, ContextCompat.getColor(this, R.color.primaryColor));

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Persisted state
        lecturePrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        settingsPrefs = getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE);
        currentWeekIndex = lecturePrefs.getInt(KEY_WEEK, 0);
        currentVideoIndex = lecturePrefs.getInt(KEY_VIDEO, 0);

        subjectName = getIntent().getStringExtra("subject_name");
        if (subjectName == null || subjectName.isEmpty()) {
            subjectName = "Unknown Subject";
            Log.w("TimeTracker", "Subject not passed via intent for LectureActivity. Using default.");
        }

        String jsonUrl = getIntent().getStringExtra("json_url");
        if (jsonUrl == null || jsonUrl.isEmpty()) {
            jsonUrl = "https://cdn.jsdelivr.net/gh/gtxPrime/gradify@main/data/lectures/Foundation/stats1.json";
        }

        dbHelper = new TimeTrackingDbHelper(this);
        AppDatabase chatDb = AppDatabase.getDatabase(this);
        chatDao = chatDb.chatDao();
        fetchLectureData(jsonUrl);

        reviewCard.setCardBackgroundColor(startColor);
        aiCard.setCardBackgroundColor(startColor);

        reviewCard.setOnClickListener(v -> Toast.makeText(this, "Coming Soon", Toast.LENGTH_SHORT).show());
        aiCard.setOnClickListener(v -> toggleChatLayout(true));

        // Initialize Gemini Chat Service
        geminiChatService = new GeminiChatService(ContextCompat.getMainExecutor(this));
        animasiHandler = new Handler(Looper.getMainLooper());

        Utils.setTheme(this, lectDecor, lectDecor1, lectDecor2, lectDecor3);
    }

    private void initializeChatViews() {
        if (chatLayout == null)
            return; // Should not happen if called after inflation
        Drawable windowBackground = getWindow().getDecorView().getBackground();

        BlurView blurView = chatLayout.findViewById(R.id.chat_toolbar_blur_view);

        blurView.setupWith(chatLayout.findViewById(R.id.blurHolder))
                .setFrameClearDrawable(windowBackground)// Optional: if you skip setupWith, you can set it manually.
                .setBlurRadius(10f) // Set your desired blur radius
                .setBlurAutoUpdate(true);

        ImageButton buttonCloseChat = chatLayout.findViewById(R.id.button_close_chat);
        chatMessagesRecyclerView = chatLayout.findViewById(R.id.recyclerView_chat_messages);
        editTextChatMessage = chatLayout.findViewById(R.id.editText_chat_message);
        Button buttonSendChatMessage = chatLayout.findViewById(R.id.button_send_chat_message);
        Chip chipSummarize = chatLayout.findViewById(R.id.chip_summarize);
        Chip chipNotes = chatLayout.findViewById(R.id.chip_notes);
        Chip chipExplain = chatLayout.findViewById(R.id.chip_explain);

        if (buttonCloseChat != null) {
            buttonCloseChat.setOnClickListener(v -> toggleChatLayout(false));
        }

        if (chatMessagesRecyclerView != null) {
            chatMessagesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            chatMessageList = new ArrayList<>();
            chatAdapter = new ChatAdapter(chatMessageList, this);
            chatMessagesRecyclerView.setAdapter(chatAdapter);
        }

        if (chipSummarize != null)
            chipSummarize.setOnClickListener(v -> sendSuggestionToAI("Summarise this video"));
        if (chipNotes != null)
            chipNotes.setOnClickListener(v -> sendSuggestionToAI("Generate detailed notes"));
        if (chipExplain != null)
            chipExplain.setOnClickListener(v -> sendSuggestionToAI("Explain this concept"));

        if (buttonSendChatMessage != null) {
            buttonSendChatMessage.setOnClickListener(v -> {
                String message = editTextChatMessage.getText().toString().trim();
                if (!message.isEmpty()) {
                    addUserChatMessage(new ChatMessage(message, true, System.currentTimeMillis()));
                    editTextChatMessage.setText("");
                    callGeminiApi(message);
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        lectureStartTimeMillis = System.currentTimeMillis();
        Log.d("TimeTracker", "LectureActivity resumed for subject: " + subjectName + " at " + lectureStartTimeMillis);
    }

    @Override
    public void onBackPressed() {
        if (fullscreenContainer.getVisibility() == VISIBLE) {
            exitFullScreen();
        } else if (chatLayout != null && chatLayout.getVisibility() == VISIBLE) {
            toggleChatLayout(false);
        } else {
            super.onBackPressed();
        }
    }

    private void fetchLectureData(String url) {
        fetchLectureDataWithRetry(url, 0);
    }

    private void fetchLectureDataWithRetry(String url, int retryCount) {
        Request request = new Request.Builder().url(url).build();
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (retryCount < 2) { // Retry up to 2 times (total 3 attempts)
                    Log.w("LectureActivty", "Fetch failed, retrying... (" + (retryCount + 1) + ")");
                    fetchLectureDataWithRetry(url, retryCount + 1);
                } else {
                    runOnUiThread(() -> Toast.makeText(LectureActivity.this,
                            "Failed to load data. Please check your internet.", Toast.LENGTH_LONG).show());
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(LectureActivity.this,
                            "Server Error: " + response.code(), Toast.LENGTH_LONG).show());
                    return;
                }
                if (response.body() != null) {
                    try (InputStreamReader reader = new InputStreamReader(response.body().byteStream())) {
                        Type type = new TypeToken<LectureData>() {
                        }.getType();
                        lectureData = new Gson().fromJson(reader, type);
                        runOnUiThread(LectureActivity.this::setupUI);
                    } catch (Exception e) {
                        runOnUiThread(() -> Toast.makeText(LectureActivity.this,
                                "Data parsing error.", Toast.LENGTH_LONG).show());
                    }
                }
            }
        });
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private void exitFullScreen() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        fullscreenContainer.removeAllViews();
        fullscreenContainer.setVisibility(GONE);

        // Make player and its controls visible
        youTubePlayerView.setVisibility(VISIBLE);

        // Restore visibility of the content area or chat layout based on current state
        if (chatLayout != null && chatLayout.getVisibility() == VISIBLE) {
            contentAreaBelowPlayer.setVisibility(GONE);
            // chatLayout remains visible
        } else {
            contentAreaBelowPlayer.setVisibility(VISIBLE);
            if (chatLayout != null)
                chatLayout.setVisibility(GONE);
        }
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    public void enterFullScreen(View view) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        // Move player to fullscreen container
        youTubePlayerView.setVisibility(GONE); // Hide original player view
        fullscreenContainer.addView(view); // Add the player's fullscreen view
        fullscreenContainer.setVisibility(VISIBLE);

        // Hide other UI elements that were part of the main screen
        contentAreaBelowPlayer.setVisibility(GONE);
        if (chatLayout != null) {
            chatLayout.setVisibility(GONE);
        }
    }

    private void setupUI() {
        List<String> weekNames = new ArrayList<>(lectureData.getWeeks().keySet());
        Map<String, List<VideoItem>> weeksMap = lectureData.getWeeks();
        adapter = new LectureRecyclerAdapter(this, weekNames, weeksMap);
        recyclerView.setAdapter(adapter);

        adapter.setOnVideoClickListener((weekName, videoIndex) -> {
            currentWeekIndex = weekNames.indexOf(weekName);
            currentVideoIndex = videoIndex;
            if (youTubePlayer != null)
                playVideo(currentWeekIndex, currentVideoIndex, 0f);
        });

        youTubePlayerView.addFullscreenListener(new FullscreenListener() {
            @Override
            public void onEnterFullscreen(@NonNull View view, @NonNull Function0<Unit> function0) {
                enterFullScreen(view);
            }

            @Override
            public void onExitFullscreen() {
                exitFullScreen();
            }
        });

        IFramePlayerOptions options = new IFramePlayerOptions.Builder(this)
                .controls(1)
                .fullscreen(1)
                .build();

        getLifecycle().addObserver(youTubePlayerView);

        youTubePlayerView.initialize(new AbstractYouTubePlayerListener() {
            @Override
            public void onReady(@NonNull YouTubePlayer player) {
                youTubePlayer = player;
                playVideo(currentWeekIndex, currentVideoIndex, resumePosition);
            }

            @Override
            public void onStateChange(@NonNull YouTubePlayer player,
                    @NonNull PlayerConstants.PlayerState state) {
                if (state == PlayerConstants.PlayerState.ENDED)
                    playNextVideo();
            }

            @Override
            public void onCurrentSecond(@NonNull YouTubePlayer youTubePlayer, float second) {
                resumePosition = second; // Update resumePosition here
            }

            @Override
            public void onError(@NonNull YouTubePlayer youTubePlayer, @NonNull PlayerConstants.PlayerError error) {
                super.onError(youTubePlayer, error);
                Log.e("YouTubePlayer", "Error loading video: " + error.name());

                if (ytLink != null && !ytLink.isEmpty() && !ytLink.equals("Error!")) {
                    Toast.makeText(LectureActivity.this, "Falling back to YouTube App...", Toast.LENGTH_SHORT).show();
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(ytLink));
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(LectureActivity.this, "Could not open YouTube App: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                        Log.e("YouTubePlayer", "Error fallback: " + e.getMessage());
                    }
                }
            }
        }, options);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void playVideo(int w, int v, float startSec) {
        List<String> weekNames = new ArrayList<>(lectureData.getWeeks().keySet());
        if (youTubePlayer == null || w >= weekNames.size())
            return;
        String key = weekNames.get(w);
        List<VideoItem> vids = lectureData.getWeeks().get(key);
        if (vids != null && v >= vids.size())
            return;
        assert vids != null;
        VideoItem item = vids.get(v);
        videoTitleTextView.setText(item.getTitle());
        adapter.setCurrentPlayingVideo(key, v);
        String id = extractYoutubeId(item.getLink());
        youTubePlayer.loadVideo(id, startSec);
        saveState();
        // Clear chat for new video and load history
        if (chatMessageList != null) {
            chatMessageList.clear();
            if (chatAdapter != null)
                chatAdapter.notifyDataSetChanged();
            if (chatLayout != null && chatLayout.getVisibility() == VISIBLE) { // Only load if chat is visible
                loadChatHistory(ytLink);
            }
        }
    }

    private void playNextVideo() {
        List<String> weekNames = new ArrayList<>(lectureData.getWeeks().keySet());
        List<VideoItem> vids = lectureData.getWeeks().get(weekNames.get(currentWeekIndex));
        if (vids != null && currentVideoIndex + 1 < vids.size())
            currentVideoIndex++;
        playVideo(currentWeekIndex, currentVideoIndex, 0f);
    }

    private void saveState() {
        lecturePrefs.edit()
                .putInt(KEY_WEEK, currentWeekIndex)
                .putInt(KEY_VIDEO, currentVideoIndex)
                .putFloat("playbackPos", resumePosition) // Save updated resumePosition
                .apply();
    }

    private String extractYoutubeId(String url) {
        ytLink = url; // Update ytLink here as it's the full link
        try {
            Uri uri = Uri.parse(url);
            String vParam = uri.getQueryParameter("v");
            if (vParam != null)
                return vParam;
            if ("youtu.be".equals(uri.getHost()))
                return uri.getLastPathSegment();
            List<String> segments = uri.getPathSegments();
            int embedIndex = segments.indexOf("embed");
            if (embedIndex != -1 && embedIndex + 1 < segments.size()) {
                return segments.get(embedIndex + 1);
            }
            Matcher m = Pattern.compile("(?:youtu\\.be/|v=|/embed/|watch\\?v=|&v=)([^#&?]+)").matcher(url);
            if (m.find())
                return m.group(1);
        } catch (Exception ignored) {
        }
        Log.w(TAG, "Could not extract YouTube ID from URL: " + url
                + ". Using full URL as fallback for chat key, but video might not play.");
        return url; // Fallback, though player might not like this
    }

    private void toggleChatLayout(boolean show) {
        if (contentAreaBelowPlayer == null) {
            Log.e(TAG, "contentAreaBelowPlayer is null. Cannot toggle chat layout.");
            return;
        }

        if (show) {
            if (chatLayout == null) {
                ViewStub chatViewStub = findViewById(R.id.chat_layout_stub);
                if (chatViewStub != null) {
                    chatLayout = (ConstraintLayout) chatViewStub.inflate();
                    initializeChatViews(); // Initializes chatMessageList and adapter
                    // loadChatHistory(ytLink); // Moved to after visibility check
                } else {
                    Toast.makeText(this, "Chat layout stub not found.", Toast.LENGTH_SHORT).show();
                    return;
                }
            } else if (chatLayout.getVisibility() == VISIBLE) {
                return; // Already visible
            }

            Animation slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in_bottom);
            chatLayout.startAnimation(slideIn);
            chatLayout.setVisibility(VISIBLE);
            contentAreaBelowPlayer.setVisibility(GONE);

            loadChatHistory(ytLink); // Load history when layout becomes visible

            if (isAiResponding) {
                // If AI is responding and chat was closed, ensure animation message is present
                // and animation continues
                if (chatMessageList != null && !chatMessageList.isEmpty()) {
                    ChatMessage lastMsg = chatMessageList.get(chatMessageList.size() - 1);
                    if (!lastMsg.isUserMessage() && (lastMsg.getMessage().startsWith("Waking up")
                            || lastMsg.getMessage().startsWith("Watching")
                            || lastMsg.getMessage().startsWith("Analyzing")
                            || lastMsg.getMessage().startsWith("Almost there"))) {
                        // It's an animation message, ensure adapter knows
                        if (chatAdapter != null)
                            chatAdapter.notifyItemChanged(chatMessageList.size() - 1);
                    } else if (!lastMsg.isUserMessage()) {
                        // It's some other AI message, but we are responding, so put animation message.
                        // This case should ideally not happen if logic is correct elsewhere.
                        // For safety, we could re-add an animation message.
                        // However, for now, let's assume the animation message is correctly managed in
                        // callGeminiApi.
                    }
                } else if (chatMessageList != null) {
                    // AI is responding but list is empty, add initial animation message.
                    addAiChatMessage(new ChatMessage(animasiMessages[currentAnimasiStep % animasiMessages.length],
                            false, System.currentTimeMillis()), false);
                }
                startLoadingAnimation(); // Ensure animation handler is running
            }

            if (chatMessageList != null && chatMessageList.isEmpty() && !isAiResponding) {
                findViewById(R.id.welcomeAITxt).setVisibility(VISIBLE);
            } else {
                findViewById(R.id.welcomeAITxt).setVisibility(GONE);
            }

        } else { // hide
            if (chatLayout == null || chatLayout.getVisibility() == GONE) {
                return; // Already hidden
            }
            Animation slideOut = AnimationUtils.loadAnimation(this, R.anim.slide_out_bottom);
            slideOut.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    chatLayout.setVisibility(GONE);
                    contentAreaBelowPlayer.setVisibility(VISIBLE);
                    // Don't stop animation here, let it run in background if isAiResponding
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
            chatLayout.startAnimation(slideOut);
        }
    }

    private void sendSuggestionToAI(String suggestion) {
        if (editTextChatMessage == null)
            return; // Guard against null views if layout not ready
        editTextChatMessage.setText(suggestion); // Set text for user to see, then send
        addUserChatMessage(new ChatMessage(suggestion, true, System.currentTimeMillis()));
        callGeminiApi(suggestion);
        editTextChatMessage.setText(""); // Clear after sending
    }

    private void callGeminiApi(String userPrompt) {
        if (ytLink == null || ytLink.equals("Error!") || ytLink.isEmpty()) {
            addAiChatMessage(new ChatMessage("Error: Current video link is not available to send to AI.", false,
                    System.currentTimeMillis()), false);
            return;
        }
        isAiResponding = true;
        // Prepare chat history based on settings
        List<Content> apiHistory = new ArrayList<>();
        boolean sendWholeHistory = settingsPrefs.getBoolean(SettingsActivity.SEND_WHOLE_HISTORY, false);
        int messageHistoryCount = settingsPrefs.getInt(SettingsActivity.MESSAGE_HISTORY_COUNT, 3);

        if (chatMessageList != null) {
            if (sendWholeHistory) {
                for (ChatMessage chatMsg : chatMessageList) {
                    if (chatMsg.getMessage().equals("AI is thinking..."))
                        continue;
                    List<com.google.ai.client.generativeai.type.Part> parts = new ArrayList<>();
                    parts.add(new com.google.ai.client.generativeai.type.TextPart(chatMsg.getMessage()));
                    apiHistory.add(new Content(chatMsg.isUserMessage() ? "user" : "model", parts));
                }
            } else {
                int userMessagesCount = 0;
                int modelMessagesCount = 0;
                // Iterate from the end to get the most recent messages
                for (int i = chatMessageList.size() - 1; i >= 0; i--) {
                    ChatMessage chatMsg = chatMessageList.get(i);
                    if (chatMsg.getMessage().equals("AI is thinking..."))
                        continue;

                    boolean added = false;
                    if (chatMsg.isUserMessage()) {
                        if (userMessagesCount < messageHistoryCount) {
                            List<com.google.ai.client.generativeai.type.Part> parts = new ArrayList<>();
                            parts.add(new com.google.ai.client.generativeai.type.TextPart(chatMsg.getMessage()));
                            apiHistory.add(0, new Content("user", parts)); // Add to front to maintain order
                            userMessagesCount++;
                            added = true;
                        }
                    } else { // Model message
                        if (modelMessagesCount < messageHistoryCount) {
                            List<com.google.ai.client.generativeai.type.Part> parts = new ArrayList<>();
                            parts.add(new com.google.ai.client.generativeai.type.TextPart(chatMsg.getMessage()));
                            apiHistory.add(0, new Content("model", parts)); // Add to front
                            modelMessagesCount++;
                            added = true;
                        }
                    }
                    if (userMessagesCount >= messageHistoryCount && modelMessagesCount >= messageHistoryCount
                            && !added) {
                        break; // Stop if we have enough of both and current message wasn't added (it means
                               // it's too old)
                    }
                }
            }
        }

        // Current userPrompt is handled by the service's
        // buildCurrentUserContentForGoogleApi method.

        // Start loading animation
        currentAnimasiStep = 0;
        // Add initial animation message, don't save to DB
        addAiChatMessage(new ChatMessage(animasiMessages[0], false, System.currentTimeMillis()), false);
        startLoadingAnimation();

        String apiKey = settingsPrefs.getString(SettingsActivity.API_KEY, "");
        if (apiKey.isEmpty()) {
            if (findViewById(R.id.openApiSettings).getVisibility() == GONE) {
                findViewById(R.id.openApiSettings).setVisibility(VISIBLE);
            }
            // Don't save API key error to DB
            addAiChatMessage(new ChatMessage("API Key not found. Please add your API key in settings.", false,
                    System.currentTimeMillis()), false);
            stopLoadingAnimation();
            removeLastNonUserMessage(); // Remove the animation message
            isAiResponding = false;
            findViewById(R.id.openApiSettings)
                    .setOnClickListener(v -> startActivity(new Intent(LectureActivity.this, SettingsActivity.class)));
            return;
        } else if (findViewById(R.id.openApiSettings).getVisibility() == VISIBLE) {
            findViewById(R.id.openApiSettings).setVisibility(GONE);
        }

        geminiChatService.generateChatResponse(apiKey, apiHistory, ytLink, userPrompt,
                new GeminiChatService.GeminiChatCallback() {
                    @Override
                    public void onSuccess(String responseText) {
                        stopLoadingAnimation();
                        isAiResponding = false;
                        runOnUiThread(() -> {
                            removeLastNonUserMessage(); // Removes the last animation message
                            ChatMessage aiMessage = new ChatMessage(responseText, false, System.currentTimeMillis());
                            addAiChatMessage(aiMessage, true); // Save successful AI response to DB
                        });
                    }

                    @Override
                    public void onError(String errorMessage) {
                        stopLoadingAnimation();
                        isAiResponding = false;
                        runOnUiThread(() -> {
                            removeLastNonUserMessage(); // Removes the last animation message
                            // Don't save error message to DB
                            addAiChatMessage(
                                    new ChatMessage("AI Error: " + errorMessage, false, System.currentTimeMillis()),
                                    false);
                            Toast.makeText(LectureActivity.this, "Gemini API Error: " + errorMessage, Toast.LENGTH_LONG)
                                    .show();
                        });
                    }
                });
    }

    private void startLoadingAnimation() {
        if (!isAiResponding)
            return; // Don't start if not actually responding

        if (animasiHandler == null)
            animasiHandler = new Handler(Looper.getMainLooper());
        animasiHandler.removeCallbacks(animasiRunnable); // Remove existing to prevent multiple

        animasiRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isAiResponding) { // Stop if AI is no longer responding
                    stopLoadingAnimation();
                    return;
                }
                currentAnimasiStep = (currentAnimasiStep + 1) % animasiMessages.length;
                updateLastNonUserMessageText(animasiMessages[currentAnimasiStep]);
                if (isAiResponding) { // Reschedule only if still responding
                    animasiHandler.postDelayed(this, 1500);
                }
            }
        };
        if (isAiResponding) { // Post only if still responding
            animasiHandler.postDelayed(animasiRunnable, 1500);
        }
    }

    private void stopLoadingAnimation() {
        if (animasiHandler != null && animasiRunnable != null) {
            animasiHandler.removeCallbacks(animasiRunnable);
        }
        // Don't set isAiResponding = false here, as it's set by the API callbacks
    }

    private void updateLastNonUserMessageText(String newMessage) {
        if (chatMessageList != null && !chatMessageList.isEmpty()) {
            int lastIndex = chatMessageList.size() - 1;
            ChatMessage lastMessage = chatMessageList.get(lastIndex);
            if (!lastMessage.isUserMessage()) {
                // Check if it's one of our known animation prefixes
                boolean isOurAnimationMessage = false;
                for (String animMsgPrefix : new String[] { "Waking up", "Watching", "Analyzing", "Almost there" }) {
                    if (lastMessage.getMessage().startsWith(animMsgPrefix)) {
                        isOurAnimationMessage = true;
                        break;
                    }
                }
                if (isOurAnimationMessage) {
                    chatMessageList.set(lastIndex, new ChatMessage(newMessage, false, lastMessage.getTimestamp()));
                    if (chatAdapter != null && chatLayout != null && chatLayout.getVisibility() == VISIBLE) {
                        chatAdapter.notifyItemChanged(lastIndex);
                    }
                }
            }
        }
    }

    private void removeLastNonUserMessage() {
        if (chatMessageList != null && !chatMessageList.isEmpty()) {
            int lastIndex = chatMessageList.size() - 1;
            ChatMessage lastMessage = chatMessageList.get(lastIndex);
            if (!lastMessage.isUserMessage()) {
                chatMessageList.remove(lastIndex);
                if (chatAdapter != null && chatLayout != null && chatLayout.getVisibility() == VISIBLE) {
                    // Use notifyItemRemoved for better animation if visible
                    chatAdapter.notifyItemRemoved(lastIndex);
                } else if (chatAdapter != null) {
                    // If not visible, a full notifyDataSetChanged might be safer or just no visual
                    // update needed yet
                    chatAdapter.notifyDataSetChanged();
                }
            }
        }
    }

    // Renamed from addChatMessage to make its purpose clear (user messages are
    // always saved)
    private void addUserChatMessage(ChatMessage message) {
        addChatMessageInternal(message, true);
    }

    // New method for AI messages, allows controlling DB save
    private void addAiChatMessage(ChatMessage message, boolean saveToDb) {
        addChatMessageInternal(message, saveToDb);
    }

    private void addChatMessageInternal(ChatMessage message, boolean saveToDb) {
        if (chatAdapter != null && chatMessagesRecyclerView != null && chatMessageList != null) {
            chatMessageList.add(message);
            if (chatLayout != null && chatLayout.getVisibility() == VISIBLE) {
                chatAdapter.notifyItemInserted(chatMessageList.size() - 1);
                chatMessagesRecyclerView.scrollToPosition(chatAdapter.getItemCount() - 1);
            } else {
                // If chat is not visible, adapter will pick it up when it becomes visible
                // or during loadChatHistory. A full refresh might be needed then.
            }

            // Hide welcome text as soon as a message is added
            if (findViewById(R.id.welcomeAITxt).getVisibility() == VISIBLE) {
                findViewById(R.id.welcomeAITxt).setVisibility(GONE);
            }
            if (saveToDb) { // Only save if specified
                saveChatMessageToDb(message);
            }
        }
    }

    private void saveChatMessageToDb(ChatMessage message) {
        if (ytLink == null || ytLink.equals("Error!") || ytLink.isEmpty()) {
            Log.w(TAG, "Cannot save chat message, video link (ID) is missing.");
            return;
        }
        ChatMessageEntity entity = new ChatMessageEntity(ytLink, message.getMessage(), message.isUserMessage(),
                message.getTimestamp());
        Executors.newSingleThreadExecutor().execute(() -> {
            chatDao.insertMessage(entity);
            Log.d(TAG, "Message saved to DB for video: " + ytLink);
        });
    }

    private void loadChatHistory(String videoId) {
        if (videoId == null || videoId.equals("Error!") || videoId.isEmpty()) {
            Log.w(TAG, "Cannot load chat history, video link (ID) is missing.");
            if (chatMessageList != null)
                chatMessageList.clear();
            if (chatAdapter != null)
                chatAdapter.notifyDataSetChanged();
            return;
        }
        Executors.newSingleThreadExecutor().execute(() -> {
            List<ChatMessageEntity> historyEntities = chatDao.getMessagesForVideo(videoId);
            List<ChatMessage> historyMessages = new ArrayList<>();
            for (ChatMessageEntity entity : historyEntities) {
                historyMessages.add(new ChatMessage(entity.messageText, entity.isUserMessage, entity.timestamp));
            }
            runOnUiThread(() -> {
                if (chatMessageList != null) {
                    // Preserve current non-DB messages (like ongoing AI animation)
                    List<ChatMessage> nonDbMessages = getChatMessages();

                    chatMessageList.clear();
                    chatMessageList.addAll(historyMessages);
                    chatMessageList.addAll(nonDbMessages); // Add back any preserved non-DB messages

                    if (chatAdapter != null) {
                        chatAdapter.notifyDataSetChanged(); // Full refresh after loading history
                        if (chatAdapter.getItemCount() > 0) {
                            chatMessagesRecyclerView.scrollToPosition(chatAdapter.getItemCount() - 1);
                            findViewById(R.id.welcomeAITxt).setVisibility(GONE);
                        } else if (!isAiResponding) { // Show welcome only if not loading and no history
                            findViewById(R.id.welcomeAITxt).setVisibility(VISIBLE);
                        }
                    }
                    Log.d(TAG, "Loaded " + historyMessages.size() + " messages from DB for video: " + videoId);
                } else {
                    Log.e(TAG, "chatMessageList is null, cannot display loaded history");
                }
            });
        });
    }

    @NonNull
    private List<ChatMessage> getChatMessages() {
        List<ChatMessage> nonDbMessages = new ArrayList<>();
        if (isAiResponding && !chatMessageList.isEmpty()) {
            ChatMessage lastMsg = chatMessageList.get(chatMessageList.size() - 1);
            // if the last message is an AI animation message, keep it.
            if (!lastMsg.isUserMessage() && (lastMsg.getMessage().startsWith("Waking up")
                    || lastMsg.getMessage().startsWith("Watching") || lastMsg.getMessage().startsWith("Analyzing")
                    || lastMsg.getMessage().startsWith("Almost there"))) {
                nonDbMessages.add(lastMsg);
            }
        }
        return nonDbMessages;
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveState(); // Save state including playback position

        if (lectureStartTimeMillis > 0) {
            long endTimeMillis = System.currentTimeMillis();
            long timeSpentMillis = endTimeMillis - lectureStartTimeMillis;
            if (timeSpentMillis > 1000) {
                String currentSubjectName = (subjectName != null && !subjectName.isEmpty()) ? subjectName
                        : "Unknown Subject";
                dbHelper.addTimeEntry(currentSubjectName, "lecture", new Date().getTime(), timeSpentMillis);
                Log.d("TimeTracker", "LectureActivity paused. Subject: " + currentSubjectName + ". Time spent: "
                        + timeSpentMillis / 1000 + "s. Saved to DB.");
            }
            lectureStartTimeMillis = 0;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Release the player when the activity is destroyed
        if (youTubePlayerView != null) {
            youTubePlayerView.release();
        }
    }

    private static final String TAG = "LectureActivity"; // Added TAG for logging
}
