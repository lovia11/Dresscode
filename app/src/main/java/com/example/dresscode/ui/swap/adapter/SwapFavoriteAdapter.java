package com.example.dresscode.ui.swap.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dresscode.data.local.OutfitCardRow;
import com.example.dresscode.databinding.ItemSwapFavoriteOutfitBinding;

import java.util.ArrayList;
import java.util.List;

public class SwapFavoriteAdapter extends RecyclerView.Adapter<SwapFavoriteAdapter.ViewHolder> {

    public interface Listener {
        void onSelect(OutfitCardRow item);
    }

    private final List<OutfitCardRow> data = new ArrayList<>();
    private final Listener listener;
    private long selectedId = -1L;

    public SwapFavoriteAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submitList(List<OutfitCardRow> items, Long selectedId) {
        data.clear();
        if (items != null) {
            data.addAll(items);
        }
        this.selectedId = selectedId == null ? -1L : selectedId;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemSwapFavoriteOutfitBinding binding = ItemSwapFavoriteOutfitBinding.inflate(inflater, parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OutfitCardRow item = data.get(position);
        holder.binding.textTitle.setText(item.title);
        holder.binding.textMeta.setText(item.tags);

        int bg = parseColorSafe(item.colorHex, 0xFFEEEEEE);
        holder.binding.viewCover.setBackgroundColor(bg);

        boolean isSelected = item.id == selectedId;
        holder.binding.getRoot().setStrokeWidth(isSelected ? dp(holder.binding.getRoot(), 2) : 0);

        holder.binding.getRoot().setOnClickListener(v -> {
            if (listener != null) {
                listener.onSelect(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemSwapFavoriteOutfitBinding binding;

        ViewHolder(ItemSwapFavoriteOutfitBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    private int parseColorSafe(String hex, int fallback) {
        if (hex == null) {
            return fallback;
        }
        try {
            return Color.parseColor(hex);
        } catch (Exception e) {
            return fallback;
        }
    }

    private int dp(View view, int dp) {
        float density = view.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}

