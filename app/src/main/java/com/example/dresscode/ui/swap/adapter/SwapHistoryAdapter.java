package com.example.dresscode.ui.swap.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dresscode.data.local.SwapHistoryRow;
import com.example.dresscode.databinding.ItemSwapHistoryBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SwapHistoryAdapter extends RecyclerView.Adapter<SwapHistoryAdapter.ViewHolder> {

    public interface Listener {
        void onLongPress(SwapHistoryRow row);
    }

    private final List<SwapHistoryRow> data = new ArrayList<>();
    private final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA);
    private final Listener listener;

    public SwapHistoryAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submitList(List<SwapHistoryRow> items) {
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
        ItemSwapHistoryBinding binding = ItemSwapHistoryBinding.inflate(inflater, parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SwapHistoryRow row = data.get(position);
        holder.binding.textTitle.setText(row.sourceTitle + " · " + row.status);
        holder.binding.textTime.setText(formatter.format(new Date(row.createdAt)));
        holder.binding.getRoot().setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onLongPress(row);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemSwapHistoryBinding binding;

        ViewHolder(ItemSwapHistoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
