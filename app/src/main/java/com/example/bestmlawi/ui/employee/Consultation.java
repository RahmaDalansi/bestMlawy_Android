package com.example.bestmlawi.ui.employee;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.bestmlawi.MainActivity;
import com.example.bestmlawi.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class Consultation extends Activity {
    private ListView lstEmployee;
    private Button btnAddEmployee, btnApplyClearFilter;
    private Button btnFilterCollaborator, btnFilterDeliver, btnFilterCoordinator;
    private EditText edtSearch;
    private TextView txtOurEmployee;

    // Navigation
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;
    private BottomNavigationView bottomNav;

    // Adapter et données
    private ArrayAdapter<Employee> adpEmployee;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private List<Employee> employeeList;
    private List<Employee> filteredEmployeeList;

    private String selectedRole = "";
    private boolean filtersApplied = false;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.consultation);
        initialiser();
        ecouteurs();
        remplir();
        setupNavigationDrawer();
        setupBottomNavigation();
    }

    private void initialiser() {
        lstEmployee = findViewById(R.id.lstEmployee);
        btnAddEmployee = findViewById(R.id.btnAddEmployee);
        btnApplyClearFilter = findViewById(R.id.btnApplyClearFilter);
        edtSearch = findViewById(R.id.edtSearch);

        // Initialiser les boutons de filtre
        btnFilterCollaborator = findViewById(R.id.btnFilterCollaborator);
        btnFilterDeliver = findViewById(R.id.btnFilterDeliver);
        btnFilterCoordinator = findViewById(R.id.btnFilterCoordinator);

        txtOurEmployee = findViewById(R.id.txtOurEmployee);

        // Initialiser la navigation
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        toolbar = findViewById(R.id.toolbar);
        bottomNav = findViewById(R.id.bottom_navigation);

        employeeList = new ArrayList<>();
        filteredEmployeeList = new ArrayList<>();

        // Créer l'adapter personnalisé
        adpEmployee = new ArrayAdapter<Employee>(this, R.layout.employee_list_item, R.id.txtEmployeeName, filteredEmployeeList) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);

                Employee employee = getItem(position);
                if (employee != null) {
                    ImageView imgEmployee = view.findViewById(R.id.imgEmployee);
                    TextView txtEmployeeName = view.findViewById(R.id.txtEmployeeName);
                    TextView txtEmployeeRole = view.findViewById(R.id.txtEmployeeRole);
                    TextView txtEmployeeLocation = view.findViewById(R.id.txtEmployeeLocation);

                    // DÉBOGUAGE - Afficher les informations de l'image
                    System.out.println("=== DÉBOGUAGE IMAGE ===");
                    System.out.println("Nom: " + employee.getName());
                    System.out.println("Données image: " + (employee.getImageUrl() != null ?
                            employee.getImageUrl().substring(0, Math.min(50, employee.getImageUrl().length())) + "..." : "null"));

                    // Charger l'image depuis Base64
                    String imageBase64 = employee.getImageUrl();
                    if (imageBase64 != null && !imageBase64.isEmpty() && imageBase64.length() > 100) {
                        // C'est une image Base64 (longue chaîne)
                        try {
                            byte[] decodedString = Base64.decode(imageBase64, Base64.DEFAULT);
                            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                            Glide.with(Consultation.this)
                                    .load(decodedByte)
                                    .apply(RequestOptions.circleCropTransform())
                                    .placeholder(R.drawable.ic_person_placeholder)
                                    .error(R.drawable.ic_person_placeholder)
                                    .into(imgEmployee);

                            System.out.println("Image Base64 chargée avec succès");
                        } catch (Exception e) {
                            System.out.println("Erreur décodage Base64: " + e.getMessage());
                            imgEmployee.setImageResource(R.drawable.ic_person_placeholder);
                        }
                    } else if (imageBase64 != null && imageBase64.startsWith("http")) {
                        // C'est une URL (si vous utilisez Firebase Storage plus tard)
                        Glide.with(Consultation.this)
                                .load(imageBase64)
                                .apply(RequestOptions.circleCropTransform())
                                .placeholder(R.drawable.ic_person_placeholder)
                                .error(R.drawable.ic_person_placeholder)
                                .into(imgEmployee);
                    } else {
                        // Aucune image - utiliser le placeholder
                        System.out.println("Aucune image trouvée - utilisation placeholder");
                        imgEmployee.setImageResource(R.drawable.ic_person_placeholder);
                    }

                    // Mettre à jour les textes
                    txtEmployeeName.setText(employee.getName());
                    txtEmployeeRole.setText(employee.getRole());

                    if (employee.getLocation() != null && !employee.getLocation().isEmpty()) {
                        txtEmployeeLocation.setText(employee.getLocation());
                        txtEmployeeLocation.setVisibility(View.VISIBLE);
                    } else {
                        txtEmployeeLocation.setVisibility(View.GONE);
                    }
                }

                return view;
            }
        };

        lstEmployee.setAdapter(adpEmployee);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        updateApplyClearButton();
    }

    private void setupNavigationDrawer() {
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();

                if (itemId == R.id.nav_dashbord) {
                    Intent intent = new Intent(Consultation.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                    return true;

                } else if (itemId == R.id.nav_employee) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return true;

                } else if (itemId == R.id.nav_orders) {
                    Toast.makeText(Consultation.this, "Orders - À implémenter", Toast.LENGTH_SHORT).show();
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return true;

                } else if (itemId == R.id.nav_sales) {
                    Toast.makeText(Consultation.this, "Sales Points - À implémenter", Toast.LENGTH_SHORT).show();
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return true;

                } else if (itemId == R.id.nav_more) {
                    Toast.makeText(Consultation.this, "Menu - À implémenter", Toast.LENGTH_SHORT).show();
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                }

                drawerLayout.closeDrawer(GravityCompat.START);
                return false;
            }
        });
    }

    private void setupBottomNavigation() {
        if (bottomNav == null) {
            Toast.makeText(this, "BottomNavigationView non trouvé", Toast.LENGTH_SHORT).show();
            return;
        }

        bottomNav.setSelectedItemId(R.id.nav_employee);

        bottomNav.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();

                if (itemId == R.id.nav_dashbord) {
                    Intent intent = new Intent(Consultation.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                    return true;

                } else if (itemId == R.id.nav_employee) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return true;

                } else if (itemId == R.id.nav_orders) {
                    Toast.makeText(Consultation.this, "Orders - À implémenter", Toast.LENGTH_SHORT).show();
                    return true;

                } else if (itemId == R.id.nav_sales) {
                    Toast.makeText(Consultation.this, "Sales Points - À implémenter", Toast.LENGTH_SHORT).show();
                    return true;

                } else if (itemId == R.id.nav_more) {
                    Toast.makeText(Consultation.this, "Menu - À implémenter", Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.consultation_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.menu_logout) {
            logout();
            return true;
        } else if (itemId == android.R.id.home) {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void logout() {
        mAuth.signOut();
        Toast.makeText(this, "Déconnexion réussie", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void ecouteurs() {
        btnAddEmployee.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Consultation.this, Ajout.class);
                startActivity(intent);
            }
        });

        btnApplyClearFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (filtersApplied) {
                    clearFiltres();
                } else {
                    appliquerFiltres();
                }
            }
        });

        btnFilterCollaborator.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleRoleFilter("collaborator", btnFilterCollaborator);
            }
        });

        btnFilterDeliver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleRoleFilter("deliver", btnFilterDeliver);
            }
        });

        btnFilterCoordinator.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleRoleFilter("coordinator", btnFilterCoordinator);
            }
        });

        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkFiltersApplied();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        lstEmployee.setOnItemClickListener((parent, view, position, id) -> {
            Employee employee = filteredEmployeeList.get(position);
            showEmployeeDialog(employee);
        });
    }

    private void toggleRoleFilter(String role, Button button) {
        if (selectedRole.equals(role)) {
            selectedRole = "";
            resetFilterButtons();
        } else {
            selectedRole = role;
            resetFilterButtons();
            setButtonSelected(button);
        }
        checkFiltersApplied();
    }

    private void resetFilterButtons() {
        btnFilterCollaborator.setBackgroundResource(R.drawable.filter_button_default);
        btnFilterCollaborator.setTextColor(ContextCompat.getColor(this, R.color.grey_text));

        btnFilterDeliver.setBackgroundResource(R.drawable.filter_button_default);
        btnFilterDeliver.setTextColor(ContextCompat.getColor(this, R.color.grey_text));

        btnFilterCoordinator.setBackgroundResource(R.drawable.filter_button_default);
        btnFilterCoordinator.setTextColor(ContextCompat.getColor(this, R.color.grey_text));
    }

    private void setButtonSelected(Button button) {
        button.setBackgroundResource(R.drawable.filter_button_selected);
        button.setTextColor(ContextCompat.getColor(this, android.R.color.white));
    }

    private void checkFiltersApplied() {
        boolean hasSearchFilter = !edtSearch.getText().toString().trim().isEmpty();
        boolean hasRoleFilter = !selectedRole.isEmpty();

        filtersApplied = hasSearchFilter || hasRoleFilter;
        updateApplyClearButton();

        if (filtersApplied) {
            filtrerEmployees();
        } else {
            filteredEmployeeList.clear();
            filteredEmployeeList.addAll(employeeList);
            mettreAJourListe();
        }
    }

    private void updateApplyClearButton() {
        if (filtersApplied) {
            btnApplyClearFilter.setText("Clear Filters");
        } else {
            btnApplyClearFilter.setText("Apply Filter");
        }
    }

    private void appliquerFiltres() {
        filtrerEmployees();
        Toast.makeText(this, "Filtres appliqués", Toast.LENGTH_SHORT).show();
    }

    private void clearFiltres() {
        edtSearch.setText("");
        selectedRole = "";
        resetFilterButtons();
        filtersApplied = false;
        updateApplyClearButton();

        filteredEmployeeList.clear();
        filteredEmployeeList.addAll(employeeList);
        mettreAJourListe();

        Toast.makeText(this, "Filtres effacés", Toast.LENGTH_SHORT).show();
    }

    private void filtrerEmployees() {
        String searchText = edtSearch.getText().toString().toLowerCase().trim();

        filteredEmployeeList.clear();

        for (Employee employee : employeeList) {
            boolean matchesSearch = searchText.isEmpty() ||
                    employee.getName().toLowerCase().contains(searchText) ||
                    (employee.getEmail() != null && employee.getEmail().toLowerCase().contains(searchText)) ||
                    (employee.getLocation() != null && employee.getLocation().toLowerCase().contains(searchText));

            boolean matchesRole = selectedRole.isEmpty() ||
                    (employee.getRole() != null && employee.getRole().equalsIgnoreCase(selectedRole));

            if (matchesSearch && matchesRole) {
                filteredEmployeeList.add(employee);
            }
        }

        mettreAJourListe();
    }

    private void mettreAJourListe() {
        adpEmployee.notifyDataSetChanged();

        if (filteredEmployeeList.isEmpty() && filtersApplied) {
            Toast.makeText(this, "Aucun employé trouvé avec ces critères", Toast.LENGTH_SHORT).show();
        }
    }

    private void showEmployeeDialog(Employee employee) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.employee_dialog, null);
        builder.setView(dialogView);

        ImageView imgDialogEmployee = dialogView.findViewById(R.id.imgDialogEmployee);
        TextView txtEmployeeName = dialogView.findViewById(R.id.txtDialogEmployeeName);
        TextView txtEmployeeRole = dialogView.findViewById(R.id.txtDialogEmployeeRole);
        TextView txtEmployeeLocation = dialogView.findViewById(R.id.txtDialogEmployeeLocation);

        LinearLayout btnViewDetails = dialogView.findViewById(R.id.btnViewDetails);
        LinearLayout btnEdit = dialogView.findViewById(R.id.btnEdit);
        LinearLayout btnDelete = dialogView.findViewById(R.id.btnDelete);

        // Charger l'image dans le dialog
        String imageBase64 = employee.getImageUrl();
        if (imageBase64 != null && !imageBase64.isEmpty() && imageBase64.length() > 100) {
            try {
                byte[] decodedString = Base64.decode(imageBase64, Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                Glide.with(Consultation.this)
                        .load(decodedByte)
                        .apply(RequestOptions.circleCropTransform())
                        .placeholder(R.drawable.ic_person_placeholder)
                        .error(R.drawable.ic_person_placeholder)
                        .into(imgDialogEmployee);
            } catch (Exception e) {
                imgDialogEmployee.setImageResource(R.drawable.ic_person_placeholder);
            }
        } else {
            imgDialogEmployee.setImageResource(R.drawable.ic_person_placeholder);
        }

        txtEmployeeName.setText(employee.getName());
        txtEmployeeRole.setText(employee.getRole());

        if (employee.getLocation() != null && !employee.getLocation().isEmpty()) {
            txtEmployeeLocation.setText(employee.getLocation());
        } else {
            txtEmployeeLocation.setText("Not specified");
        }

        AlertDialog dialog = builder.create();
        dialog.show();

        btnViewDetails.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                viewEmployeeDetails(employee);
            }
        });

        btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                editEmployee(employee);
            }
        });

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                showDeleteConfirmationDialog(employee);
            }
        });
    }

    private void showDeleteConfirmationDialog(Employee employee) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View deleteDialogView = inflater.inflate(R.layout.suppression, null);
        builder.setView(deleteDialogView);

        TextView txtDeleteTitle = deleteDialogView.findViewById(R.id.txtDeleteTitle);
        TextView txtDeleteMessage = deleteDialogView.findViewById(R.id.txtDeleteMessage);
        Button btnCancelDelete = deleteDialogView.findViewById(R.id.btnCancelDelete);
        Button btnConfirmDelete = deleteDialogView.findViewById(R.id.btnConfirmDelete);

        if (txtDeleteTitle != null) {
            txtDeleteTitle.setText("Supprimer l'employé");
        }
        if (txtDeleteMessage != null) {
            txtDeleteMessage.setText("Êtes-vous sûr de vouloir supprimer " + employee.getName() + " ?\n\nCette action est irréversible !");
        }

        AlertDialog deleteDialog = builder.create();
        deleteDialog.show();

        btnCancelDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteDialog.dismiss();
            }
        });

        btnConfirmDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteDialog.dismiss();
                deleteEmployee(employee);
            }
        });
    }

    private void viewEmployeeDetails(Employee employee) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Employee Details");

        String details = "Name: " + employee.getName() + "\n" +
                "Role: " + employee.getRole() + "\n" +
                "Location: " + (employee.getLocation() != null ? employee.getLocation() : "Not specified") + "\n" +
                "Email: " + (employee.getEmail() != null ? employee.getEmail() : "Not specified") + "\n" +
                "Phone: " + (employee.getPhoneNumber() != null ? employee.getPhoneNumber() : "Not specified");

        builder.setMessage(details);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.show();
    }

    private void editEmployee(Employee employee) {
        Intent intent = new Intent(Consultation.this, Modification.class);
        intent.putExtra("EMPLOYEE_ID", employee.getId());
        intent.putExtra("EMPLOYEE_NAME", employee.getName());
        intent.putExtra("EMPLOYEE_ROLE", employee.getRole());
        intent.putExtra("EMPLOYEE_LOCATION", employee.getLocation());
        intent.putExtra("EMPLOYEE_EMAIL", employee.getEmail());
        intent.putExtra("EMPLOYEE_PHONE", employee.getPhoneNumber());
        intent.putExtra("EMPLOYEE_POINT_OF_SALE", employee.getPointOfSaleId());
        intent.putExtra("EMPLOYEE_IMAGE_URL", employee.getImageUrl());
        startActivity(intent);
    }

    private void deleteEmployee(Employee employee) {
        db.collection("Employees").document(employee.getId())
                .delete()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(Consultation.this, "Employé supprimé avec succès", Toast.LENGTH_SHORT).show();
                            remplir();
                        } else {
                            Toast.makeText(Consultation.this, "Erreur lors de la suppression: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void remplir() {
        db.collection("Employees")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            employeeList.clear();
                            filteredEmployeeList.clear();

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                // Créer l'objet Employee manuellement pour s'assurer que tous les champs sont mappés
                                Employee employee = new Employee();
                                employee.setId(document.getId());
                                employee.setName(document.getString("name"));
                                employee.setEmail(document.getString("email"));
                                employee.setPhoneNumber(document.getString("phoneNumber"));
                                employee.setRole(document.getString("role"));
                                employee.setLocation(document.getString("location"));
                                employee.setPointOfSaleId(document.getString("point_of_sale_id"));
                                employee.setImageUrl(document.getString("image"));
                                employee.setHiredDate(document.getDate("hiredDate"));

                                employeeList.add(employee);
                            }

                            filteredEmployeeList.addAll(employeeList);
                            mettreAJourListe();

                            if (employeeList.isEmpty()) {
                                Toast.makeText(getApplicationContext(), "Aucun employé trouvé", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(getApplicationContext(), "Erreur Firebase: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    @SuppressLint("GestureBackNavigation")
    @SuppressWarnings("deprecation")
    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_employee);
        }
        remplir();
    }
}