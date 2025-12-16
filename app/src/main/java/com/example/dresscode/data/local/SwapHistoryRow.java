package com.example.dresscode.data.local;

import androidx.annotation.NonNull;

public class SwapHistoryRow {
    public long id;
    @NonNull
    public String owner;
    @NonNull
    public String sourceType;
    public long sourceRefId;
    @NonNull
    public String sourceTitle;
    @NonNull
    public String sourceImageUri;
    @NonNull
    public String personImageUri;
    @NonNull
    public String resultImageUri;
    @NonNull
    public String status;
    public long createdAt;
}
