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
    public String personImageUri;

    @NonNull
    public String resultImageUri;

    @NonNull
    public String status;

    public long createdAt;

    public SwapJobEntity(
            @NonNull String owner,
            long outfitId,
            @NonNull String personImageUri,
            @NonNull String resultImageUri,
            @NonNull String status,
            long createdAt
    ) {
        this.owner = owner;
        this.outfitId = outfitId;
        this.personImageUri = personImageUri;
        this.resultImageUri = resultImageUri;
        this.status = status;
        this.createdAt = createdAt;
    }
}
