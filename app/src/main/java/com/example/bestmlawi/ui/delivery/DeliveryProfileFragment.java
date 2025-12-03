package com.example.bestmlawi.ui.delivery;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
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
import com.google.android.gms.tasks.Task;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.google.android.gms.tasks.Task;
import android.util.Log;

public class DeliveryProfileFragment extends Fragment implements OnMapReadyCallback {

    private TextView tvName, tvEmail, tvPhone, tvAddress, tvDeliveryStats, tvDeliveryRole;
    private TextView tvCompletedDeliveries, tvRating, tvMonthlyGoal, tvProgress, tvAvgDeliveryTime;
    private TextView tvLocationStatus, tvCurrentCoordinates;
    private Button btnEditProfile, btnRefreshLocation;
    private SwitchMaterial switchActiveStatus;
    private MaterialCardView cardMap;
    private ImageView ivDeliveryPhoto;

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
        tvName = view.findViewById(R.id.delivery_name);
        tvEmail = view.findViewById(R.id.delivery_email);
        tvPhone = view.findViewById(R.id.delivery_phone);
        tvAddress = view.findViewById(R.id.delivery_address);
        tvDeliveryStats = view.findViewById(R.id.delivery_stats);
        tvDeliveryRole = view.findViewById(R.id.delivery_role);

        tvCompletedDeliveries = view.findViewById(R.id.tv_completed_deliveries);
        tvRating = view.findViewById(R.id.tv_rating);
        tvMonthlyGoal = view.findViewById(R.id.tv_monthly_goal);
        tvProgress = view.findViewById(R.id.progress);
        tvAvgDeliveryTime = view.findViewById(R.id.tv_avg_delivery_time);

        tvLocationStatus = view.findViewById(R.id.location_status);
        tvCurrentCoordinates = view.findViewById(R.id.current_coordinates);
        btnEditProfile = view.findViewById(R.id.btn_edit_profile);
        btnRefreshLocation = view.findViewById(R.id.btn_refresh_location);
        switchActiveStatus = view.findViewById(R.id.switch_active_status);
        cardMap = view.findViewById(R.id.card_map);
        ivDeliveryPhoto = view.findViewById(R.id.delivery_photo);
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
            Toast.makeText(getContext(), "Modification du profil - Fonctionnalité à venir", Toast.LENGTH_SHORT).show();
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

    // ========== MODIFICATION PRINCIPALE ==========
    // Charger le profil depuis la collection "Employees" au lieu de "users"
    private void loadDeliveryProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();

