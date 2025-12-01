package com.example.bestmlawi;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.bestmlawi.databinding.ActivityMainBinding;
import com.example.bestmlawi.ui.employee.Consultation;
import com.example.bestmlawi.ui.orders.Consultation_orders;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Vérifier l'authentification au démarrage
        checkAuthentication();

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);
        binding.appBarMain.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null)
                        .setAnchorView(R.id.fab).show();
            }
        });

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;

        // Configuration de la Bottom Navigation
        setupBottomNavigation();

        // Configuration de la Navigation Drawer avec TOUTES les destinations
        // Note: Inclure uniquement les destinations qui sont dans nav_graph.xml
        mAppBarConfiguration = new AppBarConfiguration.Builder(


                R.id.nav_dashboard,
                R.id.nav_collabs,
                R.id.nav_orders,
                R.id.nav_sales,
                R.id.nav_more,
                R.id.nav_profile)
                .setOpenableLayout(drawer)
                .build();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);

        // Configuration de la Navigation View
        NavigationUI.setupWithNavController(navigationView, navController);

        // Gérer le clic sur le menu dans la Navigation Drawer
        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();

            // Gérer les clics sur les Activities séparément
            if (itemId == R.id.nav_employee) {
                openEmployeeConsultation();
                drawer.closeDrawers();
                return true;
            } else if (itemId == R.id.nav_orders) {
                openOrdersConsultation();
                drawer.closeDrawers();
                return true;

            }

            // Pour les autres items, mapper vers les fragments correspondants
            try {
                NavController navCtrl = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);

                // Mapper les anciens IDs vers les nouveaux


                drawer.closeDrawers();
                return true;
            } catch (Exception e) {
                drawer.closeDrawers();
                return false;
            }
        });



        // Afficher le Dashboard par défaut au démarrage
        if (savedInstanceState == null) {
            showHomeFragment();
            if (bottomNav != null) {
                bottomNav.setSelectedItemId(R.id.nav_dashboard);
            }
        }
    }

    private void setupBottomNavigation() {
        bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav == null) {
            Toast.makeText(this, "BottomNavigationView non trouvé", Toast.LENGTH_SHORT).show();
            return;
        }

        // Récupérer le NavController
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);

        // Configuration AppBarConfiguration pour les destinations de la bottom nav
        AppBarConfiguration bottomAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_dashboard,
                R.id.nav_collabs,
                R.id.nav_orders,
                R.id.nav_sales,
                R.id.nav_more,
                R.id.nav_profile)
                .build();

        // Setup avec NavController
        NavigationUI.setupWithNavController(bottomNav, navController);

        // Surcharger le comportement pour certains items spéciaux
        bottomNav.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();

                if (itemId == R.id.nav_more) {
                    // Ouvrir le drawer navigation
                    binding.drawerLayout.openDrawer(binding.navView);
                    return true;
                }

                // Pour les items qui pointent vers des Activities
                if (itemId == R.id.nav_employee) {
                    openEmployeeConsultation();
                    return true;
                } else if (itemId == R.id.nav_orders) {
                    openOrdersConsultation();
                    return true;
                }

                // Laisser NavigationUI gérer les autres (fragments)
                try {
                    NavController navCtrl = Navigation.findNavController(MainActivity.this,
                            R.id.nav_host_fragment_content_main);

                    // Mapper si nécessaire
                    if (itemId == R.id.nav_collabs) {
                        navCtrl.navigate(R.id.nav_collabs);
                        return true;
                    } else if (itemId == R.id.nav_sales) {
                        navCtrl.navigate(R.id.nav_sales);
                        return true;
                    } else if (itemId == R.id.nav_profile) {
                        navCtrl.navigate(R.id.nav_profile);
                        return true;
                    } else {
                        return NavigationUI.onNavDestinationSelected(item, navCtrl);
                    }
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Navigation error: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
        });
    }

    private void showHomeFragment() {
        try {
            NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
            // Naviguer vers le dashboard
            navController.navigate(R.id.nav_dashboard);
        } catch (Exception e) {
            Toast.makeText(this, "Erreur navigation: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
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
                                // L'utilisateur n'est pas admin/gerant, rediriger
                                Toast.makeText(MainActivity.this, "Accès réservé aux administrateurs", Toast.LENGTH_LONG).show();
                                redirectToLogin();
                            }
                            // Si c'est un admin/gerant, on reste sur MainActivity
                        } else {
                            // Document utilisateur non trouvé
                            Toast.makeText(MainActivity.this, "Profil utilisateur non trouvé", Toast.LENGTH_LONG).show();
                            redirectToLogin();
                        }
                    } else {
                        // Erreur Firestore
                        Toast.makeText(MainActivity.this, "Erreur de vérification", Toast.LENGTH_LONG).show();
                        redirectToLogin();
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
        int itemId = item.getItemId();

        if (itemId == R.id.action_settings) {
            Toast.makeText(this, "Paramètres", Toast.LENGTH_SHORT).show();
            return true;
        } else if (itemId == R.id.menu_notifications) {
            showNotifications();
            return true;
        } else if (itemId == R.id.menu_logout) {
            logout();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showNotifications() {
        Toast.makeText(this, "Notifications", Toast.LENGTH_SHORT).show();
        // Intent vers NotificationsActivity ou dialog
        // Exemple:
        // Intent intent = new Intent(this, NotificationsActivity.class);
        // startActivity(intent);
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
        // Vérifier l'authentification à chaque retour sur l'activité
        checkAuthentication();

        // Remettre la sélection sur Dashboard quand on revient à MainActivity
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_dashboard);
        }
    }
}