package com.example.dresscode.data.remote;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface TryOnApi {

    @Multipart
    @POST("api/tryon")
    Call<TryOnResponse> tryOn(
            @Part MultipartBody.Part personImage,
            @Part MultipartBody.Part clothImage
    );
}

