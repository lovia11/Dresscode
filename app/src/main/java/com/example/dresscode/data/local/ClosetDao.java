package com.example.dresscode.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ClosetDao {
    @Query("SELECT * FROM closet_items ORDER BY createdAt DESC")
    LiveData<List<ClosetItemEntity>> observeAll();

    @Insert
    long insert(ClosetItemEntity item);
}

