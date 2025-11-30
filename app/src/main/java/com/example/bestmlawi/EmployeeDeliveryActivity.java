package com.example.bestmlawi.ui.deliver;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.example.bestmlawi.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class EmployeeDeliveryActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private BottomNavigationView bottomNavigationView;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_delivery);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialiser la Toolbar
        setupToolbar();

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        setupBottomNavigation();

        // Charger le fragment des commandes par défaut
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new DeliveryOrdersFragment())
                    .commit();
            updateToolbarTitle("Mes Commandes");
        }
    }

    private void setupToolbar() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.delivery_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.menu_notifications) {
            showNotifications();
            return true;
        } else if (itemId == R.id.menu_logout) {
            logout();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selectedFragment = null;
                String title = "Dashboard Livreur";

                int itemId = item.getItemId();

                if (itemId == R.id.nav_orders) {
                    selectedFragment = new DeliveryOrdersFragment();
                    title = "Mes Commandes";
                } else if (itemId == R.id.nav_active_orders) {
                    selectedFragment = new ActiveDeliveryFragment();
                    title = "En Livraison";
                } else if (itemId == R.id.nav_profile) {
                    selectedFragment = new DeliveryProfileFragment();
                    title = "Mon Profil";
                } else if (itemId == R.id.nav_stats) {
                    selectedFragment = new DeliveryStatsFragment();
                    title = "Statistiques";
                }

                // Mettre à jour le titre de la toolbar
                updateToolbarTitle(title);

                if (selectedFragment != null) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, selectedFragment)
                            .commit();
                }

                return true;
            }
        });
    }

    private void updateToolbarTitle(String title) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }
    }

    private void showNotifications() {
        Toast.makeText(this, "Notifications", Toast.LENGTH_SHORT).show();
        // Intent vers NotificationsActivity ou dialog
    }

    private void logout() {
        mAuth.signOut();
        Intent intent = new Intent(EmployeeDeliveryActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
        Toast.makeText(this, "Déconnexion réussie", Toast.LENGTH_SHORT).show();
    }
}