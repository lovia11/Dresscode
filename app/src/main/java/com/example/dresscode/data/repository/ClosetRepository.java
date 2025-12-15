package com.example.dresscode.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.dresscode.data.local.ClosetDao;
import com.example.dresscode.data.local.ClosetItemEntity;
import com.example.dresscode.data.local.DatabaseProvider;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClosetRepository {
    private final ClosetDao closetDao;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    public ClosetRepository(Context context) {
        this.closetDao = DatabaseProvider.get(context).closetDao();
    }

    public LiveData<List<ClosetItemEntity>> observeAll() {
        return closetDao.observeAll();
    }

    public void add(ClosetItemEntity item) {
        ioExecutor.execute(() -> closetDao.insert(item));
    }
}

