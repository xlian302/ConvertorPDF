package com.convertor.pdf.ui;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
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

        if (item.originalBitmap != null) {
            int w = item.originalBitmap.getWidth();
            int h = item.originalBitmap.getHeight();
            holder.dimensions.setText(w + " x " + h);
        }

        String uriStr = item.uri.toString();
        String name = uriStr.substring(uriStr.lastIndexOf('/') + 1);
        if (name.length() > 20) name = name.substring(0, 20) + "...";
        holder.filename.setText(name);

        if (item.displayBitmap != null) {
            holder.thumbnail.setImageBitmap(item.displayBitmap);
        }

        holder.itemView.setSelected(position == selectedIndex);

        holder.itemView.setOnClickListener(v -> callbacks.onItemClick(position));
        holder.btnUp.setOnClickListener(v -> callbacks.onMoveUp(position));
        holder.btnDown.setOnClickListener(v -> callbacks.onMoveDown(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbnail;
        TextView filename, dimensions;
        ImageButton btnUp, btnDown;

        ViewHolder(View v) {
            super(v);
            thumbnail = v.findViewById(R.id.image_thumbnail);
            filename = v.findViewById(R.id.text_filename);
            dimensions = v.findViewById(R.id.text_dimensions);
            btnUp = v.findViewById(R.id.btn_move_up);
            btnDown = v.findViewById(R.id.btn_move_down);
        }
    }
}
