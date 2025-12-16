package com.example.dresscode.ui.swap;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.dresscode.data.local.ClosetItemEntity;
import com.example.dresscode.data.local.OutfitCardRow;
import com.example.dresscode.data.local.SwapHistoryRow;
import com.example.dresscode.data.prefs.AuthRepository;
import com.example.dresscode.data.repository.ClosetRepository;
import com.example.dresscode.data.repository.OutfitRepository;
import com.example.dresscode.data.repository.SwapRepository;
import com.example.dresscode.data.repository.TryOnRepository;

import java.util.List;

public class SwapViewModel extends AndroidViewModel {

    private final OutfitRepository repository;
    private final ClosetRepository closetRepository;
    private final SwapRepository swapRepository;
    private final LiveData<List<OutfitCardRow>> favoriteOutfits;
    private final LiveData<List<ClosetItemEntity>> favoriteClosetItems;
    private final LiveData<List<SwapHistoryRow>> history;

    private final MutableLiveData<Long> selectedOutfitId = new MutableLiveData<>(-1L);
    private final MutableLiveData<Long> selectedClosetItemId = new MutableLiveData<>(-1L);
    private final MutableLiveData<String> personImageUri = new MutableLiveData<>("");
    private final MediatorLiveData<Boolean> canGenerate = new MediatorLiveData<>();
    private final MutableLiveData<Boolean> generating = new MutableLiveData<>(false);
    private final MutableLiveData<String> generateError = new MutableLiveData<>("");
    private final MutableLiveData<String> resultImageUri = new MutableLiveData<>("");

    private final MediatorLiveData<OutfitCardRow> selectedOutfit = new MediatorLiveData<>();
    private final MediatorLiveData<ClosetItemEntity> selectedClosetItem = new MediatorLiveData<>();
    private final MediatorLiveData<String> selectedLabel = new MediatorLiveData<>();
    private final TryOnRepository tryOnRepository;

    public SwapViewModel(@NonNull Application application) {
        super(application);
        String owner = new AuthRepository(application).getCurrentUsernameOrEmpty();
        repository = new OutfitRepository(application, owner);
        closetRepository = new ClosetRepository(application, owner);
        swapRepository = new SwapRepository(application, owner);
        tryOnRepository = new TryOnRepository(application);
        repository.ensureSeeded();
        favoriteOutfits = repository.observeFavoriteOutfits();
        favoriteClosetItems = closetRepository.observeFavorites();
        history = swapRepository.observeHistory();

        canGenerate.addSource(selectedOutfitId, ignored -> updateCanGenerate());
        canGenerate.addSource(selectedClosetItemId, ignored -> updateCanGenerate());
        canGenerate.addSource(personImageUri, ignored -> updateCanGenerate());
        canGenerate.addSource(favoriteOutfits, ignored -> updateCanGenerate());
        canGenerate.addSource(favoriteClosetItems, ignored -> updateCanGenerate());

        selectedOutfit.addSource(selectedOutfitId, ignored -> updateSelectedOutfit());
        selectedOutfit.addSource(favoriteOutfits, ignored -> updateSelectedOutfit());
        selectedClosetItem.addSource(selectedClosetItemId, ignored -> updateSelectedClosetItem());
        selectedClosetItem.addSource(favoriteClosetItems, ignored -> updateSelectedClosetItem());

        selectedLabel.addSource(selectedOutfit, ignored -> updateSelectedLabel());
        selectedLabel.addSource(selectedClosetItem, ignored -> updateSelectedLabel());
        updateSelectedOutfit();
        updateSelectedClosetItem();
        updateSelectedLabel();
        updateCanGenerate();
    }

    public LiveData<List<OutfitCardRow>> getFavoriteOutfits() {
        return favoriteOutfits;
    }

    public LiveData<List<ClosetItemEntity>> getFavoriteClosetItems() {
        return favoriteClosetItems;
    }

    public LiveData<List<SwapHistoryRow>> getHistory() {
        return history;
    }

    public MutableLiveData<Long> getSelectedOutfitId() {
        return selectedOutfitId;
    }

    public MutableLiveData<Long> getSelectedClosetItemId() {
        return selectedClosetItemId;
    }

