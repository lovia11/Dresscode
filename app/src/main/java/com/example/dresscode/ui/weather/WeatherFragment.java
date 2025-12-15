package com.example.dresscode.ui.weather;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.dresscode.databinding.FragmentWeatherBinding;

public class WeatherFragment extends Fragment {

    private FragmentWeatherBinding binding;
    private WeatherViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentWeatherBinding.inflate(inflater, container, false);

        viewModel = new ViewModelProvider(requireActivity()).get(WeatherViewModel.class);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                viewModel.getCities()
        );
        binding.inputCity.setAdapter(adapter);

        binding.inputCity.setOnItemClickListener((parent, view, position, id) -> {
            Object item = parent.getItemAtPosition(position);
            if (item != null) {
                viewModel.setCity(item.toString());
            }
        });

        viewModel.getWeatherInfo().observe(getViewLifecycleOwner(), info -> {
            if (info == null) {
                return;
            }
            binding.inputCity.setText(info.city, false);
            binding.textTemp.setText(info.temp);
            binding.textDesc.setText(info.desc);
            binding.textAqi.setText(info.aqi);
        });

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
