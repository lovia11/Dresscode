package com.example.dresscode.ui.weather;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.example.dresscode.data.prefs.WeatherPreferencesRepository;
import com.example.dresscode.data.repository.WeatherRepository;

import java.util.List;

public class WeatherViewModel extends AndroidViewModel {

    private final WeatherRepository repository = new WeatherRepository();
    private final WeatherPreferencesRepository prefs;
    private final MutableLiveData<WeatherRepository.WeatherInfo> weatherInfo = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>("");
    private final Observer<String> cityObserver;
    private volatile boolean suppressNextCityRefresh = false;
    private long requestSeq = 0;

    public WeatherViewModel(@NonNull Application application) {
        super(application);
        prefs = new WeatherPreferencesRepository(application);
        cityObserver = this::refreshByCity;
        prefs.observeCity().observeForever(cityObserver);

        WeatherPreferencesRepository.Snapshot snapshot = prefs.observeSnapshot().getValue();
        if (snapshot != null && !snapshot.temp.trim().isEmpty()) {
            weatherInfo.setValue(new WeatherRepository.WeatherInfo(
                    snapshot.city == null || snapshot.city.trim().isEmpty() ? "杭州" : snapshot.city,
                    snapshot.temp,
                    snapshot.desc.isEmpty() ? "天气" : snapshot.desc,
                    snapshot.aqi.isEmpty() ? "空气质量 --" : snapshot.aqi
            ));
        }
        // initial refresh triggered by observeCity
    }

    public List<String> getCities() {
        return repository.getCities();
    }

    public LiveData<WeatherRepository.WeatherInfo> getWeatherInfo() {
        return weatherInfo;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void setCity(String city) {
        prefs.setCity(city);
    }

    public void refreshByCity(String city) {
        String key = com.example.dresscode.BuildConfig.AMAP_KEY;
        if (key == null || key.trim().isEmpty()) {
            loading.setValue(false);
            error.setValue(getApplication().getString(com.example.dresscode.R.string.error_amap_key_missing));
            return;
        }
        String c = city == null || city.trim().isEmpty() ? "杭州" : city.trim();
        if (suppressNextCityRefresh) {
            suppressNextCityRefresh = false;
            return;
        }
        long seq = ++requestSeq;
        loading.setValue(true);
        error.setValue("");
        repository.fetchWeatherByCity(c, key, new WeatherRepository.WeatherCallback() {
            @Override
            public void onSuccess(WeatherRepository.WeatherInfo info) {
                if (seq != requestSeq) {
                    return;
                }
                loading.postValue(false);
                error.postValue("");
                weatherInfo.postValue(info);
                prefs.setCachedWeather(info.temp, info.desc, info.aqi);
            }

            @Override
            public void onError(String message) {
                if (seq != requestSeq) {
                    return;
                }
                loading.postValue(false);
                error.postValue(message == null ? "请求失败" : message);
            }
        });
    }

    public void refreshByLocation(String cityName, double lat, double lon) {
        String key = com.example.dresscode.BuildConfig.AMAP_KEY;
        if (key == null || key.trim().isEmpty()) {
            loading.setValue(false);
            error.setValue(getApplication().getString(com.example.dresscode.R.string.error_amap_key_missing));
            return;
        }
        long seq = ++requestSeq;
        loading.setValue(true);
        error.setValue("");
        repository.fetchWeatherByLocation(cityName, lat, lon, key, new WeatherRepository.WeatherCallback() {
            @Override
            public void onSuccess(WeatherRepository.WeatherInfo info) {
                if (seq != requestSeq) {
                    return;
                }
                loading.postValue(false);
                error.postValue("");
                weatherInfo.postValue(info);
                suppressNextCityRefresh = true;
                prefs.setCity(info.city);
                prefs.setCachedWeather(info.temp, info.desc, info.aqi);
            }

            @Override
            public void onError(String message) {
                if (seq != requestSeq) {
                    return;
                }
                loading.postValue(false);
                error.postValue(message == null ? "请求失败" : message);
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        try {
            prefs.observeCity().removeObserver(cityObserver);
        } catch (Exception ignored) {
        }
        prefs.close();
    }
}
