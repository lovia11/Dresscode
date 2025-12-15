package com.example.dresscode.ui.outfits;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.dresscode.R;
import com.example.dresscode.data.local.OutfitCardRow;
import com.example.dresscode.data.prefs.UserPreferencesRepository;
import com.example.dresscode.data.repository.OutfitRepository;

import java.util.List;

public class OutfitsViewModel extends AndroidViewModel {

    private final OutfitRepository repository;
    private final UserPreferencesRepository prefs;

    private final MutableLiveData<String> query = new MutableLiveData<>("");
    private final MutableLiveData<String> genderOverride = new MutableLiveData<>(null);
    private final MutableLiveData<String> style = new MutableLiveData<>("");
    private final MutableLiveData<String> season = new MutableLiveData<>("");
    private final MutableLiveData<String> scene = new MutableLiveData<>("");
    private final MutableLiveData<String> weather = new MutableLiveData<>("");

    private final MediatorLiveData<Params> params = new MediatorLiveData<>();

    private final LiveData<List<OutfitCardRow>> outfits;

    private final MutableLiveData<String> chipGenderText = new MutableLiveData<>();
    private final MutableLiveData<String> chipStyleText = new MutableLiveData<>();
    private final MutableLiveData<String> chipSeasonText = new MutableLiveData<>();
    private final MutableLiveData<String> chipSceneText = new MutableLiveData<>();
    private final MutableLiveData<String> chipWeatherText = new MutableLiveData<>();

    public OutfitsViewModel(@NonNull Application application) {
        super(application);
        repository = new OutfitRepository(application);
        prefs = new UserPreferencesRepository(application);

        repository.ensureSeeded();

        params.addSource(query, ignored -> updateParams());
        params.addSource(genderOverride, ignored -> updateParams());
        params.addSource(style, ignored -> updateParams());
        params.addSource(season, ignored -> updateParams());
        params.addSource(scene, ignored -> updateParams());
        params.addSource(weather, ignored -> updateParams());
        params.addSource(prefs.observeGender(), ignored -> updateParams());
        updateParams();

        outfits = Transformations.switchMap(params, p ->
                repository.observeOutfits(p.query, p.gender, p.style, p.season, p.scene, p.weather)
        );
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        prefs.close();
    }

    public LiveData<List<OutfitCardRow>> getOutfits() {
        return outfits;
    }

    public void toggleFavorite(long outfitId, boolean shouldFavorite) {
        repository.toggleFavorite(outfitId, shouldFavorite);
    }

    public void setQuery(String value) {
        query.setValue(value == null ? "" : value.trim());
    }

    public void useGenderFromSettings() {
        genderOverride.setValue(null);
    }

    public void setGenderOverride(String genderCodeOrEmpty) {
        genderOverride.setValue(genderCodeOrEmpty == null ? "" : genderCodeOrEmpty);
    }

    public void setStyleFilter(String value) {
        style.setValue(value == null ? "" : value);
    }

    public void setSeasonFilter(String value) {
        season.setValue(value == null ? "" : value);
    }

    public void setSceneFilter(String value) {
        scene.setValue(value == null ? "" : value);
    }

    public void setWeatherFilter(String value) {
        weather.setValue(value == null ? "" : value);
    }

    public LiveData<String> getChipGenderText() {
        return chipGenderText;
    }

    public LiveData<String> getChipStyleText() {
        return chipStyleText;
    }

    public LiveData<String> getChipSeasonText() {
        return chipSeasonText;
    }

    public LiveData<String> getChipSceneText() {
        return chipSceneText;
    }

    public LiveData<String> getChipWeatherText() {
        return chipWeatherText;
    }

    private void updateParams() {
        String effectiveGender = genderOverride.getValue();
        boolean followSettings = effectiveGender == null;
        if (followSettings) {
            effectiveGender = prefs.getGender();
        }

        Params p = new Params(
                query.getValue(),
                effectiveGender,
                style.getValue(),
                season.getValue(),
                scene.getValue(),
                weather.getValue()
        );
        params.setValue(p);

        chipGenderText.setValue(buildGenderChipText(followSettings, prefs.getGender(), effectiveGender));
        chipStyleText.setValue(buildChipText(R.string.filter_style, style.getValue()));
        chipSeasonText.setValue(buildChipText(R.string.filter_season, season.getValue()));
        chipSceneText.setValue(buildChipText(R.string.filter_scene, scene.getValue()));
        chipWeatherText.setValue(buildChipText(R.string.filter_weather, weather.getValue()));
    }

    private String buildChipText(int titleRes, String value) {
        String title = getApplication().getString(titleRes);
        if (value == null || value.trim().isEmpty()) {
            return title;
        }
        return title + "：" + value.trim();
    }

    private String buildGenderChipText(boolean followSettings, String prefGender, String effectiveGender) {
        String title = getApplication().getString(R.string.filter_gender);
        if (!followSettings) {
            if (effectiveGender == null || effectiveGender.trim().isEmpty()) {
                return title + "：不限";
            }
            return title + "：" + genderLabel(effectiveGender);
        }
        if (prefGender == null || prefGender.trim().isEmpty()) {
            return title;
        }
        return title + "：" + genderLabel(prefGender);
    }

    private String genderLabel(String code) {
        if (UserPreferencesRepository.GENDER_MALE.equals(code)) {
            return "男";
        }
        if (UserPreferencesRepository.GENDER_FEMALE.equals(code)) {
            return "女";
        }
        if ("UNISEX".equals(code)) {
            return "通用";
        }
        return code;
    }

    private static final class Params {
        final String query;
        final String gender;
        final String style;
        final String season;
        final String scene;
        final String weather;

        Params(String query, String gender, String style, String season, String scene, String weather) {
            this.query = query;
            this.gender = gender;
            this.style = style;
            this.season = season;
            this.scene = scene;
            this.weather = weather;
        }
    }
}
