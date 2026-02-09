package com.gxdevs.gradify.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.gxdevs.gradify.R;
import com.gxdevs.gradify.models.NoteItem;

import java.util.List;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder> {

    private List<NoteItem> noteItemList;
    private final Context context;

    public NotesAdapter(Context context, List<NoteItem> noteItemList) {
        this.context = context;
        this.noteItemList = noteItemList;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        NoteItem noteItem = noteItemList.get(position);
        holder.titleTextView.setText(noteItem.getWeek());
        holder.helperTextView.setText("Helper: " + noteItem.getHelper());

        holder.downloadBtn.setOnClickListener(v -> {
            String url = noteItem.getLink();
            if (url != null && !url.isEmpty()) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                try {
                    context.startActivity(browserIntent);
                } catch (Exception e) {
                    Toast.makeText(context, "No app can handle this request. Please install a web browser.", Toast.LENGTH_LONG).show();
                    Log.d("DownLinkError", e.getMessage());
                }
            } else {
                Toast.makeText(context, "Download link is not available.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return noteItemList == null ? 0 : noteItemList.size();
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView helperTextView;
        ConstraintLayout downloadBtn;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.note_title_textview);
            helperTextView = itemView.findViewById(R.id.note_helper_textview);
            downloadBtn = itemView.findViewById(R.id.downloadBtn);
        }
    }

    public void updateData(List<NoteItem> newNoteItems) {
        this.noteItemList = newNoteItems;
        notifyDataSetChanged();
    }
} 