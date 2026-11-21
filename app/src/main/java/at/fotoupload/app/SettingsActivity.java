package at.fotoupload.app;

import android.app.Activity;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends Activity {
    private EditText address;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(23, 60, 52));
        getWindow().setNavigationBarColor(Color.rgb(23, 60, 52));

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(24), dp(20), dp(24), dp(24));
        page.setBackgroundColor(Color.rgb(248, 250, 249));
        page.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(dp(24), dp(20) + insets.getSystemWindowInsetTop(), dp(24),
                dp(24) + insets.getSystemWindowInsetBottom());
            return insets;
        });

        TextView title = text("⚙  Einstellungen", 26, Color.rgb(23, 60, 52));
        title.setPadding(0, 0, 0, dp(24));
        page.addView(title);

        TextView label = text("Serveradresse", 17, Color.rgb(30, 45, 41));
        page.addView(label);

        address = new EditText(this);
        address.setSingleLine(true);
        address.setHint("https://fotos.example.org");
        address.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        address.setText(getSharedPreferences(MainActivity.PREFERENCES, MODE_PRIVATE)
            .getString(MainActivity.SERVER_KEY, ""));
        address.setSelectAllOnFocus(false);
        page.addView(address, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView help = text("Hier trägst du die Adresse deines Foto-Upload-Servers ein. "
            + "Für den Zugriff über das Internet sollte sie mit https:// beginnen.",
            15, Color.rgb(80, 94, 90));
        help.setPadding(0, dp(12), 0, dp(24));
        page.addView(help);

        Button save = new Button(this);
        save.setText("Speichern und öffnen");
        save.setAllCaps(false);
        save.setTextSize(17);
        save.setOnClickListener(view -> save());
        page.addView(save, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));

        Button cancel = new Button(this);
        cancel.setText("Abbrechen");
        cancel.setAllCaps(false);
        cancel.setOnClickListener(view -> finish());
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
        cancelParams.topMargin = dp(10);
        page.addView(cancel, cancelParams);

        setContentView(page);
    }

    private TextView text(String value, int size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void save() {
        String value = address.getText().toString().trim();
        Uri uri = Uri.parse(value);
        boolean validScheme = "https".equalsIgnoreCase(uri.getScheme())
            || "http".equalsIgnoreCase(uri.getScheme());
        boolean rootOnly = uri.getPath() == null || uri.getPath().isEmpty() || "/".equals(uri.getPath());
        if (!validScheme || uri.getHost() == null || uri.getUserInfo() != null || !rootOnly
            || uri.getQuery() != null || uri.getFragment() != null) {
            address.setError("Bitte eine vollständige Adresse eingeben, z. B. https://fotos.example.org");
            address.requestFocus();
            return;
        }
        getSharedPreferences(MainActivity.PREFERENCES, MODE_PRIVATE).edit()
            .putString(MainActivity.SERVER_KEY, uri.toString()).apply();
        Toast.makeText(this, "Serveradresse gespeichert", Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }
}
