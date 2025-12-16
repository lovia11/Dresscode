package com.example.dresscode.data.remote;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class AMapWeatherResponse {
    @SerializedName("status")
    public String status;
    @SerializedName("info")
    public String info;
    @SerializedName("infocode")
    public String infoCode;
    @SerializedName("count")
    public String count;

    @SerializedName("lives")
    public List<Live> lives;

    public static class Live {
        @SerializedName("city")
        public String city;
        @SerializedName("weather")
        public String weather;
        @SerializedName("temperature")
        public String temperature;
        @SerializedName("humidity")
        public String humidity;
        @SerializedName("winddirection")
        public String windDirection;
        @SerializedName("windpower")
        public String windPower;
        @SerializedName("reporttime")
        public String reportTime;
    }
}
