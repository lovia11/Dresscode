package com.example.dresscode.ui.outfits.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dresscode.data.local.OutfitCardRow;
import com.example.dresscode.databinding.ItemOutfitCardBinding;
import com.google.android.material.color.MaterialColors;

import java.util.ArrayList;
import java.util.List;

public class OutfitAdapter extends RecyclerView.Adapter<OutfitAdapter.ViewHolder> {

    public interface Listener {
        void onToggleFavorite(OutfitCardRow item);
        void onOpenDetail(OutfitCardRow item);
    }

    private final List<OutfitCardRow> data = new ArrayList<>();
    private final Listener listener;

    public OutfitAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submitList(List<OutfitCardRow> items) {
        data.clear();
        if (items != null) {
            data.addAll(items);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemOutfitCardBinding binding = ItemOutfitCardBinding.inflate(inflater, parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OutfitCardRow item = data.get(position);
        holder.binding.textTitle.setText(item.title);
        holder.binding.textTags.setText(item.tags);

        holder.binding.getRoot().setOnClickListener(v -> {
            if (listener != null) {
                listener.onOpenDetail(item);
            }
        });

        holder.binding.btnFavorite.setImageResource(
                item.isFavorite ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off
        );
        int tint = MaterialColors.getColor(
                holder.binding.btnFavorite,
                item.isFavorite ? androidx.appcompat.R.attr.colorPrimary : com.google.android.material.R.attr.colorOnSurfaceVariant
        );
        holder.binding.btnFavorite.setImageTintList(android.content.res.ColorStateList.valueOf(tint));
        holder.binding.btnFavorite.setAlpha(item.isFavorite ? 1f : 0.6f);
        holder.binding.btnFavorite.setOnClickListener(v -> {
            if (listener != null) {
                listener.onToggleFavorite(item);
            }
        });

        int bg = ColorUtil.parseColorSafe(item.colorHex, 0xFFEEEEEE);
        holder.binding.imageCover.setBackgroundColor(bg);
        if (item.coverResId != 0) {
            holder.binding.imageCover.setScaleType(pickScaleType(holder.binding.imageCover, item.coverResId));
            holder.binding.imageCover.setImageResource(item.coverResId);
        } else {
            holder.binding.imageCover.setImageDrawable(null);
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemOutfitCardBinding binding;

        ViewHolder(ItemOutfitCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    private static final class ColorUtil {
        static int parseColorSafe(String hex, int fallback) {
            if (hex == null) {
                return fallback;
            }
            try {
                return android.graphics.Color.parseColor(hex);
            } catch (Exception e) {
                return fallback;
            }
        }
    }

    private static android.widget.ImageView.ScaleType pickScaleType(android.widget.ImageView imageView, int resId) {
        if (imageView == null || resId == 0) {
            return android.widget.ImageView.ScaleType.CENTER_CROP;
        }
        try {
            String name = imageView.getResources().getResourceEntryName(resId);
            if (name != null && name.startsWith("outfit_")) {
                return android.widget.ImageView.ScaleType.CENTER_CROP;
            }
        } catch (Exception ignored) {
        }
        return android.widget.ImageView.ScaleType.CENTER_INSIDE;
    }
}
