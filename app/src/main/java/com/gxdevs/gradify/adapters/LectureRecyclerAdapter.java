package com.gxdevs.gradify.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.gxdevs.gradify.R;
import com.gxdevs.gradify.Utils.Utils;
import com.gxdevs.gradify.models.VideoItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            // Add week header
            items.add(week);

            // If expanded, add all videos
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
            WeekHeaderViewHolder weekHolder = (WeekHeaderViewHolder) holder;
            String week = (String) item;
            weekHolder.bind(week, expandedState.get(week));
        } else if (holder instanceof VideoItemViewHolder) {
            VideoItemViewHolder videoHolder = (VideoItemViewHolder) holder;
            VideoItem videoItem = (VideoItem) item;

            // Find which week and index this video belongs to
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

            boolean isCurrentPlaying = videoWeek != null &&
                    videoWeek.equals(currentWeek) &&
                    videoIndex == currentVideoIndex;

            videoHolder.bind(videoItem, isCurrentPlaying);
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
        notifyDataSetChanged();
    }

    public void setOnVideoClickListener(OnVideoClickListener listener) {
        this.videoClickListener = listener;
    }

    public interface OnVideoClickListener {
        void onVideoClick(String week, int videoIndex);
    }

    // View Holders
    class WeekHeaderViewHolder extends RecyclerView.ViewHolder {
        private final TextView weekTitle;
        private final TextView lecturesNum;
        private final ImageView expandIcon;

        WeekHeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            lecturesNum = itemView.findViewById(R.id.lecturesNum);
            weekTitle = itemView.findViewById(R.id.textView_week_title);
            expandIcon = itemView.findViewById(R.id.imageView_expand);

            itemView.setOnClickListener(v -> {
                String week = (String) items.get(getAdapterPosition());
                expandedState.compute(week, (k, isExpanded) -> !isExpanded);
                buildItemsList();
            });
        }

        void bind(String week, boolean isExpanded) {
            List<VideoItem> videos = weeksMap.get(week);
            if (videos != null) {
                lecturesNum.setText("Total Lectures: " + videos.size());
            }
            weekTitle.setText(week);
            expandIcon.setRotation(isExpanded ? 180 : 0);
        }
    }

    class VideoItemViewHolder extends RecyclerView.ViewHolder {
        private final TextView videoTitle;
        private final View currentIndicator;
        private final ConstraintLayout videoHolder;

        VideoItemViewHolder(@NonNull View itemView) {
            super(itemView);
            videoTitle = itemView.findViewById(R.id.textView_video_title);
            currentIndicator = itemView.findViewById(R.id.view_current);
            videoHolder = itemView.findViewById(R.id.videoHolder);

            itemView.setOnClickListener(v -> {
                if (videoClickListener != null) {
                    int position = getAdapterPosition();
                    VideoItem videoItem = (VideoItem) items.get(position);

                    // Find which week this video belongs to
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

                    if (week != null) {
                        videoClickListener.onVideoClick(week, videoIndex);
                    }
                }
            });
        }

        void bind(VideoItem videoItem, boolean isCurrentPlaying) {
            videoTitle.setText(videoItem.getTitle());
            currentIndicator.setVisibility(isCurrentPlaying ? View.VISIBLE : View.INVISIBLE);
            videoHolder.setBackground(Utils.setCardColor(context, 10));
        }
    }
} 