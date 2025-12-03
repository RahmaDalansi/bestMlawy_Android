package com.example.bestmlawi.ui.orders;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.bestmlawi.R;

public class FilterOrdersActivity extends AppCompatActivity {

    // 🔍 Références aux vues dans filter_orders.xml
    private TextView statusPlaced, statusPreparing, statusOnWay, statusDelivered, statusCanceled;
    private SeekBar seekPriceMin, seekPriceMax;
    private TextView txtPriceRange;
    private RadioGroup rgSalesPoint1;
    private Button btnApply;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.filter_orders);

        // 🔧 Initialiser les vues
        statusPlaced = findViewById(R.id.statusPlaced);
        statusPreparing = findViewById(R.id.statusPreparing);
        statusOnWay = findViewById(R.id.statusOnWay);
        statusDelivered = findViewById(R.id.statusDelivered);
        statusCanceled = findViewById(R.id.statusCanceled);

        seekPriceMin = findViewById(R.id.seekPriceMin);
        seekPriceMax = findViewById(R.id.seekPriceMax);
        txtPriceRange = findViewById(R.id.txtPriceRange);

        rgSalesPoint1 = findViewById(R.id.rgSalesPoint1);
        btnApply = findViewById(R.id.btnApply);

        // 🎯 Gestion des statuts cliquables
        setupStatusClickListeners();

        // 🔧 Afficher la plage de prix initiale
        updatePriceDisplay();

        // 🔧 Écouteurs pour les SeekBars
        seekPriceMin.setOnSeekBarChangeListener(new PriceChangeListener());
        seekPriceMax.setOnSeekBarChangeListener(new PriceChangeListener());

        // ✅ Appliquer les filtres au clic sur "Apply"
        btnApply.setOnClickListener(v -> applyFilters());
    }

    private void setupStatusClickListeners() {
        statusPlaced.setOnClickListener(v -> selectStatus(statusPlaced));
        statusPreparing.setOnClickListener(v -> selectStatus(statusPreparing));
        statusOnWay.setOnClickListener(v -> selectStatus(statusOnWay));
        statusDelivered.setOnClickListener(v -> selectStatus(statusDelivered));
        statusCanceled.setOnClickListener(v -> selectStatus(statusCanceled));

        // Désélectionner tous par défaut
        resetAllStatuses();
    }

    private void selectStatus(TextView selected) {
        resetAllStatuses();
        int red = ContextCompat.getColor(this, android.R.color.holo_red_dark);
        int white = ContextCompat.getColor(this, android.R.color.white);
        selected.setBackgroundColor(red);
        selected.setTextColor(white);
    }

    private void resetAllStatuses() {
        int yellow400 = ContextCompat.getColor(this, R.color.yellow_400);
        int green500 = ContextCompat.getColor(this, R.color.green_500);
        int gray200 = ContextCompat.getColor(this, R.color.gray_200);
        int white = ContextCompat.getColor(this, android.R.color.white);
        int black = ContextCompat.getColor(this, android.R.color.black);

        statusPlaced.setBackgroundColor(yellow400);
        statusPlaced.setTextColor(white);

        statusPreparing.setBackgroundColor(gray200);
        statusPreparing.setTextColor(black);

        statusOnWay.setBackgroundColor(gray200);
        statusOnWay.setTextColor(black);

        statusDelivered.setBackgroundColor(green500);
        statusDelivered.setTextColor(white);

        statusCanceled.setBackgroundColor(gray200);
        statusCanceled.setTextColor(black);
    }

    private void updatePriceDisplay() {
        int min = seekPriceMin.getProgress();
        int max = seekPriceMax.getProgress();
        txtPriceRange.setText("DT" + min + " - DT" + max);
    }

    private class PriceChangeListener implements SeekBar.OnSeekBarChangeListener {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            updatePriceDisplay();
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {}

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {}
    }

    private void applyFilters() {
        // 🔗 Statut sélectionné
        String selectedStatus = getSelectedStatus();

        // 🔗 Prix
        int minPrice = seekPriceMin.getProgress();
        int maxPrice = seekPriceMax.getProgress();

        // 🔗 Filtre du point de vente 1 — toujours une valeur par défaut
        int selectedRadioId = rgSalesPoint1.getCheckedRadioButtonId();
        String salesPointFilter = null;

        if (selectedRadioId != -1) {
            RadioButton button = findViewById(selectedRadioId);
            salesPointFilter = button.getText().toString(); // ex: "Available"
        }

        // ✅ Envoyer les filtres
        Intent resultIntent = new Intent();
        resultIntent.putExtra("FILTER_STATUS", selectedStatus); // peut être null
        resultIntent.putExtra("FILTER_MIN_PRICE", minPrice);
        resultIntent.putExtra("FILTER_MAX_PRICE", maxPrice);
        resultIntent.putExtra("FILTER_SALES_POINT_FILTER", salesPointFilter); // peut être null

        setResult(RESULT_OK, resultIntent);
        finish(); // Fermer cette activité
    }

    private String getSelectedStatus() {
        int white = ContextCompat.getColor(this, android.R.color.white);
        if (statusPlaced.getCurrentTextColor() == white) return "Order Placed";
        if (statusPreparing.getCurrentTextColor() == white) return "Preparing";
        if (statusOnWay.getCurrentTextColor() == white) return "On The Way";
        if (statusDelivered.getCurrentTextColor() == white) return "Delivered";
        if (statusCanceled.getCurrentTextColor() == white) return "Canceled";
        return null;
    }
}