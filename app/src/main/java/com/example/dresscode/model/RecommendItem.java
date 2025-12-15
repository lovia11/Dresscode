package com.example.dresscode.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class RecommendItem {
    @NonNull
    public final String title;
    @NonNull
    public final String meta;
    @Nullable
    public final String imageUri;

    public RecommendItem(@NonNull String title, @NonNull String meta) {
        this(title, meta, null);
    }

    public RecommendItem(@NonNull String title, @NonNull String meta, @Nullable String imageUri) {
        this.title = title;
        this.meta = meta;
        this.imageUri = imageUri;
    }
}
