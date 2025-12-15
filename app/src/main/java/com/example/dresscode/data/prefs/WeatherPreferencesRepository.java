package com.example.dresscode.data.prefs;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class WeatherPreferencesRepository {
    private static final String PREFS_NAME = "dresscode_prefs";
    private static final String LEGACY_KEY_CITY = "weather_city";

    private final SharedPreferences prefs;
    private final MutableLiveData<String> cityLiveData = new MutableLiveData<>();
    private boolean listenerRegistered = false;
    private String cityKey = LEGACY_KEY_CITY;

    private final SharedPreferences.OnSharedPreferenceChangeListener listener = (p, key) -> {
        if (cityKey.equals(key)) {
            cityLiveData.postValue(readCity(p, cityKey));
        }
    };

    public WeatherPreferencesRepository(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String owner = new AuthRepository(context).getCurrentUsernameOrEmpty();
        cityKey = buildCityKey(owner);
        migrateLegacyIfNeeded();
        cityLiveData.setValue(readCity(prefs, cityKey));
        prefs.registerOnSharedPreferenceChangeListener(listener);
        listenerRegistered = true;
    }

    public LiveData<String> observeCity() {
        return cityLiveData;
    }

    public String getCity() {
        return readCity(prefs, cityKey);
    }

    public void setCity(String city) {
        prefs.edit().putString(cityKey, city == null ? "" : city).apply();
    }

    private String readCity(SharedPreferences p, String key) {
        String value = p.getString(key, "");
        return value == null ? "" : value;
    }

    private void migrateLegacyIfNeeded() {
        if (prefs.contains(cityKey)) {
            return;
        }
        if (!prefs.contains(LEGACY_KEY_CITY)) {
            return;
        }
        String legacy = prefs.getString(LEGACY_KEY_CITY, "");
        prefs.edit().putString(cityKey, legacy == null ? "" : legacy).apply();
    }

    private String buildCityKey(String owner) {
        if (owner == null || owner.trim().isEmpty()) {
            return LEGACY_KEY_CITY;
        }
        return "weather_city_" + owner.trim();
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
