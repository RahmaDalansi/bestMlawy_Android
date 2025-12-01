package com.example.bestmlawi.ui.delivery;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.bestmlawi.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class DeliveryProfileFragment extends Fragment implements OnMapReadyCallback {

    private TextView tvName, tvEmail, tvPhone, tvAddress, tvDeliveryStats;
    private TextView tvCompletedDeliveries, tvRating, tvMonthlyGoal, tvProgress, tvAvgDeliveryTime;
    private TextView tvLocationStatus, tvCurrentCoordinates;
    private Button btnEditProfile, btnRefreshLocation;
    private SwitchMaterial switchActiveStatus;
    private MaterialCardView cardMap;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // Localisation et Maps
    private FusedLocationProviderClient fusedLocationClient;
    private GoogleMap mMap;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private boolean isTrackingLocation = false;

    // Constantes pour les permissions
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final int LOCATION_SETTINGS_REQUEST_CODE = 1002;
    private static final long UPDATE_INTERVAL = 30000; // 30 secondes
    private static final long FASTEST_INTERVAL = 15000; // 15 secondes

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_delivery_profile, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        initializeViews(view);
        setupClickListeners();
        setupLocationTracking();
        initializeMap();
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

        tvLocationStatus = view.findViewById(R.id.tv_location_status);
        tvCurrentCoordinates = view.findViewById(R.id.tv_current_coordinates);
        btnEditProfile = view.findViewById(R.id.btn_edit_profile);
        btnRefreshLocation = view.findViewById(R.id.btn_refresh_location);
        switchActiveStatus = view.findViewById(R.id.switch_active_status);
        cardMap = view.findViewById(R.id.card_map);
    }

    private void initializeMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map_container);

        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.map_container, mapFragment)
                    .commit();
        }

        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        if (hasLocationPermission()) {
            enableMapLocationFeatures();
        }

        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        if (switchActiveStatus.isChecked() && hasLocationPermission()) {
            refreshCurrentLocation();
        }
    }

    private void enableMapLocationFeatures() {
        if (hasLocationPermission()) {
            try {
                mMap.setMyLocationEnabled(true);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
    }

    private void setupClickListeners() {
        btnEditProfile.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Modification du profil", Toast.LENGTH_SHORT).show();
        });

        btnRefreshLocation.setOnClickListener(v -> {
            refreshCurrentLocation();
        });

        switchActiveStatus.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Vérifier d'abord la permission, puis activer la localisation
                if (hasLocationPermission()) {
                    checkLocationSettingsAndActivate();
                } else {
                    requestLocationPermissionWithExplanation();
                }
            } else {
                deactivateDeliveryStatus();
            }
        });
    }

    private void setupLocationTracking() {
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(UPDATE_INTERVAL);
        locationRequest.setFastestInterval(FASTEST_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null) {
                    Location location = locationResult.getLastLocation();
                    updateLocationInFirestore(location);
                    updateLocationUI(location);
                    updateMapWithLocation(location);
                }
            }
        };
    }

    private void checkLocationSettingsAndActivate() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)
                .setAlwaysShow(true); // Important pour forcer l'affichage du dialogue

        SettingsClient client = LocationServices.getSettingsClient(requireActivity());

        client.checkLocationSettings(builder.build())
                .addOnSuccessListener(requireActivity(), locationSettingsResponse -> {
                    // La localisation est activée, on peut démarrer
                    activateDeliveryStatus();
                })
                .addOnFailureListener(requireActivity(), e -> {
                    // La localisation n'est pas activée, demander à l'utilisateur de l'activer
                    showLocationSettingsDialog();
                });
    }

    private void showLocationSettingsDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Localisation requise")
                .setMessage("La localisation de votre téléphone est désactivée. Pour partager votre position avec les clients, vous devez activer la localisation.\n\nVoulez-vous activer la localisation maintenant ?")
                .setPositiveButton("Activer", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Ouvrir les paramètres de localisation
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivityForResult(intent, LOCATION_SETTINGS_REQUEST_CODE);
                    }
                })
                .setNegativeButton("Annuler", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switchActiveStatus.setChecked(false);
                        Toast.makeText(getContext(), "La localisation est nécessaire pour activer le statut", Toast.LENGTH_LONG).show();
                    }
                })
                .setCancelable(false)
                .show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == LOCATION_SETTINGS_REQUEST_CODE) {
            // Vérifier à nouveau si la localisation est activée après le retour des paramètres
            if (isLocationEnabled()) {
                activateDeliveryStatus();
            } else {
                switchActiveStatus.setChecked(false);
                Toast.makeText(getContext(), "La localisation n'est toujours pas activée", Toast.LENGTH_LONG).show();
            }
        }
    }

    private boolean isLocationEnabled() {
        try {
            int locationMode = Settings.Secure.getInt(requireContext().getContentResolver(),
                    Settings.Secure.LOCATION_MODE);
            return locationMode != Settings.Secure.LOCATION_MODE_OFF;
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void activateDeliveryStatus() {
        if (hasLocationPermission() && isLocationEnabled()) {
            startLocationTracking();
            updateDeliveryStatus(true);
            updateLocationUIForActiveStatus();
            Toast.makeText(getContext(), "Statut activé - Partage de position démarré", Toast.LENGTH_SHORT).show();
        } else {
            switchActiveStatus.setChecked(false);
            if (!isLocationEnabled()) {
                showLocationSettingsDialog();
            } else {
                requestLocationPermissionWithExplanation();
            }
        }
    }

    private void deactivateDeliveryStatus() {
        stopLocationTracking();
        updateDeliveryStatus(false);
        updateLocationUIForInactiveStatus();
        Toast.makeText(getContext(), "Statut désactivé", Toast.LENGTH_SHORT).show();
    }

    private void updateLocationUIForActiveStatus() {
        tvLocationStatus.setText("✅ Statut actif - Position partagée");
        tvLocationStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark));
        cardMap.setVisibility(View.VISIBLE);
    }

    private void updateLocationUIForInactiveStatus() {
        tvLocationStatus.setText("❌ Statut inactif - Position non partagée");
        tvLocationStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark));
        cardMap.setVisibility(View.GONE);
    }

    private void startLocationTracking() {
        if (hasLocationPermission()) {
            try {
                fusedLocationClient.requestLocationUpdates(
                        locationRequest,
                        locationCallback,
                        Looper.getMainLooper()
                );
                isTrackingLocation = true;
                refreshCurrentLocation();

            } catch (SecurityException e) {
                Toast.makeText(getContext(), "Erreur de permission de localisation", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void stopLocationTracking() {
        if (isTrackingLocation) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            isTrackingLocation = false;
        }
    }

    private void refreshCurrentLocation() {
        if (hasLocationPermission()) {
            try {
                fusedLocationClient.getLastLocation()
                        .addOnSuccessListener(requireActivity(), location -> {
                            if (location != null) {
                                updateLocationInFirestore(location);
                                updateLocationUI(location);
                                updateMapWithLocation(location);
                                Toast.makeText(getContext(), "Position mise à jour", Toast.LENGTH_SHORT).show();
                            } else {
                                startLocationTracking();
                                Toast.makeText(getContext(), "Position non disponible, démarrage du tracking...", Toast.LENGTH_SHORT).show();
                            }
                        });
            } catch (SecurityException e) {
                Toast.makeText(getContext(), "Erreur de permission", Toast.LENGTH_SHORT).show();
            }
        } else {
            requestLocationPermissionWithExplanation();
        }
    }

    private void updateLocationUI(Location location) {
        String coordinates = String.format("Lat: %.6f, Lng: %.6f",
                location.getLatitude(), location.getLongitude());
        tvCurrentCoordinates.setText("Position: " + coordinates);
    }

    private void updateMapWithLocation(Location location) {
        if (mMap != null) {
            LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

            mMap.clear();

            mMap.addMarker(new MarkerOptions()
                    .position(currentLatLng)
                    .title("Ma position actuelle")
                    .snippet("Livreur en service"));

            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f));
        }
    }

    private void updateLocationInFirestore(Location location) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Map<String, Object> locationData = new HashMap<>();
            locationData.put("latitude", location.getLatitude());
            locationData.put("longitude", location.getLongitude());
            locationData.put("lastUpdate", new Date());
            locationData.put("isOnline", true);

            db.collection("delivery_locations")
                    .document(currentUser.getUid())
                    .set(locationData)
                    .addOnSuccessListener(aVoid -> {
                        // Position mise à jour
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Erreur mise à jour position", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void updateDeliveryStatus(boolean isActive) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Map<String, Object> statusData = new HashMap<>();
            statusData.put("isActive", isActive);
            statusData.put("lastStatusUpdate", new Date());

            db.collection("delivery_status")
                    .document(currentUser.getUid())
                    .set(statusData)
                    .addOnSuccessListener(aVoid -> {
                        // Statut mis à jour
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Erreur mise à jour statut", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermissionWithExplanation() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION)) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Permission de localisation requise")
                    .setMessage("Cette application a besoin d'accéder à votre position pour partager votre localisation en temps réel avec les clients lorsque vous êtes en service.\n\nVoulez-vous autoriser l'accès à votre localisation ?")
                    .setPositiveButton("Autoriser", (dialog, which) -> {
                        requestLocationPermission();
                    })
                    .setNegativeButton("Refuser", (dialog, which) -> {
                        switchActiveStatus.setChecked(false);
                        Toast.makeText(getContext(), "La localisation est nécessaire pour activer le statut", Toast.LENGTH_LONG).show();
                    })
                    .setCancelable(false)
                    .show();
        } else {
            requestLocationPermission();
        }
    }

    private void requestLocationPermission() {
        String[] permissions = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };
        requestPermissions(permissions, LOCATION_PERMISSION_REQUEST_CODE);
    }

    private void showPermissionDeniedDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Permission refusée")
                .setMessage("Vous avez refusé la permission de localisation. Sans cette permission, vous ne pouvez pas partager votre position avec les clients.\n\nVous pouvez activer la localisation manuellement dans les paramètres de l'application.")
                .setPositiveButton("Paramètres", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", requireContext().getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .setNegativeButton("Plus tard", (dialog, which) -> {
                    switchActiveStatus.setChecked(false);
                })
                .setCancelable(false)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getContext(), "Permission de localisation accordée", Toast.LENGTH_SHORT).show();

                // Maintenant vérifier si la localisation est activée
                if (switchActiveStatus.isChecked()) {
                    checkLocationSettingsAndActivate();
                }

                if (mMap != null) {
                    enableMapLocationFeatures();
                    refreshCurrentLocation();
                }
            } else {
                switchActiveStatus.setChecked(false);
                showPermissionDeniedDialog();
            }
        }
    }

    // Le reste des méthodes (loadDeliveryProfile, loadDeliveryStatus, etc.) reste inchangé
    private void loadDeliveryProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            db.collection("users").document(currentUser.getUid())
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                tvName.setText(document.getString("name"));
                                tvEmail.setText(currentUser.getEmail());

                                String phone = document.getString("phoneNumber");
                                if (phone != null && !phone.isEmpty()) {
                                    tvPhone.setText(phone);
                                } else {
                                    tvPhone.setText("Non défini");
                                }

                                String address = document.getString("address");
                                if (address != null && !address.isEmpty()) {
                                    tvAddress.setText(address);
                                } else {
                                    tvAddress.setText("Adresse non définie");
                                }

                                loadDeliveryStats(currentUser.getUid());
                                loadDeliveryStatus(currentUser.getUid());
                            }
                        } else {
                            Toast.makeText(getContext(), "Erreur chargement profil", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void loadDeliveryStatus(String userId) {
        db.collection("delivery_status").document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists() && document.getBoolean("isActive") != null) {
                            boolean isActive = document.getBoolean("isActive");
                            switchActiveStatus.setChecked(isActive);
                            if (isActive && hasLocationPermission() && isLocationEnabled()) {
                                updateLocationUIForActiveStatus();
                                startLocationTracking();
                                new android.os.Handler().postDelayed(this::refreshCurrentLocation, 1000);
                            } else {
                                updateLocationUIForInactiveStatus();
                            }
                        }
                    }
                });
    }

    private void loadDeliveryStats(String userId) {
        db.collection("delivery_stats").document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            // ... reste du code inchangé
                        } else {
                            initializeDefaultStats();
                        }
                    } else {
                        initializeDefaultStats();
                    }
                });
    }

    private String buildStatsText(DocumentSnapshot document) {
        // ... reste du code inchangé
        return "";
    }

    private void initializeDefaultStats() {
        // reste du code inchangé
    }

    @Override
    public void onResume() {
        super.onResume();
        if (switchActiveStatus.isChecked() && hasLocationPermission() && isLocationEnabled()) {
            startLocationTracking();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopLocationTracking();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopLocationTracking();
    }
}