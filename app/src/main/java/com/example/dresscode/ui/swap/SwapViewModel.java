package com.example.dresscode.ui.swap;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.dresscode.data.local.OutfitCardRow;
import com.example.dresscode.data.repository.OutfitRepository;

import java.util.List;

public class SwapViewModel extends AndroidViewModel {

    private final OutfitRepository repository;
    private final LiveData<List<OutfitCardRow>> favoriteOutfits;

    private final MutableLiveData<Long> selectedOutfitId = new MutableLiveData<>(-1L);
    private final MutableLiveData<String> personImageUri = new MutableLiveData<>("");
    private final MediatorLiveData<Boolean> canGenerate = new MediatorLiveData<>();

    private final LiveData<OutfitCardRow> selectedOutfit;

    public SwapViewModel(@NonNull Application application) {
        super(application);
        repository = new OutfitRepository(application);
        repository.ensureSeeded();
        favoriteOutfits = repository.observeFavoriteOutfits();

        selectedOutfit = Transformations.map(favoriteOutfits, items -> {
            Long id = selectedOutfitId.getValue();
            if (items == null || id == null || id < 0) {
                return null;
            }
            for (OutfitCardRow row : items) {
                if (row.id == id) {
                    return row;
                }
            }
            return null;
        });

        canGenerate.addSource(selectedOutfitId, ignored -> updateCanGenerate());
        canGenerate.addSource(personImageUri, ignored -> updateCanGenerate());
        updateCanGenerate();
    }

    public LiveData<List<OutfitCardRow>> getFavoriteOutfits() {
        return favoriteOutfits;
    }

    public MutableLiveData<Long> getSelectedOutfitId() {
        return selectedOutfitId;
    }

    public void setSelectedOutfitId(long id) {
        selectedOutfitId.setValue(id);
        updateCanGenerate();
    }

    public LiveData<OutfitCardRow> getSelectedOutfit() {
        return selectedOutfit;
    }

    public MutableLiveData<String> getPersonImageUri() {
        return personImageUri;
    }

    public void setPersonImageUri(String uri) {
        personImageUri.setValue(uri == null ? "" : uri);
        updateCanGenerate();
    }

    public LiveData<Boolean> getCanGenerate() {
        return canGenerate;
    }

    private void updateCanGenerate() {
        Long id = selectedOutfitId.getValue();
        String uri = personImageUri.getValue();
        boolean ok = id != null && id >= 0 && uri != null && !uri.trim().isEmpty();
        canGenerate.setValue(ok);
    }
}

