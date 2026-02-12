package com.gxdevs.gradify.adapters;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.gxdevs.gradify.R;
import com.gxdevs.gradify.activities.SettingsActivity;
import com.gxdevs.gradify.models.SubjectStatsData;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.List;

public class SubjectStatsAdapter extends RecyclerView.Adapter<SubjectStatsAdapter.ViewHolder> {

    private final Context context;
    private final List<SubjectStatsData> subjectStatsList;
    private final OnItemClickListener onItemClickListener;

    // Interface for click events
    public interface OnItemClickListener {
        void onItemClick(SubjectStatsData subjectStatsData);
    }

    public SubjectStatsAdapter(Context context, List<SubjectStatsData> subjectStatsList, OnItemClickListener listener) {
        this.context = context;
        this.subjectStatsList = subjectStatsList;
        this.onItemClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_subject_stats, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SubjectStatsData currentItem = subjectStatsList.get(position);

        holder.textViewSubjectName.setText(currentItem.getSubjectName());

        holder.textViewPyqTime.setText(currentItem.getFormattedPyqTime());
        holder.progressBarPyq.setProgress(currentItem.getPyqProgress());

        holder.textViewLectureTime.setText(currentItem.getFormattedLectureTime());
        holder.progressBarLecture.setProgress(currentItem.getLectureProgress());

        int subjectColor = currentItem.getSubjectColor();
        if (subjectColor != Color.TRANSPARENT && subjectColor != Color.GRAY) {
            holder.progressBarPyq.setIndicatorColor(subjectColor);
            holder.progressBarLecture.setIndicatorColor(subjectColor);
        } else {
            int defaultProgressBarColor = Color.GRAY;
            holder.progressBarPyq.setIndicatorColor(defaultProgressBarColor);
            holder.progressBarLecture.setIndicatorColor(defaultProgressBarColor);
        }

        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(currentItem);
            }
        });
    }

    @Override
    public int getItemCount() {
        return subjectStatsList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textViewSubjectName;
        TextView textViewPyqTime;
        LinearProgressIndicator progressBarPyq;
        TextView textViewLectureTime;
        LinearProgressIndicator progressBarLecture;
        MaterialCardView subjectStatsHolder;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewSubjectName = itemView.findViewById(R.id.text_view_subject_name);
            textViewPyqTime = itemView.findViewById(R.id.text_view_pyq_time);
            progressBarPyq = itemView.findViewById(R.id.progress_bar_pyq);
            textViewLectureTime = itemView.findViewById(R.id.text_view_lecture_time);
            progressBarLecture = itemView.findViewById(R.id.progress_bar_lecture);
            subjectStatsHolder = itemView.findViewById(R.id.subjectStatsHolder);
        }
    }

    // Helper method to update data if needed later
    public void updateData(List<SubjectStatsData> newStatsList) {
        this.subjectStatsList.clear();
        this.subjectStatsList.addAll(newStatsList);
        notifyDataSetChanged();
    }
}