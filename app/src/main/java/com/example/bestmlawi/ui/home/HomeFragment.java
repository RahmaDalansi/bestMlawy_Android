package com.example.bestmlawi.ui.home;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.bestmlawi.R;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeFragment extends Fragment {

    private HomeViewModel homeViewModel;

    // KPI Cards
    private TextView tvTotalOrders, tvTotalRevenue, tvActiveUsers, tvCompletedDeliveries;
    private TextView tvMonthlyGrowth, tvAvgOrderValue, tvPendingOrders, tvTodayOrders;

    // Charts
    private BarChart barChartOrders;
    private LineChart lineChartRevenue;
    private PieChart pieChartStatus;

    // Refresh
    private SwipeRefreshLayout swipeRefreshLayout;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialiser Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialiser les vues
        initializeViews(root);

        // Configurer les graphiques AVANT de charger les données
        setupCharts();

        // AJOUT CRITIQUE : Configurer les interactions des graphiques
        configureChartInteractions();

        // Charger les données
        loadDashboardData();

        // Configurer le swipe to refresh
        setupSwipeRefresh();

        return root;
    }

    private void initializeViews(View view) {
        // KPI Cards
        tvTotalOrders = view.findViewById(R.id.tv_total_orders);
        tvTotalRevenue = view.findViewById(R.id.tv_total_revenue);
        tvActiveUsers = view.findViewById(R.id.tv_active_users);
        tvCompletedDeliveries = view.findViewById(R.id.tv_completed_deliveries);
        tvMonthlyGrowth = view.findViewById(R.id.tv_monthly_growth);
        tvAvgOrderValue = view.findViewById(R.id.tv_avg_order_value);
        tvPendingOrders = view.findViewById(R.id.tv_pending_orders);
        tvTodayOrders = view.findViewById(R.id.tv_today_orders);

        // Charts
        barChartOrders = view.findViewById(R.id.bar_chart_orders);
        lineChartRevenue = view.findViewById(R.id.line_chart_revenue);
        pieChartStatus = view.findViewById(R.id.pie_chart_status);

        // Refresh
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_dashboard);
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadDashboardData();
        });
    }

    private void loadDashboardData() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(true);
        }

        // Charger toutes les données en parallèle
        loadOrdersData();
        loadUsersData();
        loadRevenueData();
        loadTodayOrders();
    }

    private void loadOrdersData() {
        db.collection("orders").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && getActivity() != null) {
                int totalOrders = task.getResult().size();
                tvTotalOrders.setText(String.valueOf(totalOrders));

                // Commandes complétées
                long completed = task.getResult().getDocuments().stream()
                        .filter(doc -> "Livré".equals(doc.getString("status")) ||
                                "delivered".equals(doc.getString("status")))
                        .count();
                tvCompletedDeliveries.setText(String.valueOf(completed));

                // Commandes en attente
                long pending = task.getResult().getDocuments().stream()
                        .filter(doc -> "Prêt".equals(doc.getString("status")) ||
                                "pending".equals(doc.getString("status")) ||
                                "assigned".equals(doc.getString("status")))
                        .count();
                tvPendingOrders.setText(String.valueOf(pending));

                // Valeur moyenne des commandes
                double totalAmount = 0;
                for (QueryDocumentSnapshot doc : task.getResult()) {
                    Double amount = doc.getDouble("totalAmount");
                    if (amount == null) {
                        amount = doc.getDouble("amount");
                    }
                    if (amount == null) {
                        String amountStr = doc.getString("totalAmount");
                        if (amountStr != null) {
                            try {
                                amount = Double.parseDouble(amountStr);
                            } catch (NumberFormatException e) {
                                amount = 0.0;
                            }
                        }
                    }
                    if (amount != null) totalAmount += amount;
                }
                double avgOrderValue = totalOrders > 0 ? totalAmount / totalOrders : 0;
                tvAvgOrderValue.setText(String.format("%.2f DT", avgOrderValue));

                // Préparer les graphiques
                prepareOrderChartData(task.getResult().getDocuments());
                prepareStatusChartData(task.getResult().getDocuments());

                swipeRefreshLayout.setRefreshing(false);
            } else {
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
            }
        });
    }

    private void loadUsersData() {
        db.collection("users").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && getActivity() != null) {
                int totalUsers = task.getResult().size();
                tvActiveUsers.setText(String.valueOf(totalUsers));
            }
        });
    }

    private void loadRevenueData() {
        db.collection("orders")
                .whereIn("status", java.util.Arrays.asList("Livré", "delivered", "En cours"))
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && getActivity() != null) {
                        double totalRevenue = 0;
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            Double amount = doc.getDouble("totalAmount");
                            if (amount == null) {
                                amount = doc.getDouble("amount");
                            }
                            if (amount != null) totalRevenue += amount;
                        }
                        tvTotalRevenue.setText(String.format("%.2f DT", totalRevenue));

                        double monthlyGrowth = 8.5;
                        tvMonthlyGrowth.setText(String.format("+%.1f%%", monthlyGrowth));

                        prepareRevenueChartData();
                    }
                });
    }

    private void loadTodayOrders() {
        Calendar calendar = Calendar.getInstance();
        Date today = calendar.getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayStr = sdf.format(today);

        db.collection("orders")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && getActivity() != null) {
                        long todayCount = task.getResult().getDocuments().stream()
                                .filter(doc -> {
                                    Object orderDate = doc.get("orderDate");
                                    if (orderDate instanceof com.google.firebase.Timestamp) {
                                        Date date = ((com.google.firebase.Timestamp) orderDate).toDate();
                                        String dateStr = sdf.format(date);
                                        return dateStr.equals(todayStr);
                                    }
                                    return false;
                                })
                                .count();
                        tvTodayOrders.setText(String.valueOf(todayCount));
                    }
                });
    }

    private void setupCharts() {
        if (barChartOrders != null) setupBarChart();
        if (lineChartRevenue != null) setupLineChart();
        if (pieChartStatus != null) setupPieChart();
    }

    // MÉTHODE CRITIQUE : Configuration des interactions des graphiques
    private void configureChartInteractions() {
        // Pour BarChart
        if (barChartOrders != null) {
            barChartOrders.setTouchEnabled(false);
            barChartOrders.setClickable(false);
            barChartOrders.setFocusable(false);
            barChartOrders.setFocusableInTouchMode(false);
            barChartOrders.setDragEnabled(false);
            barChartOrders.setScaleEnabled(false);
            barChartOrders.setPinchZoom(false);
            barChartOrders.setDoubleTapToZoomEnabled(false);
            barChartOrders.setHighlightPerTapEnabled(false);
            barChartOrders.setHighlightPerDragEnabled(false);

            // SetOnTouchListener CRITIQUE pour permettre le scroll
            barChartOrders.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, android.view.MotionEvent event) {
                    // Toujours retourner false pour permettre au parent de gérer le touch
                    // Et permettre au NestedScrollView de scroller
                    v.getParent().requestDisallowInterceptTouchEvent(false);
                    return false;
                }
            });
        }

        // Pour LineChart
        if (lineChartRevenue != null) {
            lineChartRevenue.setTouchEnabled(false);
            lineChartRevenue.setClickable(false);
            lineChartRevenue.setFocusable(false);
            lineChartRevenue.setFocusableInTouchMode(false);
            lineChartRevenue.setDragEnabled(false);
            lineChartRevenue.setScaleEnabled(false);
            lineChartRevenue.setPinchZoom(false);
            lineChartRevenue.setDoubleTapToZoomEnabled(false);
            lineChartRevenue.setHighlightPerTapEnabled(false);
            lineChartRevenue.setHighlightPerDragEnabled(false);

            lineChartRevenue.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, android.view.MotionEvent event) {
                    v.getParent().requestDisallowInterceptTouchEvent(false);
                    return false;
                }
            });
        }

        // Pour PieChart
        if (pieChartStatus != null) {
            pieChartStatus.setTouchEnabled(false);
            pieChartStatus.setClickable(false);
            pieChartStatus.setFocusable(false);
            pieChartStatus.setFocusableInTouchMode(false);
            pieChartStatus.setRotationEnabled(false);
            pieChartStatus.setHighlightPerTapEnabled(false);
            //pieChartStatus.setHighlightPerDragEnabled(false);

            pieChartStatus.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, android.view.MotionEvent event) {
                    v.getParent().requestDisallowInterceptTouchEvent(false);
                    return false;
                }
            });
        }
    }

    private void setupBarChart() {
        barChartOrders.getDescription().setEnabled(false);
        barChartOrders.setDrawGridBackground(false);
        barChartOrders.setDrawBarShadow(false);
        barChartOrders.setDrawValueAboveBar(true);

        XAxis xAxis = barChartOrders.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(7);
        xAxis.setTextColor(Color.DKGRAY);
        xAxis.setTextSize(10f);

        YAxis leftAxis = barChartOrders.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setSpaceTop(15f);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setTextColor(Color.DKGRAY);
        leftAxis.setTextSize(10f);

        barChartOrders.getAxisRight().setEnabled(false);
        barChartOrders.getLegend().setEnabled(false);
    }

    private void setupLineChart() {
        lineChartRevenue.getDescription().setEnabled(false);
        lineChartRevenue.setDrawGridBackground(false);

        XAxis xAxis = lineChartRevenue.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(Color.DKGRAY);
        xAxis.setTextSize(10f);

        YAxis leftAxis = lineChartRevenue.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setTextColor(Color.DKGRAY);
        leftAxis.setTextSize(10f);

        lineChartRevenue.getAxisRight().setEnabled(false);
        lineChartRevenue.getLegend().setEnabled(false);
    }

    private void setupPieChart() {
        pieChartStatus.getDescription().setEnabled(false);
        pieChartStatus.setDrawHoleEnabled(true);
        pieChartStatus.setHoleColor(Color.WHITE);
        pieChartStatus.setTransparentCircleColor(Color.WHITE);
        pieChartStatus.setTransparentCircleAlpha(110);
        pieChartStatus.setHoleRadius(45f);
        pieChartStatus.setTransparentCircleRadius(50f);
        pieChartStatus.setDrawCenterText(true);

        pieChartStatus.getLegend().setEnabled(true);
        pieChartStatus.getLegend().setTextColor(Color.DKGRAY);
        pieChartStatus.getLegend().setTextSize(11f);
        pieChartStatus.getLegend().setFormSize(12f);
        pieChartStatus.getLegend().setFormToTextSpace(5f);
    }

    private void prepareOrderChartData(List<DocumentSnapshot> documents) {
        if (barChartOrders == null || getActivity() == null) return;

        List<String> days = new ArrayList<>();
        List<BarEntry> entries = new ArrayList<>();

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("EEE", Locale.getDefault());

        for (int i = 6; i >= 0; i--) {
            calendar.add(Calendar.DAY_OF_YEAR, -1);
            String dayName = sdf.format(calendar.getTime());
            days.add(dayName);

            int count = (int) (Math.random() * 30) + 10;
            entries.add(new BarEntry(6 - i, count));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Commandes");
        dataSet.setColor(getResources().getColor(R.color.chart_bar));
        dataSet.setValueTextColor(Color.DKGRAY);
        dataSet.setValueTextSize(10f);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.6f);
        barData.setValueTextSize(10f);

        barChartOrders.getXAxis().setValueFormatter(new IndexAxisValueFormatter(days));
        barChartOrders.setData(barData);
        barChartOrders.invalidate();
    }

    private void prepareRevenueChartData() {
        if (lineChartRevenue == null || getActivity() == null) return;

        List<String> months = new ArrayList<>();
        List<Entry> entries = new ArrayList<>();

        String[] monthNames = {"Jan", "Fév", "Mar", "Avr", "Mai", "Jun"};

        for (int i = 0; i < 6; i++) {
            months.add(monthNames[i]);
            float revenue = (float) (Math.random() * 3000) + 1000;
            entries.add(new Entry(i, revenue));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Revenus (DT)");
        dataSet.setColor(getResources().getColor(R.color.chart_line));
        dataSet.setCircleColor(getResources().getColor(R.color.chart_line));
        dataSet.setLineWidth(2.5f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextColor(Color.DKGRAY);
        dataSet.setValueTextSize(10f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(getResources().getColor(R.color.chart_line_light));
        dataSet.setFillAlpha(100);

        LineData lineData = new LineData(dataSet);
        lineData.setValueTextSize(10f);

        lineChartRevenue.getXAxis().setValueFormatter(new IndexAxisValueFormatter(months));
        lineChartRevenue.getXAxis().setLabelCount(6);
        lineChartRevenue.setData(lineData);
        lineChartRevenue.invalidate();
    }

    private void prepareStatusChartData(List<DocumentSnapshot> documents) {
        if (pieChartStatus == null || getActivity() == null) return;

        Map<String, Integer> statusCount = new HashMap<>();

        for (DocumentSnapshot doc : documents) {
            String status = doc.getString("status");
            if (status == null) status = "Inconnu";

            if (status.equals("Livré") || status.equals("delivered")) {
                status = "Livré";
            } else if (status.equals("Prêt") || status.equals("ready")) {
                status = "Prêt";
            } else if (status.equals("En cours") || status.equals("in_progress")) {
                status = "En cours";
            } else if (status.equals("pending") || status.equals("attente")) {
                status = "En attente";
            } else if (status.equals("cancelled") || status.equals("annulé")) {
                status = "Annulé";
            }

            statusCount.put(status, statusCount.getOrDefault(status, 0) + 1);
        }

        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : statusCount.entrySet()) {
            entries.add(new PieEntry(entry.getValue(), entry.getKey()));
        }

        if (entries.isEmpty()) {
            entries.add(new PieEntry(1, "Aucune donnée"));
        }

        PieDataSet dataSet = new PieDataSet(entries, "Statut des Commandes");

        int[] colors = new int[]{
                getResources().getColor(R.color.status_delivered),
                getResources().getColor(R.color.status_in_progress),
                getResources().getColor(R.color.status_assigned),
                getResources().getColor(R.color.status_pending),
                getResources().getColor(R.color.status_cancelled),
                Color.GRAY
        };

        dataSet.setColors(colors);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(12f);
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);

        PieData pieData = new PieData(dataSet);
        pieData.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format("%.0f", value);
            }
        });

        pieChartStatus.setData(pieData);
        pieChartStatus.invalidate();

        int totalOrders = documents.size();
        pieChartStatus.setCenterText(String.format("%d\nCommandes", totalOrders));
        pieChartStatus.setCenterTextSize(14f);
        pieChartStatus.setCenterTextColor(Color.DKGRAY);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadDashboardData();
    }

    public void onViewOrdersClick(View view) {
        // Naviguer vers les commandes
    }

    public void onViewUsersClick(View view) {
        // Naviguer vers la gestion des utilisateurs
    }
}