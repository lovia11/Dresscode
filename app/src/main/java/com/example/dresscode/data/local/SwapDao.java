package com.example.dresscode.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface SwapDao {
    @Insert
    long insert(SwapJobEntity job);

    @Query(
            "SELECT j.id, j.owner, j.sourceType, j.sourceRefId, j.sourceTitle, j.sourceImageUri, " +
                    "j.personImageUri, j.resultImageUri, j.status, j.createdAt " +
                    "FROM swap_jobs j " +
                    "WHERE j.owner = :owner " +
                    "ORDER BY j.createdAt DESC"
    )
    LiveData<List<SwapHistoryRow>> observeHistory(String owner);

    @Query("DELETE FROM swap_jobs WHERE id = :id AND owner = :owner")
    int deleteById(long id, String owner);

    @Query("UPDATE swap_jobs SET owner = :owner WHERE owner = ''")
    int claimLegacy(String owner);
}
