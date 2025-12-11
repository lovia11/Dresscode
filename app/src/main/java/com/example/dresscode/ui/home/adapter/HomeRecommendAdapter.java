package com.example.dresscode.ui.home.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dresscode.databinding.ItemHomeRecommendBinding;

import java.util.List;

public class HomeRecommendAdapter extends RecyclerView.Adapter<HomeRecommendAdapter.ViewHolder> {

    public static class RecommendItem {
        public final String title;
        public final String meta;

        public RecommendItem(String title, String meta) {
            this.title = title;
            this.meta = meta;
        }
    }

    private final List<RecommendItem> data;

    public HomeRecommendAdapter(List<RecommendItem> data) {
        this.data = data;
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
