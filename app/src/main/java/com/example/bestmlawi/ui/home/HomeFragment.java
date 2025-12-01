package com.example.bestmlawi.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.example.bestmlawi.R;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HomeFragment extends Fragment {

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialiser les vues
        initializeViews(root);

        return root;
    }

    private void initializeViews(View root) {
        // Date actuelle
        TextView currentDate = root.findViewById(R.id.current_date);
        String today = new SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(new Date());
        currentDate.setText(today);

        // Statistiques (vous pouvez remplacer par vos données réelles)
        TextView totalOrders = root.findViewById(R.id.total_orders_today);
        TextView activePoints = root.findViewById(R.id.active_points);
        TextView onlineDrivers = root.findViewById(R.id.online_drivers);
        TextView revenue = root.findViewById(R.id.revenue);

        totalOrders.setText("210");
        activePoints.setText("5");
        onlineDrivers.setText("229");
        revenue.setText("2100 DT");
    }
}