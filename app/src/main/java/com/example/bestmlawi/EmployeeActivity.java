package com.example.bestmlawi;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.example.bestmlawi.ui.home.HomeFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class EmployeeActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private BottomNavigationView bottomNavigationView;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee);

        mAuth = FirebaseAuth.getInstance();

        // Initialiser la Toolbar
        setupToolbar();

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        setupBottomNavigation();

        // Charger le fragment par défaut
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
            updateToolbarTitle("Dashboard");
        }
    }

    private void setupToolbar() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Optionnel: Afficher le bouton retour
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.consultation_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.menu_logout) {
            logout();
            return true;
        } else if (itemId == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selectedFragment = null;
                String title = "Employee Dashboard";

                int itemId = item.getItemId();

                if (itemId == R.id.nav_dashboard) {
                    selectedFragment = new HomeFragment();
                    title = "Dashboard";
                } else if (itemId == R.id.nav_employee) {

                    // Si vous avez un fragment pour les employés
                    // selectedFragment = new EmployeeFragment();
                    Toast.makeText(EmployeeActivity.this, "Gestion des employés", Toast.LENGTH_SHORT).show();
                    title = "Employés";
                } else if (itemId == R.id.nav_orders) {
                    // Si vous avez un fragment pour les commandes
                    // selectedFragment = new OrdersFragment();
                    Toast.makeText(EmployeeActivity.this, "Gestion des commandes", Toast.LENGTH_SHORT).show();
                    title = "Commandes";
                } else if (itemId == R.id.nav_sales) {
                    // Si vous avez un fragment pour les points de vente
                    // selectedFragment = new SalesFragment();
                    Toast.makeText(EmployeeActivity.this, "Points de vente", Toast.LENGTH_SHORT).show();
                    title = "Points de vente";
                } else if (itemId == R.id.nav_menu) {
                    // Menu supplémentaire
                    Toast.makeText(EmployeeActivity.this, "Menu supplémentaire", Toast.LENGTH_SHORT).show();
                    title = "Menu";
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

    private void logout() {
        mAuth.signOut();
        Intent intent = new Intent(EmployeeActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
        Toast.makeText(this, "Déconnexion réussie", Toast.LENGTH_SHORT).show();
    }
}