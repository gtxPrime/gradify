package com.gxdevs.gradify.db.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.annotation.NonNull;

@Entity(tableName = "chat_messages")
public class ChatMessageEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @NonNull
    @ColumnInfo(name = "video_id")
    public String videoId; // To link chat to a specific video (e.g., YouTube link)

    @NonNull
    @ColumnInfo(name = "message_text")
    public String messageText;

    @ColumnInfo(name = "is_user_message")
    public boolean isUserMessage;

    @ColumnInfo(name = "timestamp")
    public long timestamp;

    public ChatMessageEntity(@NonNull String videoId, @NonNull String messageText, boolean isUserMessage, long timestamp) {
        this.videoId = videoId;
        this.messageText = messageText;
        this.isUserMessage = isUserMessage;
        this.timestamp = timestamp;
    }
} 