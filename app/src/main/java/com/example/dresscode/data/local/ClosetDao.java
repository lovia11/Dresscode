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

    @Insert
    long insert(ClosetItemEntity item);

    @Query("SELECT * FROM closet_items WHERE id = :id AND owner = :owner LIMIT 1")
    ClosetItemEntity getById(long id, String owner);

    @Query("DELETE FROM closet_items WHERE id = :id AND owner = :owner")
    int deleteById(long id, String owner);

    @Query("UPDATE closet_items SET owner = :owner WHERE owner = ''")
    int claimLegacy(String owner);
}
