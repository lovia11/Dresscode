package com.example.dresscode.data.remote;

import com.google.gson.annotations.SerializedName;

public class OpenMeteoWeatherResponse {
    @SerializedName("current")
    public Current current;

    public static class Current {
        @SerializedName("temperature_2m")
        public Double temperatureC;
        @SerializedName("weather_code")
        public Integer weatherCode;
        @SerializedName("is_day")
        public Integer isDay;
    }
}

