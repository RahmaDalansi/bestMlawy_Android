package com.example.bestmlawi.ui.delivery;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.bestmlawi.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class DeliveryStatsFragment extends Fragment {

    private TextView tvCompletedDeliveries, tvRating, tvSuccessRate, tvAvgTime;
    private TextView tvMonthlyGoal, tvCurrentProgress, tvProgressPercent;
    private TextView tvMotivation, tvOnTimeDeliveries, tvLateDeliveries;
    private TextView tvMonthlyEarnings, tvBestDay;
    private ProgressBar progressMonthly;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_delivery_stats, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeViews(view);
        loadDeliveryStats();

        return view;
    }

    private void initializeViews(View view) {
        // Statistiques principales
        tvCompletedDeliveries = view.findViewById(R.id.tv_completed_deliveries);
        tvRating = view.findViewById(R.id.tv_rating);
        tvSuccessRate = view.findViewById(R.id.tv_success_rate);
        tvAvgTime = view.findViewById(R.id.tv_avg_delivery_time);

        // Objectif mensuel
        tvMonthlyGoal = view.findViewById(R.id.tv_monthly_goal);
        tvCurrentProgress = view.findViewById(R.id.tv_current_progress);
        tvProgressPercent = view.findViewById(R.id.tv_progress_percent);
        tvMotivation = view.findViewById(R.id.tv_motivation);
        progressMonthly = view.findViewById(R.id.progress_monthly);

        // Statistiques détaillées
        tvOnTimeDeliveries = view.findViewById(R.id.tv_on_time_deliveries);
        tvLateDeliveries = view.findViewById(R.id.tv_late_deliveries);
        tvMonthlyEarnings = view.findViewById(R.id.tv_monthly_earnings);
        tvBestDay = view.findViewById(R.id.tv_best_day);
    }

    private void loadDeliveryStats() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        db.collection("delivery_stats").document(currentUser.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            updateStatsUI(document);
                        } else {
                            initializeDefaultStats();
                        }
                    } else {
                        initializeDefaultStats();
                    }
                });
    }

    private void updateStatsUI(DocumentSnapshot document) {
        try {
            // Statistiques de base
            Long completed = document.getLong("completedDeliveries");
            Double rating = document.getDouble("rating");
            Double successRate = document.getDouble("successRate");
            Long avgTime = document.getLong("averageDeliveryTime");

            // Objectif mensuel
            Long monthlyGoal = document.getLong("monthlyGoal");
            Long currentProgress = document.getLong("currentProgress");

            // Statistiques détaillées
            Long onTimeDeliveries = document.getLong("onTimeDeliveries");
            Long lateDeliveries = document.getLong("lateDeliveries");
            Double monthlyEarnings = document.getDouble("monthlyEarnings");
            Long bestDay = document.getLong("bestDayRecord");

            // Mettre à jour l'interface avec gestion des valeurs nulles
            updateBasicStats(completed, rating, successRate, avgTime);
            updateMonthlyGoal(monthlyGoal, currentProgress);
            updateDetailedStats(onTimeDeliveries, lateDeliveries, monthlyEarnings, bestDay);

        } catch (Exception e) {
            e.printStackTrace();
            initializeDefaultStats();
        }
    }

    private void updateBasicStats(Long completed, Double rating, Double successRate, Long avgTime) {
        // Livraisons complétées
        if (completed != null) {
            tvCompletedDeliveries.setText(String.valueOf(completed));
        } else {
            tvCompletedDeliveries.setText("0");
        }

        // Note (format: X.X/5)
        if (rating != null) {
            tvRating.setText(String.format("%.1f/5", rating));
        } else {
            tvRating.setText("0.0/5");
        }

        // Taux de réussite
        if (successRate != null) {
            tvSuccessRate.setText(String.format("%.1f%%", successRate));
        } else {
            tvSuccessRate.setText("0%");
        }

        // Temps moyen
        if (avgTime != null && avgTime > 0) {
            tvAvgTime.setText(avgTime + " min");
        } else {
            tvAvgTime.setText("-- min");
        }
    }

    private void updateMonthlyGoal(Long monthlyGoal, Long currentProgress) {
        // Objectif
        if (monthlyGoal != null) {
            tvMonthlyGoal.setText(monthlyGoal + " livraisons");
        } else {
            tvMonthlyGoal.setText("50 livraisons");
        }

        // Progression
        if (currentProgress != null && monthlyGoal != null) {
            String progressText = currentProgress + "/" + monthlyGoal;
            tvCurrentProgress.setText(progressText);

            // Pourcentage et barre de progression
            if (monthlyGoal > 0) {
                int progressPercent = (int) ((currentProgress * 100) / monthlyGoal);
                progressMonthly.setProgress(Math.min(progressPercent, 100));
                tvProgressPercent.setText(progressPercent + "%");

                // Message de motivation
                updateMotivationMessage(currentProgress, monthlyGoal, progressPercent);
            }
        } else {
            tvCurrentProgress.setText("0/50");
            progressMonthly.setProgress(0);
            tvProgressPercent.setText("0%");
            tvMotivation.setText("Définissez votre premier objectif !");
        }
    }

    private void updateMotivationMessage(Long currentProgress, Long monthlyGoal, int progressPercent) {
        if (progressPercent >= 100) {
            tvMotivation.setText("🎉 Objectif atteint ! Félicitations !");
            tvMotivation.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else if (progressPercent >= 75) {
            tvMotivation.setText("Presque là ! Continuez comme ça !");
            tvMotivation.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
        } else if (progressPercent >= 50) {
            tvMotivation.setText("À mi-chemin ! Excellent travail !");
            tvMotivation.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
        } else if (progressPercent >= 25) {
            tvMotivation.setText("Bien commencé ! Gardez le rythme !");
            tvMotivation.setTextColor(getResources().getColor(android.R.color.holo_purple));
        } else {
            tvMotivation.setText("Commencez votre journée avec détermination !");
            tvMotivation.setTextColor(getResources().getColor(android.R.color.darker_gray));
        }
    }

    private void updateDetailedStats(Long onTimeDeliveries, Long lateDeliveries,
                                     Double monthlyEarnings, Long bestDay) {
        // Livraisons à temps
        if (onTimeDeliveries != null) {
            tvOnTimeDeliveries.setText(String.valueOf(onTimeDeliveries));
        } else {
            tvOnTimeDeliveries.setText("0");
        }

        // Retards
        if (lateDeliveries != null) {
            tvLateDeliveries.setText(String.valueOf(lateDeliveries));
        } else {
            tvLateDeliveries.setText("0");
        }

        // Revenus
        if (monthlyEarnings != null) {
            tvMonthlyEarnings.setText(String.format("%.2f DT", monthlyEarnings));
        } else {
            tvMonthlyEarnings.setText("0.00 DT");
        }

        // Meilleur jour
        if (bestDay != null && bestDay > 0) {
            tvBestDay.setText(bestDay + " livraisons");
        } else {
            tvBestDay.setText("--");
        }
    }

    private void initializeDefaultStats() {
        // Statistiques de base
        tvCompletedDeliveries.setText("0");
        tvRating.setText("0.0/5");
        tvSuccessRate.setText("0%");
        tvAvgTime.setText("-- min");

        // Objectif mensuel
        tvMonthlyGoal.setText("50 livraisons");
        tvCurrentProgress.setText("0/50");
        progressMonthly.setProgress(0);
        tvProgressPercent.setText("0%");
        tvMotivation.setText("Commencez votre premier objectif !");
        tvMotivation.setTextColor(getResources().getColor(android.R.color.darker_gray));

        // Statistiques détaillées
        tvOnTimeDeliveries.setText("0");
        tvLateDeliveries.setText("0");
        tvMonthlyEarnings.setText("0.00 DT");
        tvBestDay.setText("--");
    }

    @Override
    public void onResume() {
        super.onResume();
        loadDeliveryStats();
    }
}