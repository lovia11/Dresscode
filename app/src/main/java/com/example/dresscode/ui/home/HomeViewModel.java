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
import com.example.dresscode.data.repository.AiRecommendRepository;
import com.example.dresscode.data.remote.AiRecommendResponse;
import com.example.dresscode.model.RecommendItem;

import java.util.ArrayList;
import java.util.List;

public class HomeViewModel extends AndroidViewModel {

    private final androidx.lifecycle.MediatorLiveData<List<RecommendItem>> recommendations = new androidx.lifecycle.MediatorLiveData<>();
    private final androidx.lifecycle.MutableLiveData<String> tipsText = new androidx.lifecycle.MutableLiveData<>();

    private final UserPreferencesRepository userPrefs;
    private final WeatherPreferencesRepository weatherPrefs;
    private final AiRecommendRepository aiRecommendRepository;

    private List<ClosetItemEntity> closetItems = new ArrayList<>();
    private String gender = "";
    private WeatherPreferencesRepository.Snapshot weatherSnapshot = new WeatherPreferencesRepository.Snapshot("", "", "", "");

    private long lastAiRecommendAt = 0;
    private int aiSeq = 0;
    private String lastAiRequestKey = "";

    public HomeViewModel(@NonNull Application application) {
        super(application);
        String owner = new AuthRepository(application).getCurrentUsernameOrEmpty();
        ClosetRepository repository = new ClosetRepository(application, owner);
        userPrefs = new UserPreferencesRepository(application);
        weatherPrefs = new WeatherPreferencesRepository(application);
        aiRecommendRepository = new AiRecommendRepository(application);

        recommendations.addSource(repository.observeAll(), items -> {
            closetItems = items == null ? new ArrayList<>() : items;
            update();
        });
        recommendations.addSource(userPrefs.observeGender(), g -> {
            gender = g == null ? "" : g;
            update();
        });
        recommendations.addSource(weatherPrefs.observeSnapshot(), s -> {
            weatherSnapshot = s == null ? new WeatherPreferencesRepository.Snapshot("", "", "", "") : s;
            update();
        });
        update();
    }

    public LiveData<List<RecommendItem>> getRecommendations() {
        return recommendations;
    }

    public LiveData<String> getTipsText() {
        return tipsText;
    }

    private void update() {
        List<RecommendItem> local = buildRecommendations(closetItems, gender, weatherSnapshot);
        recommendations.setValue(local);
        // tips 卡片优先显示“本地可用”的内容，联网成功后再覆盖
        if (tipsText.getValue() == null || tipsText.getValue().trim().isEmpty()) {
            tipsText.setValue(getApplication().getString(R.string.tips_content_sample));
        }
        requestAiRecommendIfNeeded();
    }

