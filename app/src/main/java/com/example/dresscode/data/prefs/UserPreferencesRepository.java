package com.example.dresscode.data.prefs;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class UserPreferencesRepository {

    public static final String GENDER_UNSET = "";
    public static final String GENDER_MALE = "MALE";
    public static final String GENDER_FEMALE = "FEMALE";

    private static final String PREFS_NAME = "dresscode_prefs";
    private static final String LEGACY_KEY_GENDER = "gender";

    private final SharedPreferences prefs;
    private final MutableLiveData<String> genderLiveData = new MutableLiveData<>();

    private boolean listenerRegistered = false;
    private String genderKey = LEGACY_KEY_GENDER;

    private final SharedPreferences.OnSharedPreferenceChangeListener listener = (p, key) -> {
        if (genderKey.equals(key)) {
            genderLiveData.postValue(p.getString(genderKey, GENDER_UNSET));
        }
    };

    public UserPreferencesRepository(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String owner = new AuthRepository(context).getCurrentUsernameOrEmpty();
        genderKey = buildGenderKey(owner);
        migrateLegacyIfNeeded();
        genderLiveData.setValue(prefs.getString(genderKey, GENDER_UNSET));
        prefs.registerOnSharedPreferenceChangeListener(listener);
        listenerRegistered = true;
    }

    public LiveData<String> observeGender() {
        return genderLiveData;
    }

    public String getGender() {
        String value = prefs.getString(genderKey, GENDER_UNSET);
        return value == null ? GENDER_UNSET : value;
    }

    public void setGender(String gender) {
        prefs.edit().putString(genderKey, gender == null ? GENDER_UNSET : gender).apply();
    }

    private void migrateLegacyIfNeeded() {
        if (prefs.contains(genderKey)) {
            return;
        }
        if (!prefs.contains(LEGACY_KEY_GENDER)) {
            return;
        }
        String legacy = prefs.getString(LEGACY_KEY_GENDER, GENDER_UNSET);
        prefs.edit().putString(genderKey, legacy == null ? GENDER_UNSET : legacy).apply();
    }

    private String buildGenderKey(String owner) {
        if (owner == null || owner.trim().isEmpty()) {
            return LEGACY_KEY_GENDER;
        }
        return "gender_" + owner.trim();
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
