package com.example.dresscode.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "outfits")
public class OutfitEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String title;
    @NonNull
    public String tags;
    @NonNull
    public String gender;
    @NonNull
    public String style;
    @NonNull
    public String season;
    @NonNull
    public String scene;
    @NonNull
    public String weather;
    @NonNull
    public String colorHex;
    public int coverResId;

    public long createdAt;

    public OutfitEntity(
            @NonNull String title,
            @NonNull String tags,
            @NonNull String gender,
            @NonNull String style,
            @NonNull String season,
            @NonNull String scene,
            @NonNull String weather,
            @NonNull String colorHex,
            int coverResId,
            long createdAt
    ) {
        this.title = title;
        this.tags = tags;
        this.gender = gender;
        this.style = style;
        this.season = season;
        this.scene = scene;
        this.weather = weather;
        this.colorHex = colorHex;
        this.coverResId = coverResId;
        this.createdAt = createdAt;
    }
}
