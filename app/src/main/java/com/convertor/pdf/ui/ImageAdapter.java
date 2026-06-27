package com.convertor.pdf.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.convertor.pdf.R;

import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {

    public interface Callbacks {
        void onItemClick(int position);
        void onMoveUp(int position);
        void onMoveDown(int position);
    }

    private List<ImageEditActivity.ImageItem> items;
    private Callbacks callbacks;
    private int selectedIndex = -1;

    public ImageAdapter(List<ImageEditActivity.ImageItem> items, Callbacks callbacks) {
        this.items = items;
        this.callbacks = callbacks;
    }

    public void setSelectedIndex(int index) {
        selectedIndex = index;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ImageEditActivity.ImageItem item = items.get(position);

        if (item.displayBitmap != null) {
            holder.thumbnail.setImageBitmap(item.displayBitmap);
        }

        String filterLabel = getFilterLabel(item.filter);
        holder.filterInfo.setText(filterLabel);
        holder.position.setText(String.valueOf(position + 1));

        boolean isSelected = position == selectedIndex;
        holder.itemView.setSelected(isSelected);
        holder.selectionIndicator.setVisibility(isSelected ? View.VISIBLE : View.INVISIBLE);
        if (isSelected) {
            holder.itemView.setStrokeColor(holder.itemView.getContext().getColor(R.color.secondary));
        } else {
            holder.itemView.setStrokeColor(holder.itemView.getContext().getColor(R.color.divider));
        }

        holder.itemView.setOnClickListener(v -> callbacks.onItemClick(position));
        holder.btnUp.setOnClickListener(v -> callbacks.onMoveUp(position));
        holder.btnDown.setOnClickListener(v -> callbacks.onMoveDown(position));
    }

    private String getFilterLabel(String filter) {
        if (filter == null) return "Original";
        switch (filter) {
            case "grayscale": return "Grises";
            case "sepia": return "Sepia";
            case "invert": return "Invertido";
            default: return "Original";
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbnail;
        TextView filterInfo, position;
        View selectionIndicator;
        com.google.android.material.button.MaterialButton btnUp, btnDown;
        com.google.android.material.card.MaterialCardView itemView;

        ViewHolder(View v) {
            super(v);
            thumbnail = v.findViewById(R.id.image_thumb);
            filterInfo = v.findViewById(R.id.text_filter_info);
            position = v.findViewById(R.id.text_position);
            selectionIndicator = v.findViewById(R.id.view_selection_indicator);
            btnUp = v.findViewById(R.id.btn_move_up);
            btnDown = v.findViewById(R.id.btn_move_down);
            itemView = (com.google.android.material.card.MaterialCardView) v;
        }
    }
}
