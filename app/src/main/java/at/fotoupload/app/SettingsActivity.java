package at.fotoupload.app;

import android.app.Activity;
import android.app.AlertDialog;
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
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.ScrollView;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

public class SettingsActivity extends Activity {
    private EditText address;
    private EditText profileName;
    private Spinner history;
    private final ArrayList<String> profileUrls = new ArrayList<>();

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

        profileName = new EditText(this);
        profileName.setSingleLine(true);
        profileName.setHint("Name, z. B. Zuhause oder Familie");
        page.addView(profileName, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

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

        history = new Spinner(this);
        refreshHistory();
        history.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                if (position > 0 && position - 1 < profileUrls.size()) {
                    String selected = profileUrls.get(position - 1);
                    address.setText(selected);
                    profileName.setText(getSharedPreferences(MainActivity.PREFERENCES, MODE_PRIVATE)
                        .getString("profile:" + selected, ""));
                }
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });
        page.addView(history, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));

        Button scan = new Button(this);
        scan.setText("QR-Code scannen");
        scan.setAllCaps(false);
        scan.setOnClickListener(view -> scanServer());
        page.addView(scan, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));

        Button save = new Button(this);
        save.setText("Speichern und öffnen");
        save.setAllCaps(false);
        save.setTextSize(17);
        save.setOnClickListener(view -> save());
        page.addView(save, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));

        Button test = new Button(this);
        test.setText("Verbindung testen");
        test.setAllCaps(false);
        test.setOnClickListener(view -> testConnection(test));
        LinearLayout.LayoutParams testParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
        testParams.topMargin = dp(10);
        page.addView(test, testParams);

        Button remove = new Button(this);
        remove.setText("Profil aus Liste entfernen");
        remove.setAllCaps(false);
        remove.setOnClickListener(view -> removeProfile());
        page.addView(remove, testParams);

        Button cancel = new Button(this);
        cancel.setText("Abbrechen");
        cancel.setAllCaps(false);
        cancel.setOnClickListener(view -> finish());
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
        cancelParams.topMargin = dp(10);
        page.addView(cancel, cancelParams);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(page);
        setContentView(scroll);
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
        Uri uri = validAddress(value);
        if (uri == null) return;
        if ("http".equalsIgnoreCase(uri.getScheme())) {
            if (!MainActivity.isPrivateHttpHost(uri.getHost())) {
                new AlertDialog.Builder(this).setTitle("HTTPS erforderlich")
                    .setMessage("Eine öffentliche Serveradresse darf nur über HTTPS verwendet werden. HTTP ist auf lokale und Tailscale-Adressen beschränkt.")
                    .setPositiveButton("Verstanden", null).show();
                return;
            }
            new AlertDialog.Builder(this).setTitle("Ungeschützte Verbindung")
                .setMessage("HTTP ist nur für ein vertrauenswürdiges lokales Netzwerk gedacht. Für Zugriff über das Internet muss der Server HTTPS verwenden.")
                .setPositiveButton("Nur im LAN verwenden", (dialog, which) -> saveUri(uri))
                .setNegativeButton("Abbrechen", null).show();
            return;
        }
        saveUri(uri);
    }

    private void saveUri(Uri uri) {
        getSharedPreferences(MainActivity.PREFERENCES, MODE_PRIVATE).edit()
            .putString(MainActivity.SERVER_KEY, uri.toString())
            .putString("profile:" + uri, profileName.getText().toString().trim()).apply();
        Set<String> servers = new LinkedHashSet<>(getSharedPreferences(MainActivity.PREFERENCES, MODE_PRIVATE)
            .getStringSet("servers", new LinkedHashSet<>()));
        servers.add(uri.toString());
        getSharedPreferences(MainActivity.PREFERENCES, MODE_PRIVATE).edit()
            .putStringSet("servers", servers).apply();
        Toast.makeText(this, "Serveradresse gespeichert", Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }

    private void refreshHistory() {
        ArrayList<String> servers = new ArrayList<>();
        servers.add("Bisherige Server auswählen");
        String current = getSharedPreferences(MainActivity.PREFERENCES, MODE_PRIVATE)
            .getString(MainActivity.SERVER_KEY, "");
        Set<String> known = new LinkedHashSet<>(getSharedPreferences(MainActivity.PREFERENCES, MODE_PRIVATE)
            .getStringSet("servers", new LinkedHashSet<>()));
        if (!current.isEmpty()) known.add(current);
        profileUrls.clear();
        profileUrls.addAll(known);
        for (String url : profileUrls) {
            String name = getSharedPreferences(MainActivity.PREFERENCES, MODE_PRIVATE)
                .getString("profile:" + url, "");
            servers.add(name.isEmpty() ? url : name + " · " + url);
        }
        history.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, servers));
    }

    private void scanServer() {
        GmsBarcodeScannerOptions options = new GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE).enableAutoZoom().build();
        GmsBarcodeScanning.getClient(this, options).startScan()
            .addOnSuccessListener(barcode -> {
                Uri scanned = Uri.parse(barcode.getRawValue() == null ? "" : barcode.getRawValue());
                if (!("https".equalsIgnoreCase(scanned.getScheme()) || "http".equalsIgnoreCase(scanned.getScheme()))
                    || scanned.getHost() == null || scanned.getUserInfo() != null) {
                    Toast.makeText(this, "Dieser QR-Code enthält keine gültige Serveradresse.", Toast.LENGTH_LONG).show();
                    return;
                }
                Uri base = new Uri.Builder().scheme(scanned.getScheme()).encodedAuthority(scanned.getEncodedAuthority()).build();
                address.setText(base.toString());
                Toast.makeText(this, "Serveradresse aus QR-Code übernommen", Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(error -> Toast.makeText(this, "QR-Scanner konnte nicht gestartet werden.", Toast.LENGTH_LONG).show());
    }

    private void removeProfile() {
        String value = address.getText().toString().trim();
        Set<String> servers = new LinkedHashSet<>(getSharedPreferences(MainActivity.PREFERENCES, MODE_PRIVATE)
            .getStringSet("servers", new LinkedHashSet<>()));
        if (!servers.remove(value)) {
            Toast.makeText(this, "Dieses Profil ist noch nicht gespeichert.", Toast.LENGTH_SHORT).show();
            return;
        }
        getSharedPreferences(MainActivity.PREFERENCES, MODE_PRIVATE).edit()
            .putStringSet("servers", servers).remove("profile:" + value).apply();
        profileName.setText("");
        refreshHistory();
        Toast.makeText(this, "Profil aus der Liste entfernt", Toast.LENGTH_SHORT).show();
    }

    private Uri validAddress(String value) {
        Uri uri = Uri.parse(value);
        boolean validScheme = "https".equalsIgnoreCase(uri.getScheme())
            || "http".equalsIgnoreCase(uri.getScheme());
        boolean rootOnly = uri.getPath() == null || uri.getPath().isEmpty() || "/".equals(uri.getPath());
        if (!validScheme || uri.getHost() == null || uri.getUserInfo() != null || !rootOnly
            || uri.getQuery() != null || uri.getFragment() != null) {
            address.setError("Bitte eine vollständige Adresse eingeben, z. B. https://fotos.example.org");
            address.requestFocus();
            return null;
        }
        return uri;
    }

    private void testConnection(Button button) {
        Uri uri = validAddress(address.getText().toString().trim());
        if (uri == null) return;
        button.setEnabled(false);
        button.setText("Wird geprüft …");
        new Thread(() -> {
            boolean okay = false;
            HttpURLConnection connection = null;
            try {
                URL health = new URL(uri.toString().replaceAll("/$", "") + "/health?ready=1");
                connection = (HttpURLConnection) health.openConnection();
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.setInstanceFollowRedirects(false);
                okay = connection.getResponseCode() == 200;
            } catch (Exception ignored) {
                okay = false;
            } finally {
                if (connection != null) connection.disconnect();
            }
            boolean result = okay;
            runOnUiThread(() -> {
                button.setEnabled(true);
                button.setText("Verbindung testen");
                Toast.makeText(this, result ? "Server ist erreichbar" : "Server nicht erreichbar",
                    Toast.LENGTH_LONG).show();
            });
        }).start();
    }
}
