package com.example.dresscode.data.local;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(
        entities = {
                ClosetItemEntity.class
        },
        version = 1,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    public abstract ClosetDao closetDao();
}

