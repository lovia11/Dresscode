package com.example.dresscode.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "swap_jobs")
public class SwapJobEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String owner;

    public long outfitId;

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

    public SwapJobEntity(
            @NonNull String owner,
            long outfitId,
            @NonNull String sourceType,
            long sourceRefId,
            @NonNull String sourceTitle,
            @NonNull String sourceImageUri,
            @NonNull String personImageUri,
            @NonNull String resultImageUri,
            @NonNull String status,
            long createdAt
    ) {
        this.owner = owner;
        this.outfitId = outfitId;
        this.sourceType = sourceType;
        this.sourceRefId = sourceRefId;
        this.sourceTitle = sourceTitle;
        this.sourceImageUri = sourceImageUri;
        this.personImageUri = personImageUri;
        this.resultImageUri = resultImageUri;
        this.status = status;
        this.createdAt = createdAt;
    }
}
