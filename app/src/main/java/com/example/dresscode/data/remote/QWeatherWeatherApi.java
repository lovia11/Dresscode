package com.example.dresscode.data.remote;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface QWeatherWeatherApi {
    @GET("v7/weather/now")
    Call<QWeatherWeatherResponse> getWeatherNow(
            @Query("location") String location,
            @Query("key") String key,
            @Query("lang") String lang,
            @Query("unit") String unit
    );
}