    public LiveData<String> getSelectedLabel() {
        return selectedLabel;
    }

    public void selectOutfit(long id) {
        selectedOutfitId.setValue(id);
        selectedClosetItemId.setValue(-1L);
        updateCanGenerate();
    }

    public void setSelectedOutfitId(long id) {
        selectOutfit(id);
    }

    public void selectClosetItem(long id) {
        selectedClosetItemId.setValue(id);
        selectedOutfitId.setValue(-1L);
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

    public LiveData<Boolean> getGenerating() {
        return generating;
    }

    public LiveData<String> getGenerateError() {
        return generateError;
    }

    public LiveData<String> getResultImageUri() {
        return resultImageUri;
    }

    public void generateSwap() {
        ClosetItemEntity selectedCloset = selectedClosetItem.getValue();
        String person = personImageUri.getValue();
        if (person == null || person.trim().isEmpty()) {
            return;
        }
        generateError.setValue("");
        resultImageUri.setValue("");

        // 真实 try-on：优先支持衣橱收藏衣物（有真实衣物图片）
        if (selectedCloset != null && selectedCloset.imageUri != null && !selectedCloset.imageUri.trim().isEmpty()) {
            generating.setValue(true);
            tryOnRepository.tryOn(
                    android.net.Uri.parse(person),
                    android.net.Uri.parse(selectedCloset.imageUri),
                    new TryOnRepository.ResultCallback() {
                        @Override
                        public void onSuccess(String resultUri) {
                            generating.postValue(false);
                            resultImageUri.postValue(resultUri == null ? "" : resultUri);
                            swapRepository.addJob(
                                    "CLOSET",
                                    selectedCloset.id,
                                    selectedCloset.name,
                                    selectedCloset.imageUri,
                                    person,
                                    resultUri,
                                    "已生成"
                            );
                        }

                        @Override
                        public void onError(String message) {
                            generating.postValue(false);
                            generateError.postValue(message == null ? "生成失败" : message);
                        }
                    }
            );
            return;
        }

        // 离线占位：收藏穿搭目前没有单独的服装图片，先保持原占位逻辑
        OutfitCardRow selected = selectedOutfit.getValue();
        if (selected != null) {
            resultImageUri.setValue(person);
            swapRepository.addJob("OUTFIT", selected.id, selected.title, "", person, person, "已生成（占位）");
        }
    }

    public void deleteHistory(long id) {
        swapRepository.deleteJob(id);
    }

    private void updateCanGenerate() {
        String uri = personImageUri.getValue();
        OutfitCardRow selected = findSelectedFromFavorites();
        ClosetItemEntity closet = findSelectedClosetFromFavorites();
        boolean ok = (selected != null || closet != null) && uri != null && !uri.trim().isEmpty();
        canGenerate.setValue(ok);
    }

    private void updateSelectedOutfit() {
        selectedOutfit.setValue(findSelectedFromFavorites());
    }

    private void updateSelectedClosetItem() {
        selectedClosetItem.setValue(findSelectedClosetFromFavorites());
    }

    private void updateSelectedLabel() {
        OutfitCardRow o = selectedOutfit.getValue();
        if (o != null) {
            selectedLabel.setValue(o.title);
            return;
        }
        ClosetItemEntity c = selectedClosetItem.getValue();
        selectedLabel.setValue(c == null ? "" : c.name);
    }

    private OutfitCardRow findSelectedFromFavorites() {
        List<OutfitCardRow> items = favoriteOutfits.getValue();
        Long id = selectedOutfitId.getValue();
        if (items == null || items.isEmpty() || id == null || id <= 0) {
            return null;
        }
        for (OutfitCardRow row : items) {
            if (row.id == id) {
                return row;
            }
        }
        return null;
    }

    private ClosetItemEntity findSelectedClosetFromFavorites() {
        List<ClosetItemEntity> items = favoriteClosetItems.getValue();
        Long id = selectedClosetItemId.getValue();
        if (items == null || items.isEmpty() || id == null || id <= 0) {
            return null;
        }
        for (ClosetItemEntity row : items) {
            if (row.id == id) {
                return row;
            }
        }
        return null;
    }
}
