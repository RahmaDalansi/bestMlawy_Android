package com.example.bestmlawi.ui.deliver;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.example.bestmlawi.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class DeliveryProfileFragment extends Fragment {

    private TextView tvName, tvEmail, tvPhone, tvAddress, tvDeliveryStats;
    private TextView tvCompletedDeliveries, tvRating, tvMonthlyGoal, tvProgress, tvAvgDeliveryTime;
    private Button btnEditProfile;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_delivery_profile, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeViews(view);
        setupClickListeners();
        loadDeliveryProfile();

        return view;
    }

    private void initializeViews(View view) {
        tvName = view.findViewById(R.id.tv_delivery_name);
        tvEmail = view.findViewById(R.id.tv_delivery_email);
        tvPhone = view.findViewById(R.id.tv_delivery_phone);
        tvAddress = view.findViewById(R.id.tv_delivery_address);
        tvDeliveryStats = view.findViewById(R.id.tv_delivery_stats);

        tvCompletedDeliveries = view.findViewById(R.id.tv_completed_deliveries);
        tvRating = view.findViewById(R.id.tv_rating);
        tvMonthlyGoal = view.findViewById(R.id.tv_monthly_goal);
        tvProgress = view.findViewById(R.id.tv_progress);
        tvAvgDeliveryTime = view.findViewById(R.id.tv_avg_delivery_time);

        btnEditProfile = view.findViewById(R.id.btn_edit_profile);
    }

    private void setupClickListeners() {
        btnEditProfile.setOnClickListener(v -> {
            // Ouvrir l'activité d'édition du profil
            // Intent intent = new Intent(getActivity(), EditProfileActivity.class);
            // startActivity(intent);
        });
    }

    private void loadDeliveryProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            db.collection("users").document(currentUser.getUid())
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                // Informations de base
                                tvName.setText(document.getString("name"));
                                tvEmail.setText(document.getString("email"));
                                tvPhone.setText(document.getString("phoneNumber"));

                                String address = document.getString("address");
                                if (address != null && !address.isEmpty()) {
                                    tvAddress.setText(address);
                                }

                                // Charger les statistiques
                                loadDeliveryStats(currentUser.getUid());
                            }
                        }
                    });
        }
    }

    private void loadDeliveryStats(String userId) {
        db.collection("delivery_stats").document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            // Statistiques de base
                            Long completed = document.getLong("completedDeliveries");
                            Double rating = document.getDouble("rating");
                            Long monthlyGoal = document.getLong("monthlyGoal");
                            Long currentProgress = document.getLong("currentProgress");
                            Long avgTime = document.getLong("averageDeliveryTime");

                            if (completed != null) {
                                tvCompletedDeliveries.setText(String.valueOf(completed));
                            }

                            if (rating != null) {
                                tvRating.setText(String.format("%.1f", rating));
                            }

                            if (monthlyGoal != null) {
                                tvMonthlyGoal.setText(monthlyGoal + " livraisons");
                            }

                            if (currentProgress != null && monthlyGoal != null) {
                                tvProgress.setText(currentProgress + "/" + monthlyGoal);
                                // Mettre à jour la barre de progression
                                // progressMonthly.setProgress((int) ((currentProgress * 100) / monthlyGoal));
                            }

                            if (avgTime != null) {
                                tvAvgDeliveryTime.setText(avgTime + " min");
                            }

                            // Statistiques détaillées
                            String stats = buildStatsText(document);
                            tvDeliveryStats.setText(stats);
                        } else {
                            // Document de stats non trouvé, initialiser avec des valeurs par défaut
                            initializeDefaultStats();
                        }
                    }
                });
    }

    private String buildStatsText(DocumentSnapshot document) {
        StringBuilder stats = new StringBuilder();

        Long totalDeliveries = document.getLong("totalDeliveries");
        Long onTimeDeliveries = document.getLong("onTimeDeliveries");
        Double successRate = document.getDouble("successRate");

        if (totalDeliveries != null) {
            stats.append("• Total livraisons: ").append(totalDeliveries).append("\n");
        }

        if (onTimeDeliveries != null && totalDeliveries != null && totalDeliveries > 0) {
            double onTimeRate = (onTimeDeliveries * 100.0) / totalDeliveries;
            stats.append("• Livraisons à temps: ").append(String.format("%.1f", onTimeRate)).append("%\n");
        }

        if (successRate != null) {
            stats.append("• Taux de réussite: ").append(String.format("%.1f", successRate)).append("%\n");
        }

        return stats.toString();
    }

    private void initializeDefaultStats() {
        tvCompletedDeliveries.setText("0");
        tvRating.setText("0.0");
        tvMonthlyGoal.setText("50 livraisons");
        tvProgress.setText("0/50");
        tvAvgDeliveryTime.setText("-- min");
        tvDeliveryStats.setText("Aucune statistique disponible\nCommencez vos livraisons !");
    }
}