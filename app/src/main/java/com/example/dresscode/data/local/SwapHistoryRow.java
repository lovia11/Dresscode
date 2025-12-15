package com.example.dresscode.data.local;

import androidx.annotation.NonNull;

public class SwapHistoryRow {
    public long id;
    @NonNull
    public String owner;
    public long outfitId;
    @NonNull
    public String outfitTitle;
    @NonNull
    public String personImageUri;
    @NonNull
    public String resultImageUri;
    @NonNull
    public String status;
    public long createdAt;
}
