package com.example.dresscode.data.local;

import androidx.annotation.NonNull;
import androidx.room.Ignore;
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

    @NonNull
    public String scene;

    public boolean isFavorite;

    /**
     * 公网后端的衣橱记录 id（对应 /api/closet/items 返回的 item.id）。
     * 0 表示尚未同步/上传失败/未开启同步。
     */
    public long remoteId;

    /**
     * 公网可访问的图片 URL（对应 /files/{name} 或 OSS URL）。
     */
    @NonNull
    public String remoteImageUrl;

    /**
     * 模型输出的结构化标签 JSON（字符串），便于后续筛选/推荐。
     */
    @NonNull
    public String remoteTagsJson;

    @NonNull
    public String remoteTagModel;

    public long remoteTagUpdatedAt;

    public long createdAt;

    public ClosetItemEntity() {
        this.owner = "";
        this.name = "";
        this.category = "";
        this.imageUri = "";
        this.color = "";
        this.season = "";
        this.style = "";
        this.scene = "";
        this.isFavorite = false;
        this.remoteId = 0;
        this.remoteImageUrl = "";
        this.remoteTagsJson = "";
        this.remoteTagModel = "";
        this.remoteTagUpdatedAt = 0;
        this.createdAt = 0;
    }

    @Ignore
    public ClosetItemEntity(
            @NonNull String owner,
            @NonNull String name,
            @NonNull String category,
            @NonNull String imageUri,
            @NonNull String color,
            @NonNull String season,
            @NonNull String style,
            @NonNull String scene,
            boolean isFavorite,
            long createdAt
    ) {
        this.owner = owner;
        this.name = name;
        this.category = category;
        this.imageUri = imageUri;
        this.color = color;
        this.season = season;
        this.style = style;
        this.scene = scene;
        this.isFavorite = isFavorite;
        this.remoteId = 0;
        this.remoteImageUrl = "";
        this.remoteTagsJson = "";
        this.remoteTagModel = "";
        this.remoteTagUpdatedAt = 0;
        this.createdAt = createdAt;
    }
}
