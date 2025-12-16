package com.example.dresscode.data.remote;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface OpenMeteoAirQualityApi {
    @GET("v1/air-quality")
    Call<OpenMeteoAirQualityResponse> getAirQuality(
            @Query("latitude") double latitude,
            @Query("longitude") double longitude,
            @Query("current") String current
    );
}

