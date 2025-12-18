package com.example.dresscode.data.repository;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.example.dresscode.data.remote.RetrofitProvider;
import com.example.dresscode.data.remote.TryOnApi;
import com.example.dresscode.data.remote.TryOnResponse;
import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TryOnRepository {

    public interface ResultCallback {
        void onSuccess(String resultImageUri);

        void onError(String message);
    }

    private final TryOnApi api;
    private final Context appContext;
    private final Gson gson = new Gson();

    public TryOnRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.api = RetrofitProvider.backend().create(TryOnApi.class);
    }

    public void tryOn(Uri personUri, Uri clothUri, ResultCallback callback) {
        if (personUri == null || clothUri == null) {
            callback.onError("缺少图片");
            return;
        }
        try {
            byte[] personBytes = readAllBytes(personUri);
            byte[] clothBytes = readAllBytes(clothUri);
            if (personBytes == null || personBytes.length == 0 || clothBytes == null || clothBytes.length == 0) {
                callback.onError("图片读取失败");
                return;
            }

            MultipartBody.Part personPart = MultipartBody.Part.createFormData(
                    "personImage",
                    "person.jpg",
                    RequestBody.create(personBytes, MediaType.parse("image/jpeg"))
            );
            MultipartBody.Part clothPart = MultipartBody.Part.createFormData(
                    "clothImage",
                    "cloth.jpg",
                    RequestBody.create(clothBytes, MediaType.parse("image/jpeg"))
            );

            api.tryOn(personPart, clothPart).enqueue(new Callback<TryOnResponse>() {
                @Override
                public void onResponse(@NonNull Call<TryOnResponse> call, @NonNull Response<TryOnResponse> response) {
                    TryOnResponse body = response.body();
                    if (!response.isSuccessful()) {
                        TryOnResponse err = parseErrorBody(response);
                        if (err != null && err.error != null && !err.error.trim().isEmpty()) {
                            callback.onError(err.error.trim());
                        } else {
                            callback.onError("请求失败（HTTP " + response.code() + "）");
                        }
                        return;
                    }
                    if (body == null) {
                        callback.onError("请求失败（HTTP " + response.code() + "）");
                        return;
                    }
                    if (!body.ok) {
                        callback.onError(body.error == null ? "生成失败" : body.error);
                        return;
                    }
                    String b64 = body.getResultImageBase64();
                    if (b64 == null || b64.trim().isEmpty()) {
                        callback.onError("返回结果为空");
                        return;
                    }
                    String uri = saveBase64Image(b64, body.getContentType());
                    if (uri == null) {
                        callback.onError("结果保存失败");
                        return;
                    }
                    callback.onSuccess(uri);
                }

                @Override
                public void onFailure(@NonNull Call<TryOnResponse> call, @NonNull Throwable t) {
                    callback.onError("网络错误：" + (t.getMessage() == null ? "" : t.getMessage()));
                }
            });
        } catch (Exception e) {
            callback.onError("读取图片失败：" + e.getMessage());
        }
    }

    private TryOnResponse parseErrorBody(Response<TryOnResponse> response) {
        try {
            if (response == null || response.errorBody() == null) {
                return null;
            }
            String text = response.errorBody().string();
            if (text == null || text.trim().isEmpty()) {
                return null;
            }
            return gson.fromJson(text, TryOnResponse.class);
        } catch (Exception e) {
            return null;
        }
    }

    private byte[] readAllBytes(Uri uri) {
        try (InputStream in = appContext.getContentResolver().openInputStream(uri)) {
            if (in == null) {
                return null;
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8 * 1024];
            int read;
            while ((read = in.read(buf)) != -1) {
                out.write(buf, 0, read);
            }
            return out.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    private String saveBase64Image(String base64, String contentType) {
        try {
            byte[] bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT);
            File dir = new File(appContext.getFilesDir(), "swap_results");
            if (!dir.exists() && !dir.mkdirs()) {
                return null;
            }
            String ext = (contentType != null && contentType.contains("png")) ? "png" : "jpg";
            File file = new File(dir, "result_" + System.currentTimeMillis() + "." + ext);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(bytes);
                fos.flush();
            }
            return Uri.fromFile(file).toString();
        } catch (Exception e) {
            return null;
        }
    }
}
