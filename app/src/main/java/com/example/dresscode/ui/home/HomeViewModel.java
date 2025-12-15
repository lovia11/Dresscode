package com.example.dresscode.ui.home;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.example.dresscode.R;
import com.example.dresscode.data.local.ClosetItemEntity;
import com.example.dresscode.data.repository.ClosetRepository;
import com.example.dresscode.model.RecommendItem;

import java.util.ArrayList;
import java.util.List;

public class HomeViewModel extends AndroidViewModel {

    private final LiveData<List<RecommendItem>> recommendations;

    public HomeViewModel(@NonNull Application application) {
        super(application);
        ClosetRepository repository = new ClosetRepository(application);
        recommendations = Transformations.map(repository.observeAll(), this::buildRecommendations);
    }

    public LiveData<List<RecommendItem>> getRecommendations() {
        return recommendations;
    }

    private List<RecommendItem> buildRecommendations(List<ClosetItemEntity> closetItems) {
        List<RecommendItem> result = new ArrayList<>();
        if (closetItems == null || closetItems.isEmpty()) {
            result.add(new RecommendItem(
                    getApplication().getString(R.string.title_recommend_today),
                    getApplication().getString(R.string.placeholder_recommend_empty),
                    null
            ));
            return result;
        }

        ClosetItemEntity dress = pickFirst(closetItems, "连衣裙");
        ClosetItemEntity top = pickFirst(closetItems, "上衣");
        ClosetItemEntity bottom = pickFirst(closetItems, "下装");
        ClosetItemEntity outer = pickFirst(closetItems, "外套");
        ClosetItemEntity shoes = pickFirst(closetItems, "鞋子");

        if (dress != null) {
            result.add(new RecommendItem(
                    "今日推荐：" + dress.name,
                    buildMeta(dress),
                    dress.imageUri
            ));
        }

        if (top != null && bottom != null) {
            result.add(new RecommendItem(
                    "今日推荐：" + top.name + " + " + bottom.name,
                    buildMeta(top) + " · " + buildMeta(bottom),
                    top.imageUri
            ));
        }

        if (outer != null && top != null) {
            result.add(new RecommendItem(
                    "叠穿推荐：" + outer.name + " + " + top.name,
                    buildMeta(outer),
                    outer.imageUri
            ));
        }

        if (shoes != null && !result.isEmpty()) {
            RecommendItem first = result.get(0);
            result.set(0, new RecommendItem(first.title, first.meta + " · 搭配 " + shoes.name));
        }

        if (result.isEmpty()) {
            ClosetItemEntity any = closetItems.get(0);
            result.add(new RecommendItem(
                    "今日推荐：" + any.name,
                    buildMeta(any),
                    any.imageUri
            ));
        }

        return result;
    }

    private ClosetItemEntity pickFirst(List<ClosetItemEntity> items, String category) {
        for (ClosetItemEntity item : items) {
            if (category.equals(item.category)) {
                return item;
            }
        }
        return null;
    }

    private String buildMeta(ClosetItemEntity item) {
        String style = safe(item.style);
        String season = safe(item.season);
        String color = safe(item.color);
        return style + " · " + season + " · " + color;
    }

    private String safe(String s) {
        if (s == null || s.trim().isEmpty()) {
            return getApplication().getString(R.string.default_unknown);
        }
        return s.trim();
    }
}
