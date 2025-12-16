package com.example.dresscode.data.remote;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface QWeatherAirApi {
    @GET("v7/air/now")
    Call<QWeatherAirResponse> getAirNow(
            @Query("location") String location,
            @Query("key") String key,
            @Query("lang") String lang
    );
}

