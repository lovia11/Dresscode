package com.example.dresscode.data.remote;

import com.google.gson.annotations.SerializedName;

public class QWeatherWeatherResponse {
    @SerializedName("code")
    public String code;

    @SerializedName("now")
    public Now now;

    public static class Now {
        @SerializedName("temp")
        public String temp;
        @SerializedName("text")
        public String text;
        @SerializedName("humidity")
        public String humidity;
        @SerializedName("windDir")
        public String windDir;
        @SerializedName("windScale")
        public String windScale;
        @SerializedName("obsTime")
        public String obsTime;
    }
}

