package com.gxdevs.gradify.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gxdevs.gradify.R;
import com.gxdevs.gradify.Utils.Utils;

import java.util.List;

public class SubjectsAdapter extends RecyclerView.Adapter<SubjectsAdapter.SubjectViewHolder> {

    private final List<String> subjects;
    private final OnSubjectClickListener listener;

    public SubjectsAdapter(List<String> subjects, OnSubjectClickListener listener) {
        this.subjects = subjects;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SubjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_subject, parent, false);
        return new SubjectViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SubjectViewHolder holder, int position) {
        String subject = subjects.get(position);
        String subType = Utils.getSubjectType(subject);
        int lecture = Utils.getLectureCount(subject);

        switch (subType) {
            case "Maths":
                holder.patterHolder.setImageResource(R.drawable.math_gre);
                break;
            case "CS":
                holder.patterHolder.setImageResource(R.drawable.cs_gre);
                break;
            case "AI":
                holder.patterHolder.setImageResource(R.drawable.ai_gre);
                break;
            case "DB":
                holder.patterHolder.setImageResource(R.drawable.db_gre);
                break;
            case "App":
                holder.patterHolder.setImageResource(R.drawable.dev_gre);
                break;
            case "Lang":
                holder.patterHolder.setImageResource(R.drawable.lit_gre);
                break;
            default:
                holder.patterHolder.setImageResource(R.drawable.res_gre);
                break;
        }

        holder.subjectName.setText(subject);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSubjectClick(subject);
            }
        });
        holder.totalLec.setText(lecture + " Lectures");
    }

    @Override
    public int getItemCount() {
        return subjects.size();
    }

    public static class SubjectViewHolder extends RecyclerView.ViewHolder {
        TextView subjectName, totalLec;
        ImageView patterHolder;

        public SubjectViewHolder(@NonNull View itemView) {
            super(itemView);
            subjectName = itemView.findViewById(R.id.subject_name);
            totalLec = itemView.findViewById(R.id.totalLec);
            patterHolder = itemView.findViewById(R.id.patterHolder);
        }
    }

    public interface OnSubjectClickListener {
        void onSubjectClick(String subjectName);
    }
}











