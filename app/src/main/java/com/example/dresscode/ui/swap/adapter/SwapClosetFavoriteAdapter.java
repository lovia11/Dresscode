package com.example.dresscode.ui.swap.adapter;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dresscode.data.local.ClosetItemEntity;
import com.example.dresscode.databinding.ItemSwapFavoriteClosetBinding;

import java.util.ArrayList;
import java.util.List;

public class SwapClosetFavoriteAdapter extends RecyclerView.Adapter<SwapClosetFavoriteAdapter.ViewHolder> {

    public interface Listener {
        void onSelect(ClosetItemEntity item);
        void onPreview(ClosetItemEntity item);
    }

    private final List<ClosetItemEntity> data = new ArrayList<>();
    private final Listener listener;
    private long selectedId = -1L;

    public SwapClosetFavoriteAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submitList(List<ClosetItemEntity> items, Long selectedId) {
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
        ItemSwapFavoriteClosetBinding binding = ItemSwapFavoriteClosetBinding.inflate(inflater, parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ClosetItemEntity item = data.get(position);
        holder.binding.textTitle.setText(item.name);
        holder.binding.textMeta.setText(buildMeta(item));

        try {
            holder.binding.imageCover.setImageURI(null);
            holder.binding.imageCover.setImageURI(Uri.parse(item.imageUri));
        } catch (Exception e) {
            holder.binding.imageCover.setImageURI(null);
        }
        holder.binding.imageCover.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPreview(item);
            }
        });

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
        final ItemSwapFavoriteClosetBinding binding;

        ViewHolder(ItemSwapFavoriteClosetBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    private int dp(View view, int dp) {
        float density = view.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private String buildMeta(ClosetItemEntity item) {
        StringBuilder sb = new StringBuilder();
        appendPart(sb, item.category);
        appendPart(sb, item.style);
        appendPart(sb, item.scene);
        return sb.toString();
    }

    private void appendPart(StringBuilder sb, String part) {
        if (part == null) {
            return;
        }
        String p = part.trim();
        if (p.isEmpty() || "未知".equals(p)) {
            return;
        }
        if (sb.length() > 0) {
            sb.append(" · ");
        }
        sb.append(p);
    }
}
