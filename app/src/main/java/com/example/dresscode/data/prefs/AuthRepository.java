package com.example.dresscode.data.prefs;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class AuthRepository {
    private static final String PREFS_NAME = "dresscode_prefs";

    private static final String KEY_CURRENT_USER = "auth_current_user";
    private static final String KEY_LOGGED_IN = "auth_logged_in";
    private static final String KEY_USERNAMES = "auth_usernames";

    private static final String LEGACY_KEY_USERNAME = "auth_username";
    private static final String LEGACY_KEY_PASSWORD_HASH = "auth_password_hash";

    private final SharedPreferences prefs;

    public AuthRepository(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        migrateLegacyIfNeeded();
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_LOGGED_IN, false) && getCurrentUsername() != null;
    }

    @Nullable
    public String getCurrentUsername() {
        String u = prefs.getString(KEY_CURRENT_USER, null);
        return u == null || u.trim().isEmpty() ? null : u.trim();
    }

    @NonNull
    public String getCurrentUsernameOrEmpty() {
        String u = getCurrentUsername();
        return u == null ? "" : u;
    }

    public boolean canLogin() {
        Set<String> users = prefs.getStringSet(KEY_USERNAMES, null);
        return users != null && !users.isEmpty();
    }

    public List<String> getAllUsernames() {
        Set<String> users = prefs.getStringSet(KEY_USERNAMES, null);
        if (users == null || users.isEmpty()) {
            return Collections.emptyList();
        }
        ArrayList<String> list = new ArrayList<>(users);
        Collections.sort(list);
        return list;
    }

    public void register(String username, String password, String nickname) {
        String u = safe(username);
        if (u.isEmpty() || hasUser(u)) {
            return;
        }
        String hash = sha256(safe(password));
        prefs.edit()
                .putString(userPasswordHashKey(u), hash)
                .putString(userNicknameKey(u), safe(nickname))
                .putString(userAvatarUriKey(u), "")
                .putString(KEY_CURRENT_USER, u)
                .putBoolean(KEY_LOGGED_IN, true)
                .putStringSet(KEY_USERNAMES, addToSet(prefs.getStringSet(KEY_USERNAMES, null), u))
                .apply();
    }

    public void register(String username, String password) {
        register(username, password, "");
    }

    public boolean login(String username, String password) {
        String u = safe(username);
        String storedHash = safe(prefs.getString(userPasswordHashKey(u), ""));
        boolean ok = !storedHash.isEmpty() && sha256(safe(password)).equals(storedHash);
        prefs.edit()
                .putBoolean(KEY_LOGGED_IN, ok)
                .putString(KEY_CURRENT_USER, ok ? u : prefs.getString(KEY_CURRENT_USER, null))
                .apply();
        return ok;
    }

    public void logout() {
        prefs.edit().putBoolean(KEY_LOGGED_IN, false).apply();
    }

    public boolean hasUser(String username) {
        String u = safe(username);
        return !u.isEmpty() && prefs.contains(userPasswordHashKey(u));
    }

    @Nullable
    public String getNickname() {
        String u = getCurrentUsername();
        if (u == null) {
            return null;
        }
        return prefs.getString(userNicknameKey(u), "");
    }

    public void setNickname(String nickname) {
        String u = getCurrentUsername();
        if (u == null) {
            return;
        }
        prefs.edit().putString(userNicknameKey(u), safe(nickname)).apply();
    }

    @Nullable
    public String getAvatarUri() {
        String u = getCurrentUsername();
        if (u == null) {
            return null;
        }
        return prefs.getString(userAvatarUriKey(u), "");
    }

    public void setAvatarUri(String uri) {
        String u = getCurrentUsername();
        if (u == null) {
            return;
        }
        prefs.edit().putString(userAvatarUriKey(u), uri == null ? "" : uri).apply();
    }

    private void migrateLegacyIfNeeded() {
        if (!prefs.contains(LEGACY_KEY_USERNAME) || !prefs.contains(LEGACY_KEY_PASSWORD_HASH)) {
            return;
        }
        String legacyUser = safe(prefs.getString(LEGACY_KEY_USERNAME, ""));
        String legacyHash = safe(prefs.getString(LEGACY_KEY_PASSWORD_HASH, ""));
        if (legacyUser.isEmpty() || legacyHash.isEmpty()) {
            return;
        }
        if (!prefs.contains(userPasswordHashKey(legacyUser))) {
            prefs.edit()
                    .putString(userPasswordHashKey(legacyUser), legacyHash)
                    .putString(userNicknameKey(legacyUser), "")
                    .putString(userAvatarUriKey(legacyUser), "")
                    .putStringSet(KEY_USERNAMES, addToSet(prefs.getStringSet(KEY_USERNAMES, null), legacyUser))
                    .apply();
        }
        if (getCurrentUsername() == null) {
            prefs.edit().putString(KEY_CURRENT_USER, legacyUser).apply();
        }
    }

    private String userPasswordHashKey(String username) {
        return "user_" + username + "_password_hash";
    }

    private String userNicknameKey(String username) {
        return "user_" + username + "_nickname";
    }

    private String userAvatarUriKey(String username) {
        return "user_" + username + "_avatar_uri";
    }

    private Set<String> addToSet(Set<String> set, String value) {
        HashSet<String> copy = new HashSet<>();
        if (set != null) {
            copy.addAll(set);
        }
        if (value != null && !value.trim().isEmpty()) {
            copy.add(value.trim());
        }
        return copy;
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private String sha256(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return raw;
        }
    }
}
