package com.example.dresscode.ui.outfits.detail;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.dresscode.R;
import com.example.dresscode.data.local.OutfitDetailRow;
import com.example.dresscode.databinding.FragmentOutfitDetailBinding;

import java.util.ArrayList;

public class OutfitDetailFragment extends Fragment {

    public static final String ARG_OUTFIT_ID = "outfit_id";

    private FragmentOutfitDetailBinding binding;
    private OutfitDetailViewModel viewModel;
    private long outfitId = -1L;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentOutfitDetailBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(OutfitDetailViewModel.class);

        Bundle args = getArguments();
        if (args != null) {
            outfitId = args.getLong(ARG_OUTFIT_ID, -1L);
        }
        viewModel.setOutfitId(outfitId);

        binding.btnTrySwap.setOnClickListener(v -> {
            if (outfitId <= 0) {
                return;
            }
            viewModel.toggleFavorite(outfitId, true);
            Bundle b = new Bundle();
            b.putLong(com.example.dresscode.ui.swap.SwapFragment.ARG_PRESELECT_OUTFIT_ID, outfitId);
            NavHostFragment.findNavController(this).navigate(R.id.swapFragment, b);
        });

        viewModel.getOutfit().observe(getViewLifecycleOwner(), row -> {
            if (row == null) {
                return;
            }
            bind(row);
        });

        return binding.getRoot();
    }

    private void bind(OutfitDetailRow row) {
        binding.textTitle.setText(row.title);
        binding.textTags.setText(row.tags);
        binding.textMeta.setText(row.style + " · " + row.season + " · " + row.scene + " · " + row.weather);

        // 纠正 meta 分隔符（避免出现乱码分隔符）
        binding.textMeta.setText(buildMeta(row));

        if (row.coverResId != 0) {
            binding.imageCover.setImageURI(null);
            binding.imageCover.setImageResource(row.coverResId);
            binding.imageCover.setScaleType(pickScaleType(row.coverResId));
        } else {
            binding.imageCover.setImageURI((Uri) null);
        }

        binding.btnFavorite.setImageResource(row.isFavorite ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off);
        int tint = com.google.android.material.color.MaterialColors.getColor(
                binding.btnFavorite,
                row.isFavorite ? androidx.appcompat.R.attr.colorPrimary : com.google.android.material.R.attr.colorOnSurfaceVariant
        );
        binding.btnFavorite.setImageTintList(android.content.res.ColorStateList.valueOf(tint));
        binding.btnFavorite.setAlpha(row.isFavorite ? 1f : 0.6f);

        binding.btnFavorite.setOnClickListener(v -> viewModel.toggleFavorite(row.id, !row.isFavorite));
    }

    private String buildMeta(OutfitDetailRow row) {
        ArrayList<String> parts = new ArrayList<>();
        if (row.style != null && !row.style.trim().isEmpty()) {
            parts.add(row.style.trim());
        }
        if (row.season != null && !row.season.trim().isEmpty()) {
            parts.add(row.season.trim());
        }
        if (row.scene != null && !row.scene.trim().isEmpty()) {
            parts.add(row.scene.trim());
        }
        if (row.weather != null && !row.weather.trim().isEmpty()) {
            parts.add(row.weather.trim());
        }
        return parts.isEmpty() ? "" : TextUtils.join(" · ", parts);
    }

    private android.widget.ImageView.ScaleType pickScaleType(int resId) {
        if (resId == 0) {
            return android.widget.ImageView.ScaleType.CENTER_CROP;
        }
        try {
            String name = requireContext().getResources().getResourceEntryName(resId);
            if (name != null && name.startsWith("outfit_")) {
                return android.widget.ImageView.ScaleType.CENTER_CROP;
            }
        } catch (Exception ignored) {
        }
        return android.widget.ImageView.ScaleType.CENTER_INSIDE;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