    private List<RecommendItem> buildRecommendations(List<ClosetItemEntity> closetItems, String gender, WeatherPreferencesRepository.Snapshot snapshot) {
        List<RecommendItem> result = new ArrayList<>();
        String city = snapshot.city == null || snapshot.city.trim().isEmpty() ? "杭州" : snapshot.city.trim();
        String temp = snapshot.temp == null || snapshot.temp.trim().isEmpty() ? "--℃" : snapshot.temp.trim();
        String desc = snapshot.desc == null || snapshot.desc.trim().isEmpty() ? "天气" : snapshot.desc.trim();
        String genderLabel = genderLabel(gender);
        String weatherMeta = city + " " + temp + " · " + desc + " · " + genderLabel;

        if (closetItems == null || closetItems.isEmpty()) {
            result.add(new RecommendItem(
                    getApplication().getString(R.string.title_recommend_today),
                    getApplication().getString(R.string.placeholder_recommend_empty),
                    null,
                    com.example.dresscode.R.drawable.ic_outfit_outer
            ));
            return result;
        }

        String seasonHint = seasonFromTemp(temp);
        boolean rainy = desc.contains("雨");
        boolean cold = parseTemp(temp) <= 10;

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

    private void requestAiRecommendIfNeeded() {
        if (closetItems == null || closetItems.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();

        String city = safe(weatherSnapshot.city);
        String temp = safe(weatherSnapshot.temp);
        String desc = safe(weatherSnapshot.desc);
        String g = gender == null || gender.trim().isEmpty() ? "UNISEX" : gender.trim();
        String key = city + "|" + temp + "|" + desc + "|" + g + "|" + closetItems.size();
        if (key.equals(lastAiRequestKey) && now - lastAiRecommendAt < 4000) {
            return;
        }
        lastAiRequestKey = key;
        lastAiRecommendAt = now;

        int seq = ++aiSeq;

        aiRecommendRepository.recommend(closetItems, g, weatherSnapshot, new AiRecommendRepository.ResultCallback() {
            @Override
            public void onSuccess(AiRecommendResponse.Result result) {
                if (seq != aiSeq || result == null) {
                    return;
                }
                applyAiResult(result);
            }

            @Override
            public void onError(String message) {
                // 保持本地推荐即可
            }
        });
    }

    private void applyAiResult(AiRecommendResponse.Result result) {
        List<RecommendItem> items = new ArrayList<>();
        if (result.items != null) {
            for (AiRecommendResponse.Item it : result.items) {
                if (it == null) {
                    continue;
                }
                String cat = it.category == null ? "" : it.category.trim();
                String reason = it.reason == null ? "" : it.reason.trim();
                String imageUri = findFirstImageUriByCategory(cat);
                String displayTitle = cat.isEmpty() ? "推荐单品" : ("推荐单品：" + cat);
                items.add(new RecommendItem(displayTitle, reason.isEmpty() ? "来自后端推荐" : reason, imageUri));
            }
        }

        // 只展示“衣物卡片”，不展示标题/总结卡片；如果模型没给出 items，就保留本地推荐
        if (items.isEmpty()) {
            return;
        }
        recommendations.setValue(items);
        tipsText.setValue(formatTips(result));
    }

    private String findFirstImageUriByCategory(String category) {
        if (category == null || category.trim().isEmpty() || closetItems == null) {
            return null;
        }
        String c = category.trim();
        for (ClosetItemEntity it : closetItems) {
            if (it == null) {
                continue;
            }
            if (c.equals(it.category)) {
                return it.imageUri;
            }
        }
        return null;
    }

    private String formatTips(AiRecommendResponse.Result result) {
        // 只输出“正文 tips”，不输出“根据天气为...”这类总结废话
        final int maxTips = 4;
        final int maxTipChars = 36;
        StringBuilder sb = new StringBuilder();
        if (result.tips != null && !result.tips.isEmpty()) {
            int added = 0;
            for (String t : result.tips) {
                if (t == null || t.trim().isEmpty()) {
                    continue;
                }
                String line = normalizeTipLine(t);
                if (line.isEmpty()) {
                    continue;
                }
                sb.append("· ").append(trimTo(line, maxTipChars)).append("\n");
                added++;
                if (added >= maxTips) {
                    break;
                }
            }
        }
        String out = sb.toString().trim();
        return out.isEmpty() ? getApplication().getString(R.string.tips_content_sample) : out;
    }

    private String normalizeTipLine(String s) {
        if (s == null) {
            return "";
        }
        String t = s.trim();
        // 模型可能输出包含换行/空行，这里压成一行，避免 UI 被“空行”撑高
        t = t.replace("\r", " ").replace("\n", " ");
        t = t.replaceAll("\\s+", " ").trim();
        // 去掉常见“废话”前缀
        String[] prefixes = new String[]{
                "根据当前天气",
                "根据当前气温",
                "根据天气",
                "基于当前天气",
                "综合当前天气"
        };
        for (String p : prefixes) {
            if (t.startsWith(p)) {
                int idx = firstPunctIndex(t);
                if (idx >= 0 && idx + 1 < t.length()) {
                    t = t.substring(idx + 1).trim();
                }
                break;
            }
        }
        if (t.startsWith("·")) {
            t = t.substring(1).trim();
        }
        return t;
    }

    private int firstPunctIndex(String s) {
        if (s == null) {
            return -1;
        }
        int best = -1;
        for (char c : new char[]{'，', '。', '：', ':', ';', '；'}) {
            int i = s.indexOf(c);
            if (i >= 0 && (best < 0 || i < best)) {
                best = i;
            }
        }
        return best;
    }

    private String trimTo(String s, int maxChars) {
        if (s == null) {
            return "";
        }
        String t = s.trim();
        if (t.length() <= maxChars) {
            return t;
        }
        return t.substring(0, Math.max(0, maxChars - 1)) + "…";
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
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
