package com.example.dresscode.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.dresscode.databinding.FragmentHomeBinding;
import com.example.dresscode.ui.home.adapter.HomeRecommendAdapter;

import java.util.Arrays;
import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        setupRecommendationCarousel();
        return binding.getRoot();
    }

    private void setupRecommendationCarousel() {
        List<HomeRecommendAdapter.RecommendItem> items = Arrays.asList(
                new HomeRecommendAdapter.RecommendItem("今日推荐：通勤白衬衫", "晴 22°C · 春季 · 通勤"),
                new HomeRecommendAdapter.RecommendItem("运动风：速干套装", "多云 18°C · 运动"),
                new HomeRecommendAdapter.RecommendItem("约会：法式连衣裙", "晴 25°C · 约会")
        );
        binding.recyclerRecommend.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false)
        );
        binding.recyclerRecommend.setAdapter(new HomeRecommendAdapter(items));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
