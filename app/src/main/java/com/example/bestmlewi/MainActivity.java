package com.example.bestmlewi;

import android.os.Bundle;
import android.view.View;
import android.view.Menu;

import com.example.bestmlawi.ui.orders.Consultation_orders;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bestmlewi.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
<<<<<<< Updated upstream:app/src/main/java/com/example/bestmlewi/MainActivity.java
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow)
=======

        // Configuration du AppBar (inclut nav_employee)
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home,
                R.id.nav_gallery,
                R.id.nav_slideshow,
                R.id.nav_employee,
                R.id.nav_orders)  // Ajouté pour Orders
>>>>>>> Stashed changes:app/src/main/java/com/example/bestmlawi/MainActivity.java
                .setOpenableLayout(drawer)
                .build();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
<<<<<<< Updated upstream:app/src/main/java/com/example/bestmlewi/MainActivity.java
=======

        // Gestion personnalisée des clics sur le menu
        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_employee) {
                Intent intent = new Intent(MainActivity.this, Consultation.class);
                startActivity(intent);
                drawer.closeDrawers();
                return true;
            } else if (itemId == R.id.nav_orders) {
                Intent intent = new Intent(MainActivity.this, Consultation_orders.class);
                startActivity(intent);
                drawer.closeDrawers();
                return true;
            }

            // Pour les autres items, laisse NavController gérer
            return NavigationUI.onNavDestinationSelected(item, navController)
                    || super.onOptionsItemSelected(item);
        });
>>>>>>> Stashed changes:app/src/main/java/com/example/bestmlawi/MainActivity.java
    }

    // ✅ Méthode déplacée ici — à l’extérieur de onCreate()
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}