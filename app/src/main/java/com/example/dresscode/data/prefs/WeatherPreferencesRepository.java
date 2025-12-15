package com.example.dresscode.data.prefs;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class WeatherPreferencesRepository {
    private static final String PREFS_NAME = "dresscode_prefs";
    private static final String KEY_CITY = "weather_city";

    private final SharedPreferences prefs;
    private final MutableLiveData<String> cityLiveData = new MutableLiveData<>();
    private boolean listenerRegistered = false;

    private final SharedPreferences.OnSharedPreferenceChangeListener listener = (p, key) -> {
        if (KEY_CITY.equals(key)) {
            cityLiveData.postValue(readCity(p));
        }
    };

    public WeatherPreferencesRepository(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        cityLiveData.setValue(readCity(prefs));
        prefs.registerOnSharedPreferenceChangeListener(listener);
        listenerRegistered = true;
    }

    public LiveData<String> observeCity() {
        return cityLiveData;
    }

    public String getCity() {
        return readCity(prefs);
    }

    public void setCity(String city) {
        prefs.edit().putString(KEY_CITY, city == null ? "" : city).apply();
    }

    private String readCity(SharedPreferences p) {
        String value = p.getString(KEY_CITY, "");
        return value == null ? "" : value;
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

