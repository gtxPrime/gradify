package com.gxdevs.gradify.db;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.gxdevs.gradify.db.dao.ChatDao;
import com.gxdevs.gradify.db.entities.ChatMessageEntity;

@Database(entities = {ChatMessageEntity.class}, version = 1) // Only ChatMessageEntity, version 1
public abstract class AppDatabase extends RoomDatabase {

    public abstract ChatDao chatDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "gradify_chat_db") // Changed DB name slightly for clarity
                            .fallbackToDestructiveMigration() // For development. Use proper migrations for production.
                            .build();
                }
            }
        }
        return INSTANCE;
    }
} 