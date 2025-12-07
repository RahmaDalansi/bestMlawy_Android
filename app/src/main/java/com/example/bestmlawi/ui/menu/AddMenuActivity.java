package com.example.bestmlawi.ui.menu;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bestmlawi.R;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class AddMenuActivity extends AppCompatActivity {
    private EditText editName, editDescription, editPrice, editIngredients, editRating;
    private Spinner spinnerCategory;
    private ImageView imageMenu;
    private Button btnAdd, btnSelectImage, btnCancel;
    private FirebaseFirestore db;
    private Uri imageUri;
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int MAX_IMAGE_SIZE = 1024; // Max dimension for resizing

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_menu);

        db = FirebaseFirestore.getInstance();

        initializeViews();
        setupCategorySpinner();
        setupClickListeners();
    }

    private void initializeViews() {
        editName = findViewById(R.id.editName);
        editDescription = findViewById(R.id.editDescription);
        editPrice = findViewById(R.id.editPrice);
        editIngredients = findViewById(R.id.editIngredients);
        editRating = findViewById(R.id.editRating);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        imageMenu = findViewById(R.id.imageMenu);
        btnAdd = findViewById(R.id.btnAdd);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnCancel = findViewById(R.id.btnCancel);
    }

    private void setupCategorySpinner() {
        String[] categories = {"mlawi", "burgers", "pizzas", "drinks", "desserts", "sides"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
    }

    private void setupClickListeners() {
        btnSelectImage.setOnClickListener(v -> selectImage());
        btnAdd.setOnClickListener(v -> addMenuItem());
        btnCancel.setOnClickListener(v -> finish());
    }

    private void selectImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            imageUri = data.getData();
            imageMenu.setImageURI(imageUri);
        }
    }

    private void addMenuItem() {
        String name = editName.getText().toString().trim();
        String description = editDescription.getText().toString().trim();
        String priceStr = editPrice.getText().toString().trim();
        String ingredients = editIngredients.getText().toString().trim();
        String ratingStr = editRating.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();

        // Validation
        if (TextUtils.isEmpty(name)) {
            editName.setError("Name is required");
            return;
        }
        if (TextUtils.isEmpty(description)) {
            editDescription.setError("Description is required");
            return;
        }
        if (TextUtils.isEmpty(priceStr)) {
            editPrice.setError("Price is required");
            return;
        }

        double price, rating = 0.0;
        try {
            price = Double.parseDouble(priceStr);
            if (!TextUtils.isEmpty(ratingStr)) {
                rating = Double.parseDouble(ratingStr);
                if (rating < 0 || rating > 5) {
                    editRating.setError("Rating must be between 0 and 5");
                    return;
                }
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid price or rating format", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Adding menu item...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        if (imageUri != null) {
            // Convert image to Base64
            double finalRating = rating;
            new Thread(() -> {
                try {
                    String base64Image = convertImageToBase64(imageUri);

                    runOnUiThread(() -> {
                        saveMenuItem(name, description, price, category,
                                ingredients, base64Image, finalRating, progressDialog);
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Image processing failed: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
                }
            }).start();
        } else {
            saveMenuItem(name, description, price, category, ingredients, "", rating, progressDialog);
        }
    }

    private String convertImageToBase64(Uri imageUri) throws Exception {
        InputStream inputStream = getContentResolver().openInputStream(imageUri);

        // First, decode to bitmap to check size
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(inputStream, null, options);
        inputStream.close();

        // Calculate inSampleSize for resizing
        options.inSampleSize = calculateInSampleSize(options, MAX_IMAGE_SIZE, MAX_IMAGE_SIZE);
        options.inJustDecodeBounds = false;

        // Re-open stream and decode with resizing
        inputStream = getContentResolver().openInputStream(imageUri);
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
        inputStream.close();

        if (bitmap == null) {
            throw new Exception("Failed to decode image");
        }

        // Compress to JPEG (80% quality)
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();

        // Convert to Base64
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private void saveMenuItem(String name, String description, double price, String category,
                              String ingredients, String imageBase64, double rating,
                              ProgressDialog progressDialog) {

        Map<String, Object> menuData = new HashMap<>();
        menuData.put("name", name);
        menuData.put("description", description);
        menuData.put("price", price);
        menuData.put("category", category);
        menuData.put("ingredients", ingredients);
        menuData.put("imageBase64", imageBase64); // Store Base64
        menuData.put("rating", rating);
        menuData.put("createdAt", FieldValue.serverTimestamp());

        db.collection("menu_items")
                .add(menuData)
                .addOnSuccessListener(documentReference -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Menu item added successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Error adding menu item: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }
}