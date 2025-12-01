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
        tvCompletedDeliveries = view.findViewById(R.id.tv_completed_deliveries);
        tvRating = view.findViewById(R.id.tv_rating);
        tvSuccessRate = view.findViewById(R.id.tv_success_rate);
        tvAvgTime = view.findViewById(R.id.tv_avg_delivery_time);

        tvMonthlyGoal = view.findViewById(R.id.tv_monthly_goal);
        tvCurrentProgress = view.findViewById(R.id.tv_current_progress);
        tvProgressPercent = view.findViewById(R.id.tv_progress_percent);
        progressMonthly = view.findViewById(R.id.progress_monthly);
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
                    }
                });
    }

    private void updateStatsUI(DocumentSnapshot document) {
        // Statistiques de base
        Long completed = document.getLong("completedDeliveries");
        Double rating = document.getDouble("rating");
        Double successRate = document.getDouble("successRate");
        Long avgTime = document.getLong("averageDeliveryTime");

        Long monthlyGoal = document.getLong("monthlyGoal");
        Long currentProgress = document.getLong("currentProgress");

        if (completed != null) {
            tvCompletedDeliveries.setText(String.valueOf(completed));
        }

        if (rating != null) {
            tvRating.setText(String.format("%.1f/5", rating));
        }

        if (successRate != null) {
            tvSuccessRate.setText(String.format("%.1f%%", successRate));
        }

        if (avgTime != null) {
            tvAvgTime.setText(avgTime + " min");
        }

        if (monthlyGoal != null && currentProgress != null) {
            tvMonthlyGoal.setText(String.valueOf(monthlyGoal));
            tvCurrentProgress.setText(String.valueOf(currentProgress));

            int progress = (int) ((currentProgress * 100) / monthlyGoal);
            progressMonthly.setProgress(progress);
            tvProgressPercent.setText(progress + "%");
        }
    }

    private void initializeDefaultStats() {
        tvCompletedDeliveries.setText("0");
        tvRating.setText("0.0/5");
        tvSuccessRate.setText("0%");
        tvAvgTime.setText("-- min");

        tvMonthlyGoal.setText("50");
        tvCurrentProgress.setText("0");
        progressMonthly.setProgress(0);
        tvProgressPercent.setText("0%");
    }

    @Override
    public void onResume() {
        super.onResume();
        loadDeliveryStats();
    }
}