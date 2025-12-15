package com.example.dresscode.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.dresscode.data.local.DatabaseProvider;
import com.example.dresscode.data.local.SwapDao;
import com.example.dresscode.data.local.SwapHistoryRow;
import com.example.dresscode.data.local.SwapJobEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SwapRepository {
    private final SwapDao dao;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private final String owner;

    public SwapRepository(Context context, String owner) {
        dao = DatabaseProvider.get(context).swapDao();
        this.owner = owner == null ? "" : owner;
        ioExecutor.execute(() -> dao.claimLegacy(this.owner));
    }

    public LiveData<List<SwapHistoryRow>> observeHistory() {
        return dao.observeHistory(owner);
    }

    public void addJob(long outfitId, String personImageUri, String resultImageUri, String status) {
        ioExecutor.execute(() -> dao.insert(new SwapJobEntity(
                owner,
                outfitId,
                personImageUri,
                resultImageUri,
                status,
                System.currentTimeMillis()
        )));
    }

    public void deleteJob(long id) {
        ioExecutor.execute(() -> dao.deleteById(id, owner));
    }
}
