package com.example.dresscode.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.dresscode.data.local.FavoriteEntity;
import com.example.dresscode.data.local.OutfitCardRow;
import com.example.dresscode.data.local.OutfitDetailRow;
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
    private final String owner;
    private final Context appContext;

    public OutfitRepository(Context context, String owner) {
        this.appContext = context.getApplicationContext();
        this.outfitDao = DatabaseProvider.get(appContext).outfitDao();
        this.owner = owner == null ? "" : owner;
        ioExecutor.execute(() -> outfitDao.claimLegacyFavorites(this.owner));
    }

    public LiveData<List<OutfitCardRow>> observeOutfits(
            String query,
            String gender,
            String style,
            String season,
            String scene,
            String weather
    ) {
        return outfitDao.observeOutfits(owner, query, gender, style, season, scene, weather);
    }

    public LiveData<List<OutfitCardRow>> observeFavoriteOutfits() {
        return outfitDao.observeFavoriteOutfits(owner);
    }

    public LiveData<OutfitDetailRow> observeOutfitDetail(long id) {
        return outfitDao.observeOutfitDetail(owner, id);
    }

    public void toggleFavorite(long outfitId, boolean shouldFavorite) {
        ioExecutor.execute(() -> {
            if (shouldFavorite) {
                outfitDao.insertFavorite(new FavoriteEntity(owner, outfitId, System.currentTimeMillis()));
            } else {
                outfitDao.deleteFavoriteByOutfitId(owner, outfitId);
            }
        });
    }

    public void ensureSeeded() {
        ioExecutor.execute(() -> {
            if (outfitDao.countOutfits() > 0) {
                return;
            }
            List<OutfitEntity> seed = loadSeedFromAssets();
            if (seed == null || seed.isEmpty()) {
                seed = fallbackSeed();
            }
            outfitDao.insertAll(seed);
        });
    }

    private List<OutfitEntity> loadSeedFromAssets() {
        try (java.io.InputStream in = appContext.getAssets().open("outfits.json");
             java.io.InputStreamReader reader = new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)) {
            com.google.gson.Gson gson = new com.google.gson.Gson();
            com.google.gson.reflect.TypeToken<List<OutfitSeed>> type = new com.google.gson.reflect.TypeToken<List<OutfitSeed>>() {};
            List<OutfitSeed> seeds = gson.fromJson(reader, type.getType());
            if (seeds == null) {
                return null;
            }
            long now = System.currentTimeMillis();
            List<OutfitEntity> result = new ArrayList<>();
            long delta = 0;
            for (OutfitSeed s : seeds) {
                if (s == null) {
                    continue;
                }
                int coverResId = 0;
                if (s.cover != null && !s.cover.trim().isEmpty()) {
                    coverResId = appContext.getResources().getIdentifier(s.cover.trim(), "drawable", appContext.getPackageName());
                }
                result.add(new OutfitEntity(
                        safe(s.title),
                        safe(s.tags),
                        safe(s.gender),
                        safe(s.style),
                        safe(s.season),
                        safe(s.scene),
                        safe(s.weather),
                        safe(s.colorHex),
                        coverResId,
                        safe(s.tagSource).isEmpty() ? "SEED" : safe(s.tagSource),
                        safe(s.tagModel),
                        safe(s.aiTagsJson),
                        s.tagUpdatedAt <= 0 ? now : s.tagUpdatedAt,
                        now - delta
                ));
                delta += 1000L;
            }
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    private List<OutfitEntity> fallbackSeed() {
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
                com.example.dresscode.R.drawable.ic_outfit_top,
                "SEED",
                "",
                "",
                0L,
                now - 1000L
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
                com.example.dresscode.R.drawable.ic_outfit_dress,
                "SEED",
                "",
                "",
                0L,
                now - 2000L
        ));
        return seed;
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private static class OutfitSeed {
        String title;
        String tags;
        String gender;
        String style;
        String season;
        String scene;
        String weather;
        String colorHex;
        String cover;
        String tagSource;
        String tagModel;
        String aiTagsJson;
        long tagUpdatedAt;
    }
}
