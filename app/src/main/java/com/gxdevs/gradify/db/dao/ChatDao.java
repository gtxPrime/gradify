package com.gxdevs.gradify.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import com.gxdevs.gradify.db.entities.ChatMessageEntity;
import java.util.List;

@Dao
public interface ChatDao {

    @Insert
    void insertMessage(ChatMessageEntity message);

    @Query("SELECT * FROM chat_messages WHERE video_id = :videoId ORDER BY timestamp ASC")
    List<ChatMessageEntity> getMessagesForVideo(String videoId);

    // Optional: Delete all messages for a video (e.g., if user wants to clear chat)
    @Query("DELETE FROM chat_messages WHERE video_id = :videoId")
    void deleteMessagesForVideo(String videoId);

    // Optional: Delete all messages (e.g., for development or reset)
    @Query("DELETE FROM chat_messages")
    void deleteAllMessages();
} 