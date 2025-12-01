package com.example.bestmlawi.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bestmlawi.R;
import com.example.bestmlawi.ui.delivery.Order;

import java.util.List;

public class DeliveryOrderAdapter extends RecyclerView.Adapter<DeliveryOrderAdapter.OrderViewHolder> {

    private List<Order> orderList;
    private OnOrderClickListener listener;

    public interface OnOrderClickListener {
        void onOrderClick(Order order);
        void onAcceptOrder(Order order);
        void onStartDelivery(Order order);
        void onCompleteDelivery(Order order);
    }

    public DeliveryOrderAdapter(List<Order> orderList, OnOrderClickListener listener) {
        this.orderList = orderList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_delivery_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orderList.get(position);
        holder.bind(order, listener);
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        private TextView tvCustomerName, tvDeliveryAddress, tvOrderDate, tvTotalAmount, tvStatus;
        private Button btnAction1, btnAction2;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCustomerName = itemView.findViewById(R.id.tv_customer_name);
            tvDeliveryAddress = itemView.findViewById(R.id.tv_delivery_address);
            tvOrderDate = itemView.findViewById(R.id.tv_order_date);
            tvTotalAmount = itemView.findViewById(R.id.tv_total_amount);
            tvStatus = itemView.findViewById(R.id.tv_status);
            btnAction1 = itemView.findViewById(R.id.btn_action_1);
            btnAction2 = itemView.findViewById(R.id.btn_action_2);
        }

        public void bind(Order order, OnOrderClickListener listener) {
            tvCustomerName.setText(order.getCustomerName());
            tvDeliveryAddress.setText(order.getDeliveryAddress());
            tvOrderDate.setText(order.getFormattedOrderDate());
            tvTotalAmount.setText(order.getFormattedTotal());

            // Configurer le statut avec couleur
            setupStatus(order.getStatus());

            // Configurer les boutons d'action selon le statut
            setupActionButtons(order, listener);

            // Gérer le clic sur l'item
            itemView.setOnClickListener(v -> listener.onOrderClick(order));
        }

        private void setupStatus(String status) {
            tvStatus.setText(getStatusText(status));
            int colorRes;
            switch (status) {
                case "pending":
                    colorRes = R.color.status_pending;
                    break;
                case "assigned":
                    colorRes = R.color.status_assigned;
                    break;
                case "in_progress":
                    colorRes = R.color.status_in_progress;
                    break;
                case "delivered":
                    colorRes = R.color.status_delivered;
                    break;
                default:
                    colorRes = R.color.status_cancelled;
                    break;
            }
            tvStatus.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), colorRes));
        }

        private String getStatusText(String status) {
            switch (status) {
                case "pending": return "En attente";
                case "assigned": return "Assignée";
                case "in_progress": return "En cours";
                case "delivered": return "Livrée";
                case "cancelled": return "Annulée";
                default: return "Inconnu";
            }
        }

        // Dans la méthode bind() de OrderViewHolder
        private void setupActionButtons(Order order, OnOrderClickListener listener) {
            btnAction1.setVisibility(View.VISIBLE);
            btnAction2.setVisibility(View.VISIBLE);

            switch (order.getStatus()) {
                case "assigned":
                    // Commande assignée mais pas encore démarrée
                    btnAction1.setText("Commencer");
                    btnAction2.setText("Détails");
                    btnAction1.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.status_assigned));
                    btnAction1.setOnClickListener(v -> listener.onStartDelivery(order));
                    btnAction2.setOnClickListener(v -> listener.onOrderClick(order));
                    break;

                case "in_progress":
                    // Commande en cours de livraison
                    btnAction1.setText("Terminer");
                    btnAction2.setText("Détails");
                    btnAction1.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.status_in_progress));
                    btnAction1.setOnClickListener(v -> listener.onCompleteDelivery(order));
                    btnAction2.setOnClickListener(v -> listener.onOrderClick(order));
                    break;

                default:
                    btnAction1.setVisibility(View.GONE);
                    btnAction2.setText("Détails");
                    btnAction2.setOnClickListener(v -> listener.onOrderClick(order));
                    break;
            }

            // Style des boutons
            btnAction1.setTextColor(ContextCompat.getColor(itemView.getContext(), android.R.color.white));
            btnAction2.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.status_assigned));
        }
    }
}