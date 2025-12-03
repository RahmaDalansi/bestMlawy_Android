package com.example.bestmlawi.ui.employee;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.bestmlawi.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Consultation extends Fragment {
    private ListView lstEmployee;
    private Button btnAddEmployee;
    private MaterialButton btnApplyClearFilter; // Changé en MaterialButton
    private MaterialButton btnFilterCollaborator, btnFilterDeliver, btnFilterCoordinator; // Changé en MaterialButton
    private EditText edtSearch;
    private TextView txtOurEmployee;

    // Adapter et données
    private ArrayAdapter<Employee> adpEmployee;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private List<Employee> employeeList;
    private List<Employee> filteredEmployeeList;

    private String selectedRole = "";
    private boolean filtersApplied = false;

    private Map<String, String> nameToSalesPointId = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.consultation, container, false);
        initialiser(root);
        setupListeners();
        remplir();
        return root;
    }

    private void initialiser(View root) {
        lstEmployee = root.findViewById(R.id.lstEmployee);
        btnAddEmployee = root.findViewById(R.id.btnAddEmployee);
        btnApplyClearFilter = root.findViewById(R.id.btnApplyClearFilter);
        edtSearch = root.findViewById(R.id.edtSearch);

        // Initialiser les boutons de filtre (MaterialButton)
        btnFilterCollaborator = root.findViewById(R.id.btnFilterCollaborator);
        btnFilterDeliver = root.findViewById(R.id.btnFilterDeliver);
        btnFilterCoordinator = root.findViewById(R.id.btnFilterCoordinator);

        txtOurEmployee = root.findViewById(R.id.txtOurEmployee);

        employeeList = new ArrayList<>();
        filteredEmployeeList = new ArrayList<>();

        // Créer l'adapter personnalisé
        adpEmployee = new ArrayAdapter<Employee>(requireContext(), R.layout.employee_list_item, R.id.txtEmployeeName, filteredEmployeeList) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);

                Employee employee = getItem(position);
                if (employee != null) {
                    ImageView imgEmployee = view.findViewById(R.id.imgEmployee);
                    TextView txtEmployeeName = view.findViewById(R.id.txtEmployeeName);
                    TextView txtEmployeeRole = view.findViewById(R.id.txtEmployeeRole);
                    TextView txtEmployeeAddress = view.findViewById(R.id.txtEmployeeAddress);

                    // Charger l'image depuis Base64
                    String imageBase64 = employee.getImageUrl();
                    if (imageBase64 != null && !imageBase64.isEmpty() && imageBase64.length() > 100) {
                        try {
                            byte[] decodedString = Base64.decode(imageBase64, Base64.DEFAULT);
                            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                            Glide.with(requireContext())
                                    .load(decodedByte)
                                    .apply(RequestOptions.circleCropTransform())
                                    .placeholder(R.drawable.ic_person_placeholder)
                                    .error(R.drawable.ic_person_placeholder)
                                    .into(imgEmployee);
                        } catch (Exception e) {
                            imgEmployee.setImageResource(R.drawable.ic_person_placeholder);
                        }
                    } else if (imageBase64 != null && imageBase64.startsWith("http")) {
                        Glide.with(requireContext())
                                .load(imageBase64)
                                .apply(RequestOptions.circleCropTransform())
                                .placeholder(R.drawable.ic_person_placeholder)
                                .error(R.drawable.ic_person_placeholder)
                                .into(imgEmployee);
                    } else {
                        imgEmployee.setImageResource(R.drawable.ic_person_placeholder);
                    }

                    txtEmployeeName.setText(employee.getName());
                    txtEmployeeRole.setText(employee.getRole());

                    if (employee.getAddress() != null && !employee.getAddress().isEmpty()) {
                        txtEmployeeAddress.setText(employee.getAddress());
                        txtEmployeeAddress.setVisibility(View.VISIBLE);
                    } else {
                        txtEmployeeAddress.setVisibility(View.GONE);
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

    private void loadSalesPoints() {
        db.collection("SalesPoints")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<String> salesPointNames = new ArrayList<>();

                            for (QueryDocumentSnapshot doc : task.getResult()) {
                                String name = doc.getString("name");
                                String id = doc.getId();

                                if (name != null && !name.isEmpty()) {
                                    salesPointNames.add(name);
                                }
                            }

                            Toast.makeText(getContext(),
                                    "Points de vente chargés : " + salesPointNames.size(),
                                    Toast.LENGTH_SHORT).show();

                            if (salesPointNames.isEmpty()) {
                                Log.d("Consultation", "Aucun point de vente trouvé");
                            } else {
                                Log.d("Consultation", "Points de vente trouvés : " + salesPointNames);
                            }

                        } else {
                            String errorMsg = "Erreur chargement points de vente: " +
                                    (task.getException() != null ?
                                            task.getException().getMessage() : "Erreur inconnue");
                            Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();

                            if (task.getException() != null) {
                                task.getException().printStackTrace();
                            }
                        }
                    }
                });
    }

    private void setupListeners() {
        btnAddEmployee.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), Ajout.class);
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

    private void toggleRoleFilter(String role, MaterialButton button) {
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
        // Réinitialiser tous les boutons de filtre
        btnFilterCollaborator.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), android.R.color.transparent));
        btnFilterCollaborator.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_600));
        btnFilterCollaborator.setStrokeColorResource(R.color.gray_400);

        btnFilterDeliver.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), android.R.color.transparent));
        btnFilterDeliver.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_600));
        btnFilterDeliver.setStrokeColorResource(R.color.gray_400);

        btnFilterCoordinator.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), android.R.color.transparent));
        btnFilterCoordinator.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_600));
        btnFilterCoordinator.setStrokeColorResource(R.color.gray_400);
    }

    private void setButtonSelected(MaterialButton button) {
        // Appliquer le style sélectionné
        button.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.purple_700));
        button.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
        button.setStrokeColorResource(android.R.color.transparent);
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
            btnApplyClearFilter.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.gray_600));
        } else {
            btnApplyClearFilter.setText("Apply Filter");
            btnApplyClearFilter.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.purple_700));
        }
    }

    private void appliquerFiltres() {
        filtrerEmployees();
        Toast.makeText(getContext(), "Filtres appliqués", Toast.LENGTH_SHORT).show();
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

        Toast.makeText(getContext(), "Filtres effacés", Toast.LENGTH_SHORT).show();
    }

    private void filtrerEmployees() {
        String searchText = edtSearch.getText().toString().toLowerCase().trim();

        filteredEmployeeList.clear();

        for (Employee employee : employeeList) {
            boolean matchesSearch = searchText.isEmpty() ||
                    employee.getName().toLowerCase().contains(searchText) ||
                    (employee.getEmail() != null && employee.getEmail().toLowerCase().contains(searchText)) ||
                    (employee.getAddress() != null && employee.getAddress().toLowerCase().contains(searchText));

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
            Toast.makeText(getContext(), "Aucun employé trouvé avec ces critères", Toast.LENGTH_SHORT).show();
        }
    }

    private void showEmployeeDialog(Employee employee) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.employee_dialog, null);
        builder.setView(dialogView);

        ImageView imgDialogEmployee = dialogView.findViewById(R.id.imgDialogEmployee);
        TextView txtEmployeeName = dialogView.findViewById(R.id.txtDialogEmployeeName);
        TextView txtEmployeeRole = dialogView.findViewById(R.id.txtDialogEmployeeRole);
        TextView txtEmployeeAddress = dialogView.findViewById(R.id.txtDialogEmployeeAddress);

        LinearLayout btnViewDetails = dialogView.findViewById(R.id.btnViewDetails);
        LinearLayout btnEdit = dialogView.findViewById(R.id.btnEdit);
        LinearLayout btnDelete = dialogView.findViewById(R.id.btnDelete);

        // Charger l'image dans le dialog
        String imageBase64 = employee.getImageUrl();
        if (imageBase64 != null && !imageBase64.isEmpty() && imageBase64.length() > 100) {
            try {
                byte[] decodedString = Base64.decode(imageBase64, Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                Glide.with(requireContext())
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

        if (employee.getAddress() != null && !employee.getAddress().isEmpty()) {
            txtEmployeeAddress.setText(employee.getAddress());
        } else {
            txtEmployeeAddress.setText("Not specified");
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
                // Correction: Ajouter EMPLOYEE_ADDRESS pour Modification
                Intent intent = new Intent(getActivity(), Modification.class);
                intent.putExtra("EMPLOYEE_ID", employee.getId());
                intent.putExtra("EMPLOYEE_NAME", employee.getName());
                intent.putExtra("EMPLOYEE_ROLE", employee.getRole());
                intent.putExtra("EMPLOYEE_Address", employee.getAddress());
                intent.putExtra("EMPLOYEE_EMAIL", employee.getEmail());
                intent.putExtra("EMPLOYEE_PHONE", employee.getPhoneNumber());
                intent.putExtra("EMPLOYEE_ADDRESS", employee.getAddress()); // Ajout important
                intent.putExtra("EMPLOYEE_IMAGE_URL", employee.getImageUrl());
                startActivity(intent);
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
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = LayoutInflater.from(getContext());
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
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Employee Details");

        String details = "Name: " + employee.getName() + "\n" +
                "Role: " + employee.getRole() + "\n" +
                "Address: " + (employee.getAddress() != null ? employee.getAddress() : "Not specified") + "\n" +
                "Email: " + (employee.getEmail() != null ? employee.getEmail() : "Not specified") + "\n" +
                "Phone: " + (employee.getPhoneNumber() != null ? employee.getPhoneNumber() : "Not specified") + "\n" +
                "Address: " + (employee.getAddress() != null ? employee.getAddress() : "Not specified");

        builder.setMessage(details);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.show();
    }

    private void deleteEmployee(Employee employee) {
        db.collection("Employees").document(employee.getId())
                .delete()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(getContext(), "Employé supprimé avec succès", Toast.LENGTH_SHORT).show();
                            remplir();
                        } else {
                            Toast.makeText(getContext(), "Erreur lors de la suppression: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
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
                                Employee employee = new Employee();
                                employee.setId(document.getId());
                                employee.setName(document.getString("name"));
                                employee.setEmail(document.getString("email"));
                                employee.setPhoneNumber(document.getString("phoneNumber"));
                                employee.setRole(document.getString("role"));
                                employee.setAddress(document.getString("Address"));
                                employee.setAddress(document.getString("address")); // Charger l'adresse
                                employee.setPointOfSaleId(document.getString("point_of_sale_id"));
                                employee.setImageUrl(document.getString("image"));
                                employee.setHiredDate(document.getDate("hiredDate"));

                                employeeList.add(employee);
                            }

                            filteredEmployeeList.addAll(employeeList);
                            mettreAJourListe();

                            if (employeeList.isEmpty()) {
                                Toast.makeText(getContext(), "Aucun employé trouvé", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(getContext(), "Erreur Firebase: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        remplir();
        loadSalesPoints();
    }
}