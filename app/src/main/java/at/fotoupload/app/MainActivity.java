package at.fotoupload.app;

import android.app.Activity;
import android.app.DownloadManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.graphics.Color;
import android.provider.MediaStore;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceError;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.JavascriptInterface;
import android.os.Build;
import android.content.pm.PackageManager;
import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;
import java.util.ArrayList;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONTokener;

public class MainActivity extends Activity {
    private boolean uploadServiceActive = false;
    private static final int CHOOSE_FILE = 100;
    private static final int CAPTURE_PHOTO = 101;
    private static final int EDIT_SETTINGS = 102;
    static final String PREFERENCES = "foto_upload_settings";
    static final String SERVER_KEY = "server";
    private static final Uri CAMERA_URI = Uri.parse("content://at.fotoupload.app.camera/capture.jpg");
    private WebView browser;
    private LinearLayout offline;
    private ValueCallback<Uri[]> fileCallback;
    private Uri server;
    private Uri[] sharedFiles;
    private boolean sharedAlbumPicker;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(Color.rgb(23, 60, 52));
        layout.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(0, insets.getSystemWindowInsetTop(), 0, insets.getSystemWindowInsetBottom());
            return insets;
        });
        getWindow().setStatusBarColor(Color.rgb(23, 60, 52));
        getWindow().setNavigationBarColor(Color.rgb(23, 60, 52));
        LinearLayout navigation = new LinearLayout(this);
        navigation.setOrientation(LinearLayout.HORIZONTAL);
        Button home = navButton("⌂\nStart");
        home.setOnClickListener(view -> { if (server != null) browser.loadUrl(server.toString()); });
        Button refresh = navButton("↻\nNeu laden");
        refresh.setOnClickListener(view -> browser.reload());
        Button settings = navButton("⚙\nEinstellungen");
        settings.setOnClickListener(view -> editSettings());
        navigation.addView(home, new LinearLayout.LayoutParams(0, -1, 1));
        navigation.addView(refresh, new LinearLayout.LayoutParams(0, -1, 1));
        navigation.addView(settings, new LinearLayout.LayoutParams(0, -1, 1));
        FrameLayout content = new FrameLayout(this);
        browser = new WebView(this);
        content.addView(browser, new FrameLayout.LayoutParams(-1, -1));
        offline = makeOfflineView();
        content.addView(offline, new FrameLayout.LayoutParams(-1, -1));
        layout.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));
        layout.addView(navigation, new LinearLayout.LayoutParams(-1, dp(56)));
        setContentView(layout);
        WebSettings web = browser.getSettings();
        web.setJavaScriptEnabled(true);
        web.setTextZoom(100);
        web.setUserAgentString(web.getUserAgentString() + " FotoUploadAndroid/1");
        web.setDomStorageEnabled(true);
        web.setAllowFileAccess(false);
        web.setAllowContentAccess(true);
        browser.addJavascriptInterface(new UploadBridge(), "AndroidUpload");
        browser.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, true);
        browser.setWebViewClient(new WebViewClient() {
            @Override public void onPageStarted(WebView view, String url, android.graphics.Bitmap icon) {
                browser.setVisibility(View.VISIBLE);
                offline.setVisibility(View.GONE);
            }

            @Override public void onPageFinished(WebView view, String url) {
                if (sharedFiles != null && sharedAlbumPicker && sameServer(Uri.parse(url))) {
                    sharedAlbumPicker = false;
                    showSharedAlbumPicker();
                }
            }

            @Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) showOffline();
            }

            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri target = request.getUrl();
                if (sameServer(target)) return false;
                if ("https".equals(target.getScheme()) || "http".equals(target.getScheme())) {
                    startActivity(new Intent(Intent.ACTION_VIEW, target));
                }
                return true;
            }
        });
        browser.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                if (sharedFiles != null && sharedFiles.length > 0) {
                    Uri[] selected = sharedFiles;
                    sharedFiles = null;
                    callback.onReceiveValue(selected);
                    Toast.makeText(MainActivity.this, "Geteilte Dateien werden hochgeladen", Toast.LENGTH_SHORT).show();
                    return true;
                }
                fileCallback = callback;
                if (params.isCaptureEnabled()) {
                    Intent camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    camera.putExtra(MediaStore.EXTRA_OUTPUT, CAMERA_URI);
                    camera.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    try {
                        startActivityForResult(camera, CAPTURE_PHOTO);
                        return true;
                    } catch (android.content.ActivityNotFoundException error) {
                        // Fall through to the system photo picker.
                    }
                }
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                try { startActivityForResult(intent, CHOOSE_FILE); }
                catch (Exception error) { fileCallback = null; callback.onReceiveValue(null); return false; }
                return true;
            }
        });
        browser.setDownloadListener((url, agent, disposition, mime, length) -> {
            Uri target = Uri.parse(url);
            if (!sameServer(target)) return;
            DownloadManager.Request request = new DownloadManager.Request(target);
            String cookie = CookieManager.getInstance().getCookie(url);
            if (cookie != null) request.addRequestHeader("Cookie", cookie);
            request.addRequestHeader("User-Agent", agent);
            request.setMimeType(mime);
            request.setTitle("Foto-Upload");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS,
                android.webkit.URLUtil.guessFileName(url, disposition, mime));
            ((DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE)).enqueue(request);
            Toast.makeText(this, "Download gestartet", Toast.LENGTH_SHORT).show();
        });
        migratePreferences();
        receiveSharedFiles(getIntent());
        String saved = getSharedPreferences(PREFERENCES, MODE_PRIVATE).getString(SERVER_KEY, "");
        if (saved.isEmpty()) editSettings(); else openServer(saved);
        checkForUpdate();
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS")
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 200);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private Button navButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(10);
        button.setLines(2);
        button.setPadding(0, 0, 0, 0);
        return button;
    }

    private void checkForUpdate() {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(
                    "https://api.github.com/repos/ThomS263/foto_upload_offiziell_androidapp/releases/latest"
                ).openConnection();
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                String installed = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
                connection.setRequestProperty("User-Agent", "FotoUploadAndroid/" + installed);
                if (connection.getResponseCode() != 200) return;
                StringBuilder body = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null && body.length() < 65536) body.append(line);
                }
                String latest = new JSONObject(body.toString()).optString("tag_name", "").replaceFirst("^v", "");
                if (!newer(latest, installed)) return;
                runOnUiThread(() -> new AlertDialog.Builder(this)
                    .setTitle("App-Update verfügbar")
                    .setMessage("Version " + latest + " kann jetzt von GitHub installiert werden.")
                    .setPositiveButton("APK herunterladen", (dialog, which) -> startActivity(new Intent(
                        Intent.ACTION_VIEW, Uri.parse("https://github.com/ThomS263/foto_upload_offiziell_androidapp/releases/latest"))))
                    .setNegativeButton("Später", null).show());
            } catch (Exception ignored) {
                // Updates are optional; the app remains usable without GitHub.
            } finally {
                if (connection != null) connection.disconnect();
            }
        }).start();
    }

    private boolean newer(String candidate, String installed) {
        try {
            String[] left = candidate.split("\\.");
            String[] right = installed.split("\\.");
            for (int index = 0; index < Math.max(left.length, right.length); index++) {
                int a = index < left.length ? Integer.parseInt(left[index]) : 0;
                int b = index < right.length ? Integer.parseInt(right[index]) : 0;
                if (a != b) return a > b;
            }
        } catch (NumberFormatException ignored) { }
        return false;
    }

    private class UploadBridge {
        @JavascriptInterface public void setActive(boolean active, int count) {
            Intent service = new Intent(MainActivity.this, UploadService.class)
                .putExtra("active", active).putExtra("count", count);
            if (active) {
                uploadServiceActive = true;
                startForegroundService(service);
            } else if (uploadServiceActive) {
                uploadServiceActive = false;
                startService(service);
            }
        }
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        receiveSharedFiles(intent);
    }

    private void receiveSharedFiles(Intent intent) {
        if (intent == null || !(Intent.ACTION_SEND.equals(intent.getAction())
            || Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction()))) return;
        ArrayList<Uri> uris = new ArrayList<>();
        if (intent.getClipData() != null) {
            for (int index = 0; index < intent.getClipData().getItemCount(); index++) {
                Uri uri = intent.getClipData().getItemAt(index).getUri();
                if (uri != null && "content".equals(uri.getScheme())) uris.add(uri);
            }
        }
        if (uris.isEmpty()) {
            if (Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction())) {
                ArrayList<Uri> many = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                if (many != null) for (Uri uri : many) if (uri != null && "content".equals(uri.getScheme())) uris.add(uri);
            } else {
                Uri one = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (one != null && "content".equals(one.getScheme())) uris.add(one);
            }
        }
        if (!uris.isEmpty()) {
            sharedFiles = uris.toArray(new Uri[0]);
            sharedAlbumPicker = true;
            if (server != null) browser.loadUrl(server.toString());
            Toast.makeText(this, "Wähle gleich das Zielalbum aus.", Toast.LENGTH_LONG).show();
        }
    }

    private void showSharedAlbumPicker() {
        browser.evaluateJavascript(
            "JSON.stringify(Array.from(document.querySelectorAll('.album-link')).map(a=>({t:a.querySelector('strong')?.textContent||a.textContent.trim(),u:a.getAttribute('href')})))",
            result -> {
                try {
                    Object decoded = new JSONTokener(result).nextValue();
                    JSONArray albums = new JSONArray(String.valueOf(decoded));
                    if (albums.length() == 0) {
                        Toast.makeText(this, "Kein Album gefunden. Öffne ein Album und tippe Fotos auswählen.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    String[] names = new String[albums.length()];
                    for (int index = 0; index < albums.length(); index++) names[index] = albums.getJSONObject(index).getString("t");
                    new AlertDialog.Builder(this).setTitle("Zielalbum auswählen").setItems(names, (dialog, which) -> {
                        String path = albums.optJSONObject(which).optString("u", "/");
                        browser.loadUrl(server.buildUpon().path(path).build().toString());
                        Toast.makeText(this, "Falls nötig anmelden, dann Fotos auswählen antippen.", Toast.LENGTH_LONG).show();
                    }).setNegativeButton("Abbrechen", null).show();
                } catch (Exception error) {
                    Toast.makeText(this, "Albumwahl konnte nicht geladen werden.", Toast.LENGTH_LONG).show();
                }
            });
    }

    private LinearLayout makeOfflineView() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setGravity(Gravity.CENTER);
        panel.setPadding(48, 48, 48, 48);
        panel.setBackgroundColor(Color.rgb(248, 250, 249));
        panel.setVisibility(View.GONE);
        TextView title = new TextView(this);
        title.setText("Server nicht erreichbar");
        title.setTextSize(24);
        title.setTextColor(Color.rgb(23, 60, 52));
        title.setGravity(Gravity.CENTER);
        panel.addView(title);
        TextView message = new TextView(this);
        message.setText("Prüfe deine Internetverbindung oder die Serveradresse.");
        message.setTextSize(16);
        message.setGravity(Gravity.CENTER);
        message.setPadding(0, 20, 0, 24);
        panel.addView(message);
        Button retry = new Button(this);
        retry.setText("Erneut versuchen");
        retry.setAllCaps(false);
        retry.setOnClickListener(view -> browser.reload());
        panel.addView(retry);
        Button change = new Button(this);
        change.setText("Serveradresse ändern");
        change.setAllCaps(false);
        change.setOnClickListener(view -> editSettings());
        panel.addView(change);
        return panel;
    }

    private void showOffline() {
        browser.setVisibility(View.GONE);
        offline.setVisibility(View.VISIBLE);
    }

    private boolean sameServer(Uri target) {
        if (server == null || target.getHost() == null) return false;
        int leftPort = server.getPort() == -1 ? ("https".equals(server.getScheme()) ? 443 : 80) : server.getPort();
        int rightPort = target.getPort() == -1 ? ("https".equals(target.getScheme()) ? 443 : 80) : target.getPort();
        return server.getScheme().equalsIgnoreCase(target.getScheme())
            && server.getHost().equalsIgnoreCase(target.getHost()) && leftPort == rightPort;
    }

    private void openServer(String address) {
        Uri candidate = Uri.parse(address.trim());
        if (!("https".equalsIgnoreCase(candidate.getScheme()) || "http".equalsIgnoreCase(candidate.getScheme()))
            || candidate.getHost() == null || candidate.getUserInfo() != null
            || (candidate.getPath() != null && !candidate.getPath().isEmpty() && !candidate.getPath().equals("/"))
            || candidate.getQuery() != null || candidate.getFragment() != null) {
            Toast.makeText(this, "Bitte eine vollständige Serveradresse eingeben.", Toast.LENGTH_LONG).show();
            editSettings();
            return;
        }
        server = candidate;
        getSharedPreferences(PREFERENCES, MODE_PRIVATE).edit()
            .putString(SERVER_KEY, candidate.toString()).apply();
        browser.loadUrl(candidate.toString());
    }

    private void migratePreferences() {
        String saved = getSharedPreferences(PREFERENCES, MODE_PRIVATE).getString(SERVER_KEY, "");
        if (!saved.isEmpty()) return;
        String old = getPreferences(MODE_PRIVATE).getString(SERVER_KEY, "");
        if (!old.isEmpty()) {
            getSharedPreferences(PREFERENCES, MODE_PRIVATE).edit().putString(SERVER_KEY, old).apply();
        }
    }

    private void editSettings() {
        startActivityForResult(new Intent(this, SettingsActivity.class), EDIT_SETTINGS);
    }

    @Override protected void onActivityResult(int code, int result, Intent data) {
        super.onActivityResult(code, result, data);
        if (code == CHOOSE_FILE && fileCallback != null) {
            fileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result, data));
            fileCallback = null;
        } else if (code == CAPTURE_PHOTO && fileCallback != null) {
            fileCallback.onReceiveValue(result == RESULT_OK ? new Uri[]{CAMERA_URI} : null);
            fileCallback = null;
        } else if (code == EDIT_SETTINGS) {
            String saved = getSharedPreferences(PREFERENCES, MODE_PRIVATE).getString(SERVER_KEY, "");
            if (!saved.isEmpty()) openServer(saved);
            else if (server == null) finish();
        }
    }

    @Override public void onBackPressed() {
        if (browser.canGoBack()) browser.goBack(); else super.onBackPressed();
    }
}
