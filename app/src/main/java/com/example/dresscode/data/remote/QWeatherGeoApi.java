package com.example.dresscode.data.remote;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface QWeatherGeoApi {
    @GET("v2/city/lookup")
    Call<QWeatherGeoResponse> lookup(
            @Query("location") String location,
            @Query("key") String key,
            @Query("lang") String lang,
            @Query("number") int number
    );
}

