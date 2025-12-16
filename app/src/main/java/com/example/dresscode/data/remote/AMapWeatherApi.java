package com.example.dresscode.data.remote;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface AMapWeatherApi {
    @GET("v3/weather/weatherInfo")
    Call<AMapWeatherResponse> weatherInfo(
            @Query("key") String key,
            @Query("city") String city,
            @Query("extensions") String extensions
    );
}

