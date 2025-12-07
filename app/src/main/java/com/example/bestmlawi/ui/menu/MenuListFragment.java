package com.example.bestmlawi.ui.menu;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bestmlawi.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class MenuListFragment extends Fragment {
    private RecyclerView recyclerView;
    private MenuAdapter adapter;
    private List<Menu> menuList;
    private ProgressBar progressBar;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_menu_list, container, false);

        db = FirebaseFirestore.getInstance();
        recyclerView = view.findViewById(R.id.recyclerViewMenu);
        progressBar = view.findViewById(R.id.progressBar);
        FloatingActionButton fabAddMenu = view.findViewById(R.id.fabAddMenu);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        menuList = new ArrayList<>();
        adapter = new MenuAdapter(menuList, getContext());
        recyclerView.setAdapter(adapter);

        // Set click listener for menu items (for editing)
        adapter.setOnItemClickListener(menu -> {
            Intent intent = new Intent(getActivity(), EditMenuActivity.class);
            intent.putExtra("MENU_ID", menu.getId());
            intent.putExtra("MENU_NAME", menu.getName());
            intent.putExtra("MENU_DESCRIPTION", menu.getDescription());
            intent.putExtra("MENU_PRICE", menu.getPrice());
            intent.putExtra("MENU_CATEGORY", menu.getCategory());
            intent.putExtra("MENU_INGREDIENTS", menu.getIngredients());
            intent.putExtra("MENU_RATING", menu.getRating());
            intent.putExtra("MENU_IMAGE_BASE64", menu.getImageBase64());
            startActivity(intent);
        });

        // Load menu items
        loadMenuItems();

        // Set click listener for FAB
        fabAddMenu.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddMenuActivity.class);
            startActivity(intent);
        });

        return view;
    }

    private void loadMenuItems() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("menu_items")
                .orderBy("createdAt")
                .get()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        menuList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            // Get the data
                            String name = document.getString("name");
                            String description = document.getString("description");
                            Double price = document.getDouble("price");
                            String category = document.getString("category");
                            String ingredients = document.getString("ingredients");
                            String imageBase64 = document.getString("imageBase64");
                            Double rating = document.getDouble("rating");

                            // Create Menu object
                            Menu menu = new Menu(name, description,
                                    price != null ? price : 0.0,
                                    category != null ? category : "",
                                    ingredients != null ? ingredients : "",
                                    imageBase64 != null ? imageBase64 : "",
                                    rating != null ? rating : 0.0);

                            menu.setId(document.getId());
                            menuList.add(menu);
                        }
                        adapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(getContext(), "Erreur de chargement: " +
                                task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadMenuItems();
    }
}