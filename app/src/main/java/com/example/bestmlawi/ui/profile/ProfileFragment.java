package com.example.bestmlawi.ui.profile;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.bestmlawi.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private EditText etName, etPhone, etEmail, etDateOfBirth, etJob,
            etCountry, etCity, etPostalCode, etAddress;
    private RadioGroup rgGender;
    private Button btnSave;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String userId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.profile, container, false);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            userId = user.getUid();
        }

        // Initialize views
        etName = view.findViewById(R.id.etName);
        etPhone = view.findViewById(R.id.etPhone);
        etEmail = view.findViewById(R.id.etEmail);
        etDateOfBirth = view.findViewById(R.id.etDateOfBirth);
        etJob = view.findViewById(R.id.etJob);
        etCountry = view.findViewById(R.id.etCountry);
        etCity = view.findViewById(R.id.etCity);
        etPostalCode = view.findViewById(R.id.etPostalCode);
        etAddress = view.findViewById(R.id.etAddress);
        rgGender = view.findViewById(R.id.rgGender);
        btnSave = view.findViewById(R.id.btnSave);

        // Set email (from Firebase Auth) and make it read-only
        if (user != null) {
            etEmail.setText(user.getEmail());
            etEmail.setEnabled(false);
        }

        // Load user data from Firestore
        loadUserProfile();

        // Date picker for DOB
        etDateOfBirth.setOnClickListener(v -> showDatePicker());

        // Save button
        btnSave.setOnClickListener(v -> saveUserProfile());

        return view;
    }

    private void showDatePicker() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(selectedYear, selectedMonth, selectedDay);
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    String formattedDate = sdf.format(selectedDate.getTime());
                    etDateOfBirth.setText(formattedDate);
                },
                year, month, day);
        datePickerDialog.show();
    }

    private void loadUserProfile() {
        if (userId == null) return;

        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot document) {
                        if (document.exists()) {
                            etName.setText(document.getString("name"));
                            etPhone.setText(document.getString("phone"));

                            String gender = document.getString("gender");
                            if ("Male".equals(gender)) {
                                rgGender.check(R.id.rbMale);
                            } else if ("Female".equals(gender)) {
                                rgGender.check(R.id.rbFemale);
                            } else if ("Other".equals(gender)) {
                                rgGender.check(R.id.rbOther);
                            }

                            etDateOfBirth.setText(document.getString("dob"));
                            etJob.setText(document.getString("job"));
                            etCountry.setText(document.getString("country"));
                            etCity.setText(document.getString("city"));
                            etPostalCode.setText(document.getString("postalCode"));
                            etAddress.setText(document.getString("address"));
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to load profile", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveUserProfile() {
        if (userId == null) return;

        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String dob = etDateOfBirth.getText().toString().trim();
        String job = etJob.getText().toString().trim();
        String country = etCountry.getText().toString().trim();
        String city = etCity.getText().toString().trim();
        String postalCode = etPostalCode.getText().toString().trim();
        String address = etAddress.getText().toString().trim();

        // Get selected gender
        int selectedGenderId = rgGender.getCheckedRadioButtonId();
        String gender = "Other";
        if (selectedGenderId == R.id.rbMale) {
            gender = "Male";
        } else if (selectedGenderId == R.id.rbFemale) {
            gender = "Female";
        }

        // Prepare data
        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("phone", phone);
        user.put("gender", gender);
        user.put("dob", dob);
        user.put("job", job);
        user.put("country", country);
        user.put("city", city);
        user.put("postalCode", postalCode);
        user.put("address", address);

        // Save to Firestore
        db.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to save profile", Toast.LENGTH_SHORT).show();
                });
    }
}