package com.example.dresscode.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.dresscode.data.local.FavoriteEntity;
import com.example.dresscode.data.local.OutfitCardRow;
import com.example.dresscode.data.local.OutfitDao;
import com.example.dresscode.data.local.OutfitEntity;
import com.example.dresscode.data.local.DatabaseProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OutfitRepository {
    private final OutfitDao outfitDao;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    public OutfitRepository(Context context) {
        this.outfitDao = DatabaseProvider.get(context).outfitDao();
    }

    public LiveData<List<OutfitCardRow>> observeOutfits(
            String query,
            String gender,
            String style,
            String season,
            String scene,
            String weather
    ) {
        return outfitDao.observeOutfits(query, gender, style, season, scene, weather);
    }

    public LiveData<List<OutfitCardRow>> observeFavoriteOutfits() {
        return outfitDao.observeFavoriteOutfits();
    }

    public void toggleFavorite(long outfitId, boolean shouldFavorite) {
        ioExecutor.execute(() -> {
            if (shouldFavorite) {
                outfitDao.insertFavorite(new FavoriteEntity(outfitId, System.currentTimeMillis()));
            } else {
                outfitDao.deleteFavoriteByOutfitId(outfitId);
            }
        });
    }

    public void ensureSeeded() {
        ioExecutor.execute(() -> {
            if (outfitDao.countOutfits() > 0) {
                return;
            }
            long now = System.currentTimeMillis();
            List<OutfitEntity> seed = new ArrayList<>();
            seed.add(new OutfitEntity(
                    "通勤白衬衫 + 西裤",
                    "通勤 · 极简 · 显瘦",
                    "MALE",
                    "通勤",
                    "春秋",
                    "通勤",
                    "晴",
                    "#E3F2FD",
                    now - 1000L
            ));
            seed.add(new OutfitEntity(
                    "运动速干套装",
                    "运动 · 透气 · 轻便",
                    "UNISEX",
                    "运动",
                    "夏",
                    "运动",
                    "热",
                    "#E8F5E9",
                    now - 2000L
            ));
            seed.add(new OutfitEntity(
                    "法式连衣裙",
                    "约会 · 温柔 · 气质",
                    "FEMALE",
                    "约会",
                    "春夏",
                    "约会",
                    "晴",
                    "#FCE4EC",
                    now - 3000L
            ));
            seed.add(new OutfitEntity(
                    "卫衣 + 牛仔裤",
                    "休闲 · 校园 · 百搭",
                    "UNISEX",
                    "休闲",
                    "春秋",
                    "校园",
                    "多云",
                    "#FFF3E0",
                    now - 4000L
            ));
            seed.add(new OutfitEntity(
                    "风衣 + 针织衫",
                    "通勤 · 叠穿 · 有层次",
                    "UNISEX",
                    "通勤",
                    "秋",
                    "通勤",
                    "冷",
                    "#EDE7F6",
                    now - 5000L
            ));
            seed.add(new OutfitEntity(
                    "针织开衫 + 半裙",
                    "温柔 · 约会 · 显气质",
                    "FEMALE",
                    "约会",
                    "秋",
                    "约会",
                    "多云",
                    "#F3E5F5",
                    now - 6000L
            ));
            seed.add(new OutfitEntity(
                    "工装外套 + 束脚裤",
                    "街头 · 运动 · 酷飒",
                    "MALE",
                    "街头",
                    "春秋",
                    "出街",
                    "多云",
                    "#ECEFF1",
                    now - 7000L
            ));
            seed.add(new OutfitEntity(
                    "雨天防水外套",
                    "雨天 · 实用 · 防风",
                    "UNISEX",
                    "机能",
                    "秋冬",
                    "通勤",
                    "雨",
                    "#E1F5FE",
                    now - 8000L
            ));
            outfitDao.insertAll(seed);
        });
    }
}

