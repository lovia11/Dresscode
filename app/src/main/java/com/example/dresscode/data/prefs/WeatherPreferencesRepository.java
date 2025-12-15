package com.example.dresscode.data.prefs;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class WeatherPreferencesRepository {
    private static final String PREFS_NAME = "dresscode_prefs";
    private static final String LEGACY_KEY_CITY = "weather_city";
    private static final String LEGACY_KEY_TEMP = "weather_temp";
    private static final String LEGACY_KEY_DESC = "weather_desc";
    private static final String LEGACY_KEY_AQI = "weather_aqi";

    private final SharedPreferences prefs;
    private final MutableLiveData<String> cityLiveData = new MutableLiveData<>();
    private final MutableLiveData<Snapshot> snapshotLiveData = new MutableLiveData<>();
    private boolean listenerRegistered = false;
    private String cityKey = LEGACY_KEY_CITY;
    private String tempKey = LEGACY_KEY_TEMP;
    private String descKey = LEGACY_KEY_DESC;
    private String aqiKey = LEGACY_KEY_AQI;

    private final SharedPreferences.OnSharedPreferenceChangeListener listener = (p, key) -> {
        if (cityKey.equals(key)) {
            cityLiveData.postValue(readCity(p, cityKey));
        }
        if (cityKey.equals(key) || tempKey.equals(key) || descKey.equals(key) || aqiKey.equals(key)) {
            snapshotLiveData.postValue(readSnapshot(p));
        }
    };

    public WeatherPreferencesRepository(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String owner = new AuthRepository(context).getCurrentUsernameOrEmpty();
        cityKey = buildCityKey(owner);
        tempKey = buildKey(owner, LEGACY_KEY_TEMP);
        descKey = buildKey(owner, LEGACY_KEY_DESC);
        aqiKey = buildKey(owner, LEGACY_KEY_AQI);
        migrateLegacyIfNeeded();
        cityLiveData.setValue(readCity(prefs, cityKey));
        snapshotLiveData.setValue(readSnapshot(prefs));
        prefs.registerOnSharedPreferenceChangeListener(listener);
        listenerRegistered = true;
    }

    public LiveData<String> observeCity() {
        return cityLiveData;
    }

    public LiveData<Snapshot> observeSnapshot() {
        return snapshotLiveData;
    }

    public String getCity() {
        return readCity(prefs, cityKey);
    }

    public void setCity(String city) {
        prefs.edit().putString(cityKey, city == null ? "" : city).apply();
    }

    public void setCachedWeather(String temp, String desc, String aqi) {
        prefs.edit()
                .putString(tempKey, temp == null ? "" : temp)
                .putString(descKey, desc == null ? "" : desc)
                .putString(aqiKey, aqi == null ? "" : aqi)
                .apply();
    }

    private String readCity(SharedPreferences p, String key) {
        String value = p.getString(key, "");
        return value == null ? "" : value;
    }

    private Snapshot readSnapshot(SharedPreferences p) {
        String city = readCity(p, cityKey);
        String temp = safe(p.getString(tempKey, ""));
        String desc = safe(p.getString(descKey, ""));
        String aqi = safe(p.getString(aqiKey, ""));
        return new Snapshot(city, temp, desc, aqi);
    }

    private void migrateLegacyIfNeeded() {
        if (prefs.contains(cityKey)) {
            return;
        }
        if (!prefs.contains(LEGACY_KEY_CITY)) {
            return;
        }
        String legacy = prefs.getString(LEGACY_KEY_CITY, "");
        prefs.edit()
                .putString(cityKey, legacy == null ? "" : legacy)
                .putString(tempKey, safe(prefs.getString(LEGACY_KEY_TEMP, "")))
                .putString(descKey, safe(prefs.getString(LEGACY_KEY_DESC, "")))
                .putString(aqiKey, safe(prefs.getString(LEGACY_KEY_AQI, "")))
                .apply();
    }

    private String buildCityKey(String owner) {
        if (owner == null || owner.trim().isEmpty()) {
            return LEGACY_KEY_CITY;
        }
        return "weather_city_" + owner.trim();
    }

    private String buildKey(String owner, String legacyKey) {
        if (owner == null || owner.trim().isEmpty()) {
            return legacyKey;
        }
        return legacyKey + "_" + owner.trim();
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    public static class Snapshot {
        public final String city;
        public final String temp;
        public final String desc;
        public final String aqi;

        public Snapshot(String city, String temp, String desc, String aqi) {
            this.city = city == null ? "" : city;
            this.temp = temp == null ? "" : temp;
            this.desc = desc == null ? "" : desc;
            this.aqi = aqi == null ? "" : aqi;
        }
    }

    public void close() {
        if (listenerRegistered) {
            try {
                prefs.unregisterOnSharedPreferenceChangeListener(listener);
            } catch (Exception ignored) {
            }
            listenerRegistered = false;
        }
    }
}
