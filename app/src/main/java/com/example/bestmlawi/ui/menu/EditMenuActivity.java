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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bestmlawi.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class EditMenuActivity extends AppCompatActivity {
    private EditText editName, editDescription, editPrice, editIngredients, editRating;
    private Spinner spinnerCategory;
    private ImageView imageMenu;
    private Button btnUpdate, btnDelete, btnSelectImage, btnCancel;
    private FirebaseFirestore db;
    private Uri imageUri;
    private String menuId;
    private String currentImageBase64;
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int MAX_IMAGE_SIZE = 1024;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_menu);

        db = FirebaseFirestore.getInstance();

        initializeViews();
        setupCategorySpinner();
        loadMenuData();
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
        btnUpdate = findViewById(R.id.btnUpdate);
        btnDelete = findViewById(R.id.btnDelete);
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

    private void loadMenuData() {
        Intent intent = getIntent();
        menuId = intent.getStringExtra("MENU_ID");

        editName.setText(intent.getStringExtra("MENU_NAME"));
        editDescription.setText(intent.getStringExtra("MENU_DESCRIPTION"));
        editPrice.setText(String.valueOf(intent.getDoubleExtra("MENU_PRICE", 0)));
        editIngredients.setText(intent.getStringExtra("MENU_INGREDIENTS"));
        editRating.setText(String.valueOf(intent.getDoubleExtra("MENU_RATING", 0)));

        // Get Base64 image from intent
        currentImageBase64 = intent.getStringExtra("MENU_IMAGE_BASE64");

        // Set category spinner
        String category = intent.getStringExtra("MENU_CATEGORY");
        ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinnerCategory.getAdapter();
        int position = adapter.getPosition(category);
        if (position >= 0) {
            spinnerCategory.setSelection(position);
        }

        // Load Base64 image
        if (currentImageBase64 != null && !currentImageBase64.isEmpty()) {
            try {
                byte[] decodedBytes = Base64.decode(currentImageBase64, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                imageMenu.setImageBitmap(bitmap);
            } catch (Exception e) {
                imageMenu.setImageResource(R.drawable.food_placeholder_image);
            }
        } else {
            imageMenu.setImageResource(R.drawable.food_placeholder_image);
        }
    }

    private void setupClickListeners() {
        btnSelectImage.setOnClickListener(v -> selectImage());
        btnUpdate.setOnClickListener(v -> updateMenuItem());
        btnDelete.setOnClickListener(v -> showDeleteConfirmation());
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

    private void updateMenuItem() {
        String name = editName.getText().toString().trim();
        String description = editDescription.getText().toString().trim();
        String priceStr = editPrice.getText().toString().trim();
        String ingredients = editIngredients.getText().toString().trim();
        String ratingStr = editRating.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();

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
        progressDialog.setMessage("Updating menu item...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        if (imageUri != null) {
            double finalRating = rating;
            new Thread(() -> {
                try {
                    String newImageBase64 = convertImageToBase64(imageUri);

                    runOnUiThread(() -> {
                        updateMenuItemInFirestore(name, description, price, category,
                                ingredients, newImageBase64, finalRating, progressDialog);
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
            updateMenuItemInFirestore(name, description, price, category,
                    ingredients, currentImageBase64, rating, progressDialog);
        }
    }

    private String convertImageToBase64(Uri imageUri) throws Exception {
        InputStream inputStream = getContentResolver().openInputStream(imageUri);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(inputStream, null, options);
        inputStream.close();

        options.inSampleSize = calculateInSampleSize(options, MAX_IMAGE_SIZE, MAX_IMAGE_SIZE);
        options.inJustDecodeBounds = false;

        inputStream = getContentResolver().openInputStream(imageUri);
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
        inputStream.close();

        if (bitmap == null) {
            throw new Exception("Failed to decode image");
        }

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();

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

    private void updateMenuItemInFirestore(String name, String description, double price,
                                           String category, String ingredients, String imageBase64,
                                           double rating, ProgressDialog progressDialog) {

        db.collection("menu_items")
                .document(menuId)
                .update(
                        "name", name,
                        "description", description,
                        "price", price,
                        "category", category,
                        "ingredients", ingredients,
                        "imageBase64", imageBase64,
                        "rating", rating
                )
                .addOnSuccessListener(aVoid -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Menu item updated successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Error updating menu item: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Menu Item")
                .setMessage("Are you sure you want to delete this menu item?")
                .setPositiveButton("Delete", (dialog, which) -> deleteMenuItem())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteMenuItem() {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Deleting menu item...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        db.collection("menu_items")
                .document(menuId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Menu item deleted successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Error deleting menu item: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }
}