package com.gxdevs.gradify.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gxdevs.gradify.R;
import com.gxdevs.gradify.models.NoteItem;

import java.util.List;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder> {

    private Context context;
    private List<NoteItem> noteList;

    public NotesAdapter(Context context, List<NoteItem> noteList) {
        this.context = context;
        this.noteList = noteList;
    }

    public void updateData(List<NoteItem> newNotes) {
        this.noteList = newNotes;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        NoteItem note = noteList.get(position);
        holder.title.setText(note.getWeek()); // Assuming 'week' is the title like "Week 1"
        holder.helper.setText(note.getHelper());

        holder.itemView.setOnClickListener(v -> {
            String link = note.getLink();
            if (link != null && !link.isEmpty()) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                    context.startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(context, "Cannot open link", Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        // Handle download button? item_note.xml has a downloadBtn
        View downloadBtn = holder.itemView.findViewById(R.id.downloadBtn);
        if (downloadBtn != null) {
            downloadBtn.setOnClickListener(v -> {
                 String link = note.getLink();
                if (link != null && !link.isEmpty()) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                        context.startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(context, "Cannot open link", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return noteList != null ? noteList.size() : 0;
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView title, helper;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.note_title_textview);
            helper = itemView.findViewById(R.id.note_helper_textview);
        }
    }
}
