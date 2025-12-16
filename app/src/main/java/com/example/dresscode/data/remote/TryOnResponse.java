package com.example.dresscode.data.remote;

import com.google.gson.annotations.SerializedName;

public class TryOnResponse {
    @SerializedName("ok")
    public boolean ok;

    @SerializedName("result_image_base64")
    public String resultImageBase64;

    @SerializedName("resultImageBase64")
    public String resultImageBase64Alt;

    @SerializedName("content_type")
    public String contentType;

    @SerializedName("contentType")
    public String contentTypeAlt;

    @SerializedName("error")
    public String error;

    @SerializedName("elapsed_ms")
    public int elapsedMs;

    public String getResultImageBase64() {
        return resultImageBase64 != null && !resultImageBase64.trim().isEmpty()
                ? resultImageBase64
                : (resultImageBase64Alt == null ? "" : resultImageBase64Alt);
    }

    public String getContentType() {
        return contentType != null && !contentType.trim().isEmpty()
                ? contentType
                : (contentTypeAlt == null ? "" : contentTypeAlt);
    }
}

