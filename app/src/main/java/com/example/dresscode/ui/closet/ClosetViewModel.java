package com.example.dresscode.ui.closet;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.dresscode.data.local.ClosetItemEntity;
import com.example.dresscode.data.repository.ClosetRepository;

import java.util.List;

public class ClosetViewModel extends AndroidViewModel {

    private final ClosetRepository repository;
    private final LiveData<List<ClosetItemEntity>> closetItems;

    public ClosetViewModel(@NonNull Application application) {
        super(application);
        repository = new ClosetRepository(application);
        closetItems = repository.observeAll();
    }

    public LiveData<List<ClosetItemEntity>> getClosetItems() {
        return closetItems;
    }

    public void add(ClosetItemEntity item) {
        repository.add(item);
    }
}

