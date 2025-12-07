package com.example.bestmlawi;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.bestmlawi.databinding.ActivityMainBinding;
import com.example.bestmlawi.ui.employee.Consultation;
import com.example.bestmlawi.ui.orders.Consultation_orders;
import com.example.bestmlawi.ui.settings.SettingsActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private BottomNavigationView bottomNav;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Check authentication first
        checkAuthentication();

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);

        // Cacher le FAB si non utilisé
        if (binding.appBarMain.fab != null) {
            binding.appBarMain.fab.setVisibility(View.GONE);
        }

        // Initialize NavController
        navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);

        // Setup navigation (bottom nav + app bar)
        setupNavigation();

        // Set custom NavigationView listener (handles nav_logout manually)
        binding.navView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();

                if (id == R.id.nav_logout) {
                    logout();
                    binding.drawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                }

                // Handle other items via Navigation
                if (NavigationUI.onNavDestinationSelected(item, navController)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                }

                return false;
            }
        });

        // Update drawer header with real user info
        updateNavHeader();

        // Afficher le Dashboard par défaut
        if (savedInstanceState == null) {
            showHomeFragment();
        }
    }

    private void setupNavigation() {
        bottomNav = findViewById(R.id.bottom_nav_view);

        if (bottomNav == null) {
            Toast.makeText(this, "BottomNavigationView non trouvée", Toast.LENGTH_SHORT).show();
            return;
        }

        // Configuration des destinations de niveau supérieur
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_dashboard,
                R.id.nav_employee,
                R.id.nav_orders,
                R.id.nav_sales,
                R.id.nav_menu,
                R.id.nav_profile)
                .setOpenableLayout(binding.drawerLayout)
                .build();

        // Configuration de l'ActionBar avec NavController
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);

        // Configuration de BottomNavigationView avec NavController
        NavigationUI.setupWithNavController(bottomNav, navController);
        bottomNav.setVisibility(View.VISIBLE);

        // Custom bottom nav behavior
        bottomNav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_menu) {
                binding.drawerLayout.openDrawer(binding.navView);
                return false;
            }
            return NavigationUI.onNavDestinationSelected(item, navController);
        });
    }

    private void showHomeFragment() {
        try {
            NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
            navController.navigate(R.id.nav_dashboard);

            // Sélectionner l'item dans la bottom nav
            if (bottomNav != null) {
                bottomNav.setSelectedItemId(R.id.nav_dashboard);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateNavHeader() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        // Get header view from NavigationView
        View headerView = binding.navView.getHeaderView(0); // usually index 0

        TextView textViewName = headerView.findViewById(R.id.textViewName);
        TextView textViewEmail = headerView.findViewById(R.id.textViewEmail);

        // Set email (always available if signed in with email)
        String email = user.getEmail();
        textViewEmail.setText(email != null ? email : "No email");

        // Set display name (may be null)
        String displayName = user.getDisplayName();
        if (displayName != null && !displayName.isEmpty()) {
            textViewName.setText(displayName);
        } else {
            // Fallback: use part of email before '@'
            if (email != null) {
                String nameFromEmail = email.substring(0, email.indexOf('@'));
                textViewName.setText(capitalize(nameFromEmail));
            } else {
                textViewName.setText("User");
            }
        }
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    private void openEmployeeConsultation() {
        // Vérifier l'authentification avant d'ouvrir Consultation
        if (isAdmin()) {
            Intent intent = new Intent(MainActivity.this, Consultation.class);
            startActivity(intent);
        } else {
            Toast.makeText(this, "Accès réservé aux administrateurs", Toast.LENGTH_SHORT).show();
        }
    }

    private void openOrdersConsultation() {
        Intent intent = new Intent(MainActivity.this, Consultation_orders.class);
        startActivity(intent);
    }

    private void checkAuthentication() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            redirectToLogin();
            return;
        }

        // Vérifier le rôle dans Firestore
        checkUserRole(currentUser.getUid());
    }

    private void checkUserRole(String userId) {
        db.collection("users").document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            String role = document.getString("role");
                            if (role == null || (!"gerant".equals(role) && !"admin".equals(role))) {
                                // L'utilisateur n'est pas admin/gerant
                                Toast.makeText(MainActivity.this,
                                        "Accès réservé aux administrateurs", Toast.LENGTH_LONG).show();
                                // Option: rediriger vers une activité limitée
                            }
                        } else {
                            Toast.makeText(MainActivity.this,
                                    "Profil utilisateur non trouvé", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(MainActivity.this,
                                "Erreur de vérification", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private boolean isAdmin() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        return currentUser != null;
    }

    private void logout() {
        mAuth.signOut();
        Toast.makeText(this, "Déconnexion réussie", Toast.LENGTH_SHORT).show();
        redirectToLogin();
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            // Ouvrir l'activité des paramètres
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.menu_notifications) {
            showNotifications();
            return true;
        } else if (id == R.id.menu_logout) {
            logout();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showNotifications() {
        Toast.makeText(this, "Notifications", Toast.LENGTH_SHORT).show();
        // Vous pouvez implémenter une activité ou un dialog pour les notifications
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Vérifier l'authentification
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            redirectToLogin();
        }

        // S'assurer que la bottom nav est visible
        if (bottomNav != null) {
            bottomNav.setVisibility(View.VISIBLE);
            // Re-sélectionner l'item courant
            bottomNav.setSelectedItemId(R.id.nav_dashboard);
        }
    }
}