            // ÉTAPE 1: Chercher d'abord dans la collection "Employees"
            db.collection("Employees").document(userId)
                    .get()
                    .addOnCompleteListener(employeesTask -> {
                        if (employeesTask.isSuccessful()) {
                            DocumentSnapshot employeeDoc = employeesTask.getResult();
                            if (employeeDoc.exists()) {
                                // Utilisateur trouvé dans Employees
                                populateProfileFromEmployeeDocument(employeeDoc, currentUser.getEmail());
                                loadDeliveryStats(userId);
                                loadDeliveryStatus(userId);
                            } else {
                                // ÉTAPE 2: Si non trouvé dans Employees, chercher dans "users" (pour compatibilité)
                                loadProfileFromUsersCollection(userId, currentUser.getEmail());
                            }
                        } else {
                            Toast.makeText(getContext(), "Erreur chargement profil Employees", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    // Méthode pour peupler les données depuis la collection Employees
    private void populateProfileFromEmployeeDocument(DocumentSnapshot document, String userEmail) {
        // Nom
        String name = document.getString("name");
        if (name != null && !name.isEmpty()) {
            tvName.setText(name);
        } else {
            tvName.setText("Nom non défini");
        }

        // Email (depuis Firebase Auth ou document)
        if (userEmail != null && !userEmail.isEmpty()) {
            tvEmail.setText(userEmail);
        } else {
            String emailFromDoc = document.getString("email");
            tvEmail.setText(emailFromDoc != null ? emailFromDoc : "Email non défini");
        }

        // Rôle
        String role = document.getString("role");
        if (role != null && !role.isEmpty()) {
            String roleDisplay = getRoleDisplayName(role);
            tvDeliveryRole.setText(roleDisplay);
        } else {
            tvDeliveryRole.setText("Rôle non défini");
        }

        // Téléphone
        String phone = document.getString("phoneNumber");
        if (phone != null && !phone.isEmpty()) {
            tvPhone.setText(phone);
        } else {
            tvPhone.setText("Téléphone non défini");
        }

        // Adresse - Vérifier d'abord le champ location
        String location = document.getString("location");
        if (location != null && !location.isEmpty()) {
            tvAddress.setText(location);
        } else {
            // Fallback sur l'adresse fixe si location n'existe pas
            String address = document.getString("address");
            tvAddress.setText(address != null && !address.isEmpty() ? address : "Position non enregistrée");
        }

        // Image de profil (Base64)
        String imageBase64 = document.getString("image");
        if (imageBase64 != null && !imageBase64.isEmpty()) {
            loadProfileImage(imageBase64);
        }
        // Sinon, l'image par défaut reste affichée

        // Vérifier si le livreur a déjà une position enregistrée
        Double latitude = document.getDouble("latitude");
        Double longitude = document.getDouble("longitude");
        if (latitude != null && longitude != null) {
            // Afficher les coordonnées si disponibles
            tvCurrentCoordinates.setText(String.format("Dernière position: %.6f, %.6f", latitude, longitude));
        }
    }

    // Méthode pour charger depuis la collection users (ancien système)
    private void loadProfileFromUsersCollection(String userId, String userEmail) {
        db.collection("users").document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            // Nom
                            String name = document.getString("name");
                            tvName.setText(name != null ? name : "Nom non défini");

                            // Email
                            tvEmail.setText(userEmail != null ? userEmail : "Email non défini");

                            // Rôle
                            String role = document.getString("role");
                            tvDeliveryRole.setText(role != null ? getRoleDisplayName(role) : "Rôle non défini");

                            // Téléphone
                            String phone = document.getString("phoneNumber");
                            tvPhone.setText(phone != null ? phone : "Téléphone non défini");

                            // Adresse
                            String address = document.getString("address");
                            tvAddress.setText(address != null ? address : "Adresse non définie");

                            loadDeliveryStats(userId);
                            loadDeliveryStatus(userId);
                        } else {
                            Toast.makeText(getContext(), "Profil non trouvé dans Employees ni users", Toast.LENGTH_LONG).show();
                            initializeDefaultProfile();
                        }
                    } else {
                        Toast.makeText(getContext(), "Erreur chargement profil users", Toast.LENGTH_SHORT).show();
                        initializeDefaultProfile();
                    }
                });
    }

    // Méthode pour charger l'image depuis Base64 avec forme circulaire
    private void loadProfileImage(String imageBase64) {
        try {
            byte[] decodedBytes = Base64.decode(imageBase64, Base64.DEFAULT);
            Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
            if (decodedBitmap != null) {
                // Convertir le bitmap en image circulaire
                Bitmap circularBitmap = getCircularBitmap(decodedBitmap);
                ivDeliveryPhoto.setImageBitmap(circularBitmap);

                // Ajouter une bordure circulaire programmatiquement
                ivDeliveryPhoto.setBackgroundResource(R.drawable.circle_background);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Erreur chargement image", Toast.LENGTH_SHORT).show();
        }
    }

    // Méthode pour convertir un Bitmap en forme circulaire
    private Bitmap getCircularBitmap(Bitmap bitmap) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(output);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);

        Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

        canvas.drawARGB(0, 0, 0, 0);
        canvas.drawCircle(bitmap.getWidth() / 2f, bitmap.getHeight() / 2f,
                bitmap.getWidth() / 2f, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }

