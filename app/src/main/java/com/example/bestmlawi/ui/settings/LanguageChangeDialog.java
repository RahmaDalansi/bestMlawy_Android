package com.example.bestmlawi.ui.settings;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.bestmlawi.R;

public class LanguageChangeDialog extends Dialog {

    private String language;
    private ProgressBar progressBar;
    private TextView messageText;
    private Handler handler;
    private int progress = 0;

    public LanguageChangeDialog(Context context, String language) {
        super(context);
        this.language = language;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_language_change);

        progressBar = findViewById(R.id.progressBar);
        messageText = findViewById(R.id.messageText);

        // Set initial message based on language
        String message = "Applying " + language + " language...";
        messageText.setText(message);

        // Make it non-cancelable
        setCancelable(false);
        setCanceledOnTouchOutside(false);

        // Start fake progress
        handler = new Handler();
        startFakeProgress();
    }

    private void startFakeProgress() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                progress += 5;
                progressBar.setProgress(progress);

                if (progress < 100) {
                    // Update message at certain intervals
                    if (progress == 30) {
                        messageText.setText("Updating interface...");
                    } else if (progress == 60) {
                        messageText.setText("Applying translations...");
                    } else if (progress == 90) {
                        messageText.setText("Finishing up...");
                    }

                    // Continue progress
                    handler.postDelayed(this, 100);
                } else {
                    // Progress complete
                    dismiss();
                }
            }
        }, 100);
    }

    @Override
    public void dismiss() {
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        super.dismiss();
    }
}