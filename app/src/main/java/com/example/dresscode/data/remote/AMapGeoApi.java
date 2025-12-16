package com.example.dresscode.data.remote;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface AMapGeoApi {
    @GET("v3/geocode/regeo")
    Call<AMapRegeoResponse> regeo(
            @Query("key") String key,
            @Query("location") String location,
            @Query("extensions") String extensions,
            @Query("radius") int radius
    );
}

