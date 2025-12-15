package com.example.dresscode.ui.weather;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.example.dresscode.data.prefs.WeatherPreferencesRepository;
import com.example.dresscode.data.repository.WeatherRepository;

import java.util.List;

public class WeatherViewModel extends AndroidViewModel {

    private final WeatherRepository repository = new WeatherRepository();
    private final WeatherPreferencesRepository prefs;
    private final LiveData<WeatherRepository.WeatherInfo> weatherInfo;

    public WeatherViewModel(@NonNull Application application) {
        super(application);
        prefs = new WeatherPreferencesRepository(application);
        weatherInfo = Transformations.map(prefs.observeCity(), repository::getWeatherForCity);
    }

    public List<String> getCities() {
        return repository.getCities();
    }

    public LiveData<WeatherRepository.WeatherInfo> getWeatherInfo() {
        return weatherInfo;
    }

    public void setCity(String city) {
        prefs.setCity(city);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        prefs.close();
    }
}

