package com.gxdevs.gradify.adapters;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.gxdevs.gradify.R;
import com.gxdevs.gradify.models.VideoItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.bumptech.glide.Glide;

public class LectureRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_WEEK_HEADER = 0;
    private static final int TYPE_VIDEO_ITEM = 1;

    private final Context context;
    private final List<String> weekNames;
    private final Map<String, List<VideoItem>> weeksMap;
    private final Map<String, Boolean> expandedState;
    private final List<Object> items;

    private OnVideoClickListener videoClickListener;
    private String currentWeek = null;
    private int currentVideoIndex = -1;

    public LectureRecyclerAdapter(Context context, List<String> weekNames, Map<String, List<VideoItem>> weeksMap) {
        this.context = context;
        this.weekNames = weekNames;
        this.weeksMap = weeksMap;
        this.expandedState = new HashMap<>();
        this.items = new ArrayList<>();

        // Initialize all weeks as collapsed
        for (String week : weekNames) {
            expandedState.put(week, false);
        }

        // Build the initial list
        buildItemsList();
    }

    private void buildItemsList() {
        items.clear();
        for (String week : weekNames) {
            items.add(week);
            if (expandedState.get(week)) {
                items.addAll(weeksMap.get(week));
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        if (viewType == TYPE_WEEK_HEADER) {
            View view = inflater.inflate(R.layout.item_week_header, parent, false);
            return new WeekHeaderViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_video, parent, false);
            return new VideoItemViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = items.get(position);
        if (holder instanceof WeekHeaderViewHolder) {
            ((WeekHeaderViewHolder) holder).bind((String) item, expandedState.get((String) item));
        } else if (holder instanceof VideoItemViewHolder) {
            VideoItem videoItem = (VideoItem) item;
            String videoWeek = null;
            int videoIndex = -1;
            for (String weekName : weekNames) {
                List<VideoItem> videos = weeksMap.get(weekName);
                int index = videos.indexOf(videoItem);
                if (index != -1) {
                    videoWeek = weekName;
                    videoIndex = index;
                    break;
                }
            }
            boolean isCurrentPlaying = videoWeek != null && videoWeek.equals(currentWeek)
                    && videoIndex == currentVideoIndex;
            ((VideoItemViewHolder) holder).bind(videoItem, isCurrentPlaying);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof String ? TYPE_WEEK_HEADER : TYPE_VIDEO_ITEM;
    }

    public void setCurrentPlayingVideo(String week, int videoIndex) {
        this.currentWeek = week;
        this.currentVideoIndex = videoIndex;

        // Collapse all weeks and expand only the current week
        for (String w : weekNames) {
            expandedState.put(w, w.equals(week));
        }
        buildItemsList();
    }

    public void setOnVideoClickListener(OnVideoClickListener listener) {
        this.videoClickListener = listener;
    }

    public interface OnVideoClickListener {
        void onVideoClick(String week, int videoIndex);
    }

    class WeekHeaderViewHolder extends RecyclerView.ViewHolder {
        private final TextView weekLabel;
        private final TextView weekTitle;
        private final ImageView expandIcon;

        WeekHeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            weekLabel = itemView.findViewById(R.id.textView_week_label);
            weekTitle = itemView.findViewById(R.id.textView_week_title);
            expandIcon = itemView.findViewById(R.id.imageView_expand);

            itemView.setOnClickListener(v -> {
                String week = (String) items.get(getAdapterPosition());
                expandedState.compute(week, (k, isExpanded) -> !isExpanded);
                buildItemsList();
            });
        }

        void bind(String week, boolean isExpanded) {
            weekLabel.setText(week.toUpperCase());
            weekTitle.setText(week); // Or part of week info if available
            expandIcon.setRotation(isExpanded ? 180 : 0);
        }
    }

    class VideoItemViewHolder extends RecyclerView.ViewHolder {
        private final TextView videoTitle;
        private final ImageView thumbnail;
        private final ImageView playingIndicator;

        VideoItemViewHolder(@NonNull View itemView) {
            super(itemView);
            videoTitle = itemView.findViewById(R.id.textView_video_title);
            thumbnail = itemView.findViewById(R.id.imageView_video_thumbnail);
            playingIndicator = itemView.findViewById(R.id.playing);

            itemView.setOnClickListener(v -> {
                if (videoClickListener != null) {
                    VideoItem videoItem = (VideoItem) items.get(getAdapterPosition());
                    String week = null;
                    int videoIndex = -1;
                    for (String weekName : weekNames) {
                        List<VideoItem> videos = weeksMap.get(weekName);
                        videoIndex = videos.indexOf(videoItem);
                        if (videoIndex != -1) {
                            week = weekName;
                            break;
                        }
                    }
                    if (week != null)
                        videoClickListener.onVideoClick(week, videoIndex);
                }
            });
        }

        void bind(VideoItem videoItem, boolean isCurrentPlaying) {
            videoTitle.setText(videoItem.getTitle());

            // Handle Playing State
            if (isCurrentPlaying) {
                videoTitle.setTextColor(ContextCompat.getColor(context, R.color.primaryColor));
                thumbnail.setAlpha(0.6f); // Dim thumbnail slightly
                playingIndicator.setVisibility(VISIBLE);
            } else {
                playingIndicator.setVisibility(GONE);
                videoTitle.setTextColor(ContextCompat.getColor(context, R.color.textIcons));
                thumbnail.setAlpha(1.0f);
            }

            String videoId = extractYoutubeId(videoItem.getLink());
            String thumbUrl = "https://img.youtube.com/vi/" + videoId + "/0.jpg";
            Glide.with(context).load(thumbUrl).into(thumbnail);
        }

        private String extractYoutubeId(String url) {
            try {
                Uri uri = Uri.parse(url);
                String vParam = uri.getQueryParameter("v");
                if (vParam != null)
                    return vParam;
                if ("youtu.be".equals(uri.getHost()))
                    return uri.getLastPathSegment();
                Matcher m = Pattern
                        .compile("(?:youtu\\.be/|v=|/embed/|watch\\?v=|&v=)([^#&?]+)").matcher(url);
                if (m.find())
                    return m.group(1);
            } catch (Exception ignored) {
            }
            return "";
        }
    }
}
