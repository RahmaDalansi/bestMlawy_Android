package com.example.bestmlawi;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 9001;
    private static final String TAG = "LoginActivity";

    private EditText editTextEmail, editTextPassword;
    private Button buttonLogin;
    private TextView buttonGoogle, textCreateAccount, textForgotPassword;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient googleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Log.d(TAG, "onCreate: Démarrage de l'activité");

        // Initialiser Firebase Auth et Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        Log.d(TAG, "Firebase Auth initialisé: " + (mAuth != null));
        Log.d(TAG, "Firestore initialisé: " + (db != null));

        // Configuration de Google Sign-In
        try {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build();
            googleSignInClient = GoogleSignIn.getClient(this, gso);
            Log.d(TAG, "Google Sign-In configuré");
        } catch (Exception e) {
            Log.e(TAG, "Erreur configuration Google Sign-In: " + e.getMessage());
            Toast.makeText(this, "Erreur configuration Google", Toast.LENGTH_SHORT).show();
        }

        initializeViews();
        setupClickListeners();

        // Vérifier si l'utilisateur est déjà connecté
        checkCurrentUser();
    }

    private void initializeViews() {
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        buttonGoogle = findViewById(R.id.buttonGoogle);
        textCreateAccount = findViewById(R.id.textCreateAccount);
        textForgotPassword = findViewById(R.id.textForgotPassword);
    }

    private void setupClickListeners() {
        buttonLogin.setOnClickListener(v -> {
            Log.d(TAG, "Bouton login cliqué");
            loginUser();
        });
        buttonGoogle.setOnClickListener(v -> {
            Log.d(TAG, "Bouton Google cliqué");
            signInWithGoogle();
        });
        textCreateAccount.setOnClickListener(v -> createAccount());
        textForgotPassword.setOnClickListener(v -> forgotPassword());
    }

    private void checkCurrentUser() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        Log.d(TAG, "Vérification utilisateur courant: " + (currentUser != null ? currentUser.getEmail() : "null"));

        if (currentUser != null) {
            // Vérifier le rôle de l'utilisateur
            checkUserRoleAndRedirect(currentUser.getUid());
        }
    }

    private void loginUser() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        Log.d(TAG, "Tentative de connexion avec email: " + email);

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Connexion email réussie");
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            Log.d(TAG, "Utilisateur connecté: " + user.getEmail() + ", UID: " + user.getUid());
                            checkUserRoleAndRedirect(user.getUid());
                        }
                    } else {
                        Log.e(TAG, "Échec connexion email: " + task.getException().getMessage());
                        Toast.makeText(LoginActivity.this, "Échec de l'authentification: " +
                                task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void signInWithGoogle() {
        Log.d(TAG, "Début connexion Google");
        try {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        } catch (Exception e) {
            Log.e(TAG, "Erreur lancement Google Sign-In: " + e.getMessage());
            Toast.makeText(this, "Erreur connexion Google", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    Log.d(TAG, "Compte Google récupéré: " + account.getEmail());
                    Log.d(TAG, "ID Token: " + (account.getIdToken() != null ? "présent" : "null"));
                    firebaseAuthWithGoogle(account.getIdToken());
                }
            } catch (ApiException e) {
                Log.e(TAG, "Erreur Google Sign-In: " + e.getStatusCode() + " - " + e.getMessage());
                Toast.makeText(this, "Échec de la connexion Google: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        Log.d(TAG, "Authentification Firebase avec Google token");

        if (idToken == null) {
            Log.e(TAG, "Token Google null");
            Toast.makeText(this, "Token d'authentification invalide", Toast.LENGTH_SHORT).show();
            return;
        }

        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Authentification Google Firebase réussie");
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            Log.d(TAG, "Utilisateur Google connecté dans Auth: " + user.getEmail() + ", UID: " + user.getUid());

                            // Maintenant, créer ou vérifier l'utilisateur dans Firestore
                            createOrUpdateUserInFirestore(user);
                        }
                    } else {
                        Log.e(TAG, "Échec authentification Google Firebase: " +
                                (task.getException() != null ? task.getException().getMessage() : "unknown error"));
                        Toast.makeText(this, "Échec de l'authentification Google: " +
                                task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void createOrUpdateUserInFirestore(FirebaseUser user) {
        Log.d(TAG, "Création/Mise à jour utilisateur dans Firestore: " + user.getUid());

        // Vérifier d'abord si l'utilisateur existe déjà
        db.collection("users").document(user.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Log.d(TAG, "Utilisateur existe déjà dans Firestore, mise à jour des données");
                            // Mettre à jour les informations existantes
                            updateUserInFirestore(user, document);
                        } else {
                            Log.d(TAG, "Nouvel utilisateur, création dans Firestore");
                            // Créer un nouvel utilisateur
                            createNewUserInFirestore(user);
                        }
                    } else {
                        Log.e(TAG, "Erreur vérification Firestore: " + task.getException().getMessage());
                        Toast.makeText(this, "Erreur de vérification", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void createNewUserInFirestore(FirebaseUser user) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("id", user.getUid());
        userData.put("name", user.getDisplayName() != null ? user.getDisplayName() : "Utilisateur Google");
        userData.put("email", user.getEmail());
        userData.put("role", "employee"); // Rôle par défaut
        userData.put("phoneNumber", "");
        userData.put("address", "");
        userData.put("city", "");
        userData.put("country", "");
        userData.put("gender", "");
        userData.put("job", "");
        userData.put("isActive", true);
        userData.put("active", true);
        userData.put("createdAt", System.currentTimeMillis());
        userData.put("dateOfBirth", System.currentTimeMillis());

        // Ajouter la photo de profil si disponible
        if (user.getPhotoUrl() != null) {
            userData.put("photoUrl", user.getPhotoUrl().toString());
        }

        db.collection("users").document(user.getUid())
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Utilisateur créé avec succès dans Firestore");
                    Toast.makeText(LoginActivity.this, "Compte créé avec succès!", Toast.LENGTH_SHORT).show();

                    // Rediriger selon le rôle
                    checkUserRoleAndRedirect(user.getUid());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Erreur création utilisateur Firestore: " + e.getMessage());
                    Toast.makeText(LoginActivity.this, "Erreur création du profil: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void updateUserInFirestore(FirebaseUser user, DocumentSnapshot existingDocument) {
        Map<String, Object> updates = new HashMap<>();

        // Mettre à jour les champs qui pourraient avoir changé
        if (user.getDisplayName() != null && !user.getDisplayName().equals(existingDocument.getString("name"))) {
            updates.put("name", user.getDisplayName());
        }
        if (user.getEmail() != null && !user.getEmail().equals(existingDocument.getString("email"))) {
            updates.put("email", user.getEmail());
        }
        if (user.getPhotoUrl() != null) {
            updates.put("photoUrl", user.getPhotoUrl().toString());
        }

        // Mettre à jour la date de dernière connexion
        updates.put("lastLogin", System.currentTimeMillis());

        if (!updates.isEmpty()) {
            db.collection("users").document(user.getUid())
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "✅ Utilisateur mis à jour dans Firestore");
                        checkUserRoleAndRedirect(user.getUid());
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "❌ Erreur mise à jour Firestore: " + e.getMessage());
                        // Continuer quand même avec la redirection
                        checkUserRoleAndRedirect(user.getUid());
                    });
        } else {
            // Aucune mise à jour nécessaire, rediriger directement
            checkUserRoleAndRedirect(user.getUid());
        }
    }

    private void checkUserRoleAndRedirect(String userId) {
        Log.d(TAG, "Vérification du rôle pour l'utilisateur: " + userId);

        db.collection("users").document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            String role = document.getString("role");
                            Log.d(TAG, "Rôle trouvé: " + role);

                            if (role != null) {
                                redirectBasedOnRole(role);
                            } else {
                                // Si pas de rôle défini, utiliser le rôle par défaut
                                Log.d(TAG, "Aucun rôle défini, utilisation du rôle par défaut 'employee'");
                                redirectBasedOnRole("employee");
                            }
                        } else {
                            Log.e(TAG, "Document utilisateur non trouvé dans Firestore");
                            Toast.makeText(this, "Erreur: profil utilisateur non trouvé", Toast.LENGTH_SHORT).show();
                            mAuth.signOut();
                        }
                    } else {
                        Log.e(TAG, "Erreur vérification rôle: " + task.getException().getMessage());
                        Toast.makeText(this, "Erreur de vérification du rôle", Toast.LENGTH_SHORT).show();
                        mAuth.signOut();
                    }
                });
    }

    private void redirectBasedOnRole(String role) {
        Intent intent;

        if ("gerant".equals(role) || "admin".equals(role)) {
            // Rediriger vers MainActivity (Dashboard gérant)
            intent = new Intent(LoginActivity.this, MainActivity.class);
            Toast.makeText(this, "Bienvenue Gérant!", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Redirection vers MainActivity (Gérant)");
        } else {
            // Rediriger vers EmployeeDeliveryActivity (Dashboard livreur)
            intent = new Intent(LoginActivity.this, EmployeeDeliveryActivity.class);
            Toast.makeText(this, "Bienvenue Livreur!", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Redirection vers EmployeeDeliveryActivity (Livreur)");
        }

        startActivity(intent);
        finish();
    }

    private void createAccount() {
        // Rediriger vers l'activité d'inscription
        Intent intent = new Intent(this, RegisterActivity.class);
        startActivity(intent);
    }

    private void forgotPassword() {
        String email = editTextEmail.getText().toString().trim();
        if (email.isEmpty()) {
            Toast.makeText(this, "Veuillez entrer votre email", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Email de réinitialisation envoyé", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Échec de l'envoi de l'email", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Vérifier si l'utilisateur est déjà connecté avec Google
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null && mAuth.getCurrentUser() != null) {
            Log.d(TAG, "Utilisateur déjà connecté au démarrage");
            checkUserRoleAndRedirect(mAuth.getCurrentUser().getUid());
        }
    }
}