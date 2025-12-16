package com.example.dresscode.data.remote;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface OpenMeteoWeatherApi {
    @GET("v1/forecast")
    Call<OpenMeteoWeatherResponse> getWeather(
            @Query("latitude") double latitude,
            @Query("longitude") double longitude,
            @Query("current") String current,
            @Query("timezone") String timezone
    );
}

