package com.example.dresscode.data.local;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "favorites",
        indices = {
                @Index(value = {"owner", "outfitId"}, unique = true)
        }
)
public class FavoriteEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @androidx.annotation.NonNull
    public String owner;

    public long outfitId;

    public long createdAt;

    public FavoriteEntity(@androidx.annotation.NonNull String owner, long outfitId, long createdAt) {
        this.owner = owner;
        this.outfitId = outfitId;
        this.createdAt = createdAt;
    }
}
