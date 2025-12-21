package com.example.dresscode.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ClosetDao {
    @Query("SELECT * FROM closet_items WHERE owner = :owner ORDER BY createdAt DESC")
    LiveData<List<ClosetItemEntity>> observeAll(String owner);

    @Query("SELECT * FROM closet_items WHERE owner = :owner AND isFavorite = 1 ORDER BY createdAt DESC")
    LiveData<List<ClosetItemEntity>> observeFavorites(String owner);

    @Insert
    long insert(ClosetItemEntity item);

    @Query("SELECT * FROM closet_items WHERE id = :id AND owner = :owner LIMIT 1")
    ClosetItemEntity getById(long id, String owner);

    @Query("SELECT * FROM closet_items WHERE id = :id AND owner = :owner LIMIT 1")
    LiveData<ClosetItemEntity> observeById(long id, String owner);

    @Query("DELETE FROM closet_items WHERE id = :id AND owner = :owner")
    int deleteById(long id, String owner);

    @Query(
            "UPDATE closet_items SET " +
                    "name = :name, " +
                    "category = :category, " +
                    "color = :color, " +
                    "season = :season, " +
                    "style = :style, " +
                    "scene = :scene " +
                    "WHERE id = :id AND owner = :owner"
    )
    int updateById(
            long id,
            String owner,
            String name,
            String category,
            String color,
            String season,
            String style,
            String scene
    );

    @Query("UPDATE closet_items SET isFavorite = :favorite WHERE id = :id AND owner = :owner")
    int setFavorite(long id, String owner, boolean favorite);

    @Query(
            "UPDATE closet_items SET " +
                    "remoteId = :remoteId, " +
                    "remoteImageUrl = :remoteImageUrl, " +
                    "remoteTagsJson = :remoteTagsJson, " +
                    "remoteTagModel = :remoteTagModel, " +
                    "remoteTagUpdatedAt = :remoteTagUpdatedAt " +
                    "WHERE id = :id AND owner = :owner"
    )
    int updateRemoteById(
            long id,
            String owner,
            long remoteId,
            String remoteImageUrl,
            String remoteTagsJson,
            String remoteTagModel,
            long remoteTagUpdatedAt
    );

    @Query("UPDATE closet_items SET owner = :owner WHERE owner = ''")
    int claimLegacy(String owner);
}
