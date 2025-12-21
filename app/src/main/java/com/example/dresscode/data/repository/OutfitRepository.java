package com.example.dresscode.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.dresscode.data.local.FavoriteEntity;
import com.example.dresscode.data.local.OutfitCardRow;
import com.example.dresscode.data.local.OutfitDetailRow;
import com.example.dresscode.data.local.OutfitDao;
import com.example.dresscode.data.local.OutfitEntity;
import com.example.dresscode.data.local.OutfitTagCandidate;
import com.example.dresscode.data.local.DatabaseProvider;
import com.example.dresscode.data.remote.AiTagResponse;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OutfitRepository {
    private final OutfitDao outfitDao;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();
    private final String owner;
    private final Context appContext;
    private final AiTagRepository aiTagRepository;

    public OutfitRepository(Context context, String owner) {
        this.appContext = context.getApplicationContext();
        this.outfitDao = DatabaseProvider.get(appContext).outfitDao();
        this.owner = owner == null ? "" : owner;
        this.aiTagRepository = new AiTagRepository(appContext);
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
            // 1) 首次启动：导入种子
            if (outfitDao.countOutfits() <= 0) {
                List<OutfitEntity> seed = loadSeedFromAssets();
                if (seed == null || seed.isEmpty()) {
                    seed = fallbackSeed();
                }
                outfitDao.insertAll(seed);
            }

            // 2) 后续升级：如果 assets/outfits.json 新增了图片，也自动补齐（避免必须清数据/卸载重装）
            mergeNewSeedsFromAssets();

            // 3) 修复旧数据中被 AI 写坏的筛选字段（以 seed 为准）
            repairSeedFiltersFromAssets();

            autoTagMissingIfNeeded();
        });
    }

    private void mergeNewSeedsFromAssets() {
        try {
            List<OutfitEntity> seeds = loadSeedFromAssets();
            if (seeds == null || seeds.isEmpty()) {
                return;
            }
            List<Integer> existing = outfitDao.listCoverResIds();
            Set<Integer> existingSet = new HashSet<>();
            if (existing != null) {
                existingSet.addAll(existing);
            }
            List<OutfitEntity> toInsert = new ArrayList<>();
            for (OutfitEntity s : seeds) {
                if (s == null || s.coverResId == 0) {
                    continue;
                }
                if (!existingSet.contains(s.coverResId)) {
                    // 新增：插入到 DB，createdAt 用当前时间递减，保证排序靠前一点
                    toInsert.add(new OutfitEntity(
                            s.title,
                            s.tags,
                            s.gender,
                            s.style,
                            s.season,
                            s.scene,
                            s.weather,
                            s.colorHex,
                            s.coverResId,
                            s.tagSource,
                            s.tagModel,
                            s.aiTagsJson,
                            0L,
                            System.currentTimeMillis()
                    ));
                }
            }
            if (!toInsert.isEmpty()) {
                outfitDao.insertAll(toInsert);
            }
        } catch (Exception ignored) {
        }
    }

    private void repairSeedFiltersFromAssets() {
        try {
            List<OutfitEntity> seeds = loadSeedFromAssets();
            if (seeds == null || seeds.isEmpty()) {
                return;
            }
            for (OutfitEntity s : seeds) {
                if (s == null || s.coverResId == 0) {
                    continue;
                }
                // 直接覆盖为 seed 的筛选字段，确保“男/女/风格/季节/场景/天气”可用
                outfitDao.updateFiltersByCoverResId(
                        s.coverResId,
                        sanitizeGender(s.gender, "UNISEX"),
                        sanitizeStyle(s.style, ""),
                        sanitizeSeason(s.season, ""),
                        sanitizeScene(s.scene, ""),
                        sanitizeWeather(s.weather, "")
                );
            }
        } catch (Exception ignored) {
        }
    }

    private void autoTagMissingIfNeeded() {
        // 异步为穿搭封面图打标签（写入 aiTagsJson），不阻塞 UI
        List<OutfitTagCandidate> candidates = outfitDao.listAiTagCandidates(12);
        if (candidates == null || candidates.isEmpty()) {
            return;
        }
        for (OutfitTagCandidate c : candidates) {
            if (c == null || c.id <= 0 || c.coverResId == 0) {
                continue;
            }
            networkExecutor.execute(() -> {
                try {
                    byte[] jpeg = materializeCoverToJpeg(c.coverResId);
                    if (jpeg == null || jpeg.length == 0) {
                        return;
                    }
                    AiTagResponse resp = aiTagRepository.tagJpegBytesSync(jpeg);
                    if (resp == null || !resp.ok || resp.result == null) {
                        return;
                    }
                    String model = resp.model == null ? "" : resp.model;
                    String json = resp.result.toString();
                    AiOutfitFields fields = parseOutfitFieldsFromAi(json);
                    String tagSource = safe(c.tagSource);

                    // 对 SEED 数据：避免 AI 误判导致“女装筛选为空”等问题，优先保留原有筛选字段
                    boolean keepSeedFilters = !tagSource.isEmpty() && tagSource.toUpperCase(Locale.US).startsWith("SEED");
                    String genderValue = keepSeedFilters ? safe(c.gender) : safe(fields.gender);
                    String styleValue = keepSeedFilters ? safe(c.style) : safe(fields.style);
                    String seasonValue = keepSeedFilters ? safe(c.season) : safe(fields.season);
                    String sceneValue = keepSeedFilters ? safe(c.scene) : safe(fields.scene);
                    String weatherValue = keepSeedFilters ? safe(c.weather) : safe(fields.weather);

                    // 若 AI 输出不在筛选选项内，则回退原值，确保“按风格/季节/场景/天气”能筛出结果
                    final String gender = sanitizeGender(genderValue, safe(c.gender));
                    final String style = sanitizeStyle(styleValue, safe(c.style));
                    final String season = sanitizeSeason(seasonValue, safe(c.season));
                    final String scene = sanitizeScene(sceneValue, safe(c.scene));
                    final String weather = sanitizeWeather(weatherValue, safe(c.weather));
                    final String finalTagSource = tagSource.isEmpty() ? "AI" : tagSource;

                    ioExecutor.execute(() -> outfitDao.updateFromAi(
                            c.id,
                            safe(fields.title),
                            safe(fields.tags),
                            gender,
                            style,
                            season,
                            scene,
                            weather,
                            finalTagSource,
                            model,
                            json,
                            System.currentTimeMillis()
                    ));
                } catch (Exception ignored) {
                }
            });
        }
    }

    private AiOutfitFields parseOutfitFieldsFromAi(String resultJson) {
        AiOutfitFields fields = new AiOutfitFields();
        fields.title = "AI 识别中";
        fields.tags = "AI 识别中";
        fields.gender = "UNISEX";
        fields.style = "";
        fields.season = "";
        fields.scene = "";
        fields.weather = "";

        if (resultJson == null || resultJson.trim().isEmpty()) {
            return fields;
        }
        try {
            JsonElement el = JsonParser.parseString(resultJson);
            if (!el.isJsonObject()) {
                return fields;
            }
            JsonObject obj = el.getAsJsonObject();
            String title = pickString(obj, "title");
            String tags = pickString(obj, "tags");
            String gender = pickString(obj, "gender");
            String style = mapStyle(pickString(obj, "style"));
            String season = mapSeason(pickString(obj, "season"));
            String scene = mapScene(pickString(obj, "scene"));
            String weather = mapWeather(pickString(obj, "weather"));

            if (title.isEmpty()) {
                title = inferTitle(obj, style, scene);
            }
            if (tags.isEmpty()) {
                tags = buildTags(style, season, scene);
            } else {
                tags = normalizeTags(tags);
            }
            if (gender.isEmpty()) {
                gender = "UNISEX";
            }

            fields.title = title.isEmpty() ? fields.title : title;
            fields.tags = tags.isEmpty() ? fields.tags : tags;
            fields.gender = normalizeGender(gender);
            fields.style = style;
            fields.season = season;
            fields.scene = scene;
            fields.weather = weather;
        } catch (Exception ignored) {
        }
        return fields;
    }

    private String inferTitle(JsonObject obj, String style, String scene) {
        String category = pickString(obj, "category");
        category = mapCategory(category);
        if (!category.isEmpty() && !style.isEmpty()) {
            return style + category;
        }
        if (!category.isEmpty()) {
            return category + "搭配";
        }
        if (!scene.isEmpty() && !style.isEmpty()) {
            return scene + style + "穿搭";
        }
        return "穿搭推荐";
    }

    private String buildTags(String style, String season, String scene) {
        List<String> parts = new ArrayList<>();
        if (scene != null && !scene.trim().isEmpty()) {
            parts.add(scene.trim());
        }
        if (style != null && !style.trim().isEmpty()) {
            parts.add(style.trim());
        }
        if (season != null && !season.trim().isEmpty()) {
            parts.add(season.trim());
        }
        if (parts.isEmpty()) {
            return "AI 识别中";
        }
        return String.join(" · ", parts);
    }

    private String normalizeTags(String raw) {
        String t = safe(raw);
        if (t.isEmpty()) {
            return "";
        }
        t = t.replace("|", "·").replace("•", "·").replace("·", " · ");
        t = t.replaceAll("\\s+", " ").trim();
        t = t.replaceAll("\\s*·\\s*", " · ").trim();
        return t;
    }

    private String normalizeGender(String raw) {
        String g = safe(raw).toUpperCase(Locale.US);
        if (g.contains("MALE") || g.equals("M")) {
            return "MALE";
        }
        if (g.contains("FEMALE") || g.equals("F")) {
            return "FEMALE";
        }
        return "UNISEX";
    }

    private String sanitizeGender(String value, String fallback) {
        String v = normalizeGender(value);
        if ("MALE".equals(v) || "FEMALE".equals(v) || "UNISEX".equals(v)) {
            return v;
        }
        return normalizeGender(fallback);
    }

    private String mapCategory(String raw) {
        String s = safe(raw);
        if (s.isEmpty()) {
            return "";
        }
        String lower = s.toLowerCase(Locale.US);
        if (lower.contains("top") || lower.contains("shirt")) {
            return "上衣";
        }
        if (lower.contains("bottom") || lower.contains("pants") || lower.contains("jeans")) {
            return "下装";
        }
        if (lower.contains("outer") || lower.contains("coat") || lower.contains("jacket")) {
            return "外套";
        }
        if (lower.contains("dress")) {
            return "连衣裙";
        }
        if (lower.contains("shoe") || lower.contains("sneaker") || lower.contains("boot")) {
            return "鞋子";
        }
        if (lower.contains("accessory") || lower.contains("bag")) {
            return "配饰";
        }
        return s;
    }

    private String mapStyle(String raw) {
        String s = safe(raw);
        if (s.isEmpty()) {
            return "";
        }
        String lower = s.toLowerCase(Locale.US);
        if (lower.contains("casual")) {
            return "休闲";
        }
        if (lower.contains("commute") || lower.contains("work")) {
            return "通勤";
        }
        if (lower.contains("sport")) {
            return "运动";
        }
        if (lower.contains("date") || lower.contains("romantic")) {
            return "约会";
        }
        if (lower.contains("street") || lower.contains("streetwear") || lower.contains("hiphop")) {
            return "街头";
        }
        if (lower.contains("techwear") || lower.contains("functional") || lower.contains("outdoor") || lower.contains("utility")) {
            return "机能";
        }
        return s;
    }

    private String sanitizeStyle(String value, String fallback) {
        String v = safe(value);
        if (v.isEmpty()) {
            v = safe(fallback);
        }
        if (v.isEmpty()) {
            return "";
        }
        v = mapStyle(v);
        switch (v) {
            case "通勤":
            case "休闲":
            case "运动":
            case "约会":
            case "街头":
            case "机能":
                return v;
            default:
                return safe(fallback);
        }
    }

    private String mapSeason(String raw) {
        String s = safe(raw);
        if (s.isEmpty()) {
            return "";
        }
        String lower = s.toLowerCase(Locale.US);
        if (lower.contains("spring") && lower.contains("summer")) {
            return "春夏";
        }
        if (lower.contains("autumn") && lower.contains("winter")) {
            return "秋冬";
        }
        if (lower.contains("spring") && lower.contains("autumn")) {
            return "春秋";
        }
        if (lower.contains("winter")) {
            return "冬";
        }
        if (lower.contains("summer")) {
            return "夏";
        }
        if (lower.contains("spring")) {
            return "春";
        }
        if (lower.contains("autumn") || lower.contains("fall")) {
            return "秋";
        }
        if (lower.contains("all")) {
            return "春秋";
        }
        return s;
    }

    private String sanitizeSeason(String value, String fallback) {
        String v = safe(value);
        if (v.isEmpty()) {
            v = safe(fallback);
        }
        if (v.isEmpty()) {
            return "";
        }
        v = mapSeason(v);
        switch (v) {
            case "春":
            case "夏":
            case "秋":
            case "冬":
            case "春夏":
            case "春秋":
            case "秋冬":
                return v;
            default:
                return safe(fallback);
        }
    }

    private String mapScene(String raw) {
        String s = safe(raw);
        if (s.isEmpty()) {
            return "";
        }
        String lower = s.toLowerCase(Locale.US);
        if (lower.contains("everyday") || lower.contains("daily")) {
            return "出街";
        }
        if (lower.contains("campus") || lower.contains("school")) {
            return "校园";
        }
        if (lower.contains("commute") || lower.contains("work")) {
            return "通勤";
        }
        if (lower.contains("sport")) {
            return "运动";
        }
        if (lower.contains("date")) {
            return "约会";
        }
        return s;
    }

    private String sanitizeScene(String value, String fallback) {
        String v = safe(value);
        if (v.isEmpty()) {
            v = safe(fallback);
        }
        if (v.isEmpty()) {
            return "";
        }
        v = mapScene(v);
        switch (v) {
            case "通勤":
            case "校园":
            case "约会":
            case "运动":
            case "出街":
                return v;
            default:
                return safe(fallback);
        }
    }

    private String mapWeather(String raw) {
        String s = safe(raw);
        if (s.isEmpty()) {
            return "";
        }
        String lower = s.toLowerCase(Locale.US);
        if (lower.contains("rain")) {
            return "雨";
        }
        if (lower.contains("cloud")) {
            return "多云";
        }
        if (lower.contains("hot") || lower.contains("warm")) {
            return "热";
        }
        if (lower.contains("cold")) {
            return "冷";
        }
        if (lower.contains("sun") || lower.contains("clear")) {
            return "晴";
        }
        return s;
    }

    private String sanitizeWeather(String value, String fallback) {
        String v = safe(value);
        if (v.isEmpty()) {
            v = safe(fallback);
        }
        if (v.isEmpty()) {
            return "";
        }
        v = mapWeather(v);
        switch (v) {
            case "晴":
            case "多云":
            case "雨":
            case "冷":
            case "热":
                return v;
            default:
                return safe(fallback);
        }
    }

    private String pickString(JsonObject obj, String key) {
        try {
            if (obj == null || key == null) {
                return "";
            }
            JsonElement el = obj.get(key);
            if (el == null || el.isJsonNull()) {
                return "";
            }
            String v = el.getAsString();
            return v == null ? "" : v.trim();
        } catch (Exception e) {
            return "";
        }
    }

    private byte[] materializeCoverToJpeg(int coverResId) {
        try {
            android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeResource(appContext.getResources(), coverResId);
            if (bmp == null) {
                return null;
            }
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, out);
            return out.toByteArray();
        } catch (Exception e) {
            return null;
        }
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

    private static class AiOutfitFields {
        String title;
        String tags;
        String gender;
        String style;
        String season;
        String scene;
        String weather;
    }
}
