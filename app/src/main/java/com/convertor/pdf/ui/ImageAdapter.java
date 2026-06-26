package com.convertor.pdf.ui;

import android.graphics.Bitmap;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.convertor.pdf.R;
import com.convertor.pdf.utils.FileUtils;

import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {

    private final List<ImageEditActivity.ImageItem> items;
    private final Callbacks callbacks;
    private int selectedIndex = -1;

    public interface Callbacks {
        void onItemClick(int position);
        void onMoveUp(int position);
        void onMoveDown(int position);
    }

    public ImageAdapter(List<ImageEditActivity.ImageItem> items, Callbacks callbacks) {
        this.items = items;
        this.callbacks = callbacks;
    }

    public void setSelectedIndex(int index) {
        int old = selectedIndex;
        selectedIndex = index;
        if (old >= 0) notifyItemChanged(old);
        notifyItemChanged(index);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ImageEditActivity.ImageItem item = items.get(position);
        Bitmap display = item.displayBitmap != null ? item.displayBitmap : item.originalBitmap;
        if (display != null) holder.thumbnail.setImageBitmap(display);
        String fileName = FileUtils.getFileName(holder.itemView.getContext(), item.uri);
        holder.filename.setText(fileName);
        if (display != null) {
            holder.dimensions.setText(display.getWidth() + " x " + display.getHeight());
        }
        holder.itemView.setSelected(position == selectedIndex);
        holder.itemView.setOnClickListener(v -> callbacks.onItemClick(position));
        holder.btnMoveUp.setOnClickListener(v -> callbacks.onMoveUp(position));
        holder.btnMoveDown.setOnClickListener(v -> callbacks.onMoveDown(position));
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbnail;
        TextView filename, dimensions;
        ImageButton btnMoveUp, btnMoveDown;

        ViewHolder(View v) {
            super(v);
            thumbnail = v.findViewById(R.id.image_thumbnail);
            filename = v.findViewById(R.id.text_filename);
            dimensions = v.findViewById(R.id.text_dimensions);
            btnMoveUp = v.findViewById(R.id.btn_move_up);
            btnMoveDown = v.findViewById(R.id.btn_move_down);
        }
    }
}
