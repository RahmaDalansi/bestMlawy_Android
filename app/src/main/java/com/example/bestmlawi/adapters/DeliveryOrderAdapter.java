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

    // Constructeur modifié pour être compatible avec ton fragment
    public DeliveryOrderAdapter(List<Order> orderList) {
        this.orderList = orderList;
    }

    // Constructeur avec listener
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
        return orderList != null ? orderList.size() : 0;
    }

    // Méthode pour mettre à jour les données
    public void updateData(List<Order> newOrders) {
        this.orderList = newOrders;
        notifyDataSetChanged();
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        private TextView tvCustomerName, tvDeliveryAddress, tvOrderDate, tvTotalAmount, tvStatus;
        private Button btnAction1, btnAction2;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            // Assure-toi que ces IDs existent dans ton layout item_delivery_order.xml
            tvCustomerName = itemView.findViewById(R.id.tv_customer_name);
            tvDeliveryAddress = itemView.findViewById(R.id.tv_delivery_address);
            tvOrderDate = itemView.findViewById(R.id.tv_order_date);
            tvTotalAmount = itemView.findViewById(R.id.tv_total_amount);
            tvStatus = itemView.findViewById(R.id.tv_status);
            btnAction1 = itemView.findViewById(R.id.btn_action_1);
            btnAction2 = itemView.findViewById(R.id.btn_action_2);
        }

        public void bind(Order order, OnOrderClickListener listener) {
            if (order == null) return;

            // Mettre à jour les TextViews avec des valeurs par défaut
            tvCustomerName.setText(order.getCustomerName() != null ? order.getCustomerName() : "Client");
            tvDeliveryAddress.setText(order.getDeliveryAddress() != null ? order.getDeliveryAddress() : "Adresse non spécifiée");

            if (order.getOrderDate() != null) {
                tvOrderDate.setText(order.getFormattedOrderDate());
            } else {
                tvOrderDate.setText("Date non disponible");
            }

            tvTotalAmount.setText(order.getFormattedTotal() != null ? order.getFormattedTotal() : "0.00 DT");

            // Configurer le statut - VERSION CORRIGÉE
            String status = order.getStatus();
            if (status == null) {
                status = "pending";
            }
            setupStatus(status);

            // Configurer les boutons d'action
            setupActionButtons(order, listener);

            // Gérer le clic sur l'item
            if (listener != null) {
                itemView.setOnClickListener(v -> listener.onOrderClick(order));
            }
        }

        private void setupStatus(String status) {
            String statusText = getStatusText(status);
            tvStatus.setText(statusText);

            int colorRes;
            // Utiliser les mêmes valeurs que dans ta base de données
            switch (status) {
                case "Prêt":
                case "ready":
                case "assigned":
                    colorRes = R.color.status_assigned;
                    break;
                case "En cours":
                case "in_progress":
                    colorRes = R.color.status_in_progress;
                    break;
                case "Livré":
                case "delivered":
                    colorRes = R.color.status_delivered;
                    break;
                case "pending":
                    colorRes = R.color.status_pending;
                    break;
                case "cancelled":
                    colorRes = R.color.status_cancelled;
                    break;
                default:
                    colorRes = R.color.status_pending;
                    break;
            }

            try {
                tvStatus.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), colorRes));
            } catch (Exception e) {
                // Si la couleur n'existe pas, utiliser une couleur par défaut
                tvStatus.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.status_pending));
            }
        }

        private String getStatusText(String status) {
            switch (status) {
                case "Prêt":
                case "ready":
                case "assigned": return "Assignée";
                case "En cours":
                case "in_progress": return "En cours";
                case "Livré":
                case "delivered": return "Livrée";
                case "pending": return "En attente";
                case "cancelled": return "Annulée";
                default: return status; // Retourne le statut tel quel si inconnu
            }
        }

        private void setupActionButtons(Order order, OnOrderClickListener listener) {
            if (listener == null) {
                btnAction1.setVisibility(View.GONE);
                btnAction2.setVisibility(View.GONE);
                return;
            }

            String status = order.getStatus();
            if (status == null) {
                status = "pending";
            }

            btnAction1.setVisibility(View.VISIBLE);
            btnAction2.setVisibility(View.VISIBLE);

            switch (status) {
                case "Prêt":
                case "ready":
                case "assigned":
                    // Commande assignée
                    btnAction1.setText("COMMENCER");
                    btnAction2.setText("DÉTAILS");
                    setButtonStyle(btnAction1, R.color.status_in_progress, android.R.color.white);
                    setButtonStyle(btnAction2, android.R.color.transparent, R.color.status_assigned);
                    btnAction1.setOnClickListener(v -> listener.onStartDelivery(order));
                    btnAction2.setOnClickListener(v -> listener.onOrderClick(order));
                    break;

                case "En cours":
                case "in_progress":
                    // Commande en cours
                    btnAction1.setText("TERMINER");
                    btnAction2.setText("DÉTAILS");
                    setButtonStyle(btnAction1, R.color.status_delivered, android.R.color.white);
                    setButtonStyle(btnAction2, android.R.color.transparent, R.color.status_in_progress);
                    btnAction1.setOnClickListener(v -> listener.onCompleteDelivery(order));
                    btnAction2.setOnClickListener(v -> listener.onOrderClick(order));
                    break;

                case "pending":
                    // Commande en attente
                    btnAction1.setText("ACCEPTER");
                    btnAction2.setText("DÉTAILS");
                    setButtonStyle(btnAction1, R.color.status_assigned, android.R.color.white);
                    setButtonStyle(btnAction2, android.R.color.transparent, R.color.status_pending);
                    btnAction1.setOnClickListener(v -> listener.onAcceptOrder(order));
                    btnAction2.setOnClickListener(v -> listener.onOrderClick(order));
                    break;

                default:
                    // Autres statuts (livrée, annulée)
                    btnAction1.setVisibility(View.GONE);
                    btnAction2.setText("VOIR DÉTAILS");
                    setButtonStyle(btnAction2, android.R.color.transparent, R.color.status_delivered);
                    btnAction2.setOnClickListener(v -> listener.onOrderClick(order));
                    break;
            }
        }

        private void setButtonStyle(Button button, int backgroundColorRes, int textColorRes) {
            button.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), backgroundColorRes));
            button.setTextColor(ContextCompat.getColor(itemView.getContext(), textColorRes));

            // Ajouter du padding pour un meilleur look
            button.setPadding(16, 8, 16, 8);
            button.setAllCaps(false);
        }
    }
}