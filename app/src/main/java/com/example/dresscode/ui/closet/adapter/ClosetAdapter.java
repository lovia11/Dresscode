package com.example.dresscode.ui.closet.adapter;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dresscode.data.local.ClosetItemEntity;
import com.example.dresscode.databinding.ItemClosetClothingBinding;

import java.util.ArrayList;
import java.util.List;

public class ClosetAdapter extends RecyclerView.Adapter<ClosetAdapter.ViewHolder> {

    public interface Listener {
        void onLongPress(ClosetItemEntity item);
    }

    private final List<ClosetItemEntity> data = new ArrayList<>();
    private final Listener listener;

    public ClosetAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submitList(List<ClosetItemEntity> items) {
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
        ItemClosetClothingBinding binding = ItemClosetClothingBinding.inflate(inflater, parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ClosetItemEntity item = data.get(position);
        holder.binding.textName.setText(item.name);
        holder.binding.textMeta.setText(item.category + " · " + item.season + " · " + item.style);
        holder.binding.getRoot().setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onLongPress(item);
                return true;
            }
            return false;
        });
        try {
            Uri uri = Uri.parse(item.imageUri);
            holder.binding.imageClothing.setImageURI(null);
            holder.binding.imageClothing.setImageURI(uri);
        } catch (Exception e) {
            holder.binding.imageClothing.setImageURI(null);
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemClosetClothingBinding binding;

        ViewHolder(ItemClosetClothingBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
