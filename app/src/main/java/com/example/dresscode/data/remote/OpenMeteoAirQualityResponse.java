package com.example.dresscode.data.remote;

import com.google.gson.annotations.SerializedName;

public class OpenMeteoAirQualityResponse {
    @SerializedName("current")
    public Current current;

    public static class Current {
        @SerializedName("us_aqi")
        public Integer usAqi;
        @SerializedName("pm2_5")
        public Double pm25;
    }
}

