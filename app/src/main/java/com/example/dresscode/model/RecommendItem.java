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
    public final int imageResId;

    public RecommendItem(@NonNull String title, @NonNull String meta) {
        this(title, meta, null, 0);
    }

    public RecommendItem(@NonNull String title, @NonNull String meta, @Nullable String imageUri) {
        this(title, meta, imageUri, 0);
    }

    public RecommendItem(@NonNull String title, @NonNull String meta, @Nullable String imageUri, int imageResId) {
        this.title = title;
        this.meta = meta;
        this.imageUri = imageUri;
        this.imageResId = imageResId;
    }
}
