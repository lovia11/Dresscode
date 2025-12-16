package com.example.dresscode.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.dresscode.data.local.ClosetDao;
import com.example.dresscode.data.local.ClosetItemEntity;
import com.example.dresscode.data.local.DatabaseProvider;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClosetRepository {
    private final ClosetDao closetDao;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private final String owner;

    public ClosetRepository(Context context, String owner) {
        this.closetDao = DatabaseProvider.get(context).closetDao();
        this.owner = owner == null ? "" : owner;
        ioExecutor.execute(() -> closetDao.claimLegacy(this.owner));
    }

    public LiveData<List<ClosetItemEntity>> observeAll() {
        return closetDao.observeAll(owner);
    }

    public LiveData<List<ClosetItemEntity>> observeFavorites() {
        return closetDao.observeFavorites(owner);
    }

    public void add(ClosetItemEntity item) {
        ioExecutor.execute(() -> closetDao.insert(item));
    }

    public void delete(long id) {
        ioExecutor.execute(() -> {
            ClosetItemEntity item = closetDao.getById(id, owner);
            if (item != null) {
                deleteLocalFileIfPossible(item.imageUri);
            }
            closetDao.deleteById(id, owner);
        });
    }

    public void update(long id, String name, String category, String color, String season, String style, String scene) {
        ioExecutor.execute(() -> closetDao.updateById(
                id,
                owner,
                safe(name),
                safe(category),
                safe(color),
                safe(season),
                safe(style),
                safe(scene)
        ));
    }

    public void setFavorite(long id, boolean favorite) {
        ioExecutor.execute(() -> closetDao.setFavorite(id, owner, favorite));
    }

    private void deleteLocalFileIfPossible(String uri) {
        if (uri == null || uri.trim().isEmpty()) {
            return;
        }
        try {
            android.net.Uri parsed = android.net.Uri.parse(uri);
            if ("file".equals(parsed.getScheme()) && parsed.getPath() != null) {
                java.io.File f = new java.io.File(parsed.getPath());
                if (f.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    f.delete();
                }
            }
        } catch (Exception ignored) {
        }
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
