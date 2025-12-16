package com.example.dresscode.data.remote;

import com.google.gson.annotations.SerializedName;

public class QWeatherAirResponse {
    @SerializedName("code")
    public String code;

    @SerializedName("now")
    public Now now;

    public static class Now {
        @SerializedName("aqi")
        public String aqi;
        @SerializedName("category")
        public String category;
        @SerializedName("pm2p5")
        public String pm2p5;
    }
}

