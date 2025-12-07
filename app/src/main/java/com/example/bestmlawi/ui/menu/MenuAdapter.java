package com.example.bestmlawi.ui.menu;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bestmlawi.R;

import java.text.DecimalFormat;
import java.util.List;

public class MenuAdapter extends RecyclerView.Adapter<MenuAdapter.MenuViewHolder> {

    private List<Menu> menuList;
    private Context context;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Menu menu);
    }

    public MenuAdapter(List<Menu> menuList, Context context) {
        this.menuList = menuList;
        this.context = context;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public MenuViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_menu, parent, false);
        return new MenuViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MenuViewHolder holder, int position) {
        Menu menu = menuList.get(position);

        holder.textName.setText(menu.getName());
        holder.textDescription.setText(menu.getDescription());

        // Format price with 2 decimal places
        DecimalFormat decimalFormat = new DecimalFormat("#,##0.00 DT");
        holder.textPrice.setText(decimalFormat.format(menu.getPrice()));

        holder.textCategory.setText(menu.getCategory());
        holder.textRating.setText(String.valueOf(menu.getRating()));

        // Load Base64 image
        String base64Image = menu.getImageBase64();
        if (!TextUtils.isEmpty(base64Image)) {
            try {
                byte[] decodedBytes = Base64.decode(base64Image, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                holder.imageMenu.setImageBitmap(bitmap);
            } catch (Exception e) {
                holder.imageMenu.setImageResource(R.drawable.food_placeholder_image);
            }
        } else {
            holder.imageMenu.setImageResource(R.drawable.food_placeholder_image);
        }

        // Set click listener for editing
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(menu);
            }
        });
    }

    @Override
    public int getItemCount() {
        return menuList.size();
    }

    public void updateList(List<Menu> newList) {
        menuList = newList;
        notifyDataSetChanged();
    }

    static class MenuViewHolder extends RecyclerView.ViewHolder {
        ImageView imageMenu;
        TextView textName, textDescription, textPrice, textCategory, textRating;

        public MenuViewHolder(@NonNull View itemView) {
            super(itemView);

            imageMenu = itemView.findViewById(R.id.imageMenu);
            textName = itemView.findViewById(R.id.textName);
            textDescription = itemView.findViewById(R.id.textDescription);
            textPrice = itemView.findViewById(R.id.textPrice);
            textCategory = itemView.findViewById(R.id.textCategory);
            textRating = itemView.findViewById(R.id.textRating);
        }
    }
}