package com.example.bestmlawi.ui.salespoints;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.bestmlawi.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SalesPointsFragment extends Fragment {

    private ListView lstSalesPoints;
    private Button btnAddSalesPoint, btnBack;
    private TextView txtSalesPointsTitle, txtOurSalesPoints;

    private FirebaseFirestore db;
    private List<SalesPoint> salesPointList;
    private List<String> salesPointDisplayList;
    private ArrayAdapter<String> adapter;

    private WebView webSalesPointsMap;
    private boolean mapPageLoaded = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_sales_points, container, false);
        initViews(root);
        setupListeners();
        loadSalesPoints();
        return root;
    }

    private void initViews(View root) {
        lstSalesPoints = root.findViewById(R.id.lstSalesPoints);
        btnAddSalesPoint = root.findViewById(R.id.btnAddSalesPoint);
        btnBack = root.findViewById(R.id.btnBack);
        txtSalesPointsTitle = root.findViewById(R.id.txtSalesPointsTitle);
        txtOurSalesPoints = root.findViewById(R.id.txtOurSalesPoints);

        db = FirebaseFirestore.getInstance();
        salesPointList = new ArrayList<>();
        salesPointDisplayList = new ArrayList<>();
        adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, salesPointDisplayList);
        lstSalesPoints.setAdapter(adapter);

        webSalesPointsMap = root.findViewById(R.id.webSalesPointsMap);
        if (webSalesPointsMap != null) {
            WebSettings settings = webSalesPointsMap.getSettings();
            settings.setJavaScriptEnabled(true);
            webSalesPointsMap.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    mapPageLoaded = true;
                    sendPointsToMap();
                }
            });
            webSalesPointsMap.loadUrl("file:///android_asset/salespoints_map.html");
        }
    }

    private void setupListeners() {
        btnAddSalesPoint.setOnClickListener(v -> showAddOrEditDialog(null));

        btnBack.setOnClickListener(v -> requireActivity().onBackPressed());

        lstSalesPoints.setOnItemClickListener((parent, view, position, id) -> {
            SalesPoint sp = salesPointList.get(position);
            openDetailsScreen(sp);
        });

        lstSalesPoints.setOnItemLongClickListener((parent, view, position, id) -> {
            SalesPoint sp = salesPointList.get(position);

            AlertDialog.Builder optionsBuilder = new AlertDialog.Builder(requireContext());
            optionsBuilder.setTitle(sp.getName() != null ? sp.getName() : "Sales point");
            String[] options = {"Edit", "Delete"};
            optionsBuilder.setItems(options, (dialog, which) -> {
                if (which == 0) {
                    // Edit
                    showAddOrEditDialog(sp);
                } else if (which == 1) {
                    // Delete
                    deleteSalesPoint(sp);
                }
            });
            optionsBuilder.show();
            return true;
        });
    }

    private void loadSalesPoints() {
        db.collection("SalesPoints")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            salesPointList.clear();
                            salesPointDisplayList.clear();

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                SalesPoint sp = document.toObject(SalesPoint.class);
                                sp.setId(document.getId());
                                salesPointList.add(sp);

                                String display = sp.getName();
                                if (sp.getCity() != null && !sp.getCity().isEmpty()) {
                                    display += " - " + sp.getCity();
                                }
                                salesPointDisplayList.add(display);
                            }

                            adapter.notifyDataSetChanged();

                            sendPointsToMap();

                            if (salesPointList.isEmpty()) {
                                Toast.makeText(requireContext().getApplicationContext(), "No sales points found", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(requireContext().getApplicationContext(), "Sales points loaded from Firebase", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(requireContext().getApplicationContext(), "Firebase error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void sendPointsToMap() {
        if (!mapPageLoaded || webSalesPointsMap == null || salesPointList == null || salesPointList.isEmpty()) {
            return;
        }

        StringBuilder json = new StringBuilder();
        json.append("[");
        boolean first = true;
        for (SalesPoint sp : salesPointList) {
            double lat = sp.getLatitude();
            double lng = sp.getLongitude();
            if (lat == 0.0 && lng == 0.0) continue;
            if (!first) json.append(",");
            first = false;
            json.append("{\"lat\":").append(lat)
                    .append(",\"lng\":").append(lng)
                    .append(",\"name\":\"")
                    .append(sp.getName() == null ? "" : sp.getName().replace("\"", "\\\""))
                    .append("\"}");
        }
        json.append("]");

        String js = "javascript:showAllPoints(" + json.toString() + ")";
        webSalesPointsMap.evaluateJavascript(js, null);
    }

    private void openDetailsScreen(SalesPoint sp) {
        Intent intent = new Intent(requireContext(), SalesPointDetailsActivity.class);
        intent.putExtra(SalesPointDetailsActivity.EXTRA_NAME, sp.getName());
        intent.putExtra(SalesPointDetailsActivity.EXTRA_ADDRESS, sp.getAddress());
        intent.putExtra(SalesPointDetailsActivity.EXTRA_CITY, sp.getCity());
        intent.putExtra(SalesPointDetailsActivity.EXTRA_PHONE, sp.getPhone());
        intent.putExtra(SalesPointDetailsActivity.EXTRA_OPENING, sp.getOpeningHours());
        intent.putExtra(SalesPointDetailsActivity.EXTRA_LATITUDE, sp.getLatitude());
        intent.putExtra(SalesPointDetailsActivity.EXTRA_LONGITUDE, sp.getLongitude());
        startActivity(intent);
    }

    private void showAddOrEditDialog(@Nullable SalesPoint existing) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View dialogView = inflater.inflate(R.layout.sales_point_dialog, null);
        builder.setView(dialogView);

        EditText edtName = dialogView.findViewById(R.id.edtSpName);
        EditText edtAddress = dialogView.findViewById(R.id.edtSpAddress);
        EditText edtCity = dialogView.findViewById(R.id.edtSpCity);
        EditText edtPhone = dialogView.findViewById(R.id.edtSpPhone);
        EditText edtOpening = dialogView.findViewById(R.id.edtSpOpeningHours);
        TextView txtCoordinates = dialogView.findViewById(R.id.txtSpCoordinates);
        WebView webPickerMap = dialogView.findViewById(R.id.webSpPickerMap);

        final double[] selectedLat = {0.0};
        final double[] selectedLng = {0.0};

        if (existing != null) {
            edtName.setText(existing.getName());
            edtAddress.setText(existing.getAddress());
            edtCity.setText(existing.getCity());
            edtPhone.setText(existing.getPhone());
            edtOpening.setText(existing.getOpeningHours());
            builder.setTitle("Edit Sales Point");
            selectedLat[0] = existing.getLatitude();
            selectedLng[0] = existing.getLongitude();
            if (selectedLat[0] != 0.0 || selectedLng[0] != 0.0) {
                txtCoordinates.setText("Lat: " + selectedLat[0] + ", Lng: " + selectedLng[0]);
            }
        } else {
            builder.setTitle("Add Sales Point");
        }

        if (webPickerMap != null) {
            WebSettings pickerSettings = webPickerMap.getSettings();
            pickerSettings.setJavaScriptEnabled(true);

            class PickerInterface {
                @android.webkit.JavascriptInterface
                public void onLocationPicked(final double lat, final double lng) {
                    selectedLat[0] = lat;
                    selectedLng[0] = lng;
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() ->
                                txtCoordinates.setText("Lat: " + lat + ", Lng: " + lng));
                    }
                }
            }

            webPickerMap.addJavascriptInterface(new PickerInterface(), "AndroidPicker");
            webPickerMap.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    String js = "javascript:initPosition(" + selectedLat[0] + "," + selectedLng[0] + ")";
                    webPickerMap.evaluateJavascript(js, null);
                }
            });
            webPickerMap.loadUrl("file:///android_asset/salespoint_picker.html");
        }

        builder.setPositiveButton("Save", (dialog, which) -> {
            String name = edtName.getText().toString().trim();
            String address = edtAddress.getText().toString().trim();
            String city = edtCity.getText().toString().trim();
            String phone = edtPhone.getText().toString().trim();
            String openingHours = edtOpening.getText().toString().trim();

            if (name.isEmpty()) {
                Toast.makeText(requireContext().getApplicationContext(), "Name is required", Toast.LENGTH_SHORT).show();
                return;
            }

            if (existing == null) {
                addSalesPoint(name, address, city, phone, openingHours, selectedLat[0], selectedLng[0]);
            } else {
                updateSalesPoint(existing, name, address, city, phone, openingHours, selectedLat[0], selectedLng[0]);
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    private void addSalesPoint(String name, String address, String city, String phone, String openingHours, double latitude, double longitude) {
        int businessId = (int) (System.currentTimeMillis() / 1000L);

        Map<String, Object> data = new HashMap<>();
        data.put("spId", businessId);
        data.put("name", name);
        data.put("address", address);
        data.put("city", city);
        data.put("phone", phone);
        data.put("openingHours", openingHours);
        data.put("latitude", latitude);
        data.put("longitude", longitude);

        db.collection("SalesPoints")
                .add(data)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(requireContext().getApplicationContext(), "Sales point added", Toast.LENGTH_SHORT).show();
                        loadSalesPoints();
                    } else {
                        Toast.makeText(requireContext().getApplicationContext(), "Error adding: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateSalesPoint(SalesPoint existing, String name, String address, String city, String phone, String openingHours, double latitude, double longitude) {
        if (existing.getId() == null) {
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("address", address);
        updates.put("city", city);
        updates.put("phone", phone);
        updates.put("openingHours", openingHours);
        updates.put("latitude", latitude);
        updates.put("longitude", longitude);

        db.collection("SalesPoints")
                .document(existing.getId())
                .update(updates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(requireContext().getApplicationContext(), "Sales point updated", Toast.LENGTH_SHORT).show();
                        loadSalesPoints();
                    } else {
                        Toast.makeText(requireContext().getApplicationContext(), "Error updating: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void deleteSalesPoint(SalesPoint existing) {
        if (existing.getId() == null) {
            return;
        }

        db.collection("SalesPoints")
                .document(existing.getId())
                .delete()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(requireContext().getApplicationContext(), "Sales point deleted", Toast.LENGTH_SHORT).show();
                        loadSalesPoints();
                    } else {
                        Toast.makeText(requireContext().getApplicationContext(), "Error deleting: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
