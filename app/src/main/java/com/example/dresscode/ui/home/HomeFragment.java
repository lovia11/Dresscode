package com.example.dresscode.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.dresscode.R;
import com.example.dresscode.databinding.FragmentHomeBinding;
import com.example.dresscode.ui.home.adapter.HomeRecommendAdapter;
import com.example.dresscode.ui.weather.WeatherViewModel;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private HomeRecommendAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        binding.recyclerRecommend.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false)
        );
        adapter = new HomeRecommendAdapter();
        binding.recyclerRecommend.setAdapter(adapter);

        HomeViewModel viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        viewModel.getRecommendations().observe(getViewLifecycleOwner(), items -> adapter.submitList(items));
        viewModel.getTipsText().observe(getViewLifecycleOwner(), text -> {
            if (binding == null) {
                return;
            }
            binding.textTipsContent.setText(text == null ? "" : text);
        });

        WeatherViewModel weatherViewModel = new ViewModelProvider(requireActivity()).get(WeatherViewModel.class);
        weatherViewModel.getWeatherInfo().observe(getViewLifecycleOwner(), info -> {
            if (info == null) {
                return;
            }
            binding.textCity.setText(info.city);
            binding.textTemp.setText(info.temp);
            binding.textWeatherDesc.setText(info.desc);
            binding.textAqi.setText(info.aqi);
        });

        binding.cardWeather.setOnClickListener(v -> NavHostFragment.findNavController(this).navigate(R.id.weatherFragment));
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
