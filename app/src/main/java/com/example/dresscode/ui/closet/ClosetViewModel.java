package com.example.dresscode.ui.closet;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.dresscode.data.prefs.AuthRepository;
import com.example.dresscode.data.local.ClosetItemEntity;
import com.example.dresscode.data.repository.ClosetRepository;

import java.util.List;

public class ClosetViewModel extends AndroidViewModel {

    private final ClosetRepository repository;
    private final LiveData<List<ClosetItemEntity>> closetItems;
    private final LiveData<List<ClosetItemEntity>> favoriteClosetItems;

    public ClosetViewModel(@NonNull Application application) {
        super(application);
        String owner = new AuthRepository(application).getCurrentUsernameOrEmpty();
        repository = new ClosetRepository(application, owner);
        closetItems = repository.observeAll();
        favoriteClosetItems = repository.observeFavorites();
    }

    public LiveData<List<ClosetItemEntity>> getClosetItems() {
        return closetItems;
    }

    public LiveData<List<ClosetItemEntity>> getFavoriteClosetItems() {
        return favoriteClosetItems;
    }

    public void add(ClosetItemEntity item) {
        repository.add(item);
    }

    public void delete(long id) {
        repository.delete(id);
    }

    public void update(long id, String name, String category, String color, String season, String style, String scene) {
        repository.update(id, name, category, color, season, style, scene);
    }

    public void setFavorite(long id, boolean favorite) {
        repository.setFavorite(id, favorite);
    }
}
