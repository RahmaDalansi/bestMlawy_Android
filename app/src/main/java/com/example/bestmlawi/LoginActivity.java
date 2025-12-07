package com.example.bestmlawi;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
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

    private TextInputEditText editTextEmail, editTextPassword;
    private Button buttonLogin;
    private TextView buttonGoogle, textForgotPassword;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient googleSignInClient;
    private TextInputLayout textInputLayoutPassword;

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
        textForgotPassword = findViewById(R.id.textForgotPassword);
        textInputLayoutPassword = findViewById(R.id.textInputLayoutPassword);
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

        textForgotPassword.setOnClickListener(v -> forgotPassword());
    }

    private void checkCurrentUser() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        Log.d(TAG, "Vérification utilisateur courant: " + (currentUser != null ? currentUser.getEmail() : "null"));

        if (currentUser != null) {
            // MODIFICATION: Tester d'abord dans users pour vérifier si admin
            checkAdminStatusAndRedirect(currentUser.getUid());
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
                            // MODIFICATION: Tester d'abord dans users pour vérifier si admin
                            checkAdminStatusAndRedirect(user.getUid());
                        }
                    } else {
                        Log.e(TAG, "Échec connexion email: " + task.getException().getMessage());
                        String errorMessage = task.getException().getMessage();
                        if (errorMessage.contains("invalid credential") || errorMessage.contains("wrong password")) {
                            errorMessage = "Email ou mot de passe incorrect";
                        }
                        Toast.makeText(LoginActivity.this, "Échec de l'authentification: " + errorMessage, Toast.LENGTH_LONG).show();
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
                            // MODIFICATION: Pour Google, tester d'abord dans users
                            checkAdminStatusAndRedirect(user.getUid());
                        }
                    } else {
                        Log.e(TAG, "Échec authentification Google Firebase: " +
                                (task.getException() != null ? task.getException().getMessage() : "unknown error"));
                        Toast.makeText(this, "Échec de l'authentification Google: " +
                                task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    // NOUVELLE MÉTHODE: Vérifier d'abord si l'utilisateur est admin dans "users"
    private void checkAdminStatusAndRedirect(String userId) {
        Log.d(TAG, "Vérification du statut admin pour l'utilisateur: " + userId);

        // ÉTAPE 1: Chercher d'abord dans la collection "users" pour vérifier si admin
        db.collection("users").document(userId)
                .get()
                .addOnCompleteListener(usersTask -> {
                    if (usersTask.isSuccessful()) {
                        DocumentSnapshot userDoc = usersTask.getResult();
                        if (userDoc.exists()) {
                            // Utilisateur trouvé dans users
                            String role = userDoc.getString("role");
                            String name = userDoc.getString("name");
                            Log.d(TAG, "Utilisateur trouvé dans users - Rôle: " + role + ", Nom: " + name);

                            // Vérifier si c'est un admin
                            if (role != null && ("admin".equals(role) || "administrator".equals(role) || "manager".equals(role))) {
                                // C'est un admin, rediriger vers MainActivity
                                Log.d(TAG, "Utilisateur est admin, redirection vers MainActivity");
                                redirectAdminToMain();
                            } else {
                                // Ce n'est pas un admin, vérifier dans Employees
                                Log.d(TAG, "Utilisateur n'est pas admin, vérification dans Employees...");
                                checkInEmployeesCollection(userId);
                            }
                        } else {
                            // Utilisateur non trouvé dans users, vérifier dans Employees
                            Log.d(TAG, "Utilisateur non trouvé dans users, vérification dans Employees...");
                            checkInEmployeesCollection(userId);
                        }
                    } else {
                        Log.e(TAG, "Erreur vérification users: " + usersTask.getException().getMessage());
                        // En cas d'erreur, essayer dans Employees
                        checkInEmployeesCollection(userId);
                    }
                });
    }

    // MÉTHODE: Vérifier dans la collection "Employees" pour les non-admins
    private void checkInEmployeesCollection(String userId) {
        db.collection("Employees").document(userId)
                .get()
                .addOnCompleteListener(employeesTask -> {
                    if (employeesTask.isSuccessful()) {
                        DocumentSnapshot employeeDoc = employeesTask.getResult();
                        if (employeeDoc.exists()) {
                            // Utilisateur trouvé dans Employees
                            String role = employeeDoc.getString("role");
                            String name = employeeDoc.getString("name");
                            Log.d(TAG, "Utilisateur trouvé dans Employees - Rôle: " + role + ", Nom: " + name);

                            // Rediriger selon le rôle
                            if (role != null) {
                                redirectBasedOnRole(role, "employee");
                            } else {
                                // Rôle par défaut si non défini
                                Log.d(TAG, "Aucun rôle défini, utilisation du rôle par défaut 'collaborator'");
                                redirectBasedOnRole("collaborator", "employee");
                            }
                        } else {
                            // Utilisateur non trouvé dans aucune collection
                            Log.e(TAG, "Utilisateur non trouvé dans users ni Employees");
                            Toast.makeText(this, "Erreur: profil utilisateur non trouvé. Contactez l'administrateur.", Toast.LENGTH_LONG).show();
                            mAuth.signOut();
                        }
                    } else {
                        Log.e(TAG, "Erreur vérification Employees: " + employeesTask.getException().getMessage());
                        Toast.makeText(this, "Erreur de vérification du profil", Toast.LENGTH_SHORT).show();
                        mAuth.signOut();
                    }
                });
    }

    // MÉTHODE SPÉCIFIQUE POUR REDIRIGER LES ADMINS
    private void redirectAdminToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.putExtra("user_type", "admin");
        intent.putExtra("source_collection", "users");
        Toast.makeText(this, "Bienvenue Administrateur!", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Redirection vers MainActivity pour admin");
        startActivity(intent);
        finish();
    }

    // MÉTHODE POUR REDIRIGER LES EMPLOYÉS BASÉS SUR LEUR RÔLE
    private void redirectBasedOnRole(String role, String sourceCollection) {
        Intent intent;
        String welcomeMessage = "";

        // Déterminer l'activité de destination basée sur le rôle
        if ("deliver".equals(role)) {
            // Rediriger vers EmployeeDeliveryActivity (Dashboard livreur)
            intent = new Intent(LoginActivity.this, EmployeeDeliveryActivity.class);
            welcomeMessage = "Bienvenue Livreur!";
            Log.d(TAG, "Redirection vers EmployeeDeliveryActivity (Livreur)");
        } else {
            // Pour collaborator et autres rôles, rediriger vers EmployeeActivity
            intent = new Intent(LoginActivity.this, EmployeeActivity.class);
            welcomeMessage = "Bienvenue Collaborateur!";
            Log.d(TAG, "Redirection vers EmployeeActivity (Rôle: " + role + ")");
        }

        // Afficher un message de bienvenue personnalisé
        if (!welcomeMessage.isEmpty()) {
            Toast.makeText(this, welcomeMessage, Toast.LENGTH_SHORT).show();
        }

        // Ajouter la source dans les extras si nécessaire
        intent.putExtra("source_collection", sourceCollection);

        startActivity(intent);
        finish();
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
            checkAdminStatusAndRedirect(mAuth.getCurrentUser().getUid());
        }
    }
}