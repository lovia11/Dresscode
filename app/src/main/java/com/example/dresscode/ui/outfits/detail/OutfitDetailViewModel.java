package com.example.dresscode.ui.outfits.detail;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.dresscode.data.local.OutfitDetailRow;
import com.example.dresscode.data.prefs.AuthRepository;
import com.example.dresscode.data.repository.OutfitRepository;

public class OutfitDetailViewModel extends AndroidViewModel {

    private final OutfitRepository repository;
    private final MutableLiveData<Long> outfitId = new MutableLiveData<>(-1L);
    private final LiveData<OutfitDetailRow> outfit;

    public OutfitDetailViewModel(@NonNull Application application) {
        super(application);
        String owner = new AuthRepository(application).getCurrentUsernameOrEmpty();
        repository = new OutfitRepository(application, owner);
        outfit = Transformations.switchMap(outfitId, id -> repository.observeOutfitDetail(id == null ? -1L : id));
    }

    public void setOutfitId(long id) {
        outfitId.setValue(id);
    }

    public LiveData<OutfitDetailRow> getOutfit() {
        return outfit;
    }

    public void toggleFavorite(long outfitId, boolean shouldFavorite) {
        repository.toggleFavorite(outfitId, shouldFavorite);
    }
}

