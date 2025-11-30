package com.example.bestmlawi.ui.orders;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.example.bestmlawi.R;

import java.util.List;

public class OrderItemAdapter extends ArrayAdapter<OrderLineItem> {

    private Context context;
    private List<OrderLineItem> items;

    public OrderItemAdapter(Context context, List<OrderLineItem> items) {
        super(context, 0, items);
        this.context = context;
        this.items = items;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_order_item, parent, false);
        }

        OrderLineItem item = items.get(position);

        TextView txtItemName = convertView.findViewById(R.id.txtItemName);
        TextView txtItemDescription = convertView.findViewById(R.id.txtItemDescription);
        TextView txtItemPrice = convertView.findViewById(R.id.txtItemPrice);
        TextView txtItemQuantity = convertView.findViewById(R.id.txtItemQuantity);

        // Ici, on va chercher le nom et la description depuis menu_items
        // Pour l'instant, on met des valeurs par défaut
        txtItemName.setText("Article #" + item.getMenuItemId());
        txtItemDescription.setText("Description non disponible");
        txtItemPrice.setText(item.getQuantity() + " x " + item.getMenuItemId() + " DT");
        txtItemQuantity.setText("Qté: " + item.getQuantity());

        return convertView;
    }
}