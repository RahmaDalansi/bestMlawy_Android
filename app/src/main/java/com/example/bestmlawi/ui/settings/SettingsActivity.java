package com.example.bestmlawi.ui.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bestmlawi.R;

public class SettingsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "MySettings";
    private static final String LANGUAGE_KEY = "language";
    private static final String THEME_KEY = "theme";
    private static final String THEME_LIGHT = "light";
    private static final String THEME_DARK = "dark";

    private Spinner languageSpinner;
    private TextView languageLabel;
    private TextView descriptionText;
    private TextView selectLanguageLabel;
    private TextView feature1Label;
    private TextView feature1Desc;
    private TextView themeLabel;
    private TextView feature3Label;
    private TextView feature3Desc;
    private Button applyButton;
    private TextView otherSettingsLabel;
    private Switch themeSwitch;

    // Layouts for theme change
    private LinearLayout feature1Layout;
    private LinearLayout themeLayout;
    private LinearLayout feature3Layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Set theme based on saved preference
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String savedTheme = prefs.getString(THEME_KEY, THEME_LIGHT);

        if (savedTheme.equals(THEME_DARK)) {
            setTheme(R.style.SettingsDarkTheme);
        } else {
            setTheme(R.style.SettingsLightTheme);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Initialize views
        initializeViews();

        // Set up spinner
        setupLanguageSpinner();

        // Load saved language and theme
        loadSavedPreferences();

        // Apply button click listener
        applyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String selectedLanguage = languageSpinner.getSelectedItem().toString();
                saveLanguagePreference(selectedLanguage);
                updateUITexts(selectedLanguage);
                showLanguageChangeToast(selectedLanguage);
            }
        });

        // Theme switch listener
        themeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                String theme = isChecked ? THEME_DARK : THEME_LIGHT;
                saveThemePreference(theme);
                applyTheme(theme);
                updateThemeTexts(theme);
                showThemeChangeToast(theme);
            }
        });
    }

    private void initializeViews() {
        languageSpinner = findViewById(R.id.languageSpinner);
        languageLabel = findViewById(R.id.languageLabel);
        descriptionText = findViewById(R.id.descriptionText);
        selectLanguageLabel = findViewById(R.id.selectLanguageLabel);
        feature1Label = findViewById(R.id.feature1Label);
        feature1Desc = findViewById(R.id.feature1Desc);
        themeLabel = findViewById(R.id.themeLabel);
        feature3Label = findViewById(R.id.feature3Label);
        feature3Desc = findViewById(R.id.feature3Desc);
        applyButton = findViewById(R.id.applyButton);
        otherSettingsLabel = findViewById(R.id.otherSettingsLabel);
        themeSwitch = findViewById(R.id.themeSwitch);

        // Initialize layouts
        feature1Layout = findViewById(R.id.feature1Layout);
        themeLayout = findViewById(R.id.themeLayout);
        feature3Layout = findViewById(R.id.feature3Layout);
    }

    private void setupLanguageSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.languages_array,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        languageSpinner.setAdapter(adapter);

        // Spinner item selection listener
        languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Optionally update UI immediately when spinner changes
                String selectedLanguage = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    private void loadSavedPreferences() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Load language
        String savedLanguage = prefs.getString(LANGUAGE_KEY, "English");
        ArrayAdapter<CharSequence> adapter = (ArrayAdapter<CharSequence>) languageSpinner.getAdapter();
        int position = adapter.getPosition(savedLanguage);
        if (position >= 0) {
            languageSpinner.setSelection(position);
            updateUITexts(savedLanguage);
        }

        // Load theme
        String savedTheme = prefs.getString(THEME_KEY, THEME_LIGHT);
        themeSwitch.setChecked(savedTheme.equals(THEME_DARK));
        updateThemeTexts(savedTheme);
    }

    private void saveLanguagePreference(String language) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(LANGUAGE_KEY, language);
        editor.apply();
    }

    private void saveThemePreference(String theme) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(THEME_KEY, theme);
        editor.apply();
    }

    private void showLanguageChangeToast(String language) {
        String message = "Language changed to " + language;
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showThemeChangeToast(String theme) {
        String message = "Theme changed to " + (theme.equals(THEME_DARK) ? "Dark Mode" : "Light Mode");
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void applyTheme(String theme) {
        // Recreate activity to apply theme changes
        recreate();
    }

    private void updateUITexts(String language) {
        switch (language) {
            case "English":
                languageLabel.setText("Language");
                descriptionText.setText("Select your preferred language. This will change the language throughout the app.");
                selectLanguageLabel.setText("Select Language:");
                feature1Label.setText("Notifications");
                feature1Desc.setText("Receive notifications about new orders and updates");
                themeLabel.setText("Theme");
                feature3Label.setText("Data Sync");
                feature3Desc.setText("Synchronize data automatically with cloud");
                applyButton.setText("Apply Changes");
                otherSettingsLabel.setText("Other Settings");
                break;

            case "French":
                languageLabel.setText("Langue");
                descriptionText.setText("Sélectionnez votre langue préférée. Cela changera la langue dans toute l'application.");
                selectLanguageLabel.setText("Sélectionner la langue:");
                feature1Label.setText("Notifications");
                feature1Desc.setText("Recevez des notifications sur les nouvelles commandes et mises à jour");
                themeLabel.setText("Thème");
                feature3Label.setText("Synchronisation des Données");
                feature3Desc.setText("Synchronisez automatiquement les données avec le cloud");
                applyButton.setText("Appliquer les modifications");
                otherSettingsLabel.setText("Autres Paramètres");
                break;

            case "Arabic":
                languageLabel.setText("اللغة");
                descriptionText.setText("اختر اللغة المفضلة لديك. سيؤدي هذا إلى تغيير اللغة في جميع أنحاء التطبيق.");
                selectLanguageLabel.setText("اختر اللغة:");
                feature1Label.setText("الإشعارات");
                feature1Desc.setText("تلقي إشعارات حول الطلبات الجديدة والتحديثات");
                themeLabel.setText("المظهر");
                feature3Label.setText("مزامنة البيانات");
                feature3Desc.setText("مزامنة البيانات تلقائياً مع السحابة");
                applyButton.setText("تطبيق التغييرات");
                otherSettingsLabel.setText("إعدادات أخرى");
                break;
        }

        // Also update theme texts based on current theme
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String currentTheme = prefs.getString(THEME_KEY, THEME_LIGHT);
        updateThemeTexts(currentTheme);
    }

    private void updateThemeTexts(String theme) {
        // Update theme-related texts
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String language = prefs.getString(LANGUAGE_KEY, "English");

        if (language.equals("French")) {
            themeSwitch.setText(theme.equals(THEME_DARK) ? "Sombre" : "Clair");
        } else if (language.equals("Arabic")) {
            themeSwitch.setText(theme.equals(THEME_DARK) ? "داكن" : "فاتح");
        } else {
            themeSwitch.setText(theme.equals(THEME_DARK) ? "Dark" : "Light");
        }
    }
}