package com.example.dresscode.ui.home.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dresscode.model.RecommendItem;
import com.example.dresscode.databinding.ItemHomeRecommendBinding;

import java.util.ArrayList;
import java.util.List;

public class HomeRecommendAdapter extends RecyclerView.Adapter<HomeRecommendAdapter.ViewHolder> {
    private final List<RecommendItem> data = new ArrayList<>();
    public interface Listener {
        void onOpen(RecommendItem item);
    }

    private final Listener listener;

    public HomeRecommendAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submitList(List<RecommendItem> items) {
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
        ItemHomeRecommendBinding binding = ItemHomeRecommendBinding.inflate(inflater, parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RecommendItem item = data.get(position);
        holder.binding.textTitle.setText(item.title);
        holder.binding.textMeta.setText(item.meta);
        holder.binding.getRoot().setOnClickListener(v -> {
            if (listener != null) {
                listener.onOpen(item);
            }
        });
        if (item.imageResId != 0) {
            holder.binding.imageCover.setImageURI(null);
            holder.binding.imageCover.setImageResource(item.imageResId);
            holder.binding.imageCover.setScaleType(android.widget.ImageView.ScaleType.CENTER_INSIDE);
        } else if (item.imageUri != null && !item.imageUri.trim().isEmpty()) {
            try {
                holder.binding.imageCover.setImageURI(null);
                holder.binding.imageCover.setImageURI(android.net.Uri.parse(item.imageUri));
            } catch (Exception e) {
                holder.binding.imageCover.setImageURI(null);
            }
        } else {
            holder.binding.imageCover.setImageURI(null);
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemHomeRecommendBinding binding;

        ViewHolder(ItemHomeRecommendBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
