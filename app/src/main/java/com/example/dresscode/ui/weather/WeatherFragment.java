package com.example.dresscode.ui.weather;

import android.Manifest;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.dresscode.R;
import com.example.dresscode.databinding.FragmentWeatherBinding;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class WeatherFragment extends Fragment {

    private FragmentWeatherBinding binding;
    private WeatherViewModel viewModel;
    private ActivityResultLauncher<String[]> permissionLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentWeatherBinding.inflate(inflater, container, false);

        viewModel = new ViewModelProvider(requireActivity()).get(WeatherViewModel.class);

        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    if (!isAdded()) {
                        return;
                    }
                    Boolean fine = result.get(Manifest.permission.ACCESS_FINE_LOCATION);
                    Boolean coarse = result.get(Manifest.permission.ACCESS_COARSE_LOCATION);
                    if (Boolean.TRUE.equals(fine) || Boolean.TRUE.equals(coarse)) {
                        locateAndRefresh();
                    } else {
                        showMessage(R.string.error_location_permission);
                    }
                }
        );

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

        binding.btnLocate.setOnClickListener(v -> requestLocationPermissionThenLocate());

        viewModel.getWeatherInfo().observe(getViewLifecycleOwner(), info -> {
            if (info == null) {
                return;
            }
            binding.inputCity.setText(info.city, false);
            binding.textTemp.setText(info.temp);
            binding.textDesc.setText(info.desc);
            binding.textAqi.setText(info.aqi);
        });

        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            boolean show = Boolean.TRUE.equals(loading);
            binding.textStatus.setVisibility(show ? View.VISIBLE : View.GONE);
            if (show) {
                binding.textStatus.setText(R.string.label_weather_status_loading);
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), err -> {
            boolean show = err != null && !err.trim().isEmpty();
            binding.textError.setVisibility(show ? View.VISIBLE : View.GONE);
            if (show) {
                binding.textError.setText(err);
            }
        });

        return binding.getRoot();
    }

    private void requestLocationPermissionThenLocate() {
        if (!isAdded()) {
            return;
        }
        boolean fineGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == android.content.pm.PackageManager.PERMISSION_GRANTED;
        boolean coarseGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                == android.content.pm.PackageManager.PERMISSION_GRANTED;
        if (fineGranted || coarseGranted) {
            locateAndRefresh();
            return;
        }
        permissionLauncher.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        });
    }

    private void locateAndRefresh() {
        if (!isAdded()) {
            return;
        }
        if (!isLocationEnabled()) {
            showMessage(R.string.error_location_unavailable);
            return;
        }
        final String hintCity =
                (binding != null && binding.inputCity.getText() != null) ? binding.inputCity.getText().toString() : "";
        boolean fineGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == android.content.pm.PackageManager.PERMISSION_GRANTED;
        int priority = fineGranted ? Priority.PRIORITY_HIGH_ACCURACY : Priority.PRIORITY_BALANCED_POWER_ACCURACY;

        CancellationTokenSource cts = new CancellationTokenSource();
        LocationServices.getFusedLocationProviderClient(requireContext())
                .getCurrentLocation(priority, cts.getToken())
                .addOnSuccessListener(location -> {
                    if (!isAdded()) {
                        return;
                    }
                    if (location == null) {
                        showMessage(R.string.error_location_unavailable);
                        return;
                    }
                    viewModel.refreshByLocation(hintCity, location.getLatitude(), location.getLongitude());
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) {
                        return;
                    }
                    showMessage(R.string.error_location_unavailable);
                });
    }

    private boolean isLocationEnabled() {
        try {
            android.content.Context context = getContext();
            if (context == null) {
                return false;
            }
            LocationManager lm = (LocationManager) context.getSystemService(android.content.Context.LOCATION_SERVICE);
            if (lm == null) {
                return false;
            }
            return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception e) {
            return false;
        }
    }

    private void showMessage(@StringRes int messageRes) {
        android.content.Context context = getContext();
        if (context == null) {
            return;
        }
        new MaterialAlertDialogBuilder(context)
                .setMessage(context.getString(messageRes))
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
