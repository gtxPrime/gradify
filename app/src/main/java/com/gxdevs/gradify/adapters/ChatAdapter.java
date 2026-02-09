package com.gxdevs.gradify.adapters;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gxdevs.gradify.R;
import com.gxdevs.gradify.models.ChatMessage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.noties.markwon.Markwon;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_USER = 1;
    private static final int VIEW_TYPE_AI = 2;

    private final List<ChatMessage> chatMessages;
    private final SimpleDateFormat timestampFormatter;

    public ChatAdapter(List<ChatMessage> chatMessages, Context context) {
        this.chatMessages = chatMessages;
        this.timestampFormatter = new SimpleDateFormat("hh:mm a", Locale.getDefault());
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = chatMessages.get(position);
        if (message.isUserMessage()) {
            return VIEW_TYPE_USER;
        } else {
            return VIEW_TYPE_AI;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_USER) {
            View view = inflater.inflate(R.layout.item_chat_message_user, parent, false);
            return new UserMessageViewHolder(view);

        } else { // VIEW_TYPE_AI
            View view = inflater.inflate(R.layout.item_chat_message_ai, parent, false);
            return new AiMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = chatMessages.get(position);
        String formattedTime = timestampFormatter.format(new Date(message.getTimestamp()));

        if (holder.getItemViewType() == VIEW_TYPE_USER) {
            ((UserMessageViewHolder) holder).bind(message, formattedTime);
        } else { // VIEW_TYPE_AI
            ((AiMessageViewHolder) holder).bind(message, formattedTime);
        }

        // Margin logic
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) holder.itemView.getLayoutParams();
        Context context = holder.itemView.getContext();

        int specialMarginPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 54, context.getResources().getDisplayMetrics());
        int defaultInterItemMarginPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, context.getResources().getDisplayMetrics());

        int newTopMargin;
        int newBottomMargin;

        if (getItemCount() == 1) {
            // Single item: special margins on both top and bottom
            newTopMargin = specialMarginPx;
            newBottomMargin = specialMarginPx;
        } else if (position == 0) {
            // First item (but not the only one)
            newTopMargin = specialMarginPx;
            newBottomMargin = defaultInterItemMarginPx / 2; // Space before next item
        } else if (position == getItemCount() - 1) {
            // Last item (but not the only one)
            newTopMargin = defaultInterItemMarginPx / 2; // Space after previous item
            newBottomMargin = specialMarginPx - 20;
        } else {
            // Middle item
            newTopMargin = defaultInterItemMarginPx / 2;
            newBottomMargin = defaultInterItemMarginPx / 2;
        }

        params.setMargins(params.leftMargin, newTopMargin, params.rightMargin, newBottomMargin);
        holder.itemView.setLayoutParams(params);
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    public void addMessage(ChatMessage message) {
        chatMessages.add(message);
        notifyItemInserted(chatMessages.size() - 1);
    }

    static class UserMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView;
        TextView timestampTextView;

        UserMessageViewHolder(View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.textView_message_text_user);
            timestampTextView = itemView.findViewById(R.id.textView_message_timestamp_user);

            itemView.setOnLongClickListener(v -> {
                Context context = v.getContext();
                String textToCopy = messageTextView.getText().toString();
                ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clipData = ClipData.newPlainText("Copied Text", textToCopy);
                clipboardManager.setPrimaryClip(clipData);
                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show();

                return true;
            });
        }

        void bind(ChatMessage message, String formattedTime) {
            Markwon markwon = Markwon.create(itemView.getContext());
            markwon.setMarkdown(messageTextView, message.getMessage());
            timestampTextView.setText(formattedTime);
        }
    }

    static class AiMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView;
        TextView timestampTextView;

        AiMessageViewHolder(View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.textView_message_text_ai);
            timestampTextView = itemView.findViewById(R.id.textView_message_timestamp_ai);

            itemView.setOnLongClickListener(v -> {
                Context context = v.getContext();
                String textToCopy = messageTextView.getText().toString();
                ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clipData = ClipData.newPlainText("Copied Text", textToCopy);
                clipboardManager.setPrimaryClip(clipData);
                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show();

                return true;
            });
        }

        void bind(ChatMessage message, String formattedTime) {
            Markwon markwon = Markwon.create(itemView.getContext());
            markwon.setMarkdown(messageTextView, message.getMessage());
            timestampTextView.setText(formattedTime);
        }
    }

}