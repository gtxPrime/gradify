package com.gxdevs.gradify.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gxdevs.gradify.R;
import com.gxdevs.gradify.models.QuestionSummary;

import java.util.List;

public class QuestionSummaryAdapter extends RecyclerView.Adapter<QuestionSummaryAdapter.ViewHolder> {

    private List<QuestionSummary> questions;
    private Context context;
    private OnQuestionClickListener listener;

    public interface OnQuestionClickListener {
        void onQuestionClick(int position);
    }

    public QuestionSummaryAdapter(Context context, List<QuestionSummary> questions, OnQuestionClickListener listener) {
        this.context = context;
        this.questions = questions;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_question_summary, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        QuestionSummary question = questions.get(position);

        // Skip extra info items
        if (question.isExtraInfo()) {
            holder.itemView.setVisibility(View.GONE);
            holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(0, 0));
            return;
        }

        holder.itemView.setVisibility(View.VISIBLE);
        RecyclerView.LayoutParams layoutParams = new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        
        // Convert 8dp to pixels
        int marginInDp = 8;
        float density = context.getResources().getDisplayMetrics().density;
        int marginInPx = (int) (marginInDp * density);

        layoutParams.setMargins(0, marginInPx, 0, marginInPx);
        holder.itemView.setLayoutParams(layoutParams);

        holder.questionNumberTextView.setText("Q" + question.getDisplayPosition());
        holder.questionScoreTextView.setText(String.format("%.2f", question.getUserScore()) + "/" + question.getTotalMarks());

        if (question.isCorrect()) {
            holder.questionStatusTextView.setText("Correct");
            holder.questionStatusTextView.setTextColor(Color.parseColor("#4CAF50"));
        } else if (question.isPartiallyCorrect()) {
            holder.questionStatusTextView.setText("Partially Correct");
            holder.questionStatusTextView.setTextColor(Color.parseColor("#FFC107"));
        } else {
            holder.questionStatusTextView.setText("Incorrect");
            holder.questionStatusTextView.setTextColor(Color.parseColor("#F44336"));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onQuestionClick(question.getActualPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return questions.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView questionNumberTextView, questionStatusTextView, questionScoreTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            questionNumberTextView = itemView.findViewById(R.id.questionNumberTextView);
            questionStatusTextView = itemView.findViewById(R.id.questionStatusTextView);
            questionScoreTextView = itemView.findViewById(R.id.questionScoreTextView);
        }
    }
} 