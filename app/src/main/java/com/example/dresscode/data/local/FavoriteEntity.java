package com.example.dresscode.data.local;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "favorites",
        indices = {
                @Index(value = {"outfitId"}, unique = true)
        }
)
public class FavoriteEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long outfitId;

    public long createdAt;

    public FavoriteEntity(long outfitId, long createdAt) {
        this.outfitId = outfitId;
        this.createdAt = createdAt;
    }
}

