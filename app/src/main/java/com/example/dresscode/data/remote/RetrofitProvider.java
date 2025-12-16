package com.example.dresscode.data.remote;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;

public final class RetrofitProvider {
    private static volatile Retrofit qweatherDevRetrofit;
    private static volatile Retrofit qweatherGeoRetrofit;
    private static volatile Retrofit amapRetrofit;
    private static volatile Retrofit backendRetrofit;

    private RetrofitProvider() {
    }

    public static Retrofit qweatherDev() {
        if (qweatherDevRetrofit == null) {
            synchronized (RetrofitProvider.class) {
                if (qweatherDevRetrofit == null) {
                    qweatherDevRetrofit = new Retrofit.Builder()
                            .baseUrl("https://devapi.qweather.com/")
                            .addConverterFactory(GsonConverterFactory.create())
                            .build();
                }
            }
        }
        return qweatherDevRetrofit;
    }

    public static Retrofit qweatherGeo() {
        if (qweatherGeoRetrofit == null) {
            synchronized (RetrofitProvider.class) {
                if (qweatherGeoRetrofit == null) {
                    qweatherGeoRetrofit = new Retrofit.Builder()
                            .baseUrl("https://geoapi.qweather.com/")
                            .addConverterFactory(GsonConverterFactory.create())
                            .build();
                }
            }
        }
        return qweatherGeoRetrofit;
    }

    public static Retrofit amap() {
        if (amapRetrofit == null) {
            synchronized (RetrofitProvider.class) {
                if (amapRetrofit == null) {
                    amapRetrofit = new Retrofit.Builder()
                            .baseUrl("https://restapi.amap.com/")
                            .addConverterFactory(GsonConverterFactory.create())
                            .build();
                }
            }
        }
        return amapRetrofit;
    }

    public static Retrofit backend() {
        if (backendRetrofit == null) {
            synchronized (RetrofitProvider.class) {
                if (backendRetrofit == null) {
                    String baseUrl = com.example.dresscode.BuildConfig.BACKEND_BASE_URL;
                    if (baseUrl == null || baseUrl.trim().isEmpty()) {
                        baseUrl = "http://10.0.2.2:8000/";
                    }
                    OkHttpClient client = new OkHttpClient.Builder()
                            .connectTimeout(30, TimeUnit.SECONDS)
                            .readTimeout(180, TimeUnit.SECONDS)
                            .writeTimeout(180, TimeUnit.SECONDS)
                            .callTimeout(180, TimeUnit.SECONDS)
                            .build();
                    backendRetrofit = new Retrofit.Builder()
                            .baseUrl(baseUrl)
                            .client(client)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build();
                }
            }
        }
        return backendRetrofit;
    }
}
