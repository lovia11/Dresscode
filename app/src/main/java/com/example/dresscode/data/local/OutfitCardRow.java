package com.example.dresscode.data.local;

import androidx.annotation.NonNull;

public class OutfitCardRow {
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
    public boolean isFavorite;
}
