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
    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();
    private final String owner;
    private final RemoteClosetRepository remoteRepository;

    public ClosetRepository(Context context, String owner) {
        this.closetDao = DatabaseProvider.get(context).closetDao();
        this.owner = owner == null ? "" : owner;
        this.remoteRepository = new RemoteClosetRepository(context);
        ioExecutor.execute(() -> closetDao.claimLegacy(this.owner));
    }

    public LiveData<List<ClosetItemEntity>> observeAll() {
        return closetDao.observeAll(owner);
    }

    public LiveData<List<ClosetItemEntity>> observeFavorites() {
        return closetDao.observeFavorites(owner);
    }

    public void add(ClosetItemEntity item) {
        ioExecutor.execute(() -> {
            long localId = closetDao.insert(item);
            syncToRemoteIfNeeded(localId);
        });
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

    private void syncToRemoteIfNeeded(long localId) {
        if (localId <= 0) {
            return;
        }
        ClosetItemEntity item = closetDao.getById(localId, owner);
        if (item == null) {
            return;
        }
        // 已同步则不重复上传
        if (item.remoteId > 0 && item.remoteImageUrl != null && !item.remoteImageUrl.trim().isEmpty()) {
            return;
        }
        networkExecutor.execute(() -> {
            try {
                RemoteClosetRepository.RemoteResult result = remoteRepository.uploadAndTag(item);
                ioExecutor.execute(() -> closetDao.updateRemoteById(
                        localId,
                        owner,
                        result.remoteId,
                        result.remoteImageUrl,
                        result.remoteTagsJson,
                        result.remoteTagModel,
                        result.remoteTagUpdatedAt
                ));
            } catch (Exception ignored) {
            }
        });
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
