package com.example.dresscode.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface OutfitDao {

    @Query("SELECT COUNT(*) FROM outfits")
    int countOutfits();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<OutfitEntity> outfits);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertFavorite(FavoriteEntity favorite);

    @Query("DELETE FROM favorites WHERE owner = :owner AND outfitId = :outfitId")
    int deleteFavoriteByOutfitId(String owner, long outfitId);

    @Query("UPDATE favorites SET owner = :owner WHERE owner = ''")
    int claimLegacyFavorites(String owner);

    @Query(
            "SELECT o.id, o.title, o.tags, o.gender, o.style, o.season, o.scene, o.weather, o.colorHex, o.coverResId, o.createdAt, " +
                    "CASE WHEN f.outfitId IS NULL THEN 0 ELSE 1 END AS isFavorite " +
                    "FROM outfits o " +
                    "LEFT JOIN favorites f ON f.outfitId = o.id AND f.owner = :owner " +
                    "WHERE (:query IS NULL OR :query = '' OR o.title LIKE '%' || :query || '%' OR o.tags LIKE '%' || :query || '%') " +
                    "AND (:gender IS NULL OR :gender = '' OR o.gender = :gender OR o.gender = 'UNISEX') " +
                    "AND (:style IS NULL OR :style = '' OR o.style = :style) " +
                    "AND (:season IS NULL OR :season = '' OR o.season = :season) " +
                    "AND (:scene IS NULL OR :scene = '' OR o.scene = :scene) " +
                    "AND (:weather IS NULL OR :weather = '' OR o.weather = :weather) " +
                    "ORDER BY o.createdAt DESC"
    )
    LiveData<List<OutfitCardRow>> observeOutfits(
            String owner,
            String query,
            String gender,
            String style,
            String season,
            String scene,
            String weather
    );

    @Query(
            "SELECT o.id, o.title, o.tags, o.gender, o.style, o.season, o.scene, o.weather, o.colorHex, o.coverResId, o.createdAt, " +
                    "1 AS isFavorite " +
                    "FROM outfits o " +
                    "INNER JOIN favorites f ON f.outfitId = o.id AND f.owner = :owner " +
                    "ORDER BY f.createdAt DESC"
    )
    LiveData<List<OutfitCardRow>> observeFavoriteOutfits(String owner);

    @Query(
            "SELECT o.id, o.title, o.tags, o.gender, o.style, o.season, o.scene, o.weather, o.colorHex, o.coverResId, " +
                    "CASE WHEN f.outfitId IS NULL THEN 0 ELSE 1 END AS isFavorite " +
                    "FROM outfits o " +
                    "LEFT JOIN favorites f ON f.outfitId = o.id AND f.owner = :owner " +
                    "WHERE o.id = :id LIMIT 1"
    )
    LiveData<OutfitDetailRow> observeOutfitDetail(String owner, long id);
}