    // Méthode pour traduire les rôles en noms d'affichage
    private String getRoleDisplayName(String role) {
        switch (role.toLowerCase()) {
            case "deliver":
                return "Livreur";
            case "collaborator":
                return "Collaborateur";
            case "coordinator":
                return "Coordinateur";
            case "gerant":
                return "Gérant";
            case "admin":
                return "Administrateur";
            default:
                return role;
        }
    }

    // Initialiser le profil par défaut
    private void initializeDefaultProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            tvName.setText(currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "Utilisateur");
            tvEmail.setText(currentUser.getEmail());
            tvDeliveryRole.setText("Rôle non défini");
            tvPhone.setText("Téléphone non défini");
            tvAddress.setText("Adresse non définie");
        }
    }

    // ========== AJOUT DE LA MÉTHODE MANQUANTE ==========
    private void updateDeliveryStatus(boolean isActive) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();

            Map<String, Object> statusData = new HashMap<>();
            statusData.put("isActive", isActive);
            statusData.put("lastUpdate", new Date());
            statusData.put("userId", userId);

            db.collection("delivery_status")
                    .document(userId)
                    .set(statusData)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("DeliveryStatus", "Statut mis à jour: " + (isActive ? "Actif" : "Inactif"));
                    })
                    .addOnFailureListener(e -> {
                        Log.e("DeliveryStatus", "Erreur mise à jour statut: " + e.getMessage());
                        Toast.makeText(getContext(), "Erreur mise à jour statut", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void checkLocationSettingsAndActivate() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)
                .setAlwaysShow(true);

        SettingsClient client = LocationServices.getSettingsClient(requireActivity());

        client.checkLocationSettings(builder.build())
                .addOnSuccessListener(requireActivity(), locationSettingsResponse -> {
                    activateDeliveryStatus();
                })
                .addOnFailureListener(requireActivity(), e -> {
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

    // ========== CORRECTION : SUPPRIMER LA PREMIÈRE VERSION DUPLIQUÉE ==========
    // Supprimez la première version de activateDeliveryStatus() (ligne ~427)

    private void deactivateDeliveryStatus() {
        // 1. Arrêter le tracking de localisation
        stopLocationTracking();

        // 2. Mettre à jour le statut dans Firestore
        updateDeliveryStatus(false);

        // 3. Mettre à jour le statut "isOnline" dans delivery_locations
        updateDeliveryLocationStatus(false);

        // 4. Mettre à jour l'UI
        updateLocationUIForInactiveStatus();

        Toast.makeText(getContext(), "Statut désactivé - Position non partagée", Toast.LENGTH_SHORT).show();
    }

    private void updateDeliveryLocationStatus(boolean isOnline) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();

            Map<String, Object> updateData = new HashMap<>();
            updateData.put("isOnline", isOnline);
            updateData.put("lastUpdate", new Date());

            db.collection("delivery_locations")
                    .document(userId)
                    .update(updateData)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("DeliveryStatus", "Statut isOnline mis à jour: " + isOnline);
                    })
                    .addOnFailureListener(e -> {
                        Log.e("DeliveryStatus", "Erreur mise à jour isOnline: " + e.getMessage());
                    });
        }
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
            String userId = currentUser.getUid();

            // Format de l'adresse à partir des coordonnées
            String formattedAddress = formatLocationToAddress(location);

            // 1. Mettre à jour delivery_locations avec isOnline = true (car on met à jour la position)
            Map<String, Object> locationData = new HashMap<>();
            locationData.put("latitude", location.getLatitude());
            locationData.put("longitude", location.getLongitude());
            locationData.put("lastUpdate", new Date());
            locationData.put("isOnline", true); // Toujours true quand on met à jour la position
            locationData.put("employeeId", userId);

            db.collection("delivery_locations").document(userId).set(locationData);

            // 2. Mettre à jour Employees
            Map<String, Object> employeeUpdateData = new HashMap<>();
            employeeUpdateData.put("location", formattedAddress);
            employeeUpdateData.put("lastLocationUpdate", new Date());
            employeeUpdateData.put("latitude", location.getLatitude());
            employeeUpdateData.put("longitude", location.getLongitude());

            db.collection("Employees").document(userId).update(employeeUpdateData);

            // Mettre à jour l'UI
            updateAddressInUI(formattedAddress);
        }
    }

    // Méthode pour formater les coordonnées en adresse
    private String formatLocationToAddress(Location location) {
        if (location == null) return "Position non disponible";

        // Format simple pour l'instant - vous pourriez utiliser Geocoder pour une adresse réelle
        return String.format("Lat: %.6f, Lng: %.6f",
                location.getLatitude(), location.getLongitude());
    }

    // Méthode pour mettre à jour l'adresse dans l'UI
    private void updateAddressInUI(String address) {
        if (tvAddress != null && !address.isEmpty()) {
            tvAddress.setText(address);
        }
    }

    // Ajouter cette méthode dans la section de chargement du profil
    private void checkAndUpdateCurrentLocation() {
        if (hasLocationPermission()) {
            try {
                fusedLocationClient.getLastLocation()
                        .addOnSuccessListener(requireActivity(), location -> {
                            if (location != null && switchActiveStatus.isChecked()) {
                                // Mettre à jour le champ location dans Employees
                                updateEmployeeLocationField(location);
                            }
                        });
            } catch (SecurityException e) {
                Log.e("LocationCheck", "Erreur permission: " + e.getMessage());
            }
        }
    }

    private void updateEmployeeLocationField(Location location) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String formattedAddress = formatLocationToAddress(location);

            Map<String, Object> updateData = new HashMap<>();
            updateData.put("location", formattedAddress);
            updateData.put("lastLocationUpdate", new Date());
            updateData.put("latitude", location.getLatitude());
            updateData.put("longitude", location.getLongitude());

            db.collection("Employees")
                    .document(currentUser.getUid())
                    .update(updateData)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("EmployeeLocation", "Champ location mis à jour dans Employees");
                        // Mettre à jour l'UI
                        tvAddress.setText(formattedAddress);
                    })
                    .addOnFailureListener(e -> {
                        Log.e("EmployeeLocation", "Erreur mise à jour location: " + e.getMessage());
                    });
        }
    }

    // ========== GARDEZ SEULEMENT CETTE VERSION DE activateDeliveryStatus() ==========
    private void activateDeliveryStatus() {
        if (hasLocationPermission() && isLocationEnabled()) {
            // Récupérer la position actuelle d'abord
            try {
                fusedLocationClient.getLastLocation()
                        .addOnSuccessListener(requireActivity(), location -> {
                            if (location != null) {
                                // Mettre à jour le champ location avant de démarrer le tracking
                                updateEmployeeLocationField(location);

                                // Démarrer le tracking continu
                                startLocationTracking();

                                // Mettre à jour le statut
                                updateDeliveryStatus(true);
                                updateDeliveryLocationStatus(true);
                                updateLocationUIForActiveStatus();

                                // Mettre à jour l'UI avec la position
                                updateLocationUI(location);
                                updateMapWithLocation(location);

                                Toast.makeText(getContext(), "Statut activé - Position partagée", Toast.LENGTH_SHORT).show();
                            } else {
                                // Si pas de position immédiate, démarrer le tracking quand même
                                startLocationTracking();
                                updateDeliveryStatus(true);
                                updateDeliveryLocationStatus(true);
                                updateLocationUIForActiveStatus();
                                Toast.makeText(getContext(), "Statut activé - En attente de position", Toast.LENGTH_SHORT).show();
                            }
                        });
            } catch (SecurityException e) {
                Log.e("ActivateStatus", "Erreur permission: " + e.getMessage());
            }
        } else {
            switchActiveStatus.setChecked(false);
            if (!isLocationEnabled()) {
                showLocationSettingsDialog();
            } else {
                requestLocationPermissionWithExplanation();
            }
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
                                // S'assurer que le statut isOnline est à true
                                updateDeliveryLocationStatus(true);
                            } else {
                                updateLocationUIForInactiveStatus();
                                // S'assurer que le statut isOnline est à false
                                updateDeliveryLocationStatus(false);
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
                            updateDeliveryStatsUI(document);
                        } else {
                            initializeDefaultStats();
                        }
                    } else {
                        initializeDefaultStats();
                    }
                });
    }

    private void updateDeliveryStatsUI(DocumentSnapshot document) {
        // Livraisons complétées
        Long completed = document.getLong("completedDeliveries");
        tvCompletedDeliveries.setText(completed != null ? String.valueOf(completed) : "0");

        // Note
        Double rating = document.getDouble("rating");
        tvRating.setText(rating != null ? String.format("%.1f", rating) : "0.0");

        // Objectif mensuel
        Long monthlyGoal = document.getLong("monthlyGoal");
        tvMonthlyGoal.setText(monthlyGoal != null ? monthlyGoal + " livraisons" : "50 livraisons");

        // Progression
        Long currentProgress = document.getLong("currentProgress");
        tvProgress.setText(currentProgress != null ? currentProgress + "/" + (monthlyGoal != null ? monthlyGoal : 50) : "0/50");

        // Temps moyen
        Double avgTime = document.getDouble("avgDeliveryTime");
        tvAvgDeliveryTime.setText(avgTime != null ? String.format("%.0f min", avgTime) : "-- min");

        // Texte détaillé
        tvDeliveryStats.setText(buildStatsText(document));
    }

    private String buildStatsText(DocumentSnapshot document) {
        StringBuilder stats = new StringBuilder();

        Long totalDeliveries = document.getLong("totalDeliveries");
        Long onTimeDeliveries = document.getLong("onTimeDeliveries");
        Double customerRating = document.getDouble("customerRating");
        Long earnings = document.getLong("earnings");
        Date joinDate = document.getDate("joinDate");

        if (totalDeliveries != null) {
            stats.append("• Livraisons totales: ").append(totalDeliveries).append("\n");
        }
        if (onTimeDeliveries != null && totalDeliveries != null && totalDeliveries > 0) {
            double onTimePercentage = (onTimeDeliveries * 100.0) / totalDeliveries;
            stats.append("• Livraisons à temps: ").append(String.format("%.1f", onTimePercentage)).append("%\n");
        }
        if (customerRating != null) {
            stats.append("• Note clients: ").append(String.format("%.1f", customerRating)).append("/5\n");
        }
        if (earnings != null) {
            stats.append("• Gains totaux: ").append(earnings).append("DT\n");
        }
        if (joinDate != null) {
            stats.append("• Membre depuis: ").append(joinDate.toString().substring(0, 10));
        }

        return stats.toString().isEmpty() ? "Aucune statistique disponible" : stats.toString();
    }

    private void initializeDefaultStats() {
        tvCompletedDeliveries.setText("0");
        tvRating.setText("0.0");
        tvMonthlyGoal.setText("50 livraisons");
        tvProgress.setText("0/50");
        tvAvgDeliveryTime.setText("-- min");
        tvDeliveryStats.setText("Statistiques non disponibles pour le moment");
    }

    @Override
    public void onResume() {
        super.onResume();
        // Si le statut était actif, redémarrer le tracking
        if (switchActiveStatus.isChecked() && hasLocationPermission() && isLocationEnabled()) {
            startLocationTracking();
            // S'assurer que le statut est bien "en ligne"
            updateDeliveryLocationStatus(true);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Seulement arrêter le tracking, ne pas changer le statut
        stopLocationTracking();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopLocationTracking();
    }
}