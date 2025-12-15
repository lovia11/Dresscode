package com.example.dresscode.data.repository;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.List;

public class WeatherRepository {

    public static class WeatherInfo {
        @NonNull
        public final String city;
        @NonNull
        public final String temp;
        @NonNull
        public final String desc;
        @NonNull
        public final String aqi;

        public WeatherInfo(@NonNull String city, @NonNull String temp, @NonNull String desc, @NonNull String aqi) {
            this.city = city;
            this.temp = temp;
            this.desc = desc;
            this.aqi = aqi;
        }
    }

    public List<String> getCities() {
        return Arrays.asList("北京", "上海", "广州", "深圳", "杭州", "成都");
    }

    public WeatherInfo getWeatherForCity(String city) {
        String c = city == null ? "" : city.trim();
        if (c.isEmpty()) {
            c = "杭州";
        }
        switch (c) {
            case "北京":
                return new WeatherInfo(c, "5℃", "晴转多云", "空气质量 良");
            case "上海":
                return new WeatherInfo(c, "12℃", "多云", "空气质量 优");
            case "广州":
                return new WeatherInfo(c, "23℃", "晴", "空气质量 良");
            case "深圳":
                return new WeatherInfo(c, "24℃", "阵雨", "空气质量 优");
            case "成都":
                return new WeatherInfo(c, "9℃", "小雨", "空气质量 良");
            case "杭州":
            default:
                return new WeatherInfo(c, "16℃", "多云", "空气质量 优");
        }
    }
}

