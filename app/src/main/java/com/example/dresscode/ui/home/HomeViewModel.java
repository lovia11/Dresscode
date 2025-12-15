package com.example.dresscode.ui.home;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.example.dresscode.R;
import com.example.dresscode.data.local.ClosetItemEntity;
import com.example.dresscode.data.prefs.AuthRepository;
import com.example.dresscode.data.prefs.UserPreferencesRepository;
import com.example.dresscode.data.prefs.WeatherPreferencesRepository;
import com.example.dresscode.data.repository.ClosetRepository;
import com.example.dresscode.data.repository.WeatherRepository;
import com.example.dresscode.model.RecommendItem;

import java.util.ArrayList;
import java.util.List;

public class HomeViewModel extends AndroidViewModel {

    private final androidx.lifecycle.MediatorLiveData<List<RecommendItem>> recommendations = new androidx.lifecycle.MediatorLiveData<>();

    private final WeatherRepository weatherRepository = new WeatherRepository();
    private final UserPreferencesRepository userPrefs;
    private final WeatherPreferencesRepository weatherPrefs;

    private List<ClosetItemEntity> closetItems = new ArrayList<>();
    private String gender = "";
    private String city = "";

    public HomeViewModel(@NonNull Application application) {
        super(application);
        String owner = new AuthRepository(application).getCurrentUsernameOrEmpty();
        ClosetRepository repository = new ClosetRepository(application, owner);
        userPrefs = new UserPreferencesRepository(application);
        weatherPrefs = new WeatherPreferencesRepository(application);

        recommendations.addSource(repository.observeAll(), items -> {
            closetItems = items == null ? new ArrayList<>() : items;
            update();
        });
        recommendations.addSource(userPrefs.observeGender(), g -> {
            gender = g == null ? "" : g;
            update();
        });
        recommendations.addSource(weatherPrefs.observeCity(), c -> {
            city = c == null ? "" : c;
            update();
        });
        update();
    }

    public LiveData<List<RecommendItem>> getRecommendations() {
        return recommendations;
    }

    private void update() {
        recommendations.setValue(buildRecommendations(closetItems, gender, city));
    }

    private List<RecommendItem> buildRecommendations(List<ClosetItemEntity> closetItems, String gender, String city) {
        List<RecommendItem> result = new ArrayList<>();
        WeatherRepository.WeatherInfo weather = weatherRepository.getWeatherForCity(city);
        String genderLabel = genderLabel(gender);
        String weatherMeta = weather.city + " " + weather.temp + " · " + weather.desc + " · " + genderLabel;

        if (closetItems == null || closetItems.isEmpty()) {
            result.add(new RecommendItem(
                    getApplication().getString(R.string.title_recommend_today),
                    getApplication().getString(R.string.placeholder_recommend_empty),
                    null,
                    com.example.dresscode.R.drawable.ic_outfit_outer
            ));
            return result;
        }

        String seasonHint = seasonFromTemp(weather.temp);
        boolean rainy = weather.desc.contains("雨");
        boolean cold = parseTemp(weather.temp) <= 10;

        ClosetItemEntity dress = pickFirstPreferSeason(closetItems, "连衣裙", seasonHint);
        ClosetItemEntity top = pickFirstPreferSeason(closetItems, "上衣", seasonHint);
        ClosetItemEntity bottom = pickFirstPreferSeason(closetItems, "下装", seasonHint);
        ClosetItemEntity outer = pickFirstPreferSeason(closetItems, "外套", seasonHint);
        ClosetItemEntity shoes = pickFirstPreferSeason(closetItems, "鞋子", seasonHint);

        if (dress != null) {
            result.add(new RecommendItem(
                    "衣橱推荐：" + dress.name,
                    weatherMeta + " · " + seasonHint + " · 来自你的衣橱",
                    dress.imageUri,
                    0
            ));
        }

        if (top != null && bottom != null) {
            result.add(new RecommendItem(
                    "衣橱推荐：" + top.name + " + " + bottom.name,
                    weatherMeta + " · " + seasonHint + " · 来自你的衣橱",
                    top.imageUri,
                    0
            ));
        }

        if (outer != null && (cold || rainy || top != null)) {
            result.add(new RecommendItem(
                    (rainy ? "雨天外套：" : "叠穿推荐：") + outer.name,
                    weatherMeta + " · " + seasonHint + " · 出门更稳",
                    outer.imageUri,
                    0
            ));
        }

        if (shoes != null && !result.isEmpty()) {
            RecommendItem first = result.get(0);
            result.set(0, new RecommendItem(first.title, first.meta + " · 搭配 " + shoes.name, first.imageUri, first.imageResId));
        }

        if (result.isEmpty()) {
            ClosetItemEntity any = closetItems.get(0);
            result.add(new RecommendItem(
                    "衣橱推荐：" + any.name,
                    weatherMeta + " · " + seasonHint + " · 来自你的衣橱",
                    any.imageUri,
                    0
            ));
        }

        return result;
    }

    private ClosetItemEntity pickFirstPreferSeason(List<ClosetItemEntity> items, String category, String seasonHint) {
        ClosetItemEntity fallback = null;
        for (ClosetItemEntity item : items) {
            if (!category.equals(item.category)) {
                continue;
            }
            if (fallback == null) {
                fallback = item;
            }
            if (item.season != null && item.season.contains(seasonHint)) {
                return item;
            }
        }
        return fallback;
    }

    private String genderLabel(String code) {
        if (UserPreferencesRepository.GENDER_MALE.equals(code)) {
            return "男";
        }
        if (UserPreferencesRepository.GENDER_FEMALE.equals(code)) {
            return "女";
        }
        return "不限";
    }

    private String seasonFromTemp(String temp) {
        int t = parseTemp(temp);
        if (t <= 10) {
            return "秋冬";
        }
        if (t <= 18) {
            return "春秋";
        }
        return "春夏";
    }

    private int parseTemp(String temp) {
        if (temp == null) {
            return 16;
        }
        String digits = temp.replaceAll("[^0-9-]", "");
        if (digits.isEmpty() || "-".equals(digits)) {
            return 16;
        }
        try {
            return Integer.parseInt(digits);
        } catch (Exception e) {
            return 16;
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        userPrefs.close();
        weatherPrefs.close();
    }
}
