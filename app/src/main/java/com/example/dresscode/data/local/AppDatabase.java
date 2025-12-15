package com.example.dresscode.data.local;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(
        entities = {
                ClosetItemEntity.class,
                OutfitEntity.class,
                FavoriteEntity.class
        },
        version = 2,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    public abstract ClosetDao closetDao();

    public abstract OutfitDao outfitDao();
}
