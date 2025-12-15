package com.example.dresscode.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.dresscode.databinding.FragmentHomeBinding;
import com.example.dresscode.ui.home.adapter.HomeRecommendAdapter;

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
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
