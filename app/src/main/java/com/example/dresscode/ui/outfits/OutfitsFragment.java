package com.example.dresscode.ui.outfits;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.dresscode.databinding.FragmentOutfitsBinding;
import com.example.dresscode.ui.outfits.adapter.OutfitAdapter;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class OutfitsFragment extends Fragment {

    private FragmentOutfitsBinding binding;
    private OutfitsViewModel viewModel;
    private OutfitAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentOutfitsBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(OutfitsViewModel.class);

        adapter = new OutfitAdapter(item -> viewModel.toggleFavorite(item.id, !item.isFavorite));
        binding.outfitList.setLayoutManager(new GridLayoutManager(getContext(), 2));
        binding.outfitList.setAdapter(adapter);

        binding.searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.setQuery(s == null ? "" : s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        binding.chipGender.setOnClickListener(v -> showGenderDialog());
        binding.chipStyle.setOnClickListener(v -> showSingleChoiceDialog(
                binding.chipStyle.getText().toString(),
                com.example.dresscode.R.array.filter_style_options,
                viewModel::setStyleFilter
        ));
        binding.chipSeason.setOnClickListener(v -> showSingleChoiceDialog(
                binding.chipSeason.getText().toString(),
                com.example.dresscode.R.array.filter_season_options,
                viewModel::setSeasonFilter
        ));
        binding.chipScene.setOnClickListener(v -> showSingleChoiceDialog(
                binding.chipScene.getText().toString(),
                com.example.dresscode.R.array.filter_scene_options,
                viewModel::setSceneFilter
        ));
        binding.chipWeather.setOnClickListener(v -> showSingleChoiceDialog(
                binding.chipWeather.getText().toString(),
                com.example.dresscode.R.array.filter_weather_options,
                viewModel::setWeatherFilter
        ));

        viewModel.getOutfits().observe(getViewLifecycleOwner(), items -> adapter.submitList(items));
        viewModel.getChipGenderText().observe(getViewLifecycleOwner(), text -> binding.chipGender.setText(text));
        viewModel.getChipStyleText().observe(getViewLifecycleOwner(), text -> binding.chipStyle.setText(text));
        viewModel.getChipSeasonText().observe(getViewLifecycleOwner(), text -> binding.chipSeason.setText(text));
        viewModel.getChipSceneText().observe(getViewLifecycleOwner(), text -> binding.chipScene.setText(text));
        viewModel.getChipWeatherText().observe(getViewLifecycleOwner(), text -> binding.chipWeather.setText(text));
        return binding.getRoot();
    }

    private void showGenderDialog() {
        String[] items = getResources().getStringArray(com.example.dresscode.R.array.filter_gender_options);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(com.example.dresscode.R.string.filter_gender)
                .setItems(items, (dialog, which) -> {
                    if (which == 0) {
                        viewModel.useGenderFromSettings();
                    } else if (which == 1) {
                        viewModel.setGenderOverride("");
                    } else if (which == 2) {
                        viewModel.setGenderOverride("MALE");
                    } else if (which == 3) {
                        viewModel.setGenderOverride("FEMALE");
                    }
                })
                .show();
    }

    private interface SelectionConsumer {
        void accept(String value);
    }

    private void showSingleChoiceDialog(String title, int itemsRes, SelectionConsumer consumer) {
        String[] items = getResources().getStringArray(itemsRes);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(title)
                .setItems(items, (dialog, which) -> {
                    String selected = items[which];
                    if (which == 0) {
                        consumer.accept("");
                    } else {
                        consumer.accept(selected);
                    }
                })
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
