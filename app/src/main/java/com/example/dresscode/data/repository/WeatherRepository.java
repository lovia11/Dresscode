package com.example.dresscode.data.repository;

import androidx.annotation.NonNull;

import com.example.dresscode.data.remote.AMapGeoApi;
import com.example.dresscode.data.remote.AMapRegeoResponse;
import com.example.dresscode.data.remote.AMapWeatherApi;
import com.example.dresscode.data.remote.AMapWeatherResponse;
import com.example.dresscode.data.remote.RetrofitProvider;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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

    public interface WeatherCallback {
        void onSuccess(WeatherInfo info);

        void onError(String message);
    }

    private final AMapGeoApi geoApi = RetrofitProvider.amap().create(AMapGeoApi.class);
    private final AMapWeatherApi weatherApi = RetrofitProvider.amap().create(AMapWeatherApi.class);

    private static final class CityOption {
        final String name;
        final String adcode;

        CityOption(String name, String adcode) {
            this.name = name;
            this.adcode = adcode;
        }
    }

    private static final List<CityOption> CITY_OPTIONS = Arrays.asList(
            new CityOption("北京", "110000"),
            new CityOption("上海", "310000"),
            new CityOption("广州", "440100"),
            new CityOption("深圳", "440300"),
            new CityOption("杭州", "330100"),
            new CityOption("成都", "510100")
    );

    public List<String> getCities() {
        List<String> result = new ArrayList<>(CITY_OPTIONS.size());
        for (CityOption option : CITY_OPTIONS) {
            result.add(option.name);
        }
        return result;
    }

    public void fetchWeatherByCity(String city, String key, WeatherCallback callback) {
        String q = safe(city);
        if (q.isEmpty() || "当前位置".equals(q)) {
            q = "杭州";
        }
        fetchWeatherByCityParam(toCityParam(q), key, callback);
    }

    public void fetchWeatherByLocation(String cityNameHint, double lat, double lon, String key, WeatherCallback callback) {
        String location = lon + "," + lat;
        geoApi.regeo(key, location, "base", 1000).enqueue(new Callback<AMapRegeoResponse>() {
            @Override
            public void onResponse(@NonNull Call<AMapRegeoResponse> call, @NonNull Response<AMapRegeoResponse> response) {
                AMapRegeoResponse body = response.body();
                if (!response.isSuccessful() || body == null || !"1".equals(body.status) || body.regeocode == null || body.regeocode.addressComponent == null) {
                    fetchWeatherByCity(cityNameHint, key, callback);
                    return;
                }
                AMapRegeoResponse.AddressComponent ac = body.regeocode.addressComponent;
                String cityName = extractCityName(ac);
                String adcode = safe(ac.adcode);
                if (adcode.isEmpty()) {
                    fetchWeatherByCity(cityName, key, callback);
                    return;
                }
                fetchWeatherByCityParam(adcode, key, new WeatherCallback() {
                    @Override
                    public void onSuccess(WeatherInfo info) {
                        callback.onSuccess(new WeatherInfo(cityName, info.temp, info.desc, info.aqi));
                    }

                    @Override
                    public void onError(String message) {
                        fetchWeatherByCity(cityName, key, callback);
                    }
                });
            }

            @Override
            public void onFailure(@NonNull Call<AMapRegeoResponse> call, @NonNull Throwable t) {
                fetchWeatherByCity(cityNameHint, key, callback);
            }
        });
    }

    private void fetchWeatherByCityParam(String cityParam, String key, WeatherCallback callback) {
        weatherApi.weatherInfo(key, cityParam, "base").enqueue(new Callback<AMapWeatherResponse>() {
            @Override
            public void onResponse(@NonNull Call<AMapWeatherResponse> call, @NonNull Response<AMapWeatherResponse> response) {
                AMapWeatherResponse body = response.body();
                if (!response.isSuccessful() || body == null) {
                    callback.onError("天气获取失败（HTTP " + response.code() + "）");
                    return;
                }
                if (!"1".equals(body.status)) {
                    String msg = safe(body.info);
                    String code = safe(body.infoCode);
                    callback.onError("天气获取失败" + (msg.isEmpty() ? "" : ("：" + msg)) + (code.isEmpty() ? "" : ("（" + code + "）")));
                    return;
                }
                if (body.lives == null || body.lives.isEmpty()) {
                    String count = safe(body.count);
                    callback.onError("天气获取失败：无数据（city=" + safe(cityParam) + (count.isEmpty() ? "" : (", count=" + count)) + "）");
                    return;
                }
                AMapWeatherResponse.Live live = body.lives.get(0);
                String cityName = safeCity(live.city);
                String temp = safe(live.temperature);
                if (temp.isEmpty()) {
                    temp = "--";
                }
                String desc = safe(live.weather);
                if (desc.isEmpty()) {
                    desc = "天气";
                }
                callback.onSuccess(new WeatherInfo(cityName, temp + "℃", desc, buildExtra(live)));
            }

            @Override
            public void onFailure(@NonNull Call<AMapWeatherResponse> call, @NonNull Throwable t) {
                callback.onError("天气获取失败：" + t.getMessage());
            }
        });
    }

    private String buildExtra(AMapWeatherResponse.Live live) {
        String humidity = safe(live.humidity);
        String windDir = safe(live.windDirection);
        String windPower = safe(live.windPower);
        StringBuilder sb = new StringBuilder("空气质量 --");
        if (!humidity.isEmpty()) {
            sb.append(" · 湿度 ").append(humidity).append("%");
        }
        if (!windDir.isEmpty() || !windPower.isEmpty()) {
            sb.append(" · ");
            if (!windDir.isEmpty()) {
                sb.append(windDir);
            }
            if (!windPower.isEmpty()) {
                sb.append(" ").append(windPower).append("级");
            }
        }
        return sb.toString();
    }

    private String extractCityName(AMapRegeoResponse.AddressComponent ac) {
        if (ac == null) {
            return "当前位置";
        }
        String city = "";
        try {
            if (ac.city != null) {
                if (ac.city.isJsonPrimitive()) {
                    city = safe(ac.city.getAsString());
                } else if (ac.city.isJsonArray() && ac.city.getAsJsonArray().size() > 0) {
                    city = safe(ac.city.getAsJsonArray().get(0).getAsString());
                }
            }
        } catch (Exception ignored) {
        }
        if (city.isEmpty()) {
            city = safe(ac.district);
        }
        if (city.isEmpty()) {
            city = safe(ac.province);
        }
        if (city.isEmpty()) {
            city = "当前位置";
        }
        return city.replace("市", "").trim();
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private String toCityParam(String city) {
        String c = normalizeCityName(city);
        if (c.isEmpty()) {
            return c;
        }
        // adcode is numeric; keep as-is
        if (c.matches("^\\d+$")) {
            return c;
        }
        for (CityOption option : CITY_OPTIONS) {
            if (option.name.equals(c)) {
                return option.adcode;
            }
        }
        return c;
    }

    private String normalizeCityName(String city) {
        String c = safe(city);
        if (c.isEmpty()) {
            return c;
        }
        if (c.endsWith("市")) {
            c = c.substring(0, c.length() - 1);
        }
        return c.trim();
    }

    private String safeCity(String city) {
        String c = normalizeCityName(city);
        return c.isEmpty() ? "当前位置" : c;
    }
}
