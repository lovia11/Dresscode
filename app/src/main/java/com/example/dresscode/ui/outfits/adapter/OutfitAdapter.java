package com.example.dresscode.ui.outfits.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dresscode.data.local.OutfitCardRow;
import com.example.dresscode.databinding.ItemOutfitCardBinding;

import java.util.ArrayList;
import java.util.List;

public class OutfitAdapter extends RecyclerView.Adapter<OutfitAdapter.ViewHolder> {

    public interface Listener {
        void onToggleFavorite(OutfitCardRow item);
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

        holder.binding.btnFavorite.setImageResource(
                item.isFavorite ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off
        );
        holder.binding.btnFavorite.setOnClickListener(v -> {
            if (listener != null) {
                listener.onToggleFavorite(item);
            }
        });

        int bg = ColorUtil.parseColorSafe(item.colorHex, 0xFFEEEEEE);
        holder.binding.imageCover.setBackgroundColor(bg);
        holder.binding.imageCover.setImageDrawable(null);
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
}
