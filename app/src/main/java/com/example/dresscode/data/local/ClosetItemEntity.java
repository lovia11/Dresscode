package com.example.dresscode.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "closet_items")
public class ClosetItemEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String owner;

    @NonNull
    public String name;

    @NonNull
    public String category;

    @NonNull
    public String imageUri;

    @NonNull
    public String color;

    @NonNull
    public String season;

    @NonNull
    public String style;

    public long createdAt;

    public ClosetItemEntity(
            @NonNull String owner,
            @NonNull String name,
            @NonNull String category,
            @NonNull String imageUri,
            @NonNull String color,
            @NonNull String season,
            @NonNull String style,
            long createdAt
    ) {
        this.owner = owner;
        this.name = name;
        this.category = category;
        this.imageUri = imageUri;
        this.color = color;
        this.season = season;
        this.style = style;
        this.createdAt = createdAt;
    }
}